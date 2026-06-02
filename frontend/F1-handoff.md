# F1 Handoff

## 2026-06-02 QA-001 21 Golden Cases list and execution record

QA-001 已完成并 verified。本轮没有修改前端 UI，也没有新增 Chrome DevTools MCP 截图；QA evaluator 使用真实后端 API / SSE 执行 21 条 Golden Cases，单独补齐了 family / friends / boundary 的执行记录。

本轮新增或关联的文件：

- `docs/contracts/QA-001-golden-cases.md`
- `docs/qa/QA-001-run-golden-cases.py`
- `docs/qa/QA-001-golden-cases.md`
- `docs/qa/QA-001-golden-cases-results.json`

前端侧关键结论：

- 14 条正常 family / friends case 均通过真实后端规划与执行链路，到达 `plan_ready`、`execute_result` 和 `done`。
- friends case 均校验 activity + cafe/dessert + restaurant，说明后端输出仍满足前端 PlanCard / timeline 的核心渲染假设。
- 本轮是 API / SSE QA 记录，不代表真实浏览器 21 case UI regression；如后续需要浏览器回归，应另开任务并按 `WF-002` 使用 Chrome DevTools MCP。

QA evaluator 验证结果：

- 21 / 21 Golden Cases PASS。
- Normal family/friends planning P95：43.34 ms。
- Normal feasibility rate：100%。
- Normal Plan B rate：0%。
- Injected Plan B trigger accuracy：100%。
- Intent / scenario accuracy：100%。
- 最终回归检查：frontend fast verify 通过，`pnpm verify:fixtures` 与 `pnpm typecheck` 均通过；forbidden snake_case 字段扫描无命中。

## 2026-06-01 INT-003 Friends scenario complete path

INT-003 已完成并 verified。前端 real mode 现在可以跑通 friends 输入的完整真实后端链路：选择 `朋友` 场景并提交 friends demo 输入后收到真实 `plan_ready` / `CONFIRM`，方案卡显示 `activity + cafe + restaurant` 三段 friends timeline，点击确认执行后调用真实 `POST /api/plan/{planId}/execute`，随后重新连接同一个 `planId` 的 SSE stream，消费后端 `execute_result` 与 `done`，最终进入 `DONE`。

本轮新增或关联的文件：
- `docs/contracts/INT-003-friends-complete-path.md`
- `docs/qa/INT-003-friends-complete-path.md`
- `docs/qa/INT-003-devtools-real.png`
- `docs/qa/INT-003-planning-stream.network-response`
- `docs/qa/INT-003-execution-stream.network-response`

前端侧关键结论：
- 前端 real mode 执行链路无需新增代码；`plannerClient.executePlan(planId, { confirmed: true })` 和执行 SSE 重连能力已被 friends real mode 验收覆盖。
- 场景选择会向后端发送 `scenario:"friends"`，evaluator 的 network request body 已确认。
- PlanCard / ExecutionTracker / LogPanel 可以消费真实后端返回的 friends `plan_ready`、`execute_result` 和 `done`。
- mock mode 行为保持原样，仍使用 fixture 回放。

独立 evaluator Zeno (`019e8397-d87b-7f12-8312-2e83325f8c75`, `INT-003-EVAL-CODEX-20260601T2235+0800`) 验证结果：
- backend fast verify 通过：48 tests、0 failures、0 errors、`BUILD SUCCESS`、`Verify passed.`。
- frontend fast verify 通过：`pnpm verify:fixtures` 与 `pnpm typecheck` 均通过。
- Chrome DevTools MCP real mode browser path 通过：选择 `朋友` -> 提交 friends demo 输入 -> `plan_ready` / `CONFIRM` -> 确认执行 -> `DONE`。
- 页面证据包含 `Mode real`、真实 `plan_5ca109e3a00c`、summary `朋友活动、咖啡/甜品和轻松聚餐`、PlanCard timeline 的 `城市影像展` / `街角咖啡` / `暮色烤肉`、ExecutionTracker `2 / 2`、`MOCK-TBL-94672`、`MOCK-MSG-43835`、LogPanel `execute_result` 与 `done`。
- Chrome DevTools MCP diagnostics：console error / warn 为 0；network 包含 `POST /api/plan [202]`、planning stream `[200]`、`POST /execute [200]`、execution stream `[200]`，请求体确认 `scenario:"friends"`。
- QA 报告：`docs/qa/INT-003-friends-complete-path.md`；截图：`docs/qa/INT-003-devtools-real.png`。

