# Contract: QA-001 21 Golden Cases list and execution record

## 本轮目标

- 列出并执行 21 条 Golden Cases：家庭 7 条、朋友 7 条、边界 7 条。
- 使用真实后端 API 路径记录执行证据，不复用 `INT-002` / `INT-003` 的 happy path 证据。
- 记录 SLO 指标，并把正常非注入 Plan B rate 与异常注入 Plan B trigger accuracy 分开统计。
- 同步 `feature_list.json`、`progress.md` 和 `docs/handoff.md` 的 QA-001 状态。

## 明确不做

- 不新增业务能力，不修改 planner、tool、前端 UI 或 API 契约。
- 不接真实支付、真实订座、真实配送、真实地图或真实第三方服务。
- 不把 `bookingFail` 这类执行期失败计入规划期 Plan B trigger accuracy。
- 不把边界异常 case 的预期 `DEGRADE` 当成普通可行方案失败。
- 不要求本轮补 Chrome DevTools MCP 截图；本轮验收主体是后端 API / SSE 执行记录，不是前端 UI 改动。

## 用户路径

1. evaluator 启动真实后端服务，确认 `GET /health` 可用。
2. evaluator 将 `POST /api/debug/scenario` 重置为全 false。
3. 对 14 条正常 family / friends case 执行：
   - `POST /api/plan`
   - `GET /api/plan/{planId}/stream`
   - 如遇 `clarification_request`，调用 `POST /api/plan/{planId}/clarify` 后继续 stream
   - 收到 `plan_ready` 后调用 `POST /api/plan/{planId}/execute`
   - 再次 stream 并等待 `execute_result` 与 `done`
4. 对 7 条 boundary case 按各自预期执行 clarify、adjust、异常注入、execute 或 invalid request。
5. evaluator 记录每条 case 的 planId、关键 SSE 事件、最终状态、耗时、PASS / FAIL 和失败原因。

## Case 清单

| ID | 类别 | 输入 / 条件 | 预期 |
|---|---|---|---|
| FAM-001 | family | 默认家庭 demo，时长不明确 | 先 `clarification_request`，回复 `4-6小时` 后 `plan_ready`，执行到 `done` |
| FAM-002 | family | 明确 4 小时、亲子轻松活动 | `plan_ready`，`isPlanB=false`，执行到 `done` |
| FAM-003 | family | 室内亲子、天气影响小 | `plan_ready`，timeline 包含 activity / restaurant，执行到 `done` |
| FAM-004 | family | 不想离家太远、低负担餐饮 | `plan_ready`，执行到 `done` |
| FAM-005 | family | 孩子 5 岁、老婆减脂 | `plan_ready`，家庭友好文案，执行到 `done` |
| FAM-006 | family | 下午半天、要能订位 | `plan_ready`，actions 包含 `reserve_table` / `send_message`，执行到 `done` |
| FAM-007 | family | 只想轻松逛逛、需要反问 | 可通过 clarify 恢复到 `plan_ready`，执行到 `done` |
| FRI-001 | friends | 默认朋友 demo | `plan_ready`，timeline 含 activity + cafe/dessert + restaurant，执行到 `done` |
| FRI-002 | friends | 4 人、能玩能吃能拍照 | `plan_ready`，scenario 为 `friends`，执行到 `done` |
| FRI-003 | friends | 拍照、下午茶、聚餐 | `plan_ready`，中途点为 cafe 或 dessert，执行到 `done` |
| FRI-004 | friends | 不想走太远、轻松聊天 | `plan_ready`，执行到 `done` |
| FRI-005 | friends | 2 男 2 女、拍照氛围 | `plan_ready`，不出现家庭 / 亲子专属文案，执行到 `done` |
| FRI-006 | friends | 半天下午活动 | `plan_ready`，actions 包含 `reserve_table` / `send_message`，执行到 `done` |
| FRI-007 | friends | 时长不明确 | 可通过 clarify 恢复到 `plan_ready`，执行到 `done` |
| BND-001 | boundary | family clarify happy path | `clarification_request` 后可恢复并执行到 `done` |
| BND-002 | boundary | `restaurantFull=true` | 出现 `replan` / `REPLAN`，最终 `error(code=DEGRADE)` |
| BND-003 | boundary | `routeTooFar=true` | 出现 `replan` / `REPLAN`，最终 `error(code=DEGRADE)` |
| BND-004 | boundary | `bookingFail=true` | `plan_ready` 后执行不崩溃，至少一个 `execute_result.status=failed`，最终 `done` |
| BND-005 | boundary | `ageMismatch=true` | 出现 `replan` / `REPLAN` 或直接透明降级，最终 `error(code=DEGRADE)` |
| BND-006 | boundary | CONFIRM 后 `PATCH /adjust` | 收到 `adjust_result` 并保持可执行，随后执行到 `done` |
| BND-007 | boundary | invalid scenario | `POST /api/plan` 返回 400 `INVALID_INPUT`，不创建 plan |

