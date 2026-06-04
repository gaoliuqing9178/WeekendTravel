# 本地短时活动规划与执行 Agent 设计文档

## 1. 项目目标与成功指标

构建一个本地生活短时活动规划与执行 Agent：用户输入一句自然语言目标（例："今天下午想和老婆孩子出去玩几个小时，别离家太远"），系统自动完成意图理解 → 时间骨架 → 多路召回 → 可行性校验 → 异常重排 → 执行打包，并输出可一键确认的完整方案与可转发消息。

核心不是"推荐去哪玩"，而是把模糊需求推进到**可执行状态**。本次 Demo 使用 Mock API 模拟美团 POI、路线、余位、订座、取号、配送和消息能力，真实接入时可逐项替换。

**成功指标 (SLO)**：

- 端到端规划耗时 P95 ≤ 30 秒
- 方案可行率 ≥ 90%（用户确认前不被异常打断）
- 意图抽取准确率 ≥ 85%（基于 20 条 golden case 评估）
- Plan B 触发率 ≤ 30%（超过说明 Plan A 召回质量不达标）

## 2. Planning 策略

采用"**确定性状态机 + 大模型辅助理解**"架构。状态机控制流程，保证 Demo 稳定；LLM 只负责自然语言理解、约束归纳和最终话术生成，不端到端生成方案。

### 2.1 状态机

```
[START] → INTENT → SKELETON → RECALL → VALIDATE ──ok──→ PACK → CONFIRM → EXECUTE → [DONE]
                                  ↑          │
                                  └──replan──┘ (最大 3 次)
                                             │
                                          fail 3 次
                                             ↓
                                         [DEGRADE]
```

- 每次 `VALIDATE` 失败触发 `RECALL` 重排，最多 3 次。
- 3 次仍失败进入 `DEGRADE`：放松 P2 约束、扩大半径，或如实告知用户"今天该场景无可行方案"。
- 状态机全程产生结构化执行日志，前端右栏实时展示。

### 2.2 六步流程与 LLM/规则边界

| 步骤 | 实现方式 | 说明 |
|------|---------|------|
| 1. 意图抽取 | **LLM + Schema 校验** | LLM 输出 JSON，置信度 < 0.7 时反问澄清而非瞎猜 |
| 2. 时间骨架 | **规则** | 按场景模板生成（家庭 / 朋友 / 情侣） |
| 3. 多路召回 | **规则 (Mock API)** | 并行调用 POI 搜索 |
| 4. 候选排序 | **加权公式** | `score = 0.4*相关性 + 0.3*距离 + 0.2*评分 + 0.1*余位健康度` |
| 5. 可行性校验 | **规则** | 路线、年龄、饮食、排队、时长硬约束 |
| 6. 话术生成 | **LLM** | 基于结构化 Plan 生成自然语言转发消息 |

意图抽取的典型输出结构：

```json
{
  "scenario": "family",
  "time_window": "周六下午",
  "duration_hours": [4, 6],
  "people_count": 3,
  "distance_limit_minutes": 30,
  "constraints": ["孩子5岁", "妻子减脂", "别离家太远"],
  "confidence": 0.92
}
```

时间骨架默认模板：
- **家庭**：出发 → 亲子活动 → 晚餐 → 商场散步/甜品 → 回家
- **朋友**：集合 → 展览/citywalk/密室 → 小吃/拍照 → 聚餐 → 可选二场

## 3. 工具调用链路

所有 Tool 使用 Mock API，返回结构化 JSON，便于日志展示与异常注入。

| Tool | 输入 | 输出 | 调用阶段 |
|------|------|------|---------|
| `extract_user_intent(text)` | 自然语言 | 意图 JSON + confidence | INTENT |
| `search_local_places(origin, radius_km, category, tags)` | 中心点+类别+标签 | POI 列表 | RECALL |
| `calculate_route_time(from_poi, to_poi, mode)` | 两点 + 出行方式 | 耗时/距离 | VALIDATE |
| `check_availability(poi_id, time_slot, people_count)` | POI+时段+人数 | 余位/排队/可订 | VALIDATE |
| `book_or_order(action_type, target_id, details, idempotency_key)` | 动作类型+目标+幂等键 | 确认号/失败原因 | EXECUTE |
| `compose_share_message(plan, recipient_type)` | 方案+收件人类型 | 短消息文本 | PACK |

