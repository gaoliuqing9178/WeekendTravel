# Progress

## 2026-05-31 INT-002 Family scenario complete path

### 已完成

- 新增 `docs/contracts/INT-002-family-complete-path.md`，明确本轮只跑通真实 real mode family happy path：`POST /api/plan` -> planning SSE -> `plan_ready` / `CONFIRM` -> `POST /api/plan/{planId}/execute` -> execution SSE -> `execute_result` -> `done`。
- 后端补齐 `POST /api/plan/{planId}/execute`、`ExecutePlanRequest` / `ExecutePlanResponse`、`ExecuteResultEvent`、`DoneEvent`，并让 `PlanStateMachineService` 支持 `CONFIRM -> EXECUTE -> DONE`。
- 后端 family packed plan 现在带前端可渲染的 `poi` payload；执行阶段使用 `BookingTool` 处理 `reserve_table`，并以 deterministic mock 方式处理 `send_message`，返回 `MOCK-TBL-*` 与 `MOCK-MSG-*`。
- 后端餐厅选择优先挑选支持 `reserve_table` 的 POI，避免 family happy path 在执行阶段因为餐厅动作不支持而失败。
- 前端 real mode 收到 `clarification_request` 或 `plan_ready` 后会关闭规划流，避免停在确认态后持续重连；点击确认执行后会用同一个 `planId` 重新连接 SSE，消费真实 `execute_result` 与 `done`。
- `feature_list.json` 已将 `INT-002` 标记为 `verified`，并写入 generator 与 independent evaluator 证据；`B1-004` 本轮没有单独 evaluator 证据，仍不单独改为 `verified`。

### 验证记录

- Generator backend fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，后端 48 tests、0 failures、0 errors。
- Generator frontend fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，`pnpm verify:fixtures` 与 `pnpm typecheck` 均通过。
- Generator full fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast` 通过。
- Forbidden snake_case 检查无命中：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" backend\src frontend\src frontend\scripts docs\fixtures`。
- `feature_list.json` 可解析，项目名输出 `WeekendTravel`。
- 独立 evaluator Evaluator (`019e7d13-a0dc-76d2-8a94-2e28f4a36d8c`, `INT-002-EVAL-CODEX-20260531T1615+0800`) 已完成正式验收并放行，结论：`PASS`。
- Chrome DevTools MCP real mode 路径通过：默认 family 输入提交 -> `CLARIFY` -> 点击 `4-6小时` -> `plan_ready` / `CONFIRM` -> 点击确认执行 -> `DONE`。
- 页面证据包含：`Mode real`、真实 `plan_79b2807ae0b3`、PlanCard timeline POI `小小科学工坊` / `四季家庭小厨`、ExecutionTracker `2 / 2`、`MOCK-TBL-50306`、`MOCK-MSG-99469`、LogPanel `execute_result` 与 `done`。
- Console error / warn 为 0；network 包含 `POST /api/plan [202]`、planning stream `[200]`、`POST /clarify [200]`、后续 planning stream `[200]`、`POST /execute [200]`、execution stream `[200]`，执行流包含两条 `execute_result` 和 `done`。
- QA 报告：`docs/qa/INT-002-family-complete-path.md`；evaluator 截图：`docs/qa/INT-002-devtools-real.png`；generator 补充截图：`docs/qa/INT-002-generator-devtools-real.png`。

### 当前状态

- `INT-002` 已完成并 verified，family real mode 端到端路径已经从输入、澄清、方案确认、真实执行到 `DONE` 全链路打通。
- 下一步集成建议转入 `INT-003` friends complete path，或在独立任务中补真实异常 / degrade 链路证据；不要把本轮 family 证据泛化成 friends path 已完成。

## 2026-05-30 F1-008 Plan B highlight and error/degrade states

### 已完成
- 新增 `docs/contracts/F1-008-plan-b-error-states.md`，明确本轮只交付前端 Plan B 高亮、终态 / 异常态可见入口和 Chrome DevTools MCP 验收边界，不修改 `docs/api-contract.md`。
- 新增 `frontend/src/components/StateSummaryPanel.vue`，在侧栏稳定展示当前 Agent 状态、SSE 状态、`DONE / DEGRADE / FAILED` 终态轨道、Plan B 原因，以及 `DONE` / `DEGRADE` / `FAILED` / `error` 可读提示。
- 更新 `frontend/src/App.vue`，将首屏标识切到 `F1-008`，接入 `StateSummaryPanel`，并把页面说明更新为 Plan B、降级、失败和完成状态保持可见。
- 更新 `frontend/src/components/PlanCard.vue`，当 `plan.isPlanB=true` 时在卡片 header 显示 `Plan B` 徽标，并在正文显示 `Plan B 已启用` 与 `planBReason`。
- 更新 `frontend/src/components/LogPanel.vue` 和 `frontend/src/styles/main.css`，将 `replan`、`done`、`error(code=DEGRADE)` 和普通 `error` 分成不同视觉状态，并为日志项补充可访问名称。
- 更新 `frontend/scripts/verify-fixtures.mjs`，要求 `execute_result`、`done` 和 `error` 存在，并校验 Plan B `plan_ready` reason、`error(code=DEGRADE)`、`replan.reason/replanCount`、`done.summary` 等字段。
- 新增 evaluator QA 报告与截图：
  - `docs/qa/F1-008-plan-b-error-states.md`
  - `docs/qa/F1-008-devtools-mock.png`
  - `docs/qa/F1-008-generator-devtools-mock.png`
- `feature_list.json` 已将 `F1-008` 标记为 `verified`，并写入 generator 与 independent evaluator 证据。

### 验证记录

- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，覆盖：
  - `pnpm verify:fixtures passed`
  - `docs/fixtures/sse-events.jsonl -> 28 JSONL events`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 构建检查：`pnpm build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 禁止字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator Chrome DevTools MCP 冒烟：
  - mock mode 下提交 plan，回答 ClarifyBubble `4-6小时` 后进入 `CONFIRM`。
  - 页面可见 `F1-008`、`Plan B 已启用`、Plan B 原因、`状态总览`、`DONE / DEGRADE / FAILED` 终态轨道、LogPanel `replan / Plan B #1`。
  - 点击确认执行后进入 `DONE`，可见 DONE 摘要、done 日志、执行追踪 `3 / 3` 和三个 mock 确认号。
  - console error / warn 为 0；mock mode network 只有 Vite 模块和 fixture raw import，没有真实 `/api/plan/*` 请求。
- Independent evaluator Evaluator (`019e78fc-cdfb-79e2-993f-3973a3183622`) 已完成正式验收并放行：
  - 独立读取 F1-008 contract、api/frontend contract、F1 handoff、App、store、StateSummaryPanel、PlanCard、LogPanel、fixture verifier 和 SSE fixture。
  - 前端 fast verify 通过。
  - 禁止 snake_case 字段搜索无命中。
  - `feature_list.json` 可解析。
  - Chrome DevTools MCP browser path 通过：提交 mock plan -> 回答 `4-6小时` -> `CONFIRM` -> Plan B 可见 -> 终态轨道可见 -> 确认执行 -> `DONE` / `3 / 3` 可见。
  - Chrome DevTools MCP snapshot / accessibility / DOM / console / network / screenshot 证据完整。
  - 最终结论：`PASS`。

### 当前状态
- `F1-008` 已完成并 verified。
- 前端当前具备 InputPanel、useSSE、LogPanel、PlanCard、ConfirmButton、ExecutionTracker、ClarifyBubble、AdjustPanel 和 StateSummaryPanel。
- F1 计划内的前端基础能力已覆盖到 Plan B 高亮、微调、执行追踪、完成态和异常 / 降级状态可见入口；后续如需真实后端异常网络证据，应进入 `INT-002` / `INT-003` 或专门 real mode integration 任务。

## 2026-05-30 F1-007 AdjustPanel

### 已完成

- 新增 `docs/contracts/F1-007-adjust-panel.md`，明确本轮只交付前端 AdjustPanel、`PATCH /api/plan/{planId}/adjust` 调用入口、mock `adjust_result` 回放、3 次微调上限和 Chrome DevTools MCP 验收边界。
- 新增 `frontend/src/components/AdjustPanel.vue`，只在 `CONFIRM` 状态显示微调面板，包含 3 个快捷微调项、自由文本输入、提交按钮、次数进度、成功 / 错误状态和第 4 次阻止提示。
- 更新 `frontend/src/stores/planner.ts`，新增 `adjustCount`、`adjustLimit`、`isAdjusting`、`canAdjustPlan`、`adjustMessage`、`adjustErrorMessage` 和 `adjustPlan(instruction)`：
  - 空微调要求会被忽略并提示。
  - 单个 plan 前端最多提交 3 次微调。
  - real mode 调用 `plannerClient.adjustPlan(planId, { instruction })`，即 `PATCH /api/plan/{planId}/adjust`。
  - mock mode 播放 `CONFIRM` 后的 `ADJUST -> VALIDATE -> adjust_result` fixture 片段。
  - `adjust_result` 使用 `payload.plan` 更新 `currentPlan`，展示 `payload.summary`，并恢复到 `CONFIRM`。
- 更新 `frontend/src/App.vue`，把 AdjustPanel 接入侧栏、放在确认执行入口之前，并将首屏 F1 标记更新为 `F1-007`。
- 更新 `frontend/src/styles/main.css`，补齐 AdjustPanel 的布局、次数进度、快捷项、disabled / status 状态和 reduced-motion 处理。
- 更新 `docs/fixtures/sse-events.jsonl`，补齐 mock `ADJUST` / `VALIDATE` / `adjust_result` 事件，并让 `adjust_result.plan` 包含完整 timeline / actions，把餐厅槽位更新为“林间日式小食”。
- 更新 `frontend/scripts/verify-fixtures.mjs`，将 `adjust_result` 纳入 required SSE event types，防止后续 fixture 误删微调路径。
- 新增 evaluator QA 报告与截图：
  - `docs/qa/F1-007-adjust-panel.md`
  - `docs/qa/F1-007-devtools-mock.png`
  - `docs/qa/F1-007-generator-devtools-mock.png`
