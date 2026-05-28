# Contract: B1-005 Plan B and DEGRADE rules

## 本轮目标

- 在现有 `B1-004` 最小状态机基础上，为家庭 demo 补齐 deterministic `Plan B` / `DEGRADE` 行为。
- 让 `restaurantFull` 和 `routeTooFar` 注入都能触发可观察的 `replan` / `REPLAN` 链路。
- 在重排次数达到上限后进入 `DEGRADE`，并发出可读 `error(code=DEGRADE)`。
- 保持 `docs/api-contract.md` 既有字段、事件名和 camelCase 线缆不变。

## 明确不做

- 不实现 `CLARIFY / ADJUST / EXECUTE / DONE / FAILED` 的完整行为。
- 不实现 friends 通用规划器；本轮只要求稳定支持 `scenario=family`。
- 不接入真实 LLM、地图、订座、支付或配送服务。
- 不新增 snake_case 字段兼容层。
- 不把 generator 自测作为最终 `verified` 证据。

## 用户路径

1. 客户端调用 `POST /api/plan` 获取真实 `planId`。
2. 客户端连接 `GET /api/plan/{planId}/stream`。
3. family demo happy path 进入 `START -> INTENT -> SKELETON -> RECALL -> VALIDATE -> PACK`，最终收到 `plan_ready`。
4. 若注入 `restaurantFull=true` 或 `routeTooFar=true`，则当前候选在 `VALIDATE` 失败。
5. 服务发出 `replan`，并进入 `VALIDATE -> REPLAN -> RECALL -> VALIDATE`。
6. 若新的餐厅候选仍不满足约束，且达到规则上限，则进入 `DEGRADE` 并发出 `error(code=DEGRADE)`。

## UI 要求

- 本轮无前端代码改动要求。
- 前端只需按既有 contract 消费：`heartbeat`、`state_change`、`tool_call`、`tool_result`、`replan`、`plan_ready`、`error`。
- `error(code=DEGRADE)` 必须足以让前端进入 `DEGRADE` 可视状态。

## API / 后端要求

### 入口

- `POST /api/plan`：沿用既有契约。
- `GET /api/plan/{planId}/stream`：沿用既有 SSE 契约。

### 状态范围

本轮至少要求可观察到：

- `START`
- `INTENT`
- `SKELETON`
- `RECALL`
- `VALIDATE`
- `REPLAN`
- `PACK`
- `DEGRADE`

### 触发规则

- `restaurantFull`：
  - 由 `AvailabilityTool.checkAvailability(...)` 的餐厅结果触发。
  - 行为为优先锁定活动地点，再切换餐厅候选。
- `routeTooFar`：
  - 由 `RouteTool.calculateRouteTime(...)` 的距离或 `ScenarioFlags.routeTooFar` 触发。
  - 行为为优先切换餐厅候选。

### REPLAN 约束

- 每次验证失败且存在下一候选时，必须先发 `replan`，再进入 `REPLAN`。
- 本轮 `MAX_REPLAN_ATTEMPTS = 2`，即最多允许 2 次已发出的 `replan` 事件。
- 达到上限后，不再继续发送新的 `replan`，直接进入 `DEGRADE`。

### DEGRADE 要求

- 达到重排上限或无新的 fallback 选择时，必须：
  - 发出 `state_change` 到 `DEGRADE`
  - 发出 `error`
- `error` payload 需符合 `docs/api-contract.md`：

```json
{
  "type": "error",
  "planId": "plan_abc123",
  "code": "DEGRADE",
  "message": "2 次重排后仍无可行方案：原路线过远，已切换到更近餐厅",
  "timestamp": 1779362429000
}
```

- `message` 必须可读，且说明失败原因。

### plan_ready 要求

happy path 或成功 fallback 时，`plan_ready.plan` 至少包含：

- `planId`
- `scenario`
- `status`
- `summary`
- `timeline`
- `replanCount`
- `isPlanB`
- `planBReason`
- `actions`
- `shareMessage`

约束：

- happy path：`isPlanB=false`，`replanCount=0`
- 发生过 fallback 且成功到达 `PACK`：`isPlanB=true`，`planBReason` 为最后一次重排原因，`replanCount` 等于已发出的 `replan` 次数

### 数据和状态要求

最小内存上下文至少保存：

- `planId`
- `request`
- `replanCount`
- `latestReason`
- `packedPlan`

可额外保存：

- `injectedPlanB`

### 指标与统计要求

- 代码路径必须为后续区分两类指标保留信息：
  - 正常非注入场景下的 `Plan B rate`
  - 注入场景下的触发准确性
- 本轮至少在上下文中保留 `injectedPlanB` 标记，供后续统计扩展。

## 验收方式

Evaluator 子代理：

- 必须由独立 evaluator 子代理验证后端实现与 SSE 证据。
- generator 自测只能作为开发准备，不能单独作为 verified 依据。

手动路径：

1. 调用 `POST /api/plan` 获取真实 `planId`。
2. 连接 `GET /api/plan/{planId}/stream`。
3. happy path 确认至少出现：`heartbeat`、`state_change`、`tool_call`、`tool_result`、`plan_ready`。
4. `restaurantFull=true` 时确认至少出现：`replan`、`REPLAN`、`DEGRADE`、`error(code=DEGRADE)`。
5. `routeTooFar=true` 时确认至少出现：`replan`、`REPLAN`、`DEGRADE`、`error(code=DEGRADE)`。

自动验证：

- `./backend/mvnw -f "backend/pom.xml" test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`

API / 日志 / 证据：

- evaluator 需要在 QA 报告中记录 create+stream 串联验证命令、关键 SSE 文本证据和最终结论。

## 失败阈值

- family demo happy path 无法到达 `PACK`，失败。
- `restaurantFull` 或 `routeTooFar` 不能触发 `replan`，失败。
- 达到重排上限后未进入 `DEGRADE`，失败。
- `error` 缺少 `code=DEGRADE` 或缺少可读 `message`，失败。
- `tool_result` 缺少 `latencyMs`，失败。
- 线缆字段出现 snake_case，失败。
- 没有独立 evaluator 证据，不得标记为 `verified`。
