# 本地短时活动规划与执行 Agent — 完整产品文档

> 版本：v1.1 | 日期：2026-05-21 | 作者：[团队名] | 截止提交：2026-06-07（剩余 17 天）
>
> **文档用途**：团队内部施工图，供后端 B1/B2 + 前端 F1 拆任务、对接口、验收。设计文档（≤2页）另见 `WeekendTravel.md`。

---

## 目录

1. [项目总览](#1-项目总览)
2. [系统架构](#2-系统架构)
3. [技术选型](#3-技术选型)
4. [团队职责拆分](#4-团队职责拆分)
5. [后端规范（B1/B2）](#5-后端规范)
6. [前端规范（F1）](#6-前端规范)
7. [API 接口契约](#7-api-接口契约)
8. [数据模型](#8-数据模型)
9. [Mock 数据集规范](#9-mock-数据集规范)
10. [里程碑与 Sprint 计划](#10-里程碑与-sprint-计划)
11. [验收标准 & Golden Case](#11-验收标准--golden-case)
12. [开发规范](#12-开发规范)

---

## 1. 项目总览

### 1.1 一句话定义

用户发一句自然语言 → Agent 自动规划 + 校验 + 异常重排 → 输出可一键确认的完整方案 → 模拟执行所有下单/预约/配送动作。

### 1.2 Demo 场景

**家庭场景**：
> 今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。

**朋友场景**：
> 今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。

### 1.3 核心价值主张

| 对比维度 | 普通推荐 | 本产品 |
|---------|---------|--------|
| 输入 | 多字段表单 | 一句自然语言 |
| 输出 | 信息列表 | 可执行方案 + 一键确认 |
| 异常处理 | 提示用户自己换 | 自动 Plan B |
| 最终动作 | 跳转各 App | 模拟一键完成 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 Web UI (F1)                          │
│  ┌──────────────────┐        ┌──────────────────────────────┐  │
│  │   左栏：输入+方案  │        │    右栏：实时执行日志面板      │  │
│  │  - 自然语言输入框  │        │  - 状态机当前状态             │  │
│  │  - 方案卡片展示    │        │  - Tool 调用记录（流式）       │  │
│  │  - 一键确认按钮    │        │  - Plan A/B 切换标记          │  │
│  │  - 转发消息预览    │        │  - 执行包状态（待确认/完成）   │  │
│  └────────┬─────────┘        └──────────────┬───────────────┘  │
│           │ REST API                         │ SSE 流式推送      │
└───────────┼──────────────────────────────────┼─────────────────┘
            │                                  │
┌───────────▼──────────────────────────────────▼─────────────────┐
│                   后端 Spring Boot Server (B1)                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Agent 核心（状态机）                      │ │
│  │  START → INTENT → SKELETON → RECALL → VALIDATE            │ │
│  │       → REPLAN(≤3) → PACK → CONFIRM → EXECUTE → DONE     │ │
│  │                              ↓(失败3次)                     │ │
│  │                          DEGRADE                           │ │
│  └───────────────────┬────────────────────────────────────────┘ │
│                      │ 调用                                      │
│  ┌───────────────────▼────────────────────────────────────────┐ │
│  │                   Tool Layer (B2)                           │ │
│  │  extract_intent │ search_places │ calc_route               │ │
│  │  check_avail    │ book_or_order │ compose_message          │ │
│  └───────────────────┬────────────────────────────────────────┘ │
│                      │ 调用                                      │
│  ┌───────────────────▼────────────────────────────────────────┐ │
│  │                 Mock API Layer (B2)                         │ │
│  │  POI 数据集 │ 路线模拟 │ 余位模拟 │ 订单模拟 │ LLM 调用     │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流（请求生命周期）

```
1. 用户输入自然语言
2. POST /api/plan  → 返回 plan_id
3. 前端用 plan_id 建立 SSE 连接 GET /api/plan/{id}/stream
4. 后端 Agent 运行，每步骤通过 SSE 推送 LogEvent
5. Agent 完成 → SSE 推送 {type: "plan_ready", payload: Plan}
6. 用户确认 → POST /api/plan/{id}/execute
7. 后端执行 Mock 下单，SSE 推送各动作结果
8. 全部完成 → SSE 推送 {type: "done"}
```

---

## 3. 技术选型

### 3.1 确定技术栈

| 层 | 技术 | 理由 |
|---|------|------|
| 前端框架 | **Vue 3 + Vite** | Composition API + `<script setup>`，热更新快 |
| 前端 UI | **Tailwind CSS + Naive UI** | Vue 生态组件库，开箱即用，hackathon 效率高 |
| 实时通信 | **SSE (EventSource)** | 单向流推送，比 WebSocket 更轻，足够本场景 |
| 状态管理 | **Pinia** | Vue 官方推荐，比 Vuex 简洁，TypeScript 友好 |
| 后端框架 | **Java 17 + Spring Boot 3** | 团队熟悉，SseEmitter 原生支持 SSE 推送 |
| LLM 调用 | **OpenAI GPT-5.5（Codex）** | 意图抽取 + 话术生成，用 openai-java SDK |
| Mock 数据 | **本地 JSON 文件** | 不依赖外部服务，稳定可控，随时注入异常 |
| 包管理 | **pnpm（前端）/ Maven（后端）** | 前端速度快；后端团队熟悉 Maven |
| 版本控制 | **Git + GitHub** | 主分支保护，PR review |

> ⚠️ **技术栈已冻结**：后续不再变更框架，有问题内部消化，不影响联调节点。

### 3.2 目录结构

```
weekend-travel/
├── backend/                              # B1/B2 负责
│   ├── src/main/java/com/weekendtravel/
│   │   ├── WeekendTravelApplication.java # Spring Boot 入口
│   │   ├── controller/
│   │   │   ├── PlanController.java       # /api/plan 路由
│   │   │   └── DebugController.java      # /api/debug/scenario
│   │   ├── agent/
│   │   │   ├── AgentStateMachine.java    # 状态机核心（B1）
│   │   │   ├── Planner.java             # Planning 策略（B1）
│   │   │   └── SseEventEmitter.java     # SSE 推送封装（B1）
│   │   ├── tools/
│   │   │   ├── IntentTool.java          # extract_user_intent（B1）
│   │   │   ├── SearchTool.java          # search_local_places（B2）
│   │   │   ├── RouteTool.java           # calculate_route_time（B2）
│   │   │   ├── AvailabilityTool.java    # check_availability（B2）
│   │   │   ├── BookingTool.java         # book_or_order（B2）
│   │   │   └── MessageTool.java         # compose_share_message（B1）
│   │   ├── mock/
│   │   │   ├── MockApiService.java      # Mock 返回逻辑（B2）
│   │   │   └── ScenarioFlags.java       # 异常注入开关（B2）
│   │   └── model/
│   │       ├── Plan.java                # 规划结果
│   │       ├── ActionItem.java
│   │       ├── LogEvent.java
│   │       └── IntentResult.java
│   ├── src/main/resources/
│   │   ├── application.yml              # 配置（端口/CORS/OpenAI key）
│   │   └── mock/poi_data.json           # Mock POI 数据集（≥50条）
│   └── pom.xml
├── frontend/                             # F1（你）负责
│   ├── src/
│   │   ├── App.vue
│   │   ├── components/
│   │   │   ├── InputPanel.vue           # 左栏：输入框 + 场景选择
│   │   │   ├── PlanCard.vue             # 左栏：方案卡片
│   │   │   ├── ConfirmButton.vue        # 左栏：一键确认
│   │   │   ├── ShareMessage.vue         # 左栏：转发消息预览
│   │   │   ├── LogPanel.vue             # 右栏：日志面板容器
│   │   │   ├── StateBadge.vue           # 右栏：状态机当前状态
│   │   │   ├── ToolCallCard.vue         # 右栏：单条工具调用日志
│   │   │   └── ExecutionTracker.vue     # 右栏：执行包动作状态
│   │   ├── stores/
│   │   │   └── planStore.ts             # Pinia store
│   │   ├── composables/
│   │   │   └── useSSE.ts                # SSE 连接 composable
│   │   └── api/
│   │       └── client.ts                # axios 封装
│   ├── index.html
│   └── package.json
└── docs/
    ├── WeekendTravel.md                  # 提交文档（≤2页）
    └── WeekendTravel_ProductDoc.md       # 本文档
```

---

## 4. 团队职责拆分

### 4.1 总览

| 模块 | Owner | 说明 |
|------|-------|------|
| Agent 状态机 + Planner | **B1** | 状态转移、重排策略、SSE 推送 |
| Tool 实现 + Mock API | **B2** | 6 个 Tool + JSON 数据集 + 异常注入 |
| Spring Boot 路由 + 集成 | **B1** | `/plan`、`/execute` 接口、CORS 配置 |
| 前端 UI + SSE 接入 | **F1** | 所有前端组件、实时日志、交互流程 |
| LLM 提示词 | **B1 + F1 共同** | 意图抽取 prompt + 话术 prompt |
| Mock 数据集 | **B2** | ≥50 条 POI，覆盖 5 类场景 |
| Golden Case 测试 | **全员** | 每人负责 7 条，共 21 条 |
| Demo 录制 | **F1** | 60s 演示视频，重点演示 Plan B |

### 4.2 接口边界

**B1/B2 交付给 F1 的内容**（Day 6 前必须到位）：

1. `POST /api/plan` 可调通，返回 `plan_id`
2. `GET /api/plan/{id}/stream` SSE 可连接，推送格式符合 §7.3
3. `POST /api/plan/{id}/execute` 可调通
4. 本地 `http://localhost:8000` 启动无报错，CORS 已开放 `localhost:5173`

**F1 交付给 B1/B2 的内容**（Day 6 前）：

1. 明确告知 SSE LogEvent 里哪些字段前端会渲染（方便后端确认不漏字段）
2. Plan 数据结构如有显示需求变化，提前告知（避免后端改 schema 影响前端）

---

## 5. 后端规范

### 5.1 Agent 状态机（B1 负责）

```python
class AgentState(Enum):
    START     = "start"
    INTENT    = "intent"
    CLARIFY   = "clarify"   # 新增：等待用户回答关键问题，暂停
    SKELETON  = "skeleton"
    RECALL    = "recall"
    VALIDATE  = "validate"
    REPLAN    = "replan"
    PACK      = "pack"
    CONFIRM   = "confirm"   # 等待用户确认完整方案，暂停
    ADJUST    = "adjust"    # 新增：用户对方案提出局部修改，局部重规划
    EXECUTE   = "execute"
    DEGRADE   = "degrade"
    DONE      = "done"
    FAILED    = "failed"
```

**转移规则**：

```
START → INTENT
INTENT → SKELETON（confidence ≥ 0.7）
INTENT → CLARIFY（confidence < 0.7，推送一条最关键的反问，最多触发 2 次）
CLARIFY → INTENT（用户回复后补充信息重新抽取）
CLARIFY → SKELETON（第 2 次反问后仍不确定，用默认值兜底强制推进，不再等待）
SKELETON → RECALL
RECALL → VALIDATE
VALIDATE → PACK（全通过）| REPLAN（有失败项，replan_count < 3）
REPLAN → RECALL（换参数重召回）
VALIDATE → DEGRADE（replan_count >= 3）
PACK → CONFIRM（等待前端用户点击确认或提出修改）
CONFIRM → ADJUST（用户输入自然语言修改指令，如"换一家餐厅"）
ADJUST → VALIDATE（只对受影响槽位局部重新校验，不推倒全部）
VALIDATE → PACK（局部通过，更新方案回到 CONFIRM）
CONFIRM → EXECUTE（用户点击确认执行）
EXECUTE → DONE（全部动作完成）
任意状态 → FAILED（超时 > 25s 或 Tool 调用 > 30 次）
```

**反问原则（CLARIFY 阶段，B1 严格遵守）**：
- **只问一个问题**：从所有缺失字段中选最关键的一个，不允许出"①②③"列表式提问
- **优先级**：时长 > 出发地 > 人群细节（其余字段用合理默认值推断）
- **附带快捷选项**：问题必须同时提供 2-3 个选项按钮，用户可点选也可文字回复
- **2 次上限**：第 2 次反问后无论如何都推进，用默认值填充剩余不确定字段

**方案微调原则（ADJUST 阶段，B1 严格遵守）**：
- 解析用户修改指令，识别受影响槽位（活动 / 餐厅 / 时间）
- **只重规划受影响槽位**，其他槽位保持不变，避免全盘推倒
- 微调结果通过 SSE `adjust_result` 事件推送，前端局部更新对应卡片

**SSE 推送时机**：每次状态转移、每次 Tool 调用开始/结束、Plan B 切换、反问推出、微调完成时必须推送。

### 5.2 Tool 实现规范（B2 负责）

每个 Tool 必须：
1. 以 Java record 或 POJO 作为入参和返回值，用 Jackson 序列化
2. 支持 `MockApiService` 注入，全程走 Mock；真实接入时替换 Service 实现即可
3. 有独立 JUnit 单测，覆盖正常路径 + 至少 2 个异常路径
4. 执行时间记录在返回值的 `latencyMs` 字段（`System.currentTimeMillis()` 差值）

**典型 Tool 接口签名**：
```java
// 示例：SearchTool.java
@Service
public class SearchTool {
    public SearchResult execute(SearchRequest req) {
        long start = System.currentTimeMillis();
        List<Poi> pois = mockApiService.searchPlaces(req);
        return new SearchResult(pois, System.currentTimeMillis() - start);
    }
}
```

**异常注入开关**（Demo 用，B2 实现）：
```java
// mock/ScenarioFlags.java
@Component
public class ScenarioFlags {
    public boolean restaurantFull = false;   // 触发餐厅满座
    public boolean routeTooFar   = false;    // 触发路线过远
    public boolean bookingFail   = false;    // 触发下单失败
    public boolean ageMismatch   = false;    // 触发年龄不匹配
}
```

`DebugController` 暴露 `POST /api/debug/scenario` 接收 JSON 更新 flags，无需重启服务。Demo 演示 Plan B 时直接从前端或 curl 触发。

### 5.3 LLM 调用规范（B1 负责）

LLM 只用于两处，其余全用规则。使用 **openai-java SDK**，模型 `codex-gpt5.5`。

**Maven 依赖**：
```xml
<dependency>
    <groupId>com.openai</groupId>
    <artifactId>openai-java</artifactId>
    <version>0.8.0</version>
</dependency>
```

**API Key 配置**（`application.yml`，不提交 Git）：
```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  model: codex-gpt5.5
```

**①意图抽取**（`IntentTool.java`）：
- System prompt：要求严格输出 JSON，字段含 `confidence`，不输出多余文字
- 温度：`0.1`（确定性优先）
- `max_tokens: 300`
- 如 `confidence < 0.7`：返回含 `needs_clarification: true` + `clarification_question` 的 JSON，状态机回到 INTENT 等待反问

**②话术生成**（`MessageTool.java`）：
- System prompt：要求自然口语、简洁、包含出发时间/地点顺序/已完成动作/Plan B 原因说明
- 温度：`0.7`（自然表达）
- `max_tokens: 200`

**通用要求**：
- 两个 Tool 都要记录 `latency_ms`，通过 SSE 的 `tool_result` 事件推送到前端
- 超时设置：10 秒，超时走兜底（意图抽取返回低置信度，话术返回固定模板）
- 不要在 LLM 调用里做规划决策——规划全走状态机规则，LLM 只做 NLU 和 NLG

---

## 6. 前端规范

### 6.1 页面布局

```
┌─────────────────────────────────────────────────────────────────┐
│  Header: WeekendTravel ·  [场景选择: 家庭 / 朋友]                │
├────────────────────────┬────────────────────────────────────────┤
│       左栏 (40%)        │            右栏 (60%)                  │
│                        │                                        │
│  ┌──────────────────┐  │  ┌──────────────────────────────────┐  │
│  │ 输入框 + 发送按钮  │  │  │ 状态机当前状态 Badge             │  │
│  └──────────────────┘  │  │  START → INTENT → ... → DONE    │  │
│                        │  └──────────────────────────────────┘  │
│  ── 规划中时显示 ──     │                                        │
│  ┌──────────────────┐  │  ┌──────────────────────────────────┐  │
│  │ 加载动画 + 状态文字│  │  │ Tool 调用日志（流式，自动滚动）   │  │
│  └──────────────────┘  │  │  每条：图标+Tool名+耗时+结果摘要  │  │
│                        │  │  Plan B 切换时高亮显示             │  │
│  ── 方案完成后显示 ──   │  └──────────────────────────────────┘  │
│  ┌──────────────────┐  │                                        │
│  │ 方案卡片          │  │  ┌──────────────────────────────────┐  │
│  │ · 活动信息        │  │  │ 执行包状态列表                   │  │
│  │ · 餐厅信息        │  │  │ ○ 锁定门票 2大1小    [待确认]    │  │
│  │ · 路线摘要        │  │  │ ○ 预订17:30 4人桌   [待确认]    │  │
│  └──────────────────┘  │  │ ○ 备注儿童椅        [待确认]    │  │
│                        │  │ ○ 生成转发消息       [待确认]    │  │
│  ┌──────────────────┐  │  └──────────────────────────────────┘  │
│  │ 一键确认按钮      │  │                                        │
│  └──────────────────┘  │                                        │
│                        │                                        │
│  ┌──────────────────┐  │                                        │
│  │ 转发消息预览+复制  │  │                                        │
│  └──────────────────┘  │                                        │
└────────────────────────┴────────────────────────────────────────┘
```

### 6.2 组件清单与职责

| 组件 | 文件 | 职责 | Props / Emits |
|------|------|------|--------------|
| `InputPanel` | `InputPanel.vue` | 文本输入 + 场景选择 + 发送 | emit: `submit(text, scenario)` |
| `ClarifyBubble` | `ClarifyBubble.vue` | **新增**：Agent 反问气泡，含快捷选项按钮 | prop: `question, options[]`；emit: `reply(text)` |
| `PlanCard` | `PlanCard.vue` | 展示活动/餐厅/路线摘要 + Plan B 标记 | prop: `plan: Plan` |
| `AdjustPanel` | `AdjustPanel.vue` | **新增**：方案微调输入框，出现在确认阶段 | emit: `adjust(instruction: string)` |
| `ConfirmButton` | `ConfirmButton.vue` | 一键确认，disabled/loading 状态管理 | prop: `disabled`, emit: `confirm` |
| `ShareMessage` | `ShareMessage.vue` | 转发消息预览 + 一键复制到剪贴板 | prop: `message: string` |
| `LogPanel` | `LogPanel.vue` | 日志容器，自动滚动到底部 | prop: `events: LogEvent[]` |
| `StateBadge` | `StateBadge.vue` | 状态机当前状态，带颜色标注 | prop: `state: AgentState` |
| `ToolCallCard` | `ToolCallCard.vue` | 单条工具调用日志（图标+名称+耗时+摘要）| prop: `event: ToolCallEvent` |
| `ExecutionTracker` | `ExecutionTracker.vue` | 执行包动作列表 + 各动作状态 | prop: `actions: ActionItem[]` |

### 6.3 Pinia Store 结构

```typescript
// stores/planStore.ts
import { defineStore } from 'pinia'

export const usePlanStore = defineStore('plan', {
  state: () => ({
    planId: null as string | null,
    agentState: 'start' as AgentState,
    plan: null as Plan | null,
    logEvents: [] as LogEvent[],
    actions: [] as ActionItem[],
    shareMessage: '',
    isExecuting: false,
    // 反问相关
    pendingClarification: null as ClarificationRequest | null,
    // 微调相关
    adjustCount: 0,
    isAdjusting: false,
  }),
  actions: {
    async submitRequest(text: string, scenario: 'family' | 'friends') {
      // POST /api/plan → 拿到 planId → 触发 SSE 连接
    },
    async replyClarification(reply: string) {
      // POST /api/plan/{planId}/clarify
      // 成功后 pendingClarification = null
    },
    async adjustPlan(instruction: string) {
      // PATCH /api/plan/{planId}/adjust
      // adjustCount++，isAdjusting = true
    },
    async confirmPlan() {
      // POST /api/plan/{planId}/execute
    },
    appendLogEvent(event: LogEvent) {
      this.logEvents.push(event)
      if (event.type === 'clarification_request')
        this.pendingClarification = event as ClarificationRequest
      if (event.type === 'adjust_result')
        this.isAdjusting = false
    },
    reset() {
      // 重置所有状态，关闭 SSE，adjustCount = 0
    },
  },
})
```

### 6.4 SSE Composable

```typescript
// composables/useSSE.ts
import { ref, watch, onUnmounted } from 'vue'
import { usePlanStore } from '@/stores/planStore'

export function useSSE(planId: Ref<string | null>) {
  const store = usePlanStore()
  let es: EventSource | null = null
  let retryCount = 0

  function connect(id: string) {
    es = new EventSource(`/api/plan/${id}/stream`)

    es.addEventListener('state_change',          (e) => { store.agentState = JSON.parse(e.data).to })
    es.addEventListener('tool_call',             (e) => { store.appendLogEvent(JSON.parse(e.data)) })
    es.addEventListener('tool_result',           (e) => { store.appendLogEvent(JSON.parse(e.data)) })
    es.addEventListener('replan',                (e) => { store.appendLogEvent(JSON.parse(e.data)) })
    es.addEventListener('clarification_request', (e) => { store.appendLogEvent(JSON.parse(e.data)) })
    es.addEventListener('adjust_result',         (e) => { store.appendLogEvent(JSON.parse(e.data)) })
    es.addEventListener('plan_ready',            (e) => { store.plan = JSON.parse(e.data).plan })
    es.addEventListener('execute_result',        (e) => { /* 更新对应 action 状态 */ })
    es.addEventListener('done',                  (e) => { es?.close() })
    es.addEventListener('error',                 (e) => { /* 重连，最多3次 */ })
  }

  watch(planId, (id) => { if (id) connect(id) })
  onUnmounted(() => es?.close())
}
```

### 6.5 UI 细节规范

- **颜色**：状态 RUNNING=蓝，CLARIFY=紫，REPLAN=橙，ADJUST=黄，DONE=绿，FAILED/DEGRADE=红
- **Tool 调用图标**：search=🔍，route=🗺，check=✓，book=📋，message=💬，intent=🧠
- **Plan B 切换**：日志中高亮一行 `⚠️ Plan B 触发：[原因]`，方案卡片上显示 `Plan B` 徽标
- **加载中**：左栏显示骨架屏（Skeleton），右栏日志实时滚动，不能空白等待
- **确认按钮**：未完成规划前 disabled；规划中 disabled + loading；规划完成后高亮可点；执行中 loading；执行完成后变为"查看详情"

**反问气泡（ClarifyBubble）交互规范**：
- 进入 CLARIFY 状态时，在左栏输入框下方以气泡形式弹出，样式区别于用户输入（Agent 侧，紫色左对齐）
- 气泡内含问题文本 + 2-3 个快捷按钮（如"3-4小时" / "4-6小时" / "自己输入"）
- 用户点按钮或在输入框补充文字后，气泡消失，状态恢复 INTENT 继续规划
- **绝对不允许**同时弹出多个反问气泡

**方案微调（AdjustPanel）交互规范**：
- 仅在 CONFIRM 状态下显示，位于方案卡片下方、确认按钮上方
- 样式：浅色输入框 + 占位文字"想改什么？例如：换一家餐厅 / 活动提前半小时"
- 用户提交后，进入 ADJUST 状态：输入框 disabled + loading，方案卡片对应槽位出现更新动画
- 微调完成后恢复 CONFIRM 状态，被修改的槽位高亮闪烁 1 次提示变更
- 微调最多允许 3 次（防止用户无限改），第 4 次提示"建议直接确认，或重新发起规划"

---

## 7. API 接口契约

> ⚠️ 本节是 **B1/B2 与 F1 的硬约束**，字段变更需双向确认，不得单方面改动。

### 7.1 POST `/api/plan` — 发起规划

**Request**：
```json
{
  "text": "今天下午是空的，想和老婆孩子出去玩几个小时",
  "scenario": "family",
  "origin": "当前位置"
}
```

**Response** (202 Accepted)：
```json
{
  "plan_id": "plan_abc123",
  "status": "processing"
}
```

**Error** (400)：
```json
{
  "error": "INVALID_INPUT",
  "message": "scenario must be 'family' or 'friends'"
}
```

---

### 7.2 GET `/api/plan/{plan_id}/stream` — SSE 实时日志

Content-Type: `text/event-stream`

每个事件格式：
```
event: {event_type}
data: {JSON}

```

**事件类型清单**：

#### `state_change` — 状态机切换
```json
{
  "type": "state_change",
  "from": "recall",
  "to": "validate",
  "timestamp": 1748793600000
}
```

#### `tool_call` — Tool 调用
```json
{
  "type": "tool_call",
  "tool": "search_local_places",
  "status": "start",
  "input_summary": "搜索附近亲子活动，半径3km",
  "timestamp": 1748793601000
}
```

#### `tool_result` — Tool 返回
```json
{
  "type": "tool_result",
  "tool": "search_local_places",
  "status": "success",
  "output_summary": "找到 5 个候选 POI",
  "latency_ms": 234,
  "timestamp": 1748793601234
}
```

#### `clarification_request` — Agent 反问（新增）
```json
{
  "type": "clarification_request",
  "question": "请问大概想玩几个小时？",
  "field": "duration_hours",
  "options": ["3-4小时", "4-6小时", "6小时以上"],
  "timestamp": 1748793601500
}
```
> 前端收到后渲染 `ClarifyBubble`，用户回复后调用 `POST /api/plan/{id}/clarify`。

#### `adjust_result` — 微调完成（新增）
```json
{
  "type": "adjust_result",
  "affected_slots": ["restaurant"],
  "summary": "已将餐厅换为「林间日式小食」，可订位，排队约5分钟",
  "timestamp": 1748793640000
}
```
> 前端收到后局部更新 `PlanCard` 对应槽位，高亮闪烁提示变更。

#### `replan` — 触发 Plan B
```json
{
  "type": "replan",
  "reason": "餐厅「青禾花园」排队预计 70 分钟，超出阈值",
  "replan_count": 1,
  "timestamp": 1748793603000
}
```

#### `plan_ready` — 规划完成，等待确认
```json
{
  "type": "plan_ready",
  "plan": { /* Plan 对象，见 §8.1 */ }
}
```

#### `execute_result` — 单个执行动作结果
```json
{
  "type": "execute_result",
  "action_id": "act_001",
  "action_type": "reserve_table",
  "status": "success",
  "confirmation_no": "MOCK-TBL-88421",
  "timestamp": 1748793620000
}
```

#### `done` — 全部完成
```json
{
  "type": "done",
  "summary": "3 个动作全部完成，1 个需人工处理",
  "timestamp": 1748793625000
}
```

#### `error` — 异常
```json
{
  "type": "error",
  "code": "DEGRADE",
  "message": "3 次重排后仍无可行方案，建议放宽距离限制或调整时间"
}
```

---

### 7.3 POST `/api/plan/{plan_id}/execute` — 用户确认执行

**Request**：
```json
{
  "confirmed": true
}
```

**Response** (200)：
```json
{
  "status": "executing",
  "message": "开始执行，请关注右侧日志"
}
```

---

### 7.4 POST `/api/plan/{plan_id}/clarify` — 用户回答反问（新增）

> 仅在 Agent 处于 CLARIFY 状态时有效，其他状态返回 409。

**Request**：
```json
{
  "reply": "4-6小时"
}
```

**Response** (200)：
```json
{
  "status": "processing",
  "message": "已收到，继续规划中"
}
```

---

### 7.5 PATCH `/api/plan/{plan_id}/adjust` — 方案局部微调（新增）

> 仅在 Agent 处于 CONFIRM 状态时有效，其他状态返回 409。最多调用 3 次。

**Request**：
```json
{
  "instruction": "换一家餐厅，要能订位的"
}
```

**Response** (202 Accepted)：
```json
{
  "status": "adjusting",
  "message": "正在调整餐厅，请关注右侧日志"
}
```

**Error** (429 — 超出微调次数)：
```json
{
  "error": "ADJUST_LIMIT_EXCEEDED",
  "message": "已达最大微调次数，请直接确认或重新发起规划"
}
```

---

### 7.6 POST `/api/debug/scenario` — 注入异常（仅 Demo 用）

**Request**：
```json
{
  "restaurant_full": true,
  "route_too_far": false,
  "booking_fail": false,
  "age_mismatch": false
}
```

**Response** (200)：
```json
{ "updated": { "restaurant_full": true } }
```

---

## 8. 数据模型

### 8.1 后端数据模型（Java record）

```java
// model/Plan.java
public record Plan(
    String planId,
    String scenario,           // "family" | "friends"
    boolean isPlanB,
    String planBReason,        // nullable
    List<TimeSlot> timeline,
    List<ActionItem> actions,
    String shareMessage,
    double totalDurationHours,
    int replanCount
) {}

// model/TimeSlot.java
public record TimeSlot(
    int order,
    String type,               // "activity" | "restaurant" | "transit" | "dessert"
    Poi poi,
    String startTime,          // "14:00"
    String endTime,            // "17:00"
    List<String> notes
) {}

// model/Poi.java
public record Poi(
    String id,
    String name,
    String category,
    String address,
    double rating,
    int distanceMinutes,       // 从上一个地点出发
    List<String> tags,
    String availabilityStatus, // "available" | "limited" | "full"
    Integer waitMinutes        // nullable
) {}

// model/ActionItem.java
public record ActionItem(
    String id,
    String actionType,         // "buy_ticket"|"reserve_table"|"take_number"|"schedule_delivery"|"add_note"|"send_message"
    String targetPoiId,
    String description,
    String status,             // "pending"|"executing"|"success"|"failed"|"manual"
    String confirmationNo      // nullable
) {}

// model/IntentResult.java
public record IntentResult(
    String scenario,
    String timeWindow,
    int[] durationHours,       // [4, 6]
    String origin,
    int distanceLimitMinutes,
    int peopleCount,
    List<String> constraints,
    double confidence,         // 0-1，< 0.7 触发反问
    boolean needsClarification,
    String clarificationQuestion  // nullable
) {}
```

```java
// model/AdjustRequest.java
public record AdjustRequest(
    String instruction,       // 用户自然语言微调指令
    int adjustCount           // 当前已微调次数（后端校验 ≤ 3）
) {}

// model/ClarifyReply.java
public record ClarifyReply(
    String reply,             // 用户回复内容
    String field              // 对应意图字段（由后端从上下文推断）
) {}
```

### 8.2 前端类型定义（TypeScript）

```typescript
// 与后端 JSON 字段对应（camelCase，Jackson 默认映射）
interface Plan {
  planId: string
  scenario: 'family' | 'friends'
  isPlanB: boolean
  planBReason?: string
  timeline: TimeSlot[]
  actions: ActionItem[]
  shareMessage: string
  totalDurationHours: number
  replanCount: number
}

interface LogEvent {
  type: 'state_change' | 'tool_call' | 'tool_result' | 'replan'
      | 'clarification_request' | 'adjust_result'
      | 'plan_ready' | 'execute_result' | 'done' | 'error'
  timestamp: number
  [key: string]: unknown
}

interface ClarificationRequest {
  type: 'clarification_request'
  question: string
  field: string
  options: string[]
  timestamp: number
}

interface AdjustResult {
  type: 'adjust_result'
  affectedSlots: string[]   // e.g. ["restaurant"]
  summary: string
  timestamp: number
}
```

> ⚠️ **JSON 字段约定**：后端 Java record 字段为 camelCase，Jackson 默认输出 camelCase，前端 TypeScript 直接对应，**不需要做 snake_case 转换**。如后端改字段名必须同步更新此处并通知 F1。

---

## 9. Mock 数据集规范

**B2 负责**，文件：`backend/mock/poi_data.json`

### 9.1 数量要求

| 类别 | 数量 | 覆盖场景 |
|------|------|---------|
| 亲子活动 | 8条 | 含2条年龄不匹配（触发过滤测试）|
| 朋友活动（展览/citywalk/密室） | 8条 | 含2条满额 |
| 餐厅（家庭友好） | 10条 | 含3条满座 + 2条排队>45min |
| 餐厅（朋友聚餐） | 10条 | 含2条满座 |
| 甜品/咖啡 | 6条 | 全部可用 |
| 配送供应商（蛋糕/鲜花） | 4条 | 含1条下单失败 |
| **合计** | **46条+** | |

### 9.2 单条 POI JSON 格式

```json
{
  "id": "poi_001",
  "name": "奇妙亲子乐园",
  "category": "activity",
  "sub_category": "indoor_kids",
  "address": "万象城 L3",
  "lat": 30.2741,
  "lng": 120.1551,
  "rating": 4.6,
  "tags": ["适合5岁", "室内", "不太累", "有儿童设施"],
  "distance_minutes_from_center": 15,
  "price_per_person": 88,
  "age_requirement": {"min": 3, "max": 12},
  "default_availability": {
    "weekday_afternoon": {"available": true, "remaining": 20},
    "weekend_afternoon": {"available": true, "remaining": 8}
  },
  "scenario_flags": {
    "family": true,
    "friends": false
  }
}
```

---

## 10. 里程碑与 Sprint 计划

**总周期**：2026-05-21 → 2026-06-07（18天）
**缓冲**：最后 1 天（6月7日）为提交日，只整理材料不写代码

### Sprint 1：地基（Day 1-5，5月21-25日）

**目标**：API 契约冻结，各端骨架跑通，能 SSE 通信

| 任务 | Owner | 截止 | 验收标准 |
|------|-------|------|---------|
| 搭建 Spring Boot 项目 + 基础路由 | B1 | 5/22 | `GET /health` 返回 200，CORS 已开放 5173 |
| 实现 SSE 推送框架（SseEmitter）| B1 | 5/23 | 客户端能收到心跳事件 |
| Mock POI 数据集（≥30条）| B2 | 5/23 | JSON 格式符合 §9.2，Jackson 能反序列化 |
| 实现 `SearchTool` + `RouteTool` | B2 | 5/24 | JUnit 单测通过，异常路径覆盖 |
| 实现 `AvailabilityTool` + `ScenarioFlags` | B2 | 5/24 | `restaurantFull=true` 返回满座数据 |
| 搭建 Vue 3 + Vite 项目 | F1 | 5/22 | `pnpm dev` 跑通，Pinia + Naive UI 配置完成 |
| 实现 `useSSE` composable + 日志面板骨架 | F1 | 5/24 | 能收 mock SSE 事件并渲染到 LogPanel |
| 实现 InputPanel + axios 调用 | F1 | 5/25 | `POST /api/plan` 能发出并拿到 planId |
| **Sprint 1 集成联调** | 全员 | 5/25 | 输入一句话 → SSE 收到 state_change 事件 |

### Sprint 2：核心逻辑（Day 6-11，5月26-31日）

**目标**：Agent 主链路跑通，Plan B 自动触发，前端展示完整

| 任务 | Owner | 截止 | 验收标准 |
|------|-------|------|---------|
| 实现 Agent 状态机（START→PACK）| B1 | 5/28 | 两个 Demo 输入能输出 Plan |
| 实现意图抽取（LLM + schema）| B1 | 5/28 | confidence 字段正确，0.7阈值生效 |
| 实现 Plan B 触发逻辑（重排≤3次）| B1 | 5/29 | `restaurant_full=true` 时自动换店 |
| 实现 `book_or_order`（含幂等键）| B2 | 5/28 | Mock 下单返回确认号 |
| 实现 `compose_share_message`（LLM）| B2 | 5/29 | 输出自然中文转发消息 |
| 实现异常注入 API + DEGRADE 兜底 | B1+B2 | 5/30 | `POST /api/debug/scenario` 生效 |
| 实现方案卡片 + 一键确认 | F1 | 5/28 | 点击后触发 execute API |
| 实现执行包状态追踪 | F1 | 5/29 | 各动作状态实时更新 |
| 实现转发消息预览 + 复制 | F1 | 5/30 | 一键复制到剪贴板 |
| 实现 Plan B 高亮展示 | F1 | 5/30 | 触发时日志 + 方案卡片均有标记 |
| **Sprint 2 集成联调** | 全员 | 5/31 | 家庭场景完整跑通，Plan B 能演示 |

### Sprint 3：打磨与交付（Day 12-17，6月1-7日）

**目标**：朋友场景跑通，21条 golden case 全过，Demo 视频录制，材料提交

| 任务 | Owner | 截止 | 验收标准 |
|------|-------|------|---------|
| 朋友场景完整测试 + fix | 全员 | 6/2 | 朋友 Demo 输入能输出正确方案 |
| 21条 golden case 回归 | 全员各7条 | 6/3 | 可行率 ≥ 90%（19/21条通过）|
| UI 细节打磨（动画/颜色/响应式）| F1 | 6/4 | 1080p 下无布局错位 |
| 异常场景演示准备（满座/路远）| B1+B2 | 6/4 | 一键开关，Demo 时稳定复现 |
| POI 数据集补齐到 50 条 | B2 | 6/4 | schema 验证全过 |
| 录制 60s 演示视频 | F1 | 6/5 | 包含：正常流 + Plan B + 一键执行 |
| 整理提交材料（设计文档+代码+视频）| F1 | 6/6 | 所有文件命名清晰，可直接提交 |
| **提交截止** | — | **6/7** | — |

---

## 11. 验收标准 & Golden Case

### 11.1 SLO 验收

| 指标 | 目标 | 测量方式 |
|------|------|---------|
| 端到端规划耗时 P95 | ≤ 30 秒 | golden case 运行取 P95 |
| 方案可行率 | ≥ 90% | 21条 case 中通过数 ÷ 21 |
| 意图抽取准确率 | ≥ 85% | 人工对比意图 JSON 字段 |
| Plan B 触发准确率 | 100% | 注入异常时必须触发 |

### 11.2 Golden Case 清单（21条）

**家庭场景（7条）**

| # | 输入 | 预期状态 | Plan B？ |
|---|------|---------|---------|
| F1 | 标准家庭输入（见文档顶部） | DONE | 否 |
| F2 | 同上 + 注入 `restaurant_full` | DONE | 是（换餐厅）|
| F3 | 同上 + 注入 `route_too_far` | DONE | 是（换活动）|
| F4 | 同上 + 注入 `age_mismatch` | DONE | 是（换活动+说明）|
| F5 | 同上 + 注入 `booking_fail` | DONE | 部分动作标为 manual |
| F6 | 输入时长模糊："出去玩一下" | INTENT | 反问确认时长 |
| F7 | 所有约束极端冲突（近+高评分+无排队+低卡全满足）| DEGRADE or DONE | 降级方案+说明 |

**朋友场景（7条）**

| # | 输入 | 预期状态 | Plan B？ |
|---|------|---------|---------|
| P1 | 标准朋友输入（见文档顶部） | DONE | 否 |
| P2 | 同上 + 注入 `restaurant_full` | DONE | 是 |
| P3 | 同上 + 注入 `route_too_far` | DONE | 是 |
| P4 | "3男1女" 输入 | DONE | 否（人群约束正确）|
| P5 | "2个人" 4人场景输入 | DONE | 人数约束正确 |
| P6 | 极晚时间："今晚10点" | DONE or DEGRADE | 时间约束检查 |
| P7 | 朋友场景 + 注入 `booking_fail` | DONE | 部分 manual |

**边界场景（7条）**

| # | 输入 | 验证点 |
|---|------|--------|
| E1 | 极短输入："带孩子出去玩" | 意图抽取最小字段 |
| E2 | 含非法字符输入 | 400 错误处理 |
| E3 | 连发两次请求 | 返回各自独立 plan_id |
| E4 | SSE 连接中断 + 重连 | 前端自动重连成功 |
| E5 | 用户不确认，30分钟后查询 | plan 状态仍为 confirm |
| E6 | 执行时网络超时（Mock 注入）| execute_result 返回 failed |
| E7 | DEGRADE 场景后用户放宽距离重发 | 新 plan_id，正常规划 |

---

## 12. 开发规范

### 12.1 Git 规范

```
主分支：main（保护，需 PR merge）
功能分支：feat/xxx（每个模块独立分支）
分支命名示例：
  feat/backend-state-machine
  feat/backend-mock-api
  feat/frontend-log-panel
  fix/sse-reconnect
```

PR 规则：
- 每个 PR 必须包含：变更说明 + 如何测试
- Sprint 1/2 结束前各做一次集成 merge

### 12.2 本地启动

```bash
# 后端（Java / Spring Boot）
cd backend
# 首次：复制 application.yml 并填入 OpenAI key
cp src/main/resources/application.yml.example src/main/resources/application.yml
# 启动（端口 8080）
mvn spring-boot:run

# 或 IDEA 直接运行 WeekendTravelApplication.java

# 前端（Vue + Vite）
cd frontend
pnpm install
pnpm dev  # http://localhost:5173
```

**CORS 配置**（后端必须开放，B1 负责在 `application.yml` 中配置）：
```yaml
spring:
  mvc:
    cors:
      allowed-origins: "http://localhost:5173"
      allowed-methods: "GET,POST,OPTIONS"

openai:
  api-key: ${OPENAI_API_KEY}   # 从环境变量读取，不硬编码
  model: codex-gpt5.5
```

### 12.3 联调检查清单（每次集成前）

- [ ] `GET http://localhost:8000/health` 返回 200
- [ ] `POST /api/plan` 返回 `plan_id`
- [ ] SSE 连接后 30 秒内收到 `plan_ready` 事件
- [ ] `POST /api/plan/{id}/execute` 触发执行日志
- [ ] `POST /api/debug/scenario {"restaurant_full": true}` 后再发起规划，Plan B 触发
- [ ] 前端刷新页面不报错

### 12.4 每日同步规范

- 每天下午 6 点，各自在群里发 3 行：① 今天做了什么 ② 是否有阻塞 ③ 明天做什么
- 阻塞超过 2 小时必须在群里喊出来，不能自己卡着
- Sprint 结束当天做 30 分钟集成联调，不通过的 Sprint Goal 立即补

---

*文档维护：有接口变更、模型变更、架构调整，修改人在本文档对应章节更新，并在群里 @全员。*