- `feature_list.json` 已将 `F1-007` 标记为 `verified`，并写入 generator 与 independent evaluator 证据。

### 验证记录

- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，覆盖：
  - `pnpm verify:fixtures passed`
  - `docs/fixtures/sse-events.jsonl -> 28 JSONL events`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 构建检查：`npm run build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 禁止字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator Chrome DevTools MCP 冒烟：
  - mock mode 下提交 plan，回答 ClarifyBubble 后进入 `CONFIRM`。
  - `AdjustPanel` 在 `CONFIRM` 状态出现，初始显示 `0 / 3`。
  - 提交一次“换一家餐厅，要能订位”后出现 `林间日式小食`，微调计数变为 `1 / 3`，Agent 回到 `CONFIRM`。
  - 连续提交到第 3 次后显示 `3 / 3` 和“已达最大微调次数”，快捷项、输入框、提交按钮均 disabled，确认执行按钮仍可用。
  - console error / warn 为 0，mock mode 下无真实 `/api/plan/*/adjust` fetch / xhr。
- Independent evaluator Reviewer (`019e77f0-f608-7e33-9dbf-0b2bcc94091a`) 已完成正式验收并放行：
  - 独立读取 contract / api contract / frontend contract / handoff / AdjustPanel / store / App / client / fixture verifier / SSE fixture。
  - 前端 fast verify 通过。
  - 禁止 snake_case 字段搜索无命中。
  - `feature_list.json` 可解析。
  - Chrome DevTools MCP browser path 通过：提交 mock plan -> 回答 `4-6小时` -> `CONFIRM` -> AdjustPanel -> 3 次微调 -> 3/3 上限锁定。
  - Chrome DevTools MCP snapshot / DOM / console / network / screenshot 证据完整。
  - 最终结论：`PASS`。

### 当前状态

- `F1-007` 已完成并 verified。
- 前端当前具备 InputPanel、useSSE、LogPanel、PlanCard、ConfirmButton、ExecutionTracker、ClarifyBubble 和 AdjustPanel。
- 下一个 F1 小目标建议推进 `F1-008`：Plan B highlight and error/degrade states；或进入 `INT-002` / `INT-003` 做完整 family / friends 端到端联调。

## 2026-05-29 WF-002 Chrome DevTools MCP only

### 已完成

- 将前端 UI 验收规则从“Playwright MCP + Chrome DevTools MCP 双工具强制”调整为“只强制 Chrome DevTools MCP，Playwright MCP 可作为补充证据”。
- 更新活跃 workflow / quality / frontend contract / 模板 / handoff / README / decision log / feature metadata，使后续前端 UI 任务的 `verified` 门槛只依赖 Chrome DevTools MCP 证据。
- 保留早期 F1 / INT / WF 的 Playwright MCP 记录作为历史验收事实；这些历史记录不再代表当前 workflow 硬规则。
- 本轮不修改前端业务代码、不调整 `verify.ps1`，也不要求具体前端任务重跑浏览器验收。

本轮新增或更新：

- `AGENTS.md`
- `README.md`
- `docs/dev-workflow.md`
- `docs/quality.md`
- `docs/frontend-contract.md`
- `docs/contracts/_template.md`
- `docs/qa/evaluator-template.md`
- `docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`
- `docs/contracts/DOC-001-root-readme.md`
- `docs/decision-log.md`
- `docs/handoff.md`
- `docs/initiallizer-agent-prompt.md`
- `frontend/F1-handoff.md`
- `feature_list.json`
- `progress.md`

### 验证记录

- Generator JSON 检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 活跃规则反向搜索：在 `AGENTS.md`、`README.md`、workflow、quality、frontend contract、模板、handoff、decision log、initializer prompt、DOC-001 contract 和 `feature_list.json` 中，未发现仍强制要求 Playwright MCP 与 Chrome DevTools MCP 同时作为当前验收门槛的活跃表述。
- Generator 正向搜索：活跃入口均已写明前端 UI evaluator 必须提供 Chrome DevTools MCP 证据，覆盖用户路径、交互、状态等待、页面快照、console、network、DOM / accessibility 或等价浏览器诊断。
- Independent evaluator 子代理 Parfit (`019e743f-9828-7191-9738-14491269e85d`) 完成只读复核并放行：
  - `feature_list.json` 可解析。
  - 活跃 workflow 规则已调整为前端 UI evaluator 必须使用 Chrome DevTools MCP。
  - 活跃规则没有继续把 Playwright MCP 作为必需条件。
  - 历史 QA、progress 和 feature evidence 中的 Playwright MCP 命中均为旧任务验收事实或旧规则回顾，不构成当前阻塞。
- QA 报告：`docs/qa/WF-002-chrome-devtools-only.md`。

### 当前状态

- `WF-002` 当前规则是：涉及前端 UI 的任务，evaluator 子代理必须使用 Chrome DevTools MCP；缺少 Chrome DevTools MCP 证据时不能标记为 `verified`。
- Playwright MCP 仍可作为补充证据，但不是必需项。
- `WF-002` 已保持 `verified`，并追加本次 Chrome DevTools MCP only 调整的 evaluator 证据。

## 2026-05-29 F1-006 ClarifyBubble

### 已完成

- 新增 `docs/contracts/F1-006-clarify-bubble.md`，明确本轮只交付前端 ClarifyBubble、clarify reply flow、mock fixture 暂停 / 恢复路径和验收边界，不抢做 AdjustPanel 或后端状态机扩展。
- 新增 `frontend/src/components/ClarifyBubble.vue`，基于 `clarification_request` 渲染一个反问气泡，展示 `question`、`field` 和 2-3 个快捷选项。
- 更新 `frontend/src/stores/planner.ts`，新增 `replyToClarification()`、`isClarifying`、`clarifyMessage` 和 mock clarify 后续回放逻辑：
  - mock planning 先停在 `clarification_request`
  - 用户点击选项后调用 `plannerClient.clarifyPlan(planId, { reply })`
  - mock mode 继续播放 clarification 之后到 `plan_ready` / `CONFIRM` 的 fixture 事件
  - real mode 成功后重新连接同一 `planId` 的 SSE stream
- 更新 `frontend/src/App.vue` 和 `frontend/src/styles/main.css`，将 ClarifyBubble 接入输入面板与方案卡之间，并补齐响应式布局、loading / disabled 状态和可访问按钮文本。
- 更新 `docs/fixtures/sse-events.jsonl`，将 mock fixture 调整为 `INTENT -> CLARIFY -> clarification_request` 后暂停，回答后继续到 `SKELETON / RECALL / VALIDATE / plan_ready / CONFIRM`。
- 更新 `frontend/scripts/verify-fixtures.mjs`，将 `clarification_request` 纳入 required SSE types，并校验 `question`、`field` 和 2-3 个 options。
- 新增 QA 报告 `docs/qa/F1-006-clarify-bubble.md`，并保存 evaluator 截图证据：
  - `docs/qa/F1-006-playwright-mock.png`
  - `docs/qa/F1-006-devtools-mock.png`
- `feature_list.json` 已将 `F1-006` 标记为 `verified`，并写入 generator 与 independent evaluator 证据。

### 验证记录

- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，覆盖：
  - `pnpm verify:fixtures passed`
  - `docs/fixtures/sse-events.jsonl -> 26 JSONL events`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 构建检查：`npm run build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 禁止字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator 浏览器冒烟：
  - Playwright MCP 确认 mock mode 下只出现 1 个 ClarifyBubble，包含“请问大概想玩几个小时？”和 `3-4小时`、`4-6小时`、`6小时以上` 3 个选项；点击 `4-6小时` 后进入 `CONFIRM`，方案卡出现，确认执行按钮可用。
  - Chrome DevTools MCP 确认 accessibility snapshot 包含 `region "需要确认"`、问题和 3 个回答按钮；console error / warn 为 0；mock mode 未访问真实 `/api/plan/*/clarify`。
- Independent evaluator 子代理 Dewey (`019e7418-6911-76c1-888c-b41bc448e5a2`) 已完成验收并放行：
  - 前端 fast verify 通过
  - 禁止 snake_case 字段搜索无命中
  - `feature_list.json` 可解析
  - Playwright MCP mock 交互通过
  - Chrome DevTools MCP snapshot / console / network 通过
  - 最终结论：`PASS`

### 当前状态

- `F1-006` 已完成并 verified。
- 前端当前具备 InputPanel、useSSE、LogPanel、PlanCard、ConfirmButton、ExecutionTracker 和 ClarifyBubble。
- 本轮未做 `F1-007` AdjustPanel；后续可继续推进 `F1-007`，或在 integration 任务中补 real mode `/api/plan/{planId}/clarify` 浏览器网络证据。

## 2026-05-28 B1-006 CLARIFY and ADJUST states

### 已完成

- 新增 `docs/contracts/B1-006-clarify-adjust.md`，明确本轮只交付后端 `CLARIFY` / `ADJUST` 正式链路，不抢做前端 ClarifyBubble / AdjustPanel UI，也不扩展到完整 `EXECUTE / DONE`。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanState.java`，补齐 `CLARIFY`、`CONFIRM`、`ADJUST`、`EXECUTE`、`DONE`、`FAILED` 等状态枚举，使后端状态范围与 `docs/api-contract.md` 对齐。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`，新增 `currentState`、`clarifyCount`、`adjustCount`、`pendingClarification`、`selectionSnapshot`、`pendingAdjustInstruction` 等运行态字段，支持单 plan 内存态暂停与恢复。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/PendingClarification.java` 和 `PlanSelectionSnapshot.java`，承接 clarify 问题和局部 adjust 的候选快照。
- 更新 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`，新增 `POST /api/plan/{planId}/clarify` 与 `PATCH /api/plan/{planId}/adjust`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/api/ClarifyPlanRequest.java`、`ClarifyPlanResponse.java`、`AdjustPlanRequest.java`、`AdjustPlanResponse.java`，对齐前端既有 typed client 和 `docs/api-contract.md`。
- 更新 `backend/src/main/java/com/weekendtravel/backend/api/ApiExceptionHandler.java`，新增 `409 INVALID_STATE` 与 `429 ADJUST_LIMIT_EXCEEDED` 错误映射；并新增 `InvalidPlanStateException.java`、`AdjustLimitExceededException.java`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/sse/ClarificationRequestEvent.java` 与 `AdjustResultEvent.java`，补齐后端 `clarification_request` / `adjust_result` SSE payload 模型。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`，将原 one-shot 规划流程改为可恢复的内存态状态机：
  - 低置信度输入可进入 `INTENT -> CLARIFY`
  - `clarification_request` 只问一个问题，提供 3 个 options
  - `POST /clarify` 后可恢复到 `INTENT` 并继续推进到 `plan_ready` / `CONFIRM`
  - `CONFIRM -> ADJUST -> VALIDATE` 后发送 `adjust_result`
  - 单个 plan 的 adjust 次数上限为 3
  - `adjust_result.summary` 已与最终最新 `plan` 对齐