`book_or_order` 的 `action_type` 支持：`buy_ticket` / `reserve_table` / `take_number` / `schedule_delivery` / `add_note`。必须携带 `idempotency_key` 防止重复下单，并支持 `cancel_booking` 反向操作。

**典型调用链**（家庭场景）：

```
extract_user_intent
  → search_local_places(activity, ["适合5岁","室内"])
  → search_local_places(restaurant, ["低卡","儿童椅","可订座"])
  → calculate_route_time × N        # 校验距离
  → check_availability × N          # 校验余位
  → [若失败] replan → search_local_places (新中心点)
  → compose_share_message
  → book_or_order × 多个动作 (用户确认后)
```

## 4. 异常处理机制

约束分三级，优先保 P0/P1，可让 P2：
- **P0 硬约束**：安全、年龄适配、时间可行、人数可容纳
- **P1 软约束**：距离近、可订位、饮食偏好、排队可接受
- **P2 加分项**：评分高、网红属性、拍照氛围、座位偏好

**重排终止条件（核心兜底）**：单次 replan 不超过 3 次；累计调用 Tool 不超过 30 次；超时 25 秒强制进入 DEGRADE。

| 异常 | 触发条件 | 处理策略 |
|------|---------|---------|
| 餐厅满座 / 排队过长 | `available=false` 或 `wait_minutes>45` | 锁定活动地点，以活动为中心重新召回餐厅；保留人数、饮食、儿童设施约束 |
| 路线过远 | 单段 > 35 分钟 或 总路程压缩游玩 < 2 小时 | 优先换餐厅 → 其次换活动 → 最后缩短附加活动 |
| 人群/年龄不匹配 | 亲子活动不适配 5 岁、朋友活动不适配 4 人 | 直接过滤候选；候选不足则扩大半径或降级 P2 |
| 约束冲突无解 | 多约束同时无法满足 | 保 P0/P1，让 P2；输出透明说明："原本选 A，因排队过长改为 B" |
| 下单失败 | `book_or_order` 返回 fail | 同类供应商重试 1 次；仍失败标记为"需用户确认人工处理"，不阻塞其他动作 |
| DEGRADE 兜底 | replan 3 次仍失败 | 如实告知用户场景受限，给出 1-2 个降级建议（缩短时长 / 换日期 / 放宽距离） |

## 5. Demo 展示方案

**Web UI 双栏布局**：
- 左栏：用户输入框 → 最终方案卡片 → 一键确认按钮 → 可转发消息预览
- 右栏：状态机当前状态 → Tool 调用日志 → Plan A/B 切换记录 → 执行包状态

**两个固定演示输入**：

```text
今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。
```

```text
今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。
```

**期望输出示例**：

```text
搞定了，下午2点出发。先去万象城的奇妙亲子乐园，离家约15分钟，适合5岁孩子，室内不累。
玩到5点后去楼上的清禾家庭餐厅，已锁定17:30的4人桌，并备注儿童椅。
原本评分更高的青禾花园排队预计70分钟，自动换成了可订位的备选。
吃完商场散步，7点多回家。

待确认动作：
1. 锁定亲子乐园 2大1小票
2. 预订17:30餐厅4人桌
3. 备注儿童椅 + 少油少盐
4. 生成并发送给老婆的行程消息
```

## 6. 交付边界

1. 可运行 Demo（推荐 Web UI）
2. 完整 Tool 实现代码（含 Mock API 与异常注入开关，便于演示 Plan B）
3. 本设计文档
4. 20 条 golden case 测试集（覆盖家庭/朋友 × 满座/路远/约束冲突场景）

不涉及真实支付与下单：所有 `book_or_order` 为 Mock 执行，用户确认后返回模拟确认号。保留用户确认边界，对齐真实业务的人机协作闭环。
