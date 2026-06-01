# Handoff

## 2026-06-01 INT-003 Friends scenario complete path

INT-003 已完成并 verified。当前真实 friends real mode 已跑通完整路径：`POST /api/plan` -> planning SSE -> `plan_ready` / `CONFIRM` -> `POST /api/plan/{planId}/execute` -> execution SSE -> `execute_result` -> `done` / `DONE`。friends 方案已稳定包含 `activity`、`cafe` 或 `dessert` 中途点、`restaurant` 三段 timeline。

本轮新增或更新：

- `docs/contracts/INT-003-friends-complete-path.md`
- `docs/qa/INT-003-friends-complete-path.md`
- `docs/qa/INT-003-devtools-real.png`
- `docs/qa/INT-003-planning-stream.network-response`
- `docs/qa/INT-003-execution-stream.network-response`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanSelectionSnapshot.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanFlowIntegrationTests.java`
- `feature_list.json`
- `progress.md`
- `docs/handoff.md`
- `backend/HANDOFF.md`
- `frontend/F1-handoff.md`

验证结果：

- Generator backend fast verify 通过：`.\verify.ps1 -Target backend -Mode fast`，后端 48 tests、0 failures、0 errors、`BUILD SUCCESS`、`Verify passed.`。
- Generator frontend fast verify 通过：`.\verify.ps1 -Target frontend -Mode fast`，`pnpm verify:fixtures` 和 `pnpm typecheck` 通过。
- Generator quoted snake_case field-key 检查无命中。
- 独立 evaluator Zeno (`019e8397-d87b-7f12-8312-2e83325f8c75`, `INT-003-EVAL-CODEX-20260601T2235+0800`) 使用 Chrome DevTools MCP 验证 real mode friends path，结论 `PASS`。
- Chrome DevTools MCP 证据覆盖：选择 `朋友` -> 提交 friends demo 输入 -> `plan_ready` / `CONFIRM` -> 确认执行 -> `DONE`；页面可见 `Mode real`、真实 `plan_5ca109e3a00c`、summary `朋友活动、咖啡/甜品和轻松聚餐`、timeline `城市影像展` / `街角咖啡` / `暮色烤肉`、ExecutionTracker `2 / 2`、`MOCK-TBL-94672`、`MOCK-MSG-43835`、LogPanel `execute_result` / `done`。
- Console error / warn 为 0；network 包含 `POST /api/plan [202]`、规划流 `[200]`、`POST /execute [200]`、执行流 `[200]`，请求体为 `scenario:"friends"`。

接手提醒：

- `feature_list.json` 已将 `INT-003` 标记为 `verified`；`INT-002` / `INT-003` 分别只代表 family / friends happy path，不代表 21 Golden Cases 已完成。
- 下一步建议推进真实异常 / degrade 补证，或进入 `QA-001` 前先补 golden cases 执行计划。

## 2026-06-01 B1-004 State machine START to PACK close-out

B1-004 已完成并 verified。当前后端实现已经满足 `docs/contracts/B1-004-state-machine-start-pack.md`：真实 `POST /api/plan` 返回的 `planId` 可以继续用于 `GET /api/plan/{planId}/stream`，family happy path 可观察到 `START -> INTENT -> SKELETON -> RECALL -> VALIDATE -> PACK`，并发送 `heartbeat`、`state_change`、`tool_call`、`tool_result`、`plan_ready`。

本轮新增或更新：

- `docs/qa/B1-004-state-machine-start-pack.md`
- `feature_list.json`
- `progress.md`
- `docs/handoff.md`
- `backend/HANDOFF.md`

验证结果：

- Generator backend fast verify 通过：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`，后端 48 tests、0 failures、`BUILD SUCCESS`、`Verify passed.`。
- 独立 evaluator Volta (`019e837b-353a-7990-ae11-a71a58741421`) 做了只读复核并放行，结论 `PASS`。
- Evaluator 运行 backend fast verify 通过，并用真实 HTTP 抽样确认 `POST /api/plan` -> `GET /api/plan/{planId}/stream` 串联成功；样本 `planId=plan_c7092ab341f0`。
- Evaluator 观察到 `START / INTENT / SKELETON / RECALL / VALIDATE / PACK`、`latencyMs`、可消费 camelCase `plan_ready.plan`，并确认当前 `PACK -> CONFIRM` 是后续链路扩展，不阻塞 B1-004。

接手提醒：