- 更新 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`，补充 clarify invalid state、adjust invalid state、第四次 adjust 返回 `429 ADJUST_LIMIT_EXCEEDED` 的覆盖。
- 更新 `backend/src/test/java/com/weekendtravel/backend/controller/PlanFlowIntegrationTests.java`，补充 ambiguous input -> `CLARIFY` -> `POST /clarify` -> resume，以及 `PATCH /adjust` -> `adjust_result` -> `CONFIRM` 的端到端覆盖。
- 新增并完善 QA 记录 `docs/qa/B1-006-clarify-adjust.md`，整理 generator 与 independent evaluator 的验证证据。

### 验证记录

- Generator 后端测试：`./backend/mvnw -f backend/pom.xml test` 通过，`Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- Generator 后端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./verify.ps1 -Target backend -Mode fast` 通过，输出 `[ok] mvnw.cmd test passed.` 和 `Verify passed.`。
- Generator 运行态复核（fresh 实例 `8002`）：
  - 明确时长 family 输入可达 `plan_ready` / `CONFIRM`
  - 模糊输入先进入 `CLARIFY` 并发出 `clarification_request`
  - `POST /clarify` 返回 `processing`，随后恢复到 `plan_ready` / `CONFIRM`
  - `PATCH /adjust` 返回 `adjusting`，随后 stream 发出 `adjust_result` 且 `affectedSlots=["restaurant"]`
  - `adjust_result.summary` 与最终 `plan.timeline[1].title` 一致
- Independent evaluator 子代理完成只读 + 运行态验收并放行：
  - 父工作树后端测试：`D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test` 通过
  - 父工作树 fast verify：`cd "D:/Users/lenovo/Desktop/WeekendTravel" && powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:/Users/lenovo/Desktop/WeekendTravel/verify.ps1" -Target backend -Mode fast` 通过
  - fresh 实例 `8002` 上确认 ambiguous input -> `CLARIFY` -> `clarification_request` -> `POST /clarify` -> `plan_ready` / `CONFIRM`
  - fresh 实例 `8002` 上确认 `PATCH /adjust` 发出 `adjust_result`，`summary` 与最终 `plan` 一致，且字段保持 camelCase、事件名与 `type` 一致
- 独立 evaluator 最终结论为 `PASS`；runtime 证据已写入 `docs/qa/B1-006-clarify-adjust.md`。

### 当前状态

- `B1-006` 已完成并 verified。
- 后端当前已支持低置信度输入进入 `CLARIFY`，以及 `POST /clarify` 后恢复到正式规划链路。
- 后端当前已支持在 `CONFIRM` 状态下执行最多 3 次局部 `ADJUST`，并通过 `adjust_result` 返回最新完整 `plan`。
- 前端 ClarifyBubble / AdjustPanel UI 仍留给后续 F1 任务；本轮不把 UI 缺失伪装成前端已完成。

## 2026-05-28 B1-005 Plan B and DEGRADE rules

### 已完成

- 新增 `docs/contracts/B1-005-plan-b-degrade.md`，明确本轮在 `B1-004` 基础上只补 family demo 的 deterministic `Plan B / DEGRADE` 行为，不抢做 friends 通用规划、`CLARIFY / ADJUST / EXECUTE` 或真实外部服务。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanState.java`，补充 `DEGRADE` 状态以对齐 `docs/api-contract.md`。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`，新增 `injectedPlanB`、`latestReason` 等上下文字段，用于承接重排次数、原因和后续指标区分。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`，将单次 fallback 改为 bounded replan 流程；family happy path 仍可进入 `PACK`，`restaurantFull` / `routeTooFar` 可触发 `replan`，达到上限后进入 `DEGRADE` 并发送 `error(code=DEGRADE)`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/sse/ErrorEvent.java`，补齐后端 `error` SSE payload 模型。
- 更新 `backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`，在发送 heartbeat 前先校验 `planId`，避免非法 stream 请求先写出 SSE 再异常中断。
- 更新 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java`，补充 `plan_ready` 中 `isPlanB` / `replanCount` 字段断言。
- 更新 `backend/src/test/java/com/weekendtravel/backend/controller/PlanFlowIntegrationTests.java`，覆盖 happy path、`restaurantFull` 注入、`routeTooFar` 注入，以及 `friends` 场景最小文案 / 输出回归。
- 根据最终 review 结果，补齐了 `friends` 场景的最小搜索参数 / 文案分流，并修复了非法 `planId` stream 先发 heartbeat 的问题。
- 新增并完善 QA 记录 `docs/qa/B1-005-plan-b-degrade.md`，整理 generator 与 independent evaluator 的验证证据。

### 验证记录

- Generator 后端测试：`./backend/mvnw -f backend/pom.xml test` 通过，`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- Generator 后端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，输出 `[ok] mvnw.cmd test passed.` 和 `Verify passed.`。
- Generator 代码复核：补文档前对本轮差异做了精度 review，并据此修正了 replan 上限判断与 degrade 文案计数不一致的问题；最终 review 后又补齐了 friends 场景最小分流与非法 stream `planId` 先校验。
- Independent evaluator 子代理完成只读验收并放行：
  - 启动后端：`backend/mvnw -f backend/pom.xml spring-boot:run`
  - 健康检查：`curl -sS -D - http://127.0.0.1:8000/health` 返回 `200` 与 `{"status":"ok","service":"WeekendTravel",...}`
  - 后端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过
  - 后端测试：`backend/mvnw -f backend/pom.xml test` 通过，原始独立验收时为 `39 tests, 0 failures`；本轮最终修正后 generator 复跑为 `40 tests, 0 failures`
  - happy path create+stream 观察到 `PACK` 与 `plan_ready`
  - `restaurantFull=true` 观察到 `replan`、`REPLAN`、`DEGRADE`、`error(code=DEGRADE)`
  - `routeTooFar=true` 观察到 `replan`、`REPLAN`、`DEGRADE`、`error(code=DEGRADE)`
  - `tool_result` 运行时 payload 含 `latencyMs`
- 独立 evaluator 结论为 `PASS with caveat`；runtime 证据已写入 `docs/qa/B1-005-plan-b-degrade.md`。

### 当前状态

- `B1-005` 已完成并 verified。
- family demo happy path 可到达 `PACK` / `plan_ready`；`restaurantFull` 和 `routeTooFar` 注入会进入 `replan`，达到上限后进入 `DEGRADE` 并返回可读错误。
- 当前实现已补齐 friends 场景的最小文案 / 搜索参数分流，并修复了非法 `planId` stream 先发 heartbeat 的问题。
- 更复杂的 friends 规则策略、execute/clarify/adjust 正式链路仍留给后续任务。

## 2026-05-27 DOC-001 Root README

### 已完成

- 新增根目录 `README.md`，作为 WeekendTravel 的项目入口文档。
- README 覆盖项目目标、`family` / `friends` Demo 范围、前后端技术栈、默认端口、目录结构、初始化检查、依赖安装、本地运行、mock / real mode 切换和仓库级验证命令。
- README 指向 `docs/api-contract.md`、`feature_list.json`、`progress.md`、`docs/handoff.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md` 和常用长期文档入口。
- README 明确当前能力状态以 `feature_list.json` 和 evaluator 证据为准；`B1-004` 仍不标记为已完成，只说明源码中已有相关实现和测试文件。
- 新增 `docs/contracts/DOC-001-root-readme.md`，记录本轮 README 验收边界。
- 新增 `docs/qa/DOC-001-root-readme.md`，整理独立 evaluator 子代理 CodeChecker 的只读 PASS 结论。
- 更新 `feature_list.json`，新增 `DOC-001` 并按 evaluator 证据标记为 `verified`。

### 验证记录

- Generator 本地检查：
  - `rg -n "^# WeekendTravel|^## 当前状态|verify\.ps1|VITE_API_MODE|docs/api-contract\.md|evaluator|Playwright MCP|Chrome DevTools MCP" README.md` 命中关键标题、命令和规则。
  - `Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json` 通过，`feature_list.json` 可解析。
- 独立 evaluator 子代理 CodeChecker (`019e69ae-e5fe-7c13-bbab-be7d49d8a6a4`) 完成只读复核并放行：
  - 确认 `README.md` 存在。
  - 确认 README 覆盖项目目标、Demo 范围、技术栈、端口、目录结构、初始化 / 安装 / 运行 / 验证命令。
  - 确认 README 正确引用关键入口文档。
  - 确认 README 没有把 `B1-004` 或其他未 verified 条目写成已验证完成。
  - 确认 README 写明 camelCase、端口边界、`OPENAI_API_KEY`、evaluator 子代理、前端 Playwright MCP + Chrome DevTools MCP 验收规则。
  - 确认 `feature_list.json` 可解析。
- QA 报告：`docs/qa/DOC-001-root-readme.md`。

### 当前状态

- `DOC-001` 已完成并 verified。
- 本轮不涉及前端 UI、后端 API 或业务代码变更。
- `B1-004`、`F1-005`、`INT-*` 等原有未完成条目状态不变，仍以后续各自 contract 和 evaluator 证据推进。
## 2026-05-27 F1-005 PlanCard, ConfirmButton, and ExecutionTracker

### 已完成