## API / 后端要求

- 所有 REST / SSE 字段必须继续符合 `docs/api-contract.md`，正式线缆字段使用 camelCase。
- 正常 family / friends case 必须通过真实 `POST /api/plan`、SSE、`POST /execute` 和执行 SSE。
- 规划期异常注入 case 必须先用 `POST /api/debug/scenario` 设置 flags，case 结束后重置为全 false。
- `execute_result` 至少覆盖 `reserve_table` 与 `send_message` 动作类型；`bookingFail` case 允许 `reserve_table` 失败，但不得阻塞最终 `done`。
- `adjust` case 必须使用真实 `PATCH /api/plan/{planId}/adjust`，并通过后续 SSE 收到 `adjust_result`。

## 数据和状态要求

- 执行记录至少包含：caseId、category、scenario、planId、debug flags、clarify 是否发生、是否执行、planningMs、executionMs、totalMs、finalState、eventTypes、PASS / FAIL。
- SLO 指标至少包含：
  - end-to-end planning P95。
  - non-injected feasible case 的 plan feasibility rate。
  - normal non-injected Plan B rate。
  - injected Plan B trigger accuracy。
  - intent / scenario extraction accuracy。
- 指标口径：
  - normal non-injected Plan B rate 只统计 14 条 family / friends 正常 case，不统计 boundary case。
  - `restaurantFull`、`routeTooFar`、`ageMismatch` 计入 injected Plan B trigger accuracy。
  - `bookingFail` 是执行期失败注入，只统计执行降级表现，不计入 Plan B trigger accuracy。

## 验收方式

Evaluator 子代理：

- 本轮由 QA / evaluator agent 执行，不接受 generator-only 旧证据替代。
- QA 报告必须明确执行入口、命令、case 列表、统计结果和放行结论。

前端浏览器验收：

- 本轮不涉及前端 UI 变更，Chrome DevTools MCP 不适用。
- 若后续扩展为真实浏览器 golden case，则必须按 `WF-002` 使用 Chrome DevTools MCP。

手动路径：

- 使用真实后端端口 `8000` 执行 `GET /health`、`POST /api/plan`、`GET /api/plan/{planId}/stream`、`POST /clarify`、`PATCH /adjust`、`POST /execute` 和 `POST /api/debug/scenario`。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast`
- `Get-Content -Raw -Encoding UTF8 feature_list.json | ConvertFrom-Json`
- 线缆字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" backend\src frontend\src frontend\scripts docs\fixtures`

API / 日志 / 截图证据：

- QA 报告：`docs/qa/QA-001-golden-cases.md`
- 机器可读执行记录：`docs/qa/QA-001-golden-cases-results.json`
- 本轮无截图要求。

## 失败阈值

- 任一 normal family / friends case 不能到达 `plan_ready`，失败。
- 任一 normal family / friends case 执行后缺少 `execute_result` 或 `done`，失败。
- friends normal case 缺少 activity + cafe/dessert + restaurant，失败。
- boundary 注入 case 没有出现预期 `replan` / `DEGRADE` / failed action / invalid error，失败。
- SLO 指标缺失，失败。
- 正常非注入 Plan B rate 与注入 Plan B trigger accuracy 混算，失败。
- 线缆字段出现 snake_case，失败。
- QA 报告、执行记录、`feature_list.json`、`progress.md`、`docs/handoff.md` 任一缺失同步，不能标为 `verified`。