## 2026-05-31 INT-002 Family scenario complete path

INT-002 已完成并 verified。前端 real mode 现在可以跑通默认 family 输入的完整真实后端链路：提交规划后进入 `CLARIFY`，回答 `4-6小时` 后收到真实 `plan_ready` / `CONFIRM`，点击确认执行后调用真实 `POST /api/plan/{planId}/execute`，随后重新连接同一个 `planId` 的 SSE stream，消费后端 `execute_result` 与 `done`，最终进入 `DONE`。

本轮新增或关联的文件：
- `docs/contracts/INT-002-family-complete-path.md`
- `docs/qa/INT-002-family-complete-path.md`
- `docs/qa/INT-002-devtools-real.png`
- `docs/qa/INT-002-generator-devtools-real.png`
- `frontend/src/stores/planner.ts`

前端侧关键变化：
- real mode 在收到 `clarification_request` 或 `plan_ready` 后会关闭当前规划流，避免页面停在 `CLARIFY` / `CONFIRM` 时继续制造重连噪声。
- real mode 点击确认执行后仍调用 `executePlan(planId, { confirmed: true })`，但成功后会用返回的同一个 `planId` 重新连接 SSE，等待真实执行事件。
- mock mode 行为保持原样，仍使用 fixture 回放。
- `execute_result` 会更新 ExecutionTracker 的 action status / confirmationNo；`done` 会把 Agent 状态推进到 `DONE` 并停止执行态。

独立 evaluator Evaluator (`019e7d13-a0dc-76d2-8a94-2e28f4a36d8c`, `INT-002-EVAL-CODEX-20260531T1615+0800`) 验证结果：
- full fast verify 通过：后端 48 tests、0 failures；前端 `pnpm verify:fixtures` 与 `pnpm typecheck` 通过。
- forbidden snake_case 检查无命中，`feature_list.json` 可解析。
- Chrome DevTools MCP real mode browser path 通过：默认 family 输入 -> `CLARIFY` -> `4-6小时` -> `plan_ready` / `CONFIRM` -> 确认执行 -> `DONE`。
- 页面证据包含 `Mode real`、真实 `plan_79b2807ae0b3`、PlanCard timeline 的 `小小科学工坊` / `四季家庭小厨` POI、ExecutionTracker `2 / 2`、`MOCK-TBL-50306`、`MOCK-MSG-99469`、LogPanel `execute_result` 与 `done`。
- Chrome DevTools MCP diagnostics：console error / warn 为 0；network 包含 `POST /api/plan [202]`、规划流 `[200]`、`POST /clarify [200]`、后续规划流 `[200]`、`POST /execute [200]`、执行流 `[200]`。
- QA 报告：`docs/qa/INT-002-family-complete-path.md`；截图：`docs/qa/INT-002-devtools-real.png`。

## 2026-05-30 F1-008 Plan B highlight and error/degrade states

F1-008 已完成并 verified。前端现在会在 mock mode 中把 `replan` / Plan B、`DONE`、`DEGRADE`、`FAILED` 和可读错误消息放到稳定可见入口：PlanCard header 有 `Plan B` 徽标，PlanCard 正文有 `Plan B 已启用` 和 `planBReason`，LogPanel 对 `replan` / `done` / `DEGRADE` / 普通 error 使用不同视觉状态，侧栏新增 `StateSummaryPanel` 展示当前状态、SSE 状态、`DONE / DEGRADE / FAILED` 终态轨道、Plan B 原因和终态提示。

本轮新增或关联的文件：
- `docs/contracts/F1-008-plan-b-error-states.md`
- `docs/qa/F1-008-plan-b-error-states.md`
- `docs/qa/F1-008-devtools-mock.png`
- `docs/qa/F1-008-generator-devtools-mock.png`
- `frontend/src/components/StateSummaryPanel.vue`
- `frontend/src/components/PlanCard.vue`
- `frontend/src/components/LogPanel.vue`
- `frontend/src/App.vue`
- `frontend/src/styles/main.css`
- `frontend/scripts/verify-fixtures.mjs`