- 新增 `docs/contracts/F1-005-plan-card-execution.md`，明确本轮只交付 PlanCard、ConfirmButton、ExecutionTracker 和确认执行前端链路，不抢做 ClarifyBubble、AdjustPanel、后端状态机或真实执行事件。
- 新增 `frontend/src/components/PlanCard.vue`，基于 `plan_ready` / `currentPlan` 展示 summary、Plan B 原因、总时长、重排次数、Plan ID、timeline、actions 和 shareMessage。
- 新增 `frontend/src/components/ConfirmButton.vue`，只在 `agentState === CONFIRM` 且已有 plan 时允许确认；执行中 loading，完成或非确认状态禁用。
- 新增 `frontend/src/components/ExecutionTracker.vue`，按 action 展示 pending / executing / success / failed / manual 状态和 `confirmationNo`。
- 更新 `frontend/src/stores/planner.ts`，新增 `confirmPlanExecution()`、`canConfirmPlan`、`isExecuting` 和 `executeMessage`；mock planning 回放会停在 `CONFIRM`，用户点击确认后再播放 fixture 中的 `execute_result` / `done`。
- 更新 `frontend/src/App.vue` 和 `frontend/src/styles/main.css`，将 F1-005 组件接入工作台，并补齐响应式布局、语义区域、状态条和低动效支持。
- 更新 `docs/fixtures/sse-events.jsonl`，补齐第三个 fixture action 的 `execute_result`，总事件数变为 24。
- 新增独立 evaluator QA 报告 `docs/qa/F1-005-plan-card-execution.md` 和截图证据 `docs/qa/F1-005-playwright-mock.png`、`docs/qa/F1-005-devtools-mock.png`。
- 在 `feature_list.json` 将 `F1-005` 标记为 `verified`，并写入 generator 检查和独立 evaluator 证据。

### 验证记录

- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，覆盖 `pnpm verify:fixtures`、`24 JSONL events` 和 `pnpm typecheck passed`。
- Generator 构建检查：`npm run build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 禁止字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator 浏览器冒烟：Playwright MCP 和 Chrome DevTools MCP 均确认 mock 页面能停在 `CONFIRM`，点击“确认执行”后 `ExecutionTracker` 显示 `3 / 3`，并出现 `MOCK-TBL-88421`、`MOCK-NOTE-122`、`MOCK-MSG-309`；console error / warn 为 0，mock mode 没有真实后端 fetch / xhr。
- 独立 evaluator 子代理 Copernicus (`019e693f-998a-7d03-8476-9c96e93288cb`) 已完成验收并放行。
- Evaluator 独立运行前端 fast verify 通过；禁止 snake_case 字段搜索无命中；`feature_list.json` 可解析。
- Evaluator 使用 Playwright MCP 验证：初始 `START` 下确认按钮 disabled；提交后进入 `CONFIRM`，PlanCard 展示 summary、timeline、actions、shareMessage、总时长和 Plan B；点击确认后 ExecutionTracker 显示 `3 / 3` 和三个 mock 确认号。
- Evaluator 使用 Chrome DevTools MCP 验证：snapshot / accessibility 覆盖 PlanCard、确认按钮和 ExecutionTracker；console error / warn 为 0；mock mode 下没有真实 `/api/plan/*/execute` fetch / xhr。
- QA 报告：`docs/qa/F1-005-plan-card-execution.md`。
- 截图证据：`docs/qa/F1-005-playwright-mock.png`、`docs/qa/F1-005-devtools-mock.png`。

### 当前状态

- `F1-005` 已完成并 verified。
- 前端当前具备正式输入提交、mock fixture SSE planning 回放、PlanCard、确认执行和 ExecutionTracker。
- real mode 当前仍取决于后端后续 B1 状态机 / execute endpoint；F1-005 对 real mode 的职责是按 contract 调用 `POST /api/plan/{planId}/execute` 并显示错误或后续 SSE 结果，不伪造真实后端完成。
- 下一步 F1 小目标可继续推进 `F1-006` ClarifyBubble 或 `F1-007` AdjustPanel；如果要做完整端到端 family path，则需要先推进 `B1-004` / `B1-005` 后端状态机与 plan_ready 链路。

## 2026-05-27 INT-001 Sprint 1 integration: one input to SSE state_change

### 已完成

- 新增 `docs/contracts/INT-001-one-input-sse.md`，明确本轮只交付 Sprint 1 最小真实联调：一句 Demo 输入 -> `POST /api/plan` -> 返回 `planId` -> 连接 `/api/plan/{planId}/stream` -> 前端日志渲染真实后端 `state_change START -> INTENT`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerIntegrationTests.java`，用真实 HTTP 先创建 plan，再用返回的 `planId` 打开 SSE stream，断言 `heartbeat`、`state_change`、同一个 `planId`、`START` 和 `INTENT`。
- 保持实现范围不越界：没有实现完整 B1 状态机、`plan_ready`、PlanCard、ConfirmButton、ExecutionTracker、ClarifyBubble、AdjustPanel，也没有改 API 字段或接入 OpenAI。
- 新增独立 evaluator QA 报告 `docs/qa/INT-001-one-input-sse.md`。
- 在 `feature_list.json` 将 `INT-001` 标记为 `verified`，并写入 generator 检查和独立 evaluator 证据。

### 验证记录

- Generator 后端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过；后端总计 36 tests、0 failures、`BUILD SUCCESS`，新增 `PlanControllerIntegrationTests.createdPlanCanOpenStreamAndReceiveStateChangeForSamePlanId` 已执行。
- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过；`pnpm verify:fixtures passed`，`pnpm typecheck passed`。
- 独立 evaluator 子代理 Franklin (`019e691e-822d-7403-8ca3-df84e5f280c3`, `INT-001-EVAL-CODEX-20260527T1915+0800`) 已完成验收并放行。
- Evaluator 独立运行后端 fast verify 通过：`Verify passed.`、`BUILD SUCCESS`、36 tests、0 failures。
- Evaluator 独立运行前端 fast verify 通过：`pnpm verify:fixtures passed`、`pnpm typecheck passed`、`Verify passed.`。
- Evaluator 使用 Playwright MCP 验证真实用户路径：打开 `http://127.0.0.1:5173/`，页面显示 real mode，保留默认家庭 Demo 文本并点击“提交规划”；页面出现后端 `planId=plan_5c63071dd52c`，Agent 显示 `INTENT`，LogPanel 可见 `heartbeat`、`state_change` 和 `START -> INTENT`。
- Evaluator 使用 Chrome DevTools MCP 复核：snapshot / accessibility 覆盖 InputPanel、Pinia 状态和 LogPanel；console error / warn 为 0；network 包含 `POST http://localhost:8000/api/plan [202]` 和 `GET http://localhost:8000/api/plan/plan_0cfb9dd63714/stream [200]`，SSE 响应体包含同一 `planId` 的 `heartbeat` 与 `state_change START -> INTENT`。
- QA 报告：`docs/qa/INT-001-one-input-sse.md`。

### 当前状态

- `INT-001` 已完成并 verified。
- Sprint 1 最小真实联调链路已经成立：前端 real mode 可以提交一句 Demo 输入，后端返回 `planId`，前端用该 `planId` 连接 SSE 并渲染真实后端 `state_change`。
- 当前后端最小 SSE stream 会在发送必需事件后 complete，浏览器 EventSource 因此进入 `retrying` 并可能重复显示 `heartbeat` / `state_change`；这不阻塞 INT-001，完整状态机和长流行为留给后续 `B1-004` / `INT-002` / `INT-003`。
- 下一步集成建议仍是先推进 `B1-004` 状态机 START -> PACK，再接 `F1-005` / `INT-002` 的 plan_ready 和执行路径。

## 2026-05-26 F1-004 InputPanel and POST /api/plan

### 已完成

- 新增 `docs/contracts/F1-004-input-plan-api.md`，明确本轮只交付正式 `InputPanel`、提交规划入口、real mode `POST /api/plan`、创建后连接既有 `useSSE`，不抢做 F1-005 PlanCard / ConfirmButton / ExecutionTracker、F1-006 ClarifyBubble 或 F1-007 AdjustPanel。
- 新增 `frontend/src/components/InputPanel.vue`，包含家庭 / 朋友场景选择、自然语言输入、可选出发位置、提交按钮和提交状态提示；空输入或运行中禁用提交。
- 更新 `frontend/src/stores/planner.ts`，新增 `submitPlan()`、`submitMessage`、`isSubmitting` 和 `canSubmit`；提交时清理上一轮状态，调用 `plannerClient.createPlan()`，读取 camelCase `planId` / `status`，追加 client log，并将 `planId` 交给 `sse.connect()`。
- 更新 `frontend/src/App.vue`，将首页标识切换为 `F1-004`，用 `InputPanel` 替代上一轮内联“开始预演”入口，保留 Pinia 状态摘要和正式 LogPanel。
- 更新 `frontend/src/composables/useSSE.ts`，real mode 下后端最小 SSE stream 完成后的 EventSource retry 不再被渲染为错误；空 message / `undefined` message 会被忽略，避免最小占位流导致误报。
- 更新 `frontend/src/styles/main.css`，补齐 `InputPanel` 表单和状态提示的基础布局。
- 更新 `backend/src/main/java/com/weekendtravel/backend/config/WebConfig.java`，允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 两个前端 origin；这是 F1-004 real mode 浏览器联调所需的最小 CORS 修正。
- 更新 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`，新增两个前端 origin 的 POST /api/plan CORS 回归测试。
- 新增 QA 报告 `docs/qa/F1-004-input-plan-api.md` 和截图证据 `docs/qa/F1-004-playwright-mock.png`、`docs/qa/F1-004-playwright-real.png`、`docs/qa/F1-004-playwright-real-127.png`、`docs/qa/F1-004-devtools-mock.png`、`docs/qa/F1-004-devtools-real.png`。
- 在 `feature_list.json` 将 `F1-004` 标记为 `verified`，并写入 generator 检查和独立 evaluator 证据。

### 验证记录

- Generator 前端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，覆盖：
  - `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `docs/fixtures/sse-events.jsonl -> 23 JSONL events`
  - `pnpm verify:fixtures passed`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 前端构建检查：`pnpm build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 后端 fast verify：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，后端 35 tests、0 failures、`BUILD SUCCESS`。
- Generator 字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator mock 浏览器冒烟：Playwright 打开 `http://127.0.0.1:5173`，点击“提交规划”，确认 `InputPanel` 可见、`plan_family_fixture` 可见、日志 25 条、`role=log`、自动滚动到底部。
- Generator real mode 冒烟：后端启动在 `8000`，前端以 `VITE_API_MODE=real` 启动；浏览器从 `http://localhost:5173` 提交后捕获 `POST http://localhost:8000/api/plan => 202`，页面显示后端 `planId`、`heartbeat` 和 `START -> INTENT`。
- Generator CORS 回归：`Origin: http://127.0.0.1:5173` 请求 `/health` 时响应头包含 `Access-Control-Allow-Origin: http://127.0.0.1:5173`。
- 独立 evaluator 子代理 Gauss (`019e64d9-95ea-76a2-9f4d-53478e6a64a6`) 已完成验收并放行。
- Evaluator 独立运行前端 fast verify 通过；补充运行后端 fast verify 通过，35 tests、0 failures、`BUILD SUCCESS`；禁止字段搜索无命中；`feature_list.json` 可解析。
- Evaluator 使用 Playwright MCP 验证：
  - mock mode：`InputPanel`、提交、`plan_family_fixture`、25 条日志、自动滚动到底部。
  - real mode：`localhost:5173` 和 `127.0.0.1:5173` 都能 POST `/api/plan` 返回 202，页面显示后端 `planId`，并可见 `heartbeat` / `START -> INTENT`。
- Evaluator 使用 Chrome DevTools MCP 验证：
  - mock mode：snapshot / accessibility 覆盖 InputPanel 和 LogPanel；console 无 error / warn；network 没有真实 `/api/plan`。
  - real mode：snapshot / accessibility 覆盖 real InputPanel；console 无 error / warn；network 包含 `POST http://localhost:8000/api/plan [202]` 和 SSE stream 请求。
- QA 报告：`docs/qa/F1-004-input-plan-api.md`。
- 截图证据：
  - `docs/qa/F1-004-playwright-mock.png`
  - `docs/qa/F1-004-playwright-real.png`
  - `docs/qa/F1-004-playwright-real-127.png`
  - `docs/qa/F1-004-devtools-mock.png`
  - `docs/qa/F1-004-devtools-real.png`

### 当前状态

- `F1-004` 已完成并 verified。
- 前端当前具备正式 `InputPanel`、mock fixture 提交回放、real mode `POST /api/plan` 提交和创建后 SSE 连接。
- 后端最小 CORS 已支持 `localhost:5173` 与 `127.0.0.1:5173` 两个前端开发 origin。
- real mode 目前仍只接入后端最小 SSE 占位流，所以会看到 `retrying` 和重复 `heartbeat` / `START -> INTENT`；这不影响 F1-004，完整状态机流转留给后续 B1 / INT 任务。
- 下一步 F1 小目标建议推进 `F1-005`：PlanCard, ConfirmButton, and ExecutionTracker。

## 2026-05-26 F1-003 useSSE composable and log panel

### 已完成

- 新增 `docs/contracts/F1-003-sse-log-panel.md`，明确本轮只交付 `useSSE` composable、实时 LogPanel、fixture SSE 回放、real mode EventSource 连接入口和 Pinia 状态分发，不抢做 F1-004 输入链路、F1-005 PlanCard / ConfirmButton / ExecutionTracker、F1-006 ClarifyBubble 或 F1-007 AdjustPanel。
- 新增 `frontend/src/composables/useSSE.ts`，统一表达 `idle / connecting / open / retrying / closed / error` 连接状态；mock mode 逐条消费 `getSseFixtureFrames()`，real mode 通过 `PlannerApiClient.openPlanStream(planId)` 连接 `/api/plan/{planId}/stream`。
- 新增 `frontend/src/components/LogPanel.vue`，使用 `role="log"`、`aria-live="polite"` 和固定滚动容器渲染 SSE 事件；新增事件后自动滚动到底部，`replan` / Plan B 与 `error` / `DEGRADE` 有明显视觉区分。
- 更新 `frontend/src/stores/planner.ts`，将现有 fixture 日志预览升级为逐条 SSE payload 分发；`state_change` 更新 `agentState`，`plan_ready` / `adjust_result` 更新 `currentPlan`，`clarification_request` 更新 pending clarification，`execute_result` 更新 action 状态，`done` 进入 `DONE`，`error(code=DEGRADE)` 进入 `DEGRADE`。
- 更新 `frontend/src/App.vue`，将页面标识改为 `F1-003`，接入正式 LogPanel，并在状态摘要里展示 mode、Plan B、clarification 和 error / degrade 信息。
- 更新 `frontend/src/styles/main.css`，补齐 LogPanel 布局、固定滚动区域、事件行、Plan B / error 视觉状态、移动端布局和 `prefers-reduced-motion` 支持。
- 新增 QA 报告 `docs/qa/F1-003-sse-log-panel.md` 和截图证据 `docs/qa/F1-003-playwright-log-panel.png`、`docs/qa/F1-003-devtools-log-panel.png`。
- 在 `feature_list.json` 将 `F1-003` 标记为 `verified`，并写入 generator 检查和独立 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，输出覆盖：
  - `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `docs/fixtures/sse-events.jsonl -> 23 JSONL events`
  - `pnpm verify:fixtures passed`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 额外构建检查：`npm run build` 在 `frontend/` 下通过，Vite production build 成功。
- Generator 字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- Generator 浏览器冒烟：Playwright 打开 `http://127.0.0.1:5173`，点击“开始预演”，等待 `DEGRADE`，确认 `eventCount=25`、末尾事件为 `error / DEGRADE`、`done` 和 `plan_ready` 可见、日志容器 `atBottom=true`。
- Generator Chrome DevTools MCP 冒烟：snapshot 显示 `DEGRADE`、`closed`、`plan_family_fixture` 和 `log "实时日志面板"`；console error / warn 为 0；network 仅包含前端模块和 fixture raw import。
- 独立 evaluator 子代理 Rawls (`019e64a8-1b72-7722-84e4-57b5364a252f`) 已完成验收并放行。
- Evaluator 独立运行前端 fast verify 通过，并确认禁止字段搜索无命中、`feature_list.json` 可解析。
- Evaluator 使用 Playwright MCP 打开页面并点击“开始预演”，确认 LogPanel `role=log`、`ariaLive=polite`、`eventCount=25`、包含 `done` / `error` / `DEGRADE`、自动滚动到底部。
- Evaluator 使用 Chrome DevTools MCP 复核 snapshot / accessibility、console 和 network：页面含 `log "实时日志面板" live="polite"`；console 无消息；mock mode 下没有访问 `/api/plan/*/stream`，只加载三份 fixture raw import。
- QA 报告：`docs/qa/F1-003-sse-log-panel.md`。
- 截图证据：`docs/qa/F1-003-playwright-log-panel.png`、`docs/qa/F1-003-devtools-log-panel.png`。

### 当前状态

- `F1-003` 已完成并 verified。
- 前端当前具备 `useSSE`、mock fixture 逐条日志回放、real mode SSE 连接入口、LogPanel 自动滚动和基础 SSE payload 状态分发。
- 下一步 F1 小目标建议推进 `F1-004`：InputPanel and POST `/api/plan`，把当前“开始预演”入口整理成正式输入提交链路；也可在后续 `F1-005` 基于 `plan_ready` 继续落 PlanCard、ConfirmButton 和 ExecutionTracker。

## 2026-05-26 B1-003 POST /api/plan

### 已完成

- 新增 `docs/contracts/B1-003-create-plan.md`，明确本轮只交付最小 `POST /api/plan` 占位接口，不抢做真实 Planner、状态机或完整 `Plan` 返回。
- 在 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java` 上补 `POST /api/plan`，返回 HTTP `202` 和最小 `{ planId, status }` 响应，同时保留 B1-002 的 SSE stream 端点。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/PlanCreateService.java`，负责 `text` / `scenario` / 可选 `origin` 的最小归一化与校验，并生成带 `plan_` 前缀的占位 `planId`，固定返回 `status=processing`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanRequest.java` 与 `CreatePlanResponse.java`，字段保持 camelCase，对齐正式线缆契约。
- 新增 `backend/src/main/java/com/weekendtravel/backend/api/ApiErrorResponse.java` 与 `ApiExceptionHandler.java`，为 `IllegalArgumentException` 和请求体不可读场景提供统一 `400 INVALID_INPUT` 错误结构，并在 `... is required` 场景提取 `details.field`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`，使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JDK `HttpClient` 对真实 HTTP `POST /api/plan` 做集成测试。
- 在 `feature_list.json` 将 `B1-003` 标记为 `verified`，并补入 contract、测试、generator 开发侧准备检查和 evaluator 证据。
- 新增 QA 报告 `docs/qa/B1-003-create-plan.md`，记录独立 evaluator 放行结论。
- 更新 `backend/HANDOFF.md`，把最小 `POST /api/plan` 占位接口同步为已完成。

### 验证记录

- Generator 开发侧准备检查：`./backend/mvnw -f "backend/pom.xml" test` 通过，后端测试合计 34 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`PlanControllerCreateTests` 6 个测试、全后端 34 个测试均通过，`Verify passed.`。
- 独立 evaluator：`B1-003-EVAL-gpt-5.4-20260526` 已完成验收并放行。
- Evaluator 以父工作树绝对路径复核 `PlanController`、`PlanCreateService`、`CreatePlanRequest`、`CreatePlanResponse`、`ApiErrorResponse`、`ApiExceptionHandler`、`PlanControllerCreateTests` 和 `feature_list.json`，避免隔离 worktree 看不到未提交改动的问题。
- Evaluator 独立运行：
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml -Dtest=PlanControllerCreateTests test`
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test`
  均 `BUILD SUCCESS`，确认 `POST /api/plan` 的 `202` 成功响应、camelCase `planId`/`status`、可选 `origin` 以及统一错误 shape。
- QA 报告：`docs/qa/B1-003-create-plan.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B1-003` 已完成并 verified。
- 后端当前已具备 `POST /api/plan` 最小建单入口和 B1-002 的 SSE stream 入口，可继续推进 `B1-004` 状态机接入。


## 2026-05-26 B1-002 SSE emitter and heartbeat

### 已完成

- 新增 `docs/contracts/B1-002-sse-emitter.md`，明确本轮只交付 `GET /api/plan/{planId}/stream` 的最小 SSE 链路，不引入 Spring AI、WebFlux 或 Reactor Flux。
- 在 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java` 增加 `GET /api/plan/{planId}/stream`，返回 `text/event-stream`，并显式设置 `Cache-Control: no-cache` 与 `Connection: keep-alive`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`，使用 Spring MVC `SseEmitter` 建立最小 stream，按顺序发送 `heartbeat` 和 mock `state_change`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/sse/HeartbeatEvent.java` 与 `StateChangeEvent.java`，字段使用 camelCase；`state_change` 的最小 mock 转移固定为 `START -> INTENT`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java`，使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JDK `HttpClient` 对真实 HTTP SSE 端点做集成测试。
- 在 `feature_list.json` 将 `B1-002` 标记为 `verified`，并补入 contract、测试、generator 开发侧准备检查和 evaluator 证据。
- 新增 QA 报告 `docs/qa/B1-002-sse-emitter.md`，记录独立 evaluator 放行结论。
- 更新 `backend/HANDOFF.md`，将最小 SSE stream、`heartbeat`、`state_change`、事件名与 `type` 一致性以及 SSE 测试项同步为已完成。

### 验证记录

- Generator 开发侧准备检查：`./backend/mvnw -f "backend/pom.xml" test` 通过，后端测试合计 28 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`PlanControllerStreamTests` 1 个测试、全后端 28 个测试均通过，`Verify passed.`。
- 独立 evaluator：`B1-002-EVAL-gpt-5.4-20260526` 已完成验收并放行。
- Evaluator 以父工作树绝对路径复核 `PlanController`、`PlanStreamService`、`HeartbeatEvent`、`StateChangeEvent`、`PlanControllerStreamTests` 和 `feature_list.json`，避免隔离 worktree 看不到未提交改动的问题。
- Evaluator 独立运行：
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml -Dtest=PlanControllerStreamTests test`
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test`
  均 `BUILD SUCCESS`，确认端点路径、`text/event-stream`、`heartbeat`、mock `state_change`、`START -> INTENT` 和事件名与 `data.type` 一致。
- QA 报告：`docs/qa/B1-002-sse-emitter.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B1-002` 已完成并 verified。
- 后端最小 SSE 占位链路已具备，可供后续 `B1-003` `POST /api/plan` 与 `B1-004` 状态机继续接入。


## 2026-05-24 B2-005 MessageTool / composeShareMessage

### 已完成

- 新增 `docs/contracts/B2-005-message-tool.md`，明确本轮只交付 B2 Tool 层 `MessageTool / composeShareMessage`，不抢做 B1 `Plan`、Planner、状态机、SSE、`POST /api/plan` 或真实 LLM 接入。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageRequest.java`、`MessageResult.java`、`MessagePlanPayload.java`、`MessageTimeSlot.java`、`MessageActionSummary.java` 和 `MessageTool.java`。
- `MessageTool.composeShareMessage()` 会根据已定稿的 `timeline` 和 `actions` 生成 deterministic fallback 中文 `shareMessage`，`execute()` 作为兼容入口委托到同一逻辑。
- `MessageResult` 固定返回 `llmUsed=false` 和 `templateVersion=fallback-v1`，本轮不读取 `OPENAI_API_KEY`、不调用 OpenAI/LLM、不进行网络访问。
- `MessageResult.plan` 返回 Plan-compatible payload，包含 `planId`、`scenario`、`status`、`isPlanB`、`planBReason`、`summary`、`timeline`、`actions`、`shareMessage`、`totalDurationHours`、`replanCount`、`createdAt`，后续 B1 可把其中 `shareMessage` 合入正式 `Plan.shareMessage`。
- 家庭场景文案突出亲子、低负担饮食或少步行；朋友场景文案突出玩、吃、拍照和聊天；`isPlanB=true` 时透明输出 `Plan B 说明`。
- 时间线按正数 `order` 升序输出，缺失或非正 `order` 的槽位保持输入顺序排在后面；`send_message` 动作不会进入“已安排”执行摘要。
- 缺失 `planId`、非法 `scenario`、空 `timeline` 会抛出明确 `IllegalArgumentException`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/MessageToolTests.java`，覆盖家庭/朋友 deterministic fallback、Plan B、动作摘要、`send_message` 过滤、`plan.shareMessage`、`llmUsed=false`、时间线排序和非法请求。
- 在 `feature_list.json` 将 `B2-005` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 27 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`MessageToolTests` 4 个测试、全后端 27 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Mencius (`019e57b3-5e9e-7e92-b1e5-7744b2a4d329`, `B2-005-EVAL-CODEX-20260524T0959+0800`) 已完成中文 QA 验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-005-message-tool.md`、`feature_list.json` 中 B2-005 条目、`docs/api-contract.md` 的 `Plan.shareMessage`、`docs/backend-contract.md` 的 LLM 边界、`backend/HANDOFF.md`、B2 MessageTool 源码和测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 deterministic fallback 中文 `shareMessage`、`execute` 兼容入口、`llmUsed=false`、无 OpenAI/LLM/网络调用、Plan-compatible payload 包含 `shareMessage`，以及家庭/朋友/Plan B/动作摘要/错误路径覆盖。
- QA 报告：`docs/qa/B2-005-message-tool.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-005` 已完成并 verified。
- B2 Sprint 1 当前 `B2-001` 到 `B2-005` 均已 verified；`shareMessage` 已在 Tool 层进入 Plan-compatible payload。
- 真正写入正式 API `Plan`、推送 `plan_ready`、接入 B1 状态机/Planner/SSE 仍等待 B1 后续任务，当前没有伪装完成 B1 链路。

