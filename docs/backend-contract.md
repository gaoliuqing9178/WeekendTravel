# Backend Contract

后端位于 `backend/`，技术栈为 Java 17 + Spring Boot 3 + Maven + SseEmitter + 本地 JSON Mock 数据。默认端口为 `8000`。

## B1 职责

- Spring Boot 应用骨架。
- CORS：允许 `http://localhost:5173`。
- Controller：
  - `GET /health`
  - `POST /api/plan`
  - `GET /api/plan/{planId}/stream`
  - `POST /api/plan/{planId}/execute`
  - `POST /api/plan/{planId}/clarify`
  - `PATCH /api/plan/{planId}/adjust`
- Agent 状态机和 Planner。
- SSE emitter 封装、心跳、事件顺序。
- 意图抽取 prompt 和话术 prompt。
- LLM 调用边界和超时兜底。

## B2 职责

- 本地 POI JSON 数据集：先不少于 30 条，最终不少于 50 条。
- MockApiService。
- ScenarioFlags 和异常注入。
- Tools：
  - SearchTool / searchLocalPlaces
  - RouteTool / calculateRouteTime
  - AvailabilityTool / checkAvailability
  - BookingTool / bookOrOrder
  - MessageTool / composeShareMessage
- `POST /api/debug/scenario` 的 flags 数据支撑。
- 下单、预约、配送、消息生成的 Mock 执行结果。

## 状态机

后端状态机至少包含：

```text
START
INTENT
CLARIFY
SKELETON
RECALL
VALIDATE
REPLAN
PACK
CONFIRM
ADJUST
EXECUTE
DEGRADE
DONE
FAILED
```

核心转移：

```text
START -> INTENT
INTENT -> SKELETON | CLARIFY
CLARIFY -> INTENT | SKELETON
SKELETON -> RECALL
RECALL -> VALIDATE
VALIDATE -> PACK | REPLAN | DEGRADE
REPLAN -> RECALL
PACK -> CONFIRM
CONFIRM -> ADJUST | EXECUTE
ADJUST -> VALIDATE
EXECUTE -> DONE
任意状态 -> FAILED
```

约束：

- `CLARIFY` 每次只问一个最关键问题，最多 2 次。
- `ADJUST` 只重规划受影响槽位，最多 3 次。
- `REPLAN` 不超过规则上限，失败后进入 `DEGRADE`。
- 每次状态变化必须推送 `state_change`。

## Tool 和 Mock API

- Tool 入参和返回值使用 Java record 或 POJO，由 Jackson 序列化为 camelCase。
- 每个 Tool 必须记录 `latencyMs`。
- Tool 必须可以被 MockApiService 驱动，不依赖真实外部服务。
- Mock API 覆盖 POI、路线、余位、订座、取号、配送和消息能力；真实外部服务接入时逐项替换 Mock service。
- 候选排序初始公式：`score = 0.4 * 相关性 + 0.3 * 距离 + 0.2 * 评分 + 0.1 * 余位健康度`。
- `bookOrOrder` 必须携带 `idempotencyKey`，避免重复模拟下单。
- `bookOrOrder` 支持动作类型：`buy_ticket`、`reserve_table`、`take_number`、`schedule_delivery`、`add_note`；反向操作支持 `cancel_booking`。
- 异常注入至少覆盖：
  - `restaurantFull`
  - `routeTooFar`
  - `bookingFail`
  - `ageMismatch`
- `POST /api/debug/scenario` 更新 flags 后无需重启服务。

## LLM 使用边界

允许：

- 意图抽取。
- 分享消息或执行话术生成。

不允许：

- 把规划决策交给 LLM。
- 让 LLM 直接选择最终 POI。
- 让 LLM 决定状态转移。
- 把 OpenAI key 写进配置文件或源码。
- 在未核验官方 OpenAI developer docs / MCP 前锁 SDK 版本或模型名。

实现前必须追加 `docs/decision-log.md`，记录 SDK、模型、调用方式、超时和兜底策略。

## 约束分级和异常处理

约束优先级：

- P0 硬约束：安全、年龄适配、时间可行、人数可容纳。
- P1 软约束：距离近、可订位、饮食偏好、排队可接受。
- P2 加分项：评分高、网红属性、拍照氛围、座位偏好。

重排终止条件：

- 单个 plan 最多 3 次 replan。
- 累计 Tool 调用不超过 30 次。
- 规划超过 25 秒强制进入 `DEGRADE`。

典型异常处理：

- 餐厅满座或排队超过 45 分钟：锁定活动地点，以活动为中心重新召回餐厅。
- 路线过远：优先换餐厅，其次换活动，最后缩短附加活动。
- 人群或年龄不匹配：直接过滤候选；候选不足时扩大半径或放松 P2。
- 约束冲突无解：保 P0/P1，放松 P2，并透明说明取舍。
- 下单失败：同类供应商重试 1 次；仍失败标记为 `manual`，不阻塞其他动作。

## 后端独立验证

后续 scaffold 后，测试阶段必须由独立 evaluator 子代理运行 `.\verify.ps1 -Target backend -Mode fast`，至少应覆盖：

- Maven 项目存在。
- `mvn test` 可运行。
- `GET /health` 可被 API 测试覆盖。

Sprint 1 后端最低可用标准：

- `GET /health` 返回 200。
- `POST /api/plan` 返回 `planId` 和 `status`。
- `GET /api/plan/{planId}/stream` 能推送 SSE 心跳和至少一个 mock `state_change`。
- `POST /api/debug/scenario` 能更新异常开关。

Generator 可以在开发过程中先运行同类命令排除明显错误，但后端任务能否标记 `verified`，只看 evaluator 子代理的验证证据。

## 后端不做的事

- 不依赖前端源码。
- 不擅自改 API 字段。
- 不删减 SSE 事件来让测试变简单。
- 不接入真实支付、真实预约、真实配送。
- 不在初始化阶段伪装完整业务链路已经实现。