独立 evaluator Evaluator (`019e78fc-cdfb-79e2-993f-3973a3183622`) 验证结果：
- 前端 fast verify 通过：`pnpm verify:fixtures passed`、`docs/fixtures/sse-events.jsonl -> 28 JSONL events`、`pnpm typecheck passed`、`Verify passed.`
- 禁止 snake_case 字段搜索无命中：`plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no` 未出现在 `frontend\src frontend\scripts docs\fixtures`。
- `feature_list.json` 可解析。
- Chrome DevTools MCP：打开 `http://127.0.0.1:5173/`，提交 mock plan，回答 `4-6小时` 后进入 `CONFIRM`，可见 `F1-008`、`Plan B`、`Plan B 已启用`、Plan B 原因、`状态总览`、`DONE / DEGRADE / FAILED` 终态轨道和 LogPanel `replan / Plan B #1`。
- Chrome DevTools MCP：点击“确认执行”后进入 `DONE`，可见 DONE 摘要、done 日志、执行追踪 `3 / 3`、`MOCK-TBL-88421`、`MOCK-NOTE-122`、`MOCK-MSG-309`。
- Chrome DevTools MCP 诊断：console error / warn 为 0；mock mode network 只有 Vite 模块和 fixture raw import，没有真实 `/api/plan/*` 请求。
- 最终结论：`PASS`。

注意：本轮只交付前端 UI 和 fixture 校验增强，不修改 `docs/api-contract.md`，不新增后端异常注入或真实后端 `error(code=DEGRADE)` 浏览器网络证据。`INT-002` / `INT-003` happy path 当前已完成，真实后端异常链路建议留给单独 real mode integration 任务。

## 2026-05-30 F1-007 AdjustPanel

F1-007 已完成并 verified。前端现在可以在 mock mode 下提交 Demo 输入、回答 ClarifyBubble、进入 `CONFIRM` 后显示 AdjustPanel；用户可在确认执行前提交最多 3 次局部微调。每次微调会调用 `adjustPlan(planId, { instruction })`，mock mode 继续播放 `adjust_result` 并更新方案卡，real mode 则调用 `PATCH /api/plan/{planId}/adjust` 后重新连接同一 `planId` 的 SSE stream。

本轮新增或关联的文件：

- `docs/contracts/F1-007-adjust-panel.md`
- `docs/qa/F1-007-adjust-panel.md`
- `docs/qa/F1-007-devtools-mock.png`
- `docs/qa/F1-007-generator-devtools-mock.png`
- `frontend/src/components/AdjustPanel.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/App.vue`
- `frontend/src/styles/main.css`
- `frontend/scripts/verify-fixtures.mjs`
- `docs/fixtures/sse-events.jsonl`

独立 evaluator Reviewer (`019e77f0-f608-7e33-9dbf-0b2bcc94091a`) 验证结果：

- 前端 fast verify 通过：`pnpm verify:fixtures passed`、`docs/fixtures/sse-events.jsonl -> 28 JSONL events`、`pnpm typecheck passed`、`Verify passed.`
- 禁止 snake_case 字段搜索无命中：`plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no` 未出现在 `frontend\src frontend\scripts docs\fixtures`。
- `feature_list.json` 可解析。
- Chrome DevTools MCP：打开 `http://127.0.0.1:5173/`，提交 mock plan，回答 `4-6小时`，等待 `CONFIRM` 后确认 AdjustPanel 可见；提交一次微调后出现 `林间日式小食`、计数 `1 / 3`、Agent `CONFIRM`；再提交两次后出现 `3 / 3`、`已达最大微调次数`，三个快捷项、微调输入框和提交按钮均 disabled，确认执行按钮仍可用。
- Chrome DevTools MCP 诊断：snapshot / accessibility 覆盖 ClarifyBubble、AdjustPanel、PlanCard、`adjust_result` 日志和 `3 / 3` 上限状态；DOM 复核 `hasRestaurant=true`、`hasAdjustCount=true`、`hasLimitReason=true`；console error / warn 为 0；mock mode 下 fetch / xhr 为空，没有真实 `/api/plan/*/adjust` 请求。
- 最终结论：`PASS`。

注意：本轮 F1-007 的 real mode 职责是通过既有 `PlannerApiClient.adjustPlan(planId, { instruction })` 调用 `PATCH /api/plan/{planId}/adjust`，并显示后端响应或错误；真实后端 `adjust_result` 浏览器网络证据可在后续 integration 任务中补充。