## 2026-05-24 B2-004 BookingTool / bookOrOrder

### 已完成

- 新增 `docs/contracts/B2-004-booking-tool.md`，明确本轮只交付 B2 Tool 层 `BookingTool / bookOrOrder`，不抢做 B1 `execute` API、SSE `execute_result`、完整 `MockApiService` 或 `MessageTool`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingRequest.java`、`BookingResult.java` 和 `BookingTool.java`。
- `BookingTool.bookOrOrder()` 返回与 `docs/api-contract.md` 中 `execute_result` 对齐的核心字段：`type`、`planId`、`actionId`、`actionType`、`status`、`confirmationNo`、`timestamp`，并补充工具层 `message` 和 `latencyMs`。
- 成功动作会按 `actionType` 生成 deterministic mock `confirmationNo`，覆盖 `MOCK-TKT-`、`MOCK-TBL-`、`MOCK-QNO-`、`MOCK-DLV-`、`MOCK-NOTE-` 和 `MOCK-CXL-` 前缀。
- `bookOrOrder` 要求 `idempotencyKey`，并通过内存 `ConcurrentHashMap` 保证相同 key 重复调用返回同一份 `BookingResult`，不重复生成 mock 订单。
- `bookingFail=true` 时，非 `cancel_booking` 动作返回 `status=failed` 且 `confirmationNo=null`；`cancel_booking` 作为 Tool 层反向操作仍可成功返回 `MOCK-CXL-` 确认号。
- 非取消动作会校验 `targetPoiId` 必填、POI 存在、目标 POI 的 `actionTypes` 支持当前 `actionType`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/BookingToolTests.java`，覆盖成功、幂等重放、`bookingFail`、`cancel_booking` 和非法请求。
- 调整 `backend/pom.xml`，关闭 Maven compiler incremental compilation，避免当前 Windows/Javac 环境在标准 `verify.ps1` 新增源文件后反复出现 `无法关闭编译器资源` 的不稳定失败。
- 在 `feature_list.json` 将 `B2-004` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 23 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`BookingToolTests` 5 个测试、全后端 23 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Confucius (`019e57a4-d8b5-7b10-aba7-5557ecee3057`) 已完成中文 QA 验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-004-booking-tool.md`、`feature_list.json` 中 B2-004 条目、`docs/api-contract.md` 的 ActionType / ActionStatus / `execute_result` / `idempotencyKey` 相关段落、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 BookingTool 源码和测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认成功确认号、`bookingFail` 失败、`execute_result` 核心字段、`latencyMs`、`idempotencyKey` 幂等和 `cancel_booking` 反向操作。
- QA 报告：`docs/qa/B2-004-booking-tool.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-004` 已完成并 verified。
- `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-24 B2-003 AvailabilityTool and ScenarioFlags

### 已完成