- `B1-004` 现在已经有独立 evaluator 证据，不再是 `INT-002` / `INT-003` 的未收口前置项。
- `INT-003` 已在 2026-06-01 完成并 verified，不要把 B1-004 后端证据或 INT-002 family 证据泛化成 friends 端到端；应引用 INT-003 自己的 QA 证据。

## 2026-05-31 INT-002 Family scenario complete path

INT-002 已完成并 verified。当前真实 family real mode 已跑通完整路径：`POST /api/plan` -> planning SSE -> `CLARIFY` -> `POST /api/plan/{planId}/clarify` -> `plan_ready` / `CONFIRM` -> `POST /api/plan/{planId}/execute` -> execution SSE -> `execute_result` -> `done` / `DONE`。

本轮新增或更新：

- `docs/contracts/INT-002-family-complete-path.md`
- `docs/qa/INT-002-family-complete-path.md`
- `docs/qa/INT-002-devtools-real.png`
- `docs/qa/INT-002-generator-devtools-real.png`
- `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/ExecutePlanRequest.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/ExecutePlanResponse.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/ExecuteResultEvent.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/DoneEvent.java`
- `frontend/src/stores/planner.ts`
- `feature_list.json`
- `progress.md`
- `backend/HANDOFF.md`
- `frontend/F1-handoff.md`

验证结果：
- Generator fast verify 通过：backend fast、frontend fast、all fast 均通过；后端 48 tests、0 failures，前端 `pnpm verify:fixtures` 与 `pnpm typecheck` 通过。
- Forbidden snake_case 检查无命中，`feature_list.json` 可解析。
- 独立 evaluator Evaluator (`019e7d13-a0dc-76d2-8a94-2e28f4a36d8c`, `INT-002-EVAL-CODEX-20260531T1615+0800`) 使用 Chrome DevTools MCP 验证 real mode family path，结论 `PASS`。
- Chrome DevTools MCP 证据覆盖：默认 family 输入 -> `CLARIFY` -> `4-6小时` -> `plan_ready` / `CONFIRM` -> 确认执行 -> `DONE`；页面可见 `Mode real`、真实 `plan_79b2807ae0b3`、POI `小小科学工坊` / `四季家庭小厨`、ExecutionTracker `2 / 2`、`MOCK-TBL-50306`、`MOCK-MSG-99469`、LogPanel `execute_result` / `done`。
- Console error / warn 为 0；network 包含 `POST /api/plan [202]`、规划流 `[200]`、`POST /clarify [200]`、后续规划流 `[200]`、`POST /execute [200]`、执行流 `[200]`。

接手提醒：
- `feature_list.json` 已将 `INT-002` 标记为 `verified`；`B1-004` 已在 2026-06-01 另行补齐单独 evaluator 证据并标记为 `verified`。
- 历史说明：本节生成时 `INT-003` 尚未完成；当前 `INT-003` 已在 2026-06-01 verified。后续建议单独补真实异常 / degrade 证据，且不要把 family / friends happy path 泛化成 21 golden cases 已完成。

## 2026-05-29 WF-002 Chrome DevTools MCP only

本轮按用户反馈调整前端 UI 验收规则：Playwright MCP 与 Chrome DevTools MCP 在交互、等待和截图检查上有重叠，后续 workflow 只强制保留 Chrome DevTools MCP 检查。Playwright MCP 可作为补充证据，但不再是前端任务标记 `verified` 的硬门槛。

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
- `docs/qa/WF-002-chrome-devtools-only.md`

接手提醒：

- 涉及前端 UI 的后续任务，evaluator 子代理必须使用 Chrome DevTools MCP 覆盖用户路径、模拟交互、状态等待、页面快照、console、network、DOM / accessibility 或等价浏览器诊断。
- 缺少 Chrome DevTools MCP 证据时，前端 UI 任务不能标记为 `verified`。
- 历史 QA / progress / feature evidence 中的 Playwright MCP 记录是当时的验收事实，不代表当前仍强制 Playwright MCP。

验证结果：

- Generator 已确认 `feature_list.json` 可解析。
- Generator 已确认活跃 workflow 文档不再把 Playwright MCP 作为前端 UI verified 的必需证据。
- 独立 evaluator 子代理 Parfit (`019e743f-9828-7191-9738-14491269e85d`) 已完成只读复核并放行：`feature_list.json` 可解析，活跃 workflow 文档要求 Chrome DevTools MCP，且不再强制 Playwright MCP。
- QA 报告：`docs/qa/WF-002-chrome-devtools-only.md`。