## 2026-05-29 F1-006 ClarifyBubble

F1-006 已完成并 verified。前端现在可以在 mock mode 下先停在 `clarification_request`，渲染唯一一个 ClarifyBubble；用户点击 `3-4小时`、`4-6小时` 或 `6小时以上` 后，前端会调用 `clarifyPlan(planId, { reply })`，并继续回放到 `plan_ready` / `CONFIRM`。

本轮新增或关联的文件：
- `docs/contracts/F1-006-clarify-bubble.md`
- `docs/qa/F1-006-clarify-bubble.md`
- `docs/qa/F1-006-playwright-mock.png`
- `docs/qa/F1-006-devtools-mock.png`
- `frontend/src/components/ClarifyBubble.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/App.vue`
- `frontend/src/styles/main.css`
- `frontend/scripts/verify-fixtures.mjs`
- `docs/fixtures/sse-events.jsonl`

独立 evaluator Dewey (`019e7418-6911-76c1-888c-b41bc448e5a2`) 验证结果：
- 前端 fast verify 通过：`pnpm verify:fixtures passed`、`docs/fixtures/sse-events.jsonl -> 26 JSONL events`、`pnpm typecheck passed`、`Verify passed.`
- 禁止 snake_case 字段搜索无命中：`plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no` 未出现在 `frontend\src frontend\scripts docs\fixtures`。
- `feature_list.json` 可解析。
- Playwright MCP：打开页面并提交 mock plan 后，`.clarify-bubble` 数量为 `1`，问题为“请问大概想玩几个小时？”，field 为 `durationHours`，选项为 `3-4小时`、`4-6小时`、`6小时以上`；点击 `4-6小时` 后气泡消失，方案卡出现，Agent 为 `CONFIRM`，确认执行按钮可用。
- Chrome DevTools MCP：snapshot / accessibility 覆盖 `region "需要确认"`、问题和 3 个回答按钮；console error / warn 为 `0`；mock mode 未访问真实 `/api/plan/*/clarify`。

注意：本轮 real mode 的职责是通过既有 `PlannerApiClient.clarifyPlan(planId, { reply })` 调用 `POST /api/plan/{planId}/clarify` 并显示后端响应或错误；evaluator 本轮只做 mock mode 浏览器验收，real mode 网络证据可在后续 integration 任务中补充。

## 2026-05-27 F1-005 PlanCard / ConfirmButton / ExecutionTracker

F1-005 已完成并 verified。前端现在可以在 mock mode 下提交 Demo 输入，逐条回放 planning fixture，停在 `CONFIRM` 等待用户点击“确认执行”；确认后调用 `executePlan(planId, { confirmed: true })`，再用 fixture 中的 `execute_result` / `done` 更新 ExecutionTracker。