- 新增 `docs/contracts/B2-003-availability-scenario-flags.md`，明确本轮只交付 B2 本地 `AvailabilityTool / checkAvailability`、`ScenarioFlags` 和 `POST /api/debug/scenario`，不抢做 BookingTool、MessageTool、MockApiService、状态机、SSE 或 Plan B 决策。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlags.java`、`ScenarioFlagsState.java`、`ScenarioFlagsUpdateRequest.java` 和 `ScenarioFlagsResponse.java`。
- `ScenarioFlags` 作为 Spring singleton component 使用 `AtomicReference` 保存可热更新状态，覆盖 `restaurantFull`、`routeTooFar`、`bookingFail`、`ageMismatch` 四个异常注入开关。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityTool.java`、`AvailabilityRequest.java` 和 `AvailabilityResult.java`。
- `AvailabilityTool.checkAvailability()` 从本地 POI JSON 的 `defaultAvailability` 读取余位，返回 `available`、`availabilityStatus`、`remaining`、`waitMinutes`、`ageMatched`、`groupSizeMatched`、`reasons`、`scenarioFlags` 和 `latencyMs`。
- `restaurantFull=true` 时，餐厅 POI 被强制为不可用、`remaining=0`、`availabilityStatus=full`、等待时间至少 70 分钟。
- `ageMismatch=true` 且请求携带 `minAge` 时，availability 结果会返回 `ageMatched=false` 并不可用；`routeTooFar` 和 `bookingFail` 本轮完成存储、更新和返回，供后续路线/下单任务消费。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/controller/DebugScenarioController.java`，实现 `POST /api/debug/scenario`，返回 `{ "updated": { ... } }`，无需重启服务即可更新 flags。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/AvailabilityToolTests.java`、`ScenarioFlagsTests.java` 和 `DebugScenarioControllerTests.java`，覆盖默认 availability、异常注入、非法入参、flags 热更新和真实 HTTP debug API。
- 在 `feature_list.json` 将 `B2-003` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 18 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`AvailabilityToolTests` 4 个测试、`DebugScenarioControllerTests` 1 个测试、`ScenarioFlagsTests` 1 个测试、全后端 18 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Dirac (`019e5795-2750-75c0-845e-f3104401f0a0`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-003-availability-scenario-flags.md`、`feature_list.json` 中 B2-003 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 availability / scenario / debug controller 源码和 B2-003 测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 `AvailabilityTool` 读取本地 JSON 默认余位并返回 `latencyMs`，`ScenarioFlags` 覆盖四个开关，`POST /api/debug/scenario` 在同一运行上下文内连续 POST 可热更新 flags。
- QA 报告：`docs/qa/B2-003-availability-scenario-flags.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-003` 已完成并 verified。
- `B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-24 B2-002 SearchTool and RouteTool

### 已完成

- 新增 `docs/contracts/B2-002-search-route-tools.md`，明确本轮只交付 B2 本地 `SearchTool / searchLocalPlaces` 和 `RouteTool / calculateRouteTime`，不抢做 AvailabilityTool、ScenarioFlags、MockApiService、debug API、REST / SSE 或真实地图服务。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/model/*` 和 `backend/src/main/java/com/weekendtravel/backend/b2/repository/PoiRepository.java`，把 B2-001 的 `mock/poi_data.json` 加载为本地 POI catalog。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/SearchTool.java`、`SearchRequest.java`、`SearchResult.java` 和 `SearchCandidate.java`。
- `SearchTool.searchLocalPlaces()` 支持 `family` / `friends` 场景校验，按类别、关键词、距离、年龄和人数过滤本地 POI，并按 `0.4 relevance + 0.3 distance + 0.2 rating + 0.1 availability` 确定性排序。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/RouteTool.java`、`RouteRequest.java` 和 `RouteResult.java`。
- `RouteTool.calculateRouteTime()` 支持当前位置到 POI、POI 到 POI、同一 POI 0 分钟、未知 POI 明确异常；返回 `distanceMinutes`、可读 route summary 和 `latencyMs`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/SearchToolTests.java` 和 `backend/src/test/java/com/weekendtravel/backend/b2/RouteToolTests.java`，覆盖搜索候选、无结果、非法入参、路线分钟数、summary、同地路线和异常路径。
- 在 `feature_list.json` 将 `B2-002` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test` 通过，后端测试合计 12 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`RouteToolTests` 5 个测试、`SearchToolTests` 4 个测试、全后端 12 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Averroes (`019e5780-ab21-7250-9644-2889eae14565`, `B2-002-EVAL-CODEX-20260524T0904+0800`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-002-search-route-tools.md`、`feature_list.json` 中 B2-002 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 tool / repository 源码和 B2 tool 测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 `SearchTool` 从本地 JSON 返回候选 POI，`RouteTool` 返回 `distanceMinutes` 和 route summary，两个工具均返回 `latencyMs`。
- QA 报告：`docs/qa/B2-002-search-route-tools.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-002` 已完成并 verified。
- `B2-003`、`B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-22 B2-001 POI mock data

### 已完成

- 新增 `docs/contracts/B2-001-poi-data.md`，明确本轮只交付 B2 本地 POI mock 数据，不实现 Tool、MockApiService、ScenarioFlags 或 debug API。
- 新增 `backend/src/main/resources/mock/poi_data.json`，作为后端 classpath resource `mock/poi_data.json`，顶层包含 `version`、`updatedAt`、`city`、`center` 和 `pois`。
- POI 数据一次性补到 52 条，满足首批不少于 30 条和最终不少于 50 条要求。
- 数据覆盖 `activity=20`、`restaurant=20`、`cafe=3`、`dessert=3`、`supplier=6`。
- 场景覆盖 `family=28`、`friends=32`，其中 8 条同时支持 family / friends。
- 每条 POI 使用 camelCase 本地字段，包含 `id`、`name`、`category`、`subCategory`、`scenarios`、经纬度、评分、标签、距离、价格、年龄、人数、availability、scenario flags 和 action types。
- 新增 `backend/src/test/java/com/weekendtravel/backend/PoiDataTests.java`，用 Jackson 3 `tools.jackson.databind.ObjectMapper` 解析 JSON，并校验数量、唯一 ID、必填字段、availability 槽位、family / friends 和类别覆盖。
- 修复 `backend/mvnw.cmd` 在普通 `.m2` 目录下访问 `.Target[0]` 的空值问题，使 Maven Wrapper 能在当前 Windows 环境启动。
- 调整 `backend/src/test/java/com/weekendtravel/backend/BackendApplicationTests.java`，用反射校验真实 `HealthController.health()` 返回字段，避开当前 Maven testCompile 类路径对主类 compile-time import 的限制。
- 在 `feature_list.json` 将 `B2-001` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧 JSON 检查：`Get-Content -Raw -Encoding UTF8 backend\src\main\resources\mock\poi_data.json | ConvertFrom-Json` 通过，统计为 `count=52`、`activity=20`、`cafe=3`、`dessert=3`、`restaurant=20`、`supplier=6`、`family=28`、`friends=32`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过；后端测试结果为 3 tests、0 failures、`BUILD SUCCESS`、`Verify passed.`。
- 独立 evaluator 子代理 Newton (`019e4fdf-35fd-7c40-a198-aa35368a6fae`, `B2-001-EVAL-CODEX-20260522T2130+0800`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-001-poi-data.md`、`feature_list.json` 中 B2-001 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、`backend/src/main/resources/mock/poi_data.json` 和后端测试。
- Evaluator 独立确认 POI JSON 以 UTF-8 可解析，`pois=52`，类别覆盖 `activity=20`、`restaurant=20`、`cafe=3`、`dessert=3`、`supplier=6`，场景覆盖 `family=28`、`friends=32`、`both=8`。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`BackendApplicationTests` 2 个测试和 `PoiDataTests` 1 个测试均通过。
- QA 报告：`docs/qa/B2-001-poi-data.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-001` 已完成并 verified。
- `B2-002`、`B2-003`、`B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-22 F1-002 API client and mock fixture mode

### 已完成

- 新增 `docs/contracts/F1-002-api-client-fixtures.md`，明确本轮只交付 API client、mock fixture mode 和 fixture 解析验证，不抢做完整 SSE composable 或 PlanCard。
- 新增 `frontend/src/api/types.ts`，按 `docs/api-contract.md` 建立 `Scenario`、`AgentState`、`Plan`、`TimeSlot`、`ActionItem`、REST request / response 和 SSE payload 类型。
- 新增 `frontend/src/api/client.ts`，支持默认 `mock` mode 和 `VITE_API_MODE=real` 的真实 API mode；真实路径只使用 `docs/api-contract.md` 里的 REST / SSE 端点。
- 新增 `frontend/src/api/fixtures.ts`，通过 Vite raw import 读取 `docs/fixtures/plan-ready-family.json`、`docs/fixtures/plan-ready-friends.json` 和 `docs/fixtures/sse-events.jsonl`，并做 camelCase key、plan_ready 和 SSE frame 基础校验。
- 新增 `frontend/scripts/verify-fixtures.mjs` 和 `frontend/package.json` 的 `verify:fixtures`，把 fixture JSON / JSONL 解析纳入前端 fast verify。
- 更新 `verify.ps1`，当前 `.\verify.ps1 -Target frontend -Mode fast` 会先跑 `pnpm verify:fixtures`，再跑 `pnpm typecheck`。
- 更新 `frontend/src/stores/planner.ts` 和 `frontend/src/App.vue`，点击“开始预演”后默认使用 mock client 加载 fixture，显示 `CONFIRM` / `closed` 状态、Plan ID、Plan B 摘要和 fixture 日志预览。
- 在 `feature_list.json` 将 `F1-002` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，输出覆盖：
  - `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `docs/fixtures/sse-events.jsonl -> 23 JSONL events`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 额外构建检查：`npm run build` 通过，Vite production build 成功。
- Generator 字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- 独立 evaluator 子代理 Hooke (`019e4dd8-6378-79b2-a8d6-9f89bcadb13d`) 已完成验证并放行。
- Evaluator 验证范围包含：`docs/contracts/F1-002-api-client-fixtures.md`、`docs/api-contract.md`、`frontend/src/api/*`、`frontend/scripts/verify-fixtures.mjs`、`verify.ps1`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`。
- Evaluator 运行前端 fast verify 通过，并确认 fixture 解析、`pnpm typecheck` 和 `Verify passed.`。
- Evaluator 使用 Playwright MCP 打开 `http://127.0.0.1:5173` 并点击“开始预演”，确认页面进入 `CONFIRM` / `closed`，可见 `plan_family_fixture`、Plan B 和 summary，console 错误 / 警告为 0。
- Evaluator 使用 Chrome DevTools MCP 复核 snapshot、console、network、DOM：三份 fixture raw import 均为 200，页面含 `CONFIRM`、`closed`、Plan B alert 和 fixture 日志。
- QA 报告：`docs/qa/F1-002-api-client-fixtures.md`。
- 截图证据：`docs/qa/F1-002-devtools-confirm.png`、`docs/qa/F1-002-devtools-confirm-root.png`、`docs/qa/F1-002-playwright-confirm.png`。

### 当前状态

- `F1-002` 已完成并 verified。
- 下一步 F1 小目标应推进 `F1-003`：`useSSE` composable and log panel，把当前 fixture 解析能力接到真实日志面板和流式事件分发。

## 2026-05-22 WF-002 前端 evaluator 浏览器 MCP 规则

### 已完成

- 新增 `docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`，明确前端 evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- 更新 `docs/dev-workflow.md`，把前端 UI 验收从泛泛的浏览器 / Playwright 要求升级为双 MCP 要求。
- 更新 `docs/frontend-contract.md`，明确 Playwright MCP 负责模拟交互、状态等待、截图或 trace，Chrome DevTools MCP 负责页面快照、console、network、DOM / accessibility 和视觉复核。
- 更新 `docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`frontend/F1-handoff.md` 和 `docs/decision-log.md`，统一前端 evaluator 证据要求。
- 在 `feature_list.json` 新增 `WF-002`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Playwright MCP|Chrome DevTools MCP|截图|视觉|console|network|DOM|accessibility|trace`，确认入口、前端 contract、workflow、quality、模板、handoff、decision log、progress 和 feature metadata 均有覆盖。
- 独立 evaluator 子代理 Euler (`019e4db4-865e-7fe2-85ed-4dd5e5b6f3c2`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/frontend-contract.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`docs/initiallizer-agent-prompt.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：前端验收入口、QA 模板、handoff、decision log、progress 和 feature metadata 都已覆盖 Playwright MCP + Chrome DevTools MCP 双工具要求；`feature_list.json` 可解析；未发现冲突规则。
- QA 报告已写入 `docs/qa/WF-002-frontend-evaluator-browser-mcp.md`。

## 2026-05-22 WF-001 harness 测试职责调整

### 已完成

- 新增 `docs/contracts/WF-001-generator-subagent-testing.md`，把“generator 测试阶段必须使用 evaluator 子代理”作为本轮 workflow contract。
- 更新 `AGENTS.md`，把完成定义改为必须有独立 evaluator 子代理验证证据。
- 更新 `docs/dev-workflow.md`，将旧的 generator 自行最终验证流程改为 `Generator -> Handoff to Evaluator Subagent -> Evaluator Test -> Fix -> Evaluator Retest -> Handoff`。
- 更新 `docs/quality.md`，明确 generator 自己运行的 typecheck、build、脚本、curl 或冒烟只能作为开发准备记录，不能单独作为 `verified` 证据。
- 更新 `docs/contracts/_template.md` 和 `docs/qa/evaluator-template.md`，要求后续 contract 与 QA 报告记录 evaluator 子代理。
- 更新 `docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md` 和 `docs/initiallizer-agent-prompt.md`，统一新规则。
- 在 `feature_list.json` 新增 `WF-001`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Self Verify|小任务可以同一个 agent|Generator 负责实现和基础验证|Generator -> Self Verify`，只剩 `docs/contracts/WF-001-generator-subagent-testing.md` 的负例搜索命令命中，不是活跃规则。
- 独立 evaluator 子代理 Newton (`019e4da9-79c0-79a3-b7f1-c79f5e594c58`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-001-generator-subagent-testing.md`、`docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：活跃规则已统一为“generator 完成开发后，测试阶段必须交给独立 evaluator 子代理”；没有发现仍允许 generator-only final testing 的活跃表述；`feature_list.json` 可解析。
- QA 报告已写入 `docs/qa/WF-001-generator-subagent-testing.md`。

## 2026-05-21 Initializer

### 已完成

- 读取根目录产品文档、外部 harness 总结材料、当前仓库状态和已有 `docs/initiallizer-agent-prompt.md`。
- 创建短入口 `AGENTS.md`。
- 创建长期 docs：
  - `docs/product-spec.md`
  - `docs/architecture.md`
  - `docs/api-contract.md`
  - `docs/frontend-contract.md`
  - `docs/backend-contract.md`
  - `docs/dev-workflow.md`
  - `docs/quality.md`
  - `docs/decision-log.md`
  - `docs/contracts/_template.md`
  - `docs/qa/evaluator-template.md`
- 创建前端 fixture：
  - `docs/fixtures/sse-events.jsonl`
  - `docs/fixtures/plan-ready-family.json`
  - `docs/fixtures/plan-ready-friends.json`
- 创建结构化任务清单 `feature_list.json`。
- 创建 `backend/README.md` 和 `frontend/README.md`，明确尚未 scaffold。
- 创建 `init.ps1` 和 `verify.ps1` 的 Windows harness 脚本。

### 关键决策

- 后端默认端口统一为 `8000`。
- API 和 SSE 线缆字段统一使用 camelCase。
- 前端先用 fixture 开发，后端先用本地 Mock API 开发。
- OpenAI 接入暂不锁 SDK 版本或模型名；真正实现前必须重新核验官方 OpenAI developer docs / MCP。
- 本轮不 scaffold Spring Boot 或 Vue 项目，不把空目录伪装为已完成项目。

### 当前状态

- 仓库已经具备长期 agent 开发 harness。
- 前后端业务代码尚未初始化。
- `feature_list.json` 中功能状态仍为 `todo`，因为没有业务功能通过验证。

### 验证记录

- 回读 `AGENTS.md` 和 `docs/api-contract.md`，中文显示正常。
- `feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-family.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-friends.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/sse-events.jsonl` 共 23 行，逐行可通过 `ConvertFrom-Json` 解析。
- 直接运行 `.\init.ps1 -Target all` 和 `.\verify.ps1 -Target all -Mode fast` 被当前 PowerShell 执行策略拦截，错误类型为 `PSSecurityException / UnauthorizedAccess`。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all` 成功运行：Java、Node、pnpm 可见，Maven 缺失；backend/frontend 均明确提示尚未初始化。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast` 成功运行脚本逻辑并返回失败：`backend\pom.xml` 缺失、`frontend\package.json` 缺失。这是当前真实未初始化状态，不是通过。
- `git status --short` 可运行，当前文件均为未跟踪；Git 同时输出 `unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied` 权限警告，未阻塞本轮初始化。

## 2026-05-21 B1-001 health and CORS

### 已完成

- 在 `backend/` 内完成 Spring Boot + Maven 最小骨架落地。
- 新增全局 CORS 配置 `backend/src/main/java/com/weekendtravel/backend/config/WebConfig.java`，允许 `http://localhost:5173`。
- 新增 `GET /health` controller：`backend/src/main/java/com/weekendtravel/backend/controller/HealthController.java`。
- 新增健康检查响应 DTO：`backend/src/main/java/com/weekendtravel/backend/dto/HealthResponse.java`。
- 在 `backend/src/main/resources/application.properties` 中补充 `server.port=8000`。
- 恢复并保留测试目录 `backend/src/test/`，测试文件验证健康检查返回约定字段。
- 新增本轮 contract：`docs/contracts/B1-001-health-cors.md`。

### 验证记录

- `./backend/mvnw -f "backend/pom.xml" test` 通过，结果为 `BUILD SUCCESS`。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 现已通过；脚本已改为调用 `backend/mvnw.cmd`，不再依赖全局 `mvn`。
- `./backend/mvnw -f "backend/pom.xml" spring-boot:run` 成功启动服务，Tomcat 监听 `8000`。
- `curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 `HTTP/1.1 200`，并包含 `Access-Control-Allow-Origin: http://localhost:5173`，响应体包含 `status`、`service`、`timestamp`。

### 当前状态

- `feature_list.json` 中 `B1-001` 已按真实运行证据标记为 `verified`。
- 后端已不再是“未初始化”状态；下一优先任务应转到 `B1-002` SSE emitter and heartbeat 或 `B1-003` POST `/api/plan`。
- `verify.ps1` 已改为使用 Maven Wrapper，与当前 backend 工作流一致。


### 已补充

- 补读 2 页设计文档后，将候选排序公式、P0/P1/P2 约束分级、Mock API 覆盖范围、`bookOrOrder` 幂等键、`cancel_booking` 反向操作、正常场景 Plan B 触发率指标补入 docs。
- 在 `docs/decision-log.md` 记录了 20 vs 21 条 golden case、Plan B 指标口径、情侣模板是否纳入 Demo 的冲突处理。

### 保持不变

- Golden Case 仍以 21 条为准。
- Demo 场景仍只有 `family` 和 `friends`。
- API 字段名仍统一 camelCase；部分动作枚举值保留工具语义 lower_snake_case。

## 2026-05-21 F1-sprint1-generator

### 已完成

- 创建 `docs/contracts/F1-001-vue-skeleton.md`，明确本轮只交付 Vue 前端骨架，不接入真实 API / SSE / OpenAI。
- 在 `frontend/` scaffold Vue 3 + Vite + Pinia + Naive UI 项目，并保留 Tailwind CSS 入口以匹配前端技术栈。
- 配置 `vite.config.ts`：`server.port = 5173`、`strictPort = true`，`pnpm dev` 默认访问 `http://localhost:5173`。
- 创建 Pinia store `src/stores/planner.ts`，包含 `planId`、`agentState`、用户输入、场景、origin、SSE 状态占位和日志事件数组占位。
- 页面 `src/App.vue` 使用 Naive UI 组件渲染输入区、场景切换、状态摘要和日志占位，不调用后端。
- 通过宿主 npm 安装全局 `pnpm`，并在 `frontend/` 生成 `pnpm-lock.yaml`。
- 新增 `frontend/.gitignore`，忽略 `node_modules/`、`dist/` 和本地生成物。
- 新增根 `.gitignore`，忽略 Playwright MCP 本地快照目录 `.playwright-mcp/` 以及后端/前端常见生成物。
- 将 `feature_list.json` 中 `F1-001` 标记为 `verified` 并写入验证证据。

### 验证记录

- 直接运行 `.\verify.ps1 -Target frontend -Mode fast` 时，当前沙盒 shell 找不到宿主全局 `pnpm`，脚本按预期失败在环境检查。
- 使用宿主 PowerShell 运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：
- `pnpm typecheck` passed。
- `Verify passed.`
- 额外运行 `pnpm build` 通过，Vite 生产构建成功，主 JS chunk 约 290.51 kB / gzip 91.94 kB。
- 已启动 `pnpm dev`，监听 `127.0.0.1:5173`；Playwright 打开页面可见 WeekendTravel 骨架，点击“开始预演”后 Pinia 状态从 `START/idle` 变为 `INTENT/connecting`，console 无 error/warning。

### 下一步

- F1 下一步建议推进 `F1-002`：API client 和 mock fixture mode。
- 后端下一步推进 `B1-002`。