## 2026-05-28 B1-006 CLARIFY and ADJUST

B1-006 已完成并 verified。当前后端已支持：低置信度输入先进入 `CLARIFY`、发送单个 `clarification_request`、通过 `POST /api/plan/{planId}/clarify` 恢复到正式规划链路；同时在 `CONFIRM` 状态下支持最多 3 次 `PATCH /api/plan/{planId}/adjust` 局部微调，并通过 `adjust_result` 返回最新完整 `plan`。

本轮新增或更新：
- `docs/contracts/B1-006-clarify-adjust.md`
- `docs/qa/B1-006-clarify-adjust.md`
- `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
- `backend/src/main/java/com/weekendtravel/backend/api/ApiExceptionHandler.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanState.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PendingClarification.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanSelectionSnapshot.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/ClarifyPlanRequest.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/ClarifyPlanResponse.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/AdjustPlanRequest.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/AdjustPlanResponse.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/ClarificationRequestEvent.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/AdjustResultEvent.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/InvalidPlanStateException.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/AdjustLimitExceededException.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanFlowIntegrationTests.java`
- `feature_list.json`
- `progress.md`
- `docs/handoff.md`
- `backend/HANDOFF.md`

验证结果：
- Generator 后端测试通过：45 tests、0 failures、`BUILD SUCCESS`。
- Generator backend fast verify 通过：`Verify passed.`。
- 独立 evaluator 子代理完成父工作树与 fresh 实例 `8002` 验收并放行：
  - 模糊输入先进入 `CLARIFY` 并发出 `clarification_request`
  - `POST /clarify` 返回 `processing`，随后恢复到 `plan_ready` / `CONFIRM`
  - `PATCH /adjust` 后 stream 发出 `adjust_result`、`affectedSlots` 和最新完整 `plan`
  - `adjust_result.summary` 与最终 `plan.timeline` 一致
  - 事件名与 payload `type` 一致，字段保持 camelCase
- QA 报告：`docs/qa/B1-006-clarify-adjust.md`

接手提醒：
- 本轮只完成后端正式链路，前端 ClarifyBubble / AdjustPanel UI 仍留给后续 `F1-006` / `F1-007`。
- 当前 clarify 启发式只覆盖 duration 不明确这一类低置信度输入，并非通用意图澄清器。
- 当前 adjust 仍是 deterministic 局部微调，主要覆盖 activity / restaurant 槽位，不是通用自由文本重规划。
- 涉及 API / SSE 字段调整时，仍必须先改 `docs/api-contract.md`。

## 2026-05-27 DOC-001 Root README

本轮新增根目录 `README.md`，作为 WeekendTravel 仓库入口文档。README 覆盖项目目标、`family` / `friends` Demo 范围、前后端技术栈、默认端口、目录结构、初始化检查、依赖安装、本地运行、mock / real mode 切换和仓库级验证命令。

本轮新增或更新：

- `README.md`
- `docs/contracts/DOC-001-root-readme.md`
- `docs/qa/DOC-001-root-readme.md`
- `feature_list.json`
- `progress.md`
- `docs/handoff.md`

接手提醒：

- README 是入口导航，不替代 `docs/api-contract.md`、`feature_list.json`、`progress.md` 或各 handoff。
- 当前能力状态仍以 `feature_list.json` 和 evaluator 证据为准。
- 历史说明：本节生成时 `B1-004` 尚未在 `feature_list.json` 标记为 verified；该条目已在 2026-06-01 通过独立 evaluator 证据补齐。
- 涉及 API 字段变更仍必须先改 `docs/api-contract.md`。
- 涉及前端 UI 的后续任务仍必须由 evaluator 使用 Chrome DevTools MCP；Playwright MCP 可作为补充但不再强制要求。

验证结果：

- Generator 本地检查确认 README 关键标题、命令和规则可搜索，`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- 独立 evaluator 子代理 CodeChecker (`019e69ae-e5fe-7c13-bbab-be7d49d8a6a4`) 对 `README.md` 和 `feature_list.json` 做只读复核，结论 PASS，无阻塞问题。
- QA 报告：`docs/qa/DOC-001-root-readme.md`。
## 2026-05-27 INT-001 Sprint 1 最小联调

