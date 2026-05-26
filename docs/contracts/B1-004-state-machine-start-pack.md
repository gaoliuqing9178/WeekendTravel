# Contract: B1-004 State machine START to PACK

## 本轮目标

- 实现最小后端规则状态机，使家庭场景 demo 能从 `START` 推进到 `PACK`。
- 在 `GET /api/plan/{planId}/stream` 中输出契约要求的最小 SSE 事件链路。
- 复用现有 `SearchTool`、`RouteTool`、`AvailabilityTool`、`ScenarioFlags` 和必要时的 `MessageTool`。
- 保持 `POST /api/plan` 的 HTTP 入口、成功响应和错误结构不变。
- 为后续 `B1-005`、`B1-006` 的 `DEGRADE / CLARIFY / ADJUST / EXECUTE` 扩展保留边界。

## 明确不做

- 不实现完整 `CLARIFY / CONFIRM / ADJUST / EXECUTE / DONE / FAILED` 行为。
- 不接真实 LLM 做 POI 选择、状态转移或 Plan B 判断。
- 不接真实地图、订座、支付、配送等外部服务。
- 不修改 `docs/api-contract.md` 已确认的正式线缆字段。
- 不为了测试简单而删减 SSE 事件。

## 用户路径

1. 客户端向 `POST /api/plan` 发送自然语言需求，获取 `planId` 和 `status=processing`。
2. 客户端以该 `planId` 建立 `GET /api/plan/{planId}/stream` SSE 连接。
3. 服务先推送 `heartbeat`，再启动最小规则状态机。
4. 状态机按 `START -> INTENT -> SKELETON -> RECALL -> VALIDATE -> PACK` 推进。
5. 每次状态变化都推送 `state_change`。
6. 每次 Tool 调用都推送 `tool_call` 和 `tool_result`。
7. 若校验失败但可通过最小 fallback 修复，则推送 `replan` 并进入 `REPLAN -> RECALL`。
8. 到达 `PACK` 后推送 `plan_ready`，其中包含最小 mock plan payload。

## UI 要求

- 本轮无 UI 改动。
- 前端只需能消费以下最小事件：`heartbeat`、`state_change`、`tool_call`、`tool_result`、`replan`、`plan_ready`。
- 前端不依赖 Java 类，只依赖 `docs/api-contract.md`、本契约和 `docs/fixtures/sse-events.jsonl`。

## API / 后端要求

### 入口

- `POST /api/plan`：沿用 B1-003 现有契约。
- `GET /api/plan/{planId}/stream`：沿用 B1-002 现有 SSE 契约。
- Response header 仍需包含：
  - `Content-Type: text/event-stream`
  - `Cache-Control: no-cache`
  - `Connection: keep-alive`

### 状态范围

本轮至少实现并可观察到以下状态：

- `START`
- `INTENT`
- `SKELETON`
- `RECALL`
- `VALIDATE`
- `REPLAN`
- `PACK`

允许代码中保留更完整的状态枚举，但本轮验收只要求上述状态链路可运行。

### 最小状态转移

主链路：

```text
START -> INTENT -> SKELETON -> RECALL -> VALIDATE -> PACK
```

重排支路：

```text
VALIDATE -> REPLAN -> RECALL
```

约束：

- 每次状态变化必须推送 `state_change`。
- `REPLAN` 只做最小 deterministic fallback，不提前实现完整 `DEGRADE` 流程。
- 本轮至少要保证一条家庭 demo 输入可以在不报错的情况下到达 `PACK`。

### SSE 事件要求

本轮必须支持并输出：

- `heartbeat`
- `state_change`
- `tool_call`
- `tool_result`
- `replan`
- `plan_ready`

统一要求：

- SSE event name 必须等于 payload 中的 `type`。
- 线缆字段统一使用 camelCase。
- `tool_result` 必须包含 `latencyMs`。
- `plan_ready` 必须包含可消费的最小 `plan` payload，而不是只返回空状态。

推荐 happy path 事件节奏：