本轮新增或关联的文件：
- `docs/contracts/F1-005-plan-card-execution.md`
- `docs/qa/F1-005-plan-card-execution.md`
- `docs/qa/F1-005-playwright-mock.png`
- `docs/qa/F1-005-devtools-mock.png`
- `frontend/src/components/PlanCard.vue`
- `frontend/src/components/ConfirmButton.vue`
- `frontend/src/components/ExecutionTracker.vue`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/styles/main.css`
- `docs/fixtures/sse-events.jsonl`

独立 evaluator Copernicus (`019e693f-998a-7d03-8476-9c96e93288cb`) 验证结果：
- 前端 fast verify 通过，`docs/fixtures/sse-events.jsonl -> 24 JSONL events`、`pnpm verify:fixtures passed`、`pnpm typecheck passed`。
- 禁止 snake_case 字段搜索无命中：`plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no` 未出现在 `frontend\src frontend\scripts docs\fixtures`。
- `feature_list.json` 可解析。
- Playwright MCP：初始 `START` 下确认按钮 disabled；点击“提交规划”后进入 `CONFIRM`，PlanCard 展示 summary、timeline、actions、shareMessage、总时长和 Plan B；点击“确认执行”后 ExecutionTracker 显示 `3 / 3`，并出现 `MOCK-TBL-88421`、`MOCK-NOTE-122`、`MOCK-MSG-309`。
- Chrome DevTools MCP：snapshot / accessibility 覆盖 PlanCard、确认按钮和 ExecutionTracker；console error / warn 为 0；mock mode 下没有真实 `/api/plan/*/execute` fetch / xhr。

注意：F1-005 的 real mode 职责是调用 `POST /api/plan/{planId}/execute` 并显示返回消息或错误；真实 `plan_ready`、真实 execute event 和完整 family / friends 端到端执行链路已由后续 `B1-004`、`INT-002` 和 `INT-003` 收口。

## 2026-05-27 INT-001 联调验收

INT-001 已完成并 verified。真实 real mode 链路已经由独立 evaluator 放行：前端提交一条 Demo 自然语言输入，后端返回 `planId`，前端用该 `planId` 打开 `/api/plan/{planId}/stream`，并在 LogPanel 渲染真实后端 `state_change START -> INTENT`。

本轮新增或关联的文件：
- `docs/contracts/INT-001-one-input-sse.md`
- `docs/qa/INT-001-one-input-sse.md`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerIntegrationTests.java`

独立 evaluator Franklin (`019e691e-822d-7403-8ca3-df84e5f280c3`, `INT-001-EVAL-CODEX-20260527T1915+0800`) 验证结果：
- 后端 fast verify 通过，36 tests、0 failures、`BUILD SUCCESS`。
- 前端 fast verify 通过，`pnpm verify:fixtures passed`，`pnpm typecheck passed`。
- Playwright MCP：打开 `http://127.0.0.1:5173/`，确认 real mode，点击“提交规划”，页面出现后端 `planId`、Agent `INTENT`，LogPanel 可见 `heartbeat`、`state_change` 和 `START -> INTENT`。
- Chrome DevTools MCP：snapshot / accessibility 覆盖 InputPanel、Pinia 状态和 LogPanel；console error / warn 为 0；network 包含 `POST http://localhost:8000/api/plan [202]` 与 `/api/plan/{planId}/stream [200]`，SSE 响应体里 `state_change.data.planId` 与 POST 返回一致。

注意：这是 INT-001 当时的历史限制；后续 `B1-004`、`F1-005`、`INT-002` 和 `INT-003` 已分别补齐完整状态机、执行入口与 family / friends happy path。

## 当前状态

F1-001、F1-002、F1-003、F1-004、F1-005、F1-006、F1-007、F1-008 已完成并验证；INT-001 最小真实联调、INT-002 family 完整路径和 INT-003 friends 完整路径也已完成并验证。`frontend/` 现在是 Vue 3 + Vite + Pinia + Naive UI 前端骨架，`pnpm dev` 默认监听 `127.0.0.1:5173`。

当前前端具备骨架、API client、mock fixture mode、正式 `InputPanel`、`useSSE`、实时日志面板、PlanCard、ConfirmButton、ExecutionTracker 和 ClarifyBubble：
- 已有首页工作台骨架。
- 已有家庭 / 朋友两个 Demo 场景切换。
- 已有正式 `frontend/src/components/InputPanel.vue`，包含自然语言输入、origin 输入、场景选择、提交按钮和提交状态提示。
- real mode 下提交会调用 `POST /api/plan`，读取 camelCase `planId` / `status`，并把 `planId` 交给 `useSSE` 连接 `/api/plan/{planId}/stream`。
- 已接入 Pinia store。
- 已使用 Naive UI 组件。
- 已有 `frontend/src/api/client.ts`，默认 `mock` mode，可用 `VITE_API_MODE=real` 切换真实 API mode。
- 已有 `frontend/src/api/fixtures.ts` 和 `frontend/scripts/verify-fixtures.mjs`，可解析 `docs/fixtures/plan-ready-family.json`、`docs/fixtures/plan-ready-friends.json`、`docs/fixtures/sse-events.jsonl`。
- 已有 `frontend/src/composables/useSSE.ts`，mock mode 逐条回放 fixture SSE，real mode 通过 `openPlanStream(planId)` 连接 `/api/plan/{planId}/stream`。
- 已有 `frontend/src/components/LogPanel.vue`，可以展示 `heartbeat`、`state_change`、`tool_call`、`tool_result`、`replan`、`clarification_request`、`adjust_result`、`plan_ready`、`execute_result`、`done`、`error`，并自动滚动到最新事件。
- real mode 已通过 INT-001 验证：提交后页面能显示真实后端 `planId`，并在 LogPanel 渲染后端 SSE `state_change START -> INTENT`。
- real mode 已通过 INT-002 验证：默认 family 输入可完成 `CLARIFY -> plan_ready / CONFIRM -> execute_result -> done -> DONE`，ExecutionTracker 可显示真实执行结果和 mock confirmationNo。
- real mode 已通过 INT-003 验证：friends 输入可完成 `plan_ready / CONFIRM -> execute_result -> done -> DONE`，PlanCard 可显示 `activity + cafe + restaurant` 三段 timeline，ExecutionTracker 可显示 `2 / 2` 与 mock confirmationNo。
- 点击“提交规划”后，mock client 会创建 fixture plan，`useSSE` 逐条回放 `docs/fixtures/sse-events.jsonl`；当前 fixture 会先停在 `CLARIFY` 并显示 ClarifyBubble，用户回答后继续到 `CONFIRM`；页面可见 PlanCard、Plan B、timeline、执行包和分享消息。
- ClarifyBubble 基于 `pendingClarification` 渲染唯一一个反问气泡，包含问题、`durationHours` 和 3 个快捷选项；回答后调用 `clarifyPlan(planId, { reply })`。
- 点击“确认执行”后，前端调用 `executePlan`，mock mode 继续播放 fixture execution 事件，ExecutionTracker 会按 action 显示完成状态和确认号。
- 已实现 `frontend/src/components/AdjustPanel.vue`，仅在 `CONFIRM` 状态出现，最多允许 3 次微调；mock mode 下提交微调会消费 `adjust_result` 并更新方案卡，real mode 下调用 `PATCH /api/plan/{planId}/adjust`。

## 已完成文件

- `docs/contracts/F1-001-vue-skeleton.md`
- `docs/contracts/F1-002-api-client-fixtures.md`
- `docs/contracts/F1-003-sse-log-panel.md`
- `docs/contracts/F1-004-input-plan-api.md`
- `docs/contracts/F1-005-plan-card-execution.md`
- `docs/contracts/F1-006-clarify-bubble.md`
- `docs/contracts/F1-007-adjust-panel.md`
- `docs/contracts/INT-001-one-input-sse.md`
- `docs/qa/F1-002-api-client-fixtures.md`
- `docs/qa/F1-002-devtools-confirm.png`
- `docs/qa/F1-002-devtools-confirm-root.png`
- `docs/qa/F1-002-playwright-confirm.png`
- `docs/qa/F1-003-sse-log-panel.md`
- `docs/qa/F1-003-devtools-log-panel.png`
- `docs/qa/F1-003-playwright-log-panel.png`
- `docs/qa/F1-004-input-plan-api.md`
- `docs/qa/F1-004-devtools-mock.png`
- `docs/qa/F1-004-devtools-real.png`
- `docs/qa/F1-004-playwright-mock.png`
- `docs/qa/F1-004-playwright-real.png`
- `docs/qa/F1-004-playwright-real-127.png`
- `docs/qa/F1-005-plan-card-execution.md`
- `docs/qa/F1-005-devtools-mock.png`
- `docs/qa/F1-005-playwright-mock.png`
- `docs/qa/F1-006-clarify-bubble.md`
- `docs/qa/F1-006-devtools-mock.png`
- `docs/qa/F1-006-playwright-mock.png`
- `docs/qa/F1-007-adjust-panel.md`
- `docs/qa/F1-007-devtools-mock.png`
- `docs/qa/F1-007-generator-devtools-mock.png`
- `docs/qa/INT-001-one-input-sse.md`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/index.html`
- `frontend/public/favicon.svg`
- `frontend/src/api/types.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/fixtures.ts`
- `frontend/src/composables/useSSE.ts`
- `frontend/src/components/InputPanel.vue`
- `frontend/src/components/LogPanel.vue`
- `frontend/src/components/PlanCard.vue`
- `frontend/src/components/ConfirmButton.vue`
- `frontend/src/components/ExecutionTracker.vue`
- `frontend/src/components/ClarifyBubble.vue`
- `frontend/src/components/AdjustPanel.vue`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/styles/main.css`
- `frontend/scripts/verify-fixtures.mjs`
- `frontend/.gitignore`
- `frontend/README.md`

## 当前入口

- 应用入口：`frontend/src/main.ts`
- 页面入口：`frontend/src/App.vue`
- 状态入口：`frontend/src/stores/planner.ts`
- SSE composable：`frontend/src/composables/useSSE.ts`
- 日志面板：`frontend/src/components/LogPanel.vue`
- API client：`frontend/src/api/client.ts`
- API / SSE 类型：`frontend/src/api/types.ts`
- Fixture loader：`frontend/src/api/fixtures.ts`
- Fixture 验证脚本：`frontend/scripts/verify-fixtures.mjs`
- 样式入口：`frontend/src/styles/main.css`
- Vite 配置：`frontend/vite.config.ts`

## 开发命令

在 `frontend/` 下：

```powershell
pnpm install
pnpm verify:fixtures
pnpm dev
```

访问：

```text
http://127.0.0.1:5173
```

仓库根目录验证：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

## 已验证结果

F1-001 验证已通过：

```text
pnpm typecheck passed
Verify passed.
```

额外构建验证也通过：

```powershell
pnpm build
```

构建结果摘要：
- Vite production build 成功。
- 主 JS chunk 约 `290.51 kB`，gzip 后约 `91.94 kB`。

UI 冒烟验证：
- `pnpm dev` 已监听 `127.0.0.1:5173`。
- Playwright 打开页面可见 WeekendTravel 骨架。
- 点击“开始预演”后，Pinia 状态从 `START/idle` 变为 `INTENT/connecting`。
- console 无 error / warning。

F1-002 验证已通过：

```text
pnpm verify:fixtures passed
pnpm typecheck passed
Verify passed.
```

Fixture 解析覆盖：
- `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
- `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
- `docs/fixtures/sse-events.jsonl -> 23 JSONL events`

额外构建验证也通过：

```powershell
npm run build
```

独立 evaluator 子代理 Hooke (`019e4dd8-6378-79b2-a8d6-9f89bcadb13d`) 已放行：
- Playwright MCP：打开 `http://127.0.0.1:5173`，点击“开始预演”，页面进入 `CONFIRM` / `closed`，可见 `plan_family_fixture`、Plan B 和 summary，console error / warning 为 0。
- Chrome DevTools MCP：snapshot、console、network、DOM 复核通过；三份 fixture raw import 均为 200。
- QA 报告：`docs/qa/F1-002-api-client-fixtures.md`。
- 截图证据：`docs/qa/F1-002-devtools-confirm.png`、`docs/qa/F1-002-devtools-confirm-root.png`、`docs/qa/F1-002-playwright-confirm.png`。

F1-003 验证已通过：

```text
pnpm verify:fixtures passed
pnpm typecheck passed
Verify passed.
```

额外构建验证也通过：

```powershell
npm run build
```

Generator 字段检查无命中：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

独立 evaluator 子代理 Rawls (`019e64a8-1b72-7722-84e4-57b5364a252f`) 已放行：
- Playwright MCP：打开 `http://127.0.0.1:5173`，点击“开始预演”，等待 mock fixture SSE 完成；确认 LogPanel `role=log`、`ariaLive=polite`、`eventCount=25`、包含 `done` / `error` / `DEGRADE`，并自动滚动到底部。
- Chrome DevTools MCP：snapshot / accessibility 显示 `log "实时日志面板" live="polite"`；完成态含 `DEGRADE`、`closed`、`plan_family_fixture`、`done`、`error`；console 无消息；mock mode 下未访问 `/api/plan/*/stream`，只加载前端模块和 fixture raw import。
- QA 报告：`docs/qa/F1-003-sse-log-panel.md`。
- 截图证据：`docs/qa/F1-003-playwright-log-panel.png`、`docs/qa/F1-003-devtools-log-panel.png`。

F1-004 验证已通过：

```text
pnpm verify:fixtures passed
pnpm typecheck passed
Verify passed.
```

额外构建验证也通过：

```powershell
pnpm build
```

后端 CORS 补充回归也通过：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果覆盖 35 tests、0 failures、`BUILD SUCCESS`。

Generator 字段检查无命中：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

独立 evaluator 子代理 Gauss (`019e64d9-95ea-76a2-9f4d-53478e6a64a6`) 已放行：
- Playwright MCP：mock mode 下编辑 `InputPanel` 并点击“提交规划”，确认 `plan_family_fixture`、25 条日志和自动滚动；real mode 下 `localhost:5173` 与 `127.0.0.1:5173` 都能 POST `/api/plan` 返回 202，页面显示后端 `planId`，并可见 `heartbeat` / `START -> INTENT`。
- Chrome DevTools MCP：mock mode snapshot / accessibility 覆盖 InputPanel 和 LogPanel，console 无 error / warn，network 没有真实 `/api/plan`；real mode snapshot / accessibility 覆盖 real InputPanel，console 无 error / warn，network 包含 `POST http://localhost:8000/api/plan [202]` 和 SSE stream 请求。
- QA 报告：`docs/qa/F1-004-input-plan-api.md`。
- 截图证据：`docs/qa/F1-004-playwright-mock.png`、`docs/qa/F1-004-playwright-real.png`、`docs/qa/F1-004-playwright-real-127.png`、`docs/qa/F1-004-devtools-mock.png`、`docs/qa/F1-004-devtools-real.png`。

