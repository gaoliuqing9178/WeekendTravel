# Contract: B1-006 CLARIFY and ADJUST

## 本轮目标

- 在现有 B1 family / friends demo 状态机基础上，补齐 `CLARIFY` 和 `ADJUST` 的后端正式链路。
- 当用户输入意图置信度不足时，状态机可进入 `CLARIFY`，通过单个 `clarification_request` 问题暂停规划，并在 `POST /api/plan/{planId}/clarify` 后继续推进。
- 当计划已进入 `CONFIRM` 时，支持 `PATCH /api/plan/{planId}/adjust` 做最多 3 次局部微调，并通过 `adjust_result` 返回最新完整 `plan`。
- 保持 `docs/api-contract.md` 既有字段、事件名和 camelCase 线缆不变。

## 明确不做

- 不实现前端 `ClarifyBubble` 或 `AdjustPanel` UI。
- 不实现完整 `EXECUTE / DONE` 正式链路。
- 不引入数据库或跨重启持久化，plan 运行态只保存在进程内存中。
- 不把局部微调扩展成通用自由文本重规划引擎。
- 不新增 snake_case 兼容层。
- 不把 generator 自测作为最终 `verified` 证据。

## 用户路径

### Clarify 路径

1. 客户端调用 `POST /api/plan` 获取真实 `planId`。
2. 客户端连接 `GET /api/plan/{planId}/stream`。
3. 当输入存在低置信度时，状态机进入 `INTENT -> CLARIFY`。
4. 服务发送一个 `clarification_request`，携带单个问题、字段名和 2-3 个选项。
5. 客户端调用 `POST /api/plan/{planId}/clarify` 提交回答。
6. 服务返回 `{ planId, status: "processing", message }`，随后在下一次 stream 中继续推进到 `plan_ready` / `CONFIRM` 或其他后续状态。

### Adjust 路径

1. 客户端在计划已进入 `CONFIRM` 后调用 `PATCH /api/plan/{planId}/adjust`。
2. 服务返回 `{ planId, status: "adjusting", message }`。
3. 客户端重新连接 stream。
4. 服务发送 `CONFIRM -> ADJUST -> VALIDATE` 的状态变化，并在完成后发送 `adjust_result`。
5. `adjust_result.plan` 必须是最新完整 `plan`，且状态回到 `CONFIRM`。

## UI 要求

- 本轮无前端代码改动要求。
- 前端只需按既有 contract 消费：`heartbeat`、`state_change`、`clarification_request`、`tool_call`、`tool_result`、`replan`、`adjust_result`、`plan_ready`、`error`。
- `clarification_request` 必须足以让前端只展示 1 个 ClarifyBubble。
- `adjust_result` 必须足以让前端更新当前 plan，并继续停在 `CONFIRM`。

## API / 后端要求

### 入口

- `POST /api/plan`：沿用既有契约。
- `GET /api/plan/{planId}/stream`：沿用既有 SSE 契约。
- `POST /api/plan/{planId}/clarify`：按 `docs/api-contract.md` 返回 200 + `processing`。
- `PATCH /api/plan/{planId}/adjust`：按 `docs/api-contract.md` 返回 202 + `adjusting`。

### 状态范围

本轮至少要求可观察到：

- `START`
- `INTENT`
- `CLARIFY`
- `SKELETON`
- `RECALL`
- `VALIDATE`
- `REPLAN`
- `PACK`
- `CONFIRM`
- `ADJUST`
- `DEGRADE`

### Clarify 约束

- `CLARIFY` 每次只问一个最关键问题。
- `clarification_request.options` 必须是 2-3 个选项。
- 单个 plan 最多 2 次 clarify round。
- `POST /clarify` 仅允许在 `CLARIFY` 状态下成功；否则返回 `409 INVALID_STATE`。
- 本轮低置信度问题只要求覆盖 duration 场景，但线缆形状必须与正式契约一致。

`clarification_request` 示例：

```json
{
  "type": "clarification_request",
  "planId": "plan_abc123",
  "question": "请问大概想玩几个小时？",
  "field": "durationHours",
  "options": ["3-4小时", "4-6小时", "6小时以上"],
  "timestamp": 1779362400500
}
```

### Adjust 约束

- `PATCH /adjust` 仅允许在 `CONFIRM` 状态下成功；否则返回 `409 INVALID_STATE`。
- 单个 plan 最多允许 3 次 adjust；第 4 次必须返回 `429 ADJUST_LIMIT_EXCEEDED`。
- `ADJUST` 只允许局部微调受影响槽位，未受影响槽位必须保持不变。
- `adjust_result` 必须包含：
  - `affectedSlots`
  - `summary`
  - 最新完整 `plan`
- `adjust_result.summary` 不要求与某个单字段完全相等，但必须是可读文案，且与最终 `plan` 一致。

`adjust_result` 示例：

```json
{
  "type": "adjust_result",
  "planId": "plan_abc123",
  "affectedSlots": ["restaurant"],
  "summary": "已将餐厅换为四季家庭小厨，其余安排保持不变",
  "plan": {}
}
```

### 数据和状态要求

最小内存上下文至少保存：

- `planId`
- `request`
- `currentState`
- `replanCount`
- `clarifyCount`
- `adjustCount`
- `latestReason`
- `packedPlan`
- `pendingClarification`
- `selectionSnapshot`
- `pendingAdjustInstruction`

### SSE 和字段要求

- 所有 SSE 事件名必须与 payload `type` 一致。
- 所有正式线缆字段统一使用 camelCase。
- `tool_result` 必须继续提供 `latencyMs`。
- `plan_ready.plan.status` 和 `adjust_result.plan.status` 本轮允许为 `CONFIRM`。

## 验收方式

Evaluator 子代理：

- 必须由独立 evaluator 子代理验证后端实现与运行态证据。
- generator 自测只能作为开发准备，不能单独作为 verified 依据。

自动验证：

- `./backend/mvnw -f backend/pom.xml test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./verify.ps1 -Target backend -Mode fast`

运行时路径：

1. 明确时长 family 输入可到 `plan_ready` / `CONFIRM`。
2. 模糊输入先进入 `CLARIFY` 并发出 `clarification_request`。
3. `POST /clarify` 后可恢复到 `plan_ready` / `CONFIRM`。
4. `PATCH /adjust` 后发出 `adjust_result`，并包含 `affectedSlots` 和最新完整 `plan`。
5. 第 4 次 `adjust` 返回 `429 ADJUST_LIMIT_EXCEEDED`。

## 失败阈值

- 低置信度输入不能进入 `CLARIFY`，失败。
- `clarification_request` 缺少 2-3 个 options，失败。
- `POST /clarify` 不能恢复规划，失败。
- `PATCH /adjust` 不能发出 `adjust_result`，失败。
- 第 4 次 adjust 不返回 `429 ADJUST_LIMIT_EXCEEDED`，失败。
- `adjust_result.plan` 不是最新完整 plan，失败。
- `summary` 与最终 `plan` 明显不一致，失败。
- 线缆字段出现 snake_case，失败。
- 没有独立 evaluator 证据，不得标记为 `verified`。