INT-001 已完成并 verified。当前真实联调链路已经成立：前端 real mode 提交一条 Demo 自然语言输入，后端 `POST /api/plan` 返回非空 `planId` 和 `status=processing`，前端用该 `planId` 打开 `/api/plan/{planId}/stream`，LogPanel 渲染真实后端 SSE `state_change START -> INTENT`。

本轮新增或更新：
- `docs/contracts/INT-001-one-input-sse.md`
- `docs/qa/INT-001-one-input-sse.md`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerIntegrationTests.java`
- `feature_list.json`
- `progress.md`
- `frontend/F1-handoff.md`
- `backend/HANDOFF.md`

验证结果：
- Generator 后端 fast verify 通过：36 tests、0 failures、`BUILD SUCCESS`。
- Generator 前端 fast verify 通过：`pnpm verify:fixtures passed`，`pnpm typecheck passed`。
- 独立 evaluator 子代理 Franklin (`019e691e-822d-7403-8ca3-df84e5f280c3`, `INT-001-EVAL-CODEX-20260527T1915+0800`) 已放行。
- Evaluator 使用 Playwright MCP 验证真实用户路径：real mode 页面提交默认家庭 Demo 文本后显示后端 `planId`、Agent `INTENT`，LogPanel 可见 `heartbeat`、`state_change`、`START -> INTENT`。
- Evaluator 使用 Chrome DevTools MCP 复核：console error / warn 为 0，network 包含 `POST http://localhost:8000/api/plan [202]` 和 `/api/plan/{planId}/stream [200]`，SSE 响应体中 `state_change.data.planId` 与 POST 返回一致。

接手提醒：
- 当前后端 SSE 仍是最小占位流：发送 `heartbeat` 和 `START -> INTENT` 后 complete；浏览器 EventSource 会显示 `retrying` 并可能重复收到最小事件。这不阻塞 INT-001，但后续 `B1-004` / `INT-002` 需要处理完整状态机和更真实的流生命周期。
- 历史说明：INT-001 当时不是完整 `plan_ready` 或执行链路；其中 `B1-004`、`F1-005`、`INT-002` 和 `INT-003` 已在后续任务收口。

## 2026-05-22 前端 evaluator 浏览器 MCP 规则

当前前端验收硬规则：所有涉及前端 UI 的任务，evaluator 子代理必须使用 Chrome DevTools MCP。Chrome DevTools MCP 用于模拟真实用户交互、状态等待、页面快照、console、network、DOM / accessibility 和视觉复核；Playwright MCP 可作为补充但不再强制要求。

本轮新增或更新：
- `docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`
- `docs/qa/WF-002-frontend-evaluator-browser-mcp.md`
- `docs/dev-workflow.md`
- `docs/frontend-contract.md`
- `docs/quality.md`
- `docs/contracts/_template.md`
- `docs/qa/evaluator-template.md`
- `frontend/F1-handoff.md`
- `docs/decision-log.md`
- `feature_list.json`
- `progress.md`

接手提醒：
- 后续前端 evaluator 报告必须写 Chrome DevTools MCP 证据。
- 缺少 Chrome DevTools MCP 证据时，前端 UI 任务不能标记为 `verified`。

验证结果：
- 独立 evaluator 子代理 Euler (`019e4db4-865e-7fe2-85ed-4dd5e5b6f3c2`) 已只读检查本轮前端 harness 文档并放行。
- evaluator 曾确认前端验收入口、QA 模板、handoff、decision log、progress 和 feature metadata 覆盖双工具要求；该规则后续已调整为只强制 Chrome DevTools MCP。
- `feature_list.json` 已通过 `ConvertFrom-Json` 解析，`WF-002` 已按 evaluator 证据标记为 `verified`。

## 2026-05-22 Harness 测试职责调整

本轮将 generator / evaluator 流程从旧的“generator 可自行最终验证”调整为硬性职责分离：所有 generator agent 完成开发后，测试阶段必须委托独立 evaluator 子代理执行；generator 自己运行的本地冒烟、typecheck、build、curl、Playwright 或脚本结果只能作为开发准备记录，不能单独作为 `verified` 证据。

本轮新增或更新：
- `AGENTS.md`
- `docs/architecture.md`
- `docs/dev-workflow.md`
- `docs/quality.md`
- `docs/backend-contract.md`
- `docs/frontend-contract.md`
- `docs/contracts/_template.md`
- `docs/contracts/WF-001-generator-subagent-testing.md`
- `docs/qa/WF-001-generator-subagent-testing.md`
- `docs/qa/evaluator-template.md`
- `docs/decision-log.md`
- `docs/initiallizer-agent-prompt.md`
- `backend/HANDOFF.md`
- `frontend/F1-handoff.md`
- `feature_list.json`
- `progress.md`