F1-005 验证已通过：

```text
pnpm verify:fixtures passed
pnpm typecheck passed
Verify passed.
```

Fixture 解析覆盖：
- `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
- `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
- `docs/fixtures/sse-events.jsonl -> 24 JSONL events`

额外构建验证也通过：

```powershell
npm run build
```

Generator 字段检查无命中：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

独立 evaluator 子代理 Copernicus (`019e693f-998a-7d03-8476-9c96e93288cb`) 已放行：
- Playwright MCP：初始 `START` 下确认按钮 disabled；点击“提交规划”后进入 `CONFIRM`，PlanCard 展示 summary、timeline、actions、shareMessage、总时长和 Plan B；点击“确认执行”后 ExecutionTracker 显示 `3 / 3`，并出现 `MOCK-TBL-88421`、`MOCK-NOTE-122`、`MOCK-MSG-309`。
- Chrome DevTools MCP：snapshot / accessibility 覆盖 PlanCard、确认按钮和 ExecutionTracker；console error / warn 为 0；mock mode 下没有真实 `/api/plan/*/execute` fetch / xhr。
- QA 报告：`docs/qa/F1-005-plan-card-execution.md`。
- 截图证据：`docs/qa/F1-005-playwright-mock.png`、`docs/qa/F1-005-devtools-mock.png`。