```text
heartbeat
state_change START -> INTENT
state_change INTENT -> SKELETON
state_change SKELETON -> RECALL
tool_call searchLocalPlaces
tool_result searchLocalPlaces
tool_call calculateRouteTime
tool_result calculateRouteTime
tool_call checkAvailability
tool_result checkAvailability
state_change RECALL -> VALIDATE
state_change VALIDATE -> PACK
plan_ready
```

如发生最小重排，需额外出现：

```text
replan
state_change VALIDATE -> REPLAN
state_change REPLAN -> RECALL
```

### 规划与排序要求

- 规划决策必须在 B1 规则状态机中完成，不能交给 LLM。
- 候选排序直接复用 `SearchTool` 当前实现，并以其结果作为候选优先顺序。
- 当前目标公式：

```text
score = 0.4 * relevance + 0.3 * distance + 0.2 * rating + 0.1 * availability health
```

- 不允许在 B1 再并行维护第二套评分公式。
- `VALIDATE` 可以过滤或触发 fallback，但不能无依据打乱 `SearchTool` 已确定的候选顺序。

### Tool 复用要求

- `SearchTool.searchLocalPlaces(...)`：负责候选召回与排序。
- `RouteTool.calculateRouteTime(...)`：负责最小路线可行性判断。
- `AvailabilityTool.checkAvailability(...)`：负责余位、排队、年龄和人数匹配校验。
- `ScenarioFlags`：负责 `restaurantFull`、`routeTooFar`、`ageMismatch` 等动态注入。
- `MessageTool.composeShareMessage(...)`：如使用，仅可用于 `PACK` 末端格式化 summary/share message，不得参与选点与状态转移。

### family demo 最小规则要求

- 只要求稳定支持 `scenario=family` 的 demo 主路径。
- `INTENT` 阶段可以使用规则默认值，而不是通用自然语言理解：
  - family-friendly
  - 近距离
  - 低负担
  - 至少包含活动 + 餐饮两个槽位
- `SKELETON` 阶段生成固定 slot 骨架即可。
- `VALIDATE` 至少校验：
  - 年龄匹配
  - 人数可容纳
  - 路线不过远
  - availability 可接受
- `REPLAN` 至少支持：
  - 锁定活动后优先更换餐厅
  - 必要时轻微放宽距离或 P2 偏好

## 数据和状态要求

- `POST /api/plan` 生成的 `planId` 必须能用于后续 stream。
- 后端需要有最小内存态上下文把 create 和 stream 串起来。
- 上下文至少保存：
  - `planId`
  - `text`
  - `scenario`
  - `origin`
  - `replanCount`
  - 最终 packed plan 或等价结果
- `plan_ready.plan` 至少包含：
  - `planId`
  - `scenario`
  - `status`
  - `summary`
  - `timeline`
  - `replanCount`
- 可按需要补充：
  - `isPlanB`
  - `planBReason`
  - `actions`
  - `shareMessage`

## 验收方式

Evaluator 子代理：

- 必须由独立 evaluator 子代理验证后端实现与 SSE 证据。
- generator 自测只能作为开发准备，不能单独作为 verified 依据。

手动路径：

- 先调用 `POST /api/plan` 获取真实 `planId`。
- 再连接 `GET /api/plan/{planId}/stream`。
- 确认 stream 至少出现 `heartbeat`、`state_change`、`tool_call`、`tool_result`、`plan_ready`。
- 确认家庭 demo 可观察到 `START`、`INTENT`、`SKELETON`、`RECALL`、`VALIDATE`、`PACK`。
- 如构造 `restaurantFull` 场景，确认可观察到 `replan` 与 `REPLAN` 相关状态变化。

自动验证：

- `./backend/mvnw -f "backend/pom.xml" test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录 create+stream 串联验证命令、SSE 关键事件证据和最终放行结论。

## 失败阈值

- 家庭 demo 无法从 `START` 到达 `PACK`，失败。
- 某次状态变化没有推送 `state_change`，失败。
- stream 中缺少 `tool_call` 或 `tool_result`，失败。
- `tool_result` 缺少 `latencyMs`，失败。
- 排序实现未文档化，或未复用既定 `0.4 / 0.3 / 0.2 / 0.1` 公式，失败。
- 线缆字段出现 snake_case，失败。
- 没有独立 evaluator 证据，不得标记为 `verified`.