接手提醒：
- 后续任意 B1 / B2 / F1 / INT generator 做完实现后，都要把待测范围交给 evaluator 子代理。
- 小任务也不能由同一个 generator 自测后直接标记 `verified`。
- 如果当前环境无法启动 evaluator 子代理，任务只能保持 `todo` / `in_progress` / `blocked`，不能标为 `verified`。

验证结果：
- 独立 evaluator 子代理 Newton (`019e4da9-79c0-79a3-b7f1-c79f5e594c58`) 已只读检查本轮 harness 文档并放行。
- evaluator 确认未发现仍允许 generator 自己完成最终测试、自己直接标记完成或以 generator-only 命令作为 `verified` 证据的活跃表述。
- `feature_list.json` 已通过 `ConvertFrom-Json` 解析，`WF-001` 已按 evaluator 证据标记为 `verified`。

## 2026-05-21 F1-sprint1-generator 更新

F1-001 已完成并验证。`frontend/` 现在已经是 Vue 3 + Vite + Pinia + Naive UI 项目，`pnpm dev` 默认端口为 `5173`，`feature_list.json` 中 `F1-001` 已标记为 `verified`。

本轮新增或更新：
- `docs/contracts/F1-001-vue-skeleton.md`
- `.gitignore`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/index.html`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/styles/main.css`
- `frontend/public/favicon.svg`
- `frontend/.gitignore`
- `frontend/README.md`

验证命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

验证结果：`pnpm typecheck` passed，`Verify passed.` 直接在当前沙盒 shell 运行 `.\verify.ps1 -Target frontend -Mode fast` 会因为找不到宿主全局 `pnpm` 失败；宿主 PowerShell 路径已验证通过。

额外构建验证：`pnpm build` 通过，Vite 生产构建成功，主 JS chunk 约 290.51 kB / gzip 91.94 kB。

UI 冒烟验证：`pnpm dev` 已启动并监听 `127.0.0.1:5173`；Playwright 打开页面可见 WeekendTravel 骨架，点击“开始预演”后 Pinia 状态从 `START/idle` 变为 `INTENT/connecting`，console 无 error/warning。

下一步建议：
- F1：推进 `F1-002`，实现 API client 和 mock fixture mode。
- B1：推进 `B1-002`，实现 SSE emitter and heartbeat。

## 当前状态

Initializer 已把仓库整理成长期 agent 开发 harness。前端 F1-001 Vue 骨架已 scaffold 并通过验证；后端已经完成 B1-001 的最小初始化：Spring Boot + Maven 项目已存在，`GET /health`、全局 CORS 和基础测试已落地，默认端口已固定为 `8000`。

## 已创建文件