## 环境注意

- 当前沙盒 shell 里直接运行 `.\verify.ps1 -Target frontend -Mode fast` 可能找不到宿主全局 `pnpm`。
- 已验证可用方式是用宿主 PowerShell：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

- Git status 可能提示：

```text
unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied
```

这是宿主 Git 全局 ignore 权限警告，不影响前端验证。

## 下一步建议

INT-002 和 INT-003 均已完成。下一步建议转入真实异常链路补证或 QA 执行记录：

1. 若补 real mode adjust 网络证据，应在真实后端进入 `CONFIRM` 后提交 AdjustPanel，确认 network 包含 `PATCH /api/plan/{planId}/adjust` 和 `{ "instruction": "<微调要求>" }`，随后 SSE 收到 `adjust_result`。
2. 若补真实后端异常证据，应触发 `error(code=DEGRADE)` 和非 DEGRADE error，确认 StateSummaryPanel 与 LogPanel 的 `DEGRADE` / `FAILED` 可见入口。
3. `QA-001` 已在 2026-06-02 完成 API / SSE 21 Golden Cases 执行记录；如需浏览器版 21 case regression，应另开任务并使用 Chrome DevTools MCP。
4. 保持 `.\verify.ps1 -Target frontend -Mode fast` 通过；涉及 UI 验收时 evaluator 必须使用 Chrome DevTools MCP，Playwright MCP 可作为补充但不再强制要求。

## F1 边界

- API 字段变更先改 `docs/api-contract.md`。
- 前端不得自定义 snake_case 兼容分支，除非 `docs/api-contract.md` 明确要求。
- 不在前端暴露真实 token、key 或个人路径。
- 后端未完成前，优先用 `docs/fixtures/` 做独立开发。
- Generator 完成开发后，测试阶段必须交给独立 evaluator 子代理执行；generator 自己跑的 typecheck、build、Playwright 或本地冒烟只能作为开发准备记录。
- 前端 evaluator 子代理必须使用 Chrome DevTools MCP：覆盖模拟交互、状态等待、页面快照、console、network、DOM / accessibility 和视觉复核；Playwright MCP 可作为补充但不再强制要求。
- 没有 evaluator 子代理验证证据，不要把 `feature_list.json` 中的任务标为 `verified`。