- `AGENTS.md`
- `docs/product-spec.md`
- `docs/architecture.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `docs/backend-contract.md`
- `docs/dev-workflow.md`
- `docs/quality.md`
- `docs/handoff.md`
- `docs/decision-log.md`
- `docs/contracts/_template.md`
- `docs/contracts/B1-001-health-cors.md`
- `docs/contracts/F1-001-vue-skeleton.md`
- `docs/qa/evaluator-template.md`
- `docs/fixtures/sse-events.jsonl`
- `docs/fixtures/plan-ready-family.json`
- `docs/fixtures/plan-ready-friends.json`
- `feature_list.json`
- `progress.md`
- `init.ps1`
- `verify.ps1`
- `backend/README.md`
- `frontend/README.md`

已有文件已保留：

- `WeekendTravel_ProductDoc.md`
- `docs/initiallizer-agent-prompt.md`

## 尚未实现内容

- `INT-003` friends complete path 已在 2026-06-01 完成；friends 场景必须引用自己的 INT-003 evaluator 证据，不能复用 `INT-002` 的 family happy path 证据。
- `QA-001` 21 Golden Cases 尚未执行；需要单独记录 family、friends、boundary case 和 SLO 指标。
- 真实异常 / degrade 链路仍建议单独补证，尤其是真实后端 `error(code=DEGRADE)`、非 DEGRADE error、bookingFail / routeTooFar 等浏览器网络证据。
- 历史说明：本提醒中的 `B1-004` 状态已在 2026-06-01 补齐单独 contract / evaluator 证据并标记为 `verified`；不要再按旧状态处理。

## 最高优先级下一步

集成优先：

1. 进入真实异常 / degrade 补证任务，覆盖 `error(code=DEGRADE)`、非 DEGRADE error 和对应前端可见状态。
2. 进入 `QA-001` 前先补 golden cases 执行计划，避免把 family / friends 两条 happy path 当作 21 cases 覆盖。
3. 按 `docs/dev-workflow.md` 继续为每轮任务补 contract、独立 evaluator 证据、`feature_list.json`、`progress.md` 和 handoff。

前端 / 联调优先：

1. 继续保持 `frontend` fast verify 通过，避免 real mode 调整影响 mock fixture 与 typecheck。
2. 如补真实异常链路，优先让 Chrome DevTools MCP 记录 console、network、DOM / accessibility 和最终可见状态。

## 验证结果

已运行回读和轻量验证：

```powershell
Get-Content -Raw -Encoding UTF8 -LiteralPath 'AGENTS.md'
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\api-contract.md'
Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\fixtures\plan-ready-family.json' | ConvertFrom-Json
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\fixtures\plan-ready-friends.json' | ConvertFrom-Json
Get-Content -Encoding UTF8 -LiteralPath 'docs\fixtures\sse-events.jsonl' | ForEach-Object { $_ | ConvertFrom-Json | Out-Null }
```

结果：

- `AGENTS.md`、`docs/api-contract.md` 中文回读正常。
- `feature_list.json` 可解析。
- 两个 `plan-ready` fixture 可解析。
- `sse-events.jsonl` 共 23 行，逐行可解析。

脚本验证：

```powershell
.\init.ps1 -Target all
.\verify.ps1 -Target all -Mode fast
```

当前 PowerShell 执行策略直接拦截 `.ps1`，错误类型为 `PSSecurityException / UnauthorizedAccess`。已用一次性方式验证脚本逻辑：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast
```

结果：

- `init.ps1` 成功运行：Java、Node、pnpm 可见；初始化阶段 Maven 缺失，backend/frontend 均提示尚未初始化。
- `verify.ps1` 在 initializer 阶段曾返回失败：当时 `backend\pom.xml` 缺失、`frontend\package.json` 缺失。
- 2026-05-21 已完成 B1-001 后续验证：`./backend/mvnw -f "backend/pom.xml" test` 通过；`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 通过；`./backend/mvnw -f "backend/pom.xml" spring-boot:run` 可启动后端；`curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 200，且包含 `Access-Control-Allow-Origin: http://localhost:5173`。
- `verify.ps1` 已改为调用 `backend/mvnw.cmd`，backend verify 不再依赖系统 `mvn` 在 PATH 中。
- F1-001 已通过 frontend fast verify、`pnpm build` 和 Playwright 冒烟验证。
- `git status --short` 可运行；Git 仍可能输出全局 ignore 权限警告，但未阻塞本地开发。

## 已知冲突和处理

- 产品文档中后端端口同时出现 `8000` 和 `8080`。本仓库契约统一为 `8000`，见 `docs/decision-log.md`。
- 产品文档 API 示例出现 snake_case，但数据模型强调 camelCase。本仓库正式 API 和 SSE 线缆字段统一为 camelCase，见 `docs/api-contract.md`。
- 产品文档里的 OpenAI SDK 版本和模型名可能随时间变化。本轮不锁死；真正接入前必须核验官方 OpenAI developer docs / MCP，并追加 `docs/decision-log.md`。
- 补充设计文档提到 20 条 golden case，但本仓库正式 QA 仍以 21 条为准，见 `docs/decision-log.md`。
- 补充设计文档提到正常场景 Plan B 触发率小于等于 30%，本仓库已与“异常注入 Plan B 触发准确率 100%”拆成两个指标。
- 补充设计文档提到“情侣”时间骨架模板，但当前 Demo 场景仍只有 `family` 和 `friends`，不扩接口枚举。

## 接手提醒

- 每轮只推进一个清楚小目标。
- API 字段变更先改 `docs/api-contract.md`。
- 没有验证证据，不要把 `feature_list.json` 状态改成 `verified`。
- 当前 `INT-002` 和 `INT-003` 已完成并 verified；继续推进时优先做真实异常 / degrade 补证或 `QA-001`，不要回退到旧的 B1/F1 初始化队列。
- `verify.ps1` 已与 Maven Wrapper 工作流对齐，可直接用于 backend fast verify。
