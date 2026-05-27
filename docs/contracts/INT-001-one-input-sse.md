# Contract: INT-001 Sprint 1 integration: one input to SSE state_change

## 本轮目标

- 打通 Sprint 1 最小真实联调路径：前端 real mode 输入一句 Demo 需求后提交到后端。
- 后端 `POST /api/plan` 返回 camelCase `planId` 和 `status=processing`。
- 前端用该 `planId` 连接 `GET /api/plan/{planId}/stream`，并在日志面板渲染真实后端 SSE `state_change`。
- 本轮只证明“一句话输入 -> 创建 plan -> 收到 state_change”链路可用。

## 明确不做

- 不实现完整 B1 状态机，不推进 `B1-004`。
- 不要求 real mode 推送 `plan_ready`、`tool_call`、`tool_result`、`execute_result` 或 `done`。
- 不实现 PlanCard、ConfirmButton、ExecutionTracker、ClarifyBubble 或 AdjustPanel。
- 不新增 API 字段，不修改 `docs/api-contract.md` 的线缆契约。
- 不接入 OpenAI、真实地图、真实下单、真实预约或真实配送。

## 用户路径

1. 后端运行在 `http://localhost:8000`。
2. 前端以 `VITE_API_MODE=real` 运行在 `http://localhost:5173` 或 `http://127.0.0.1:5173`。
3. 用户在 `InputPanel` 保留或输入一条家庭 / 朋友 Demo 自然语言需求。
4. 用户点击“提交规划”。
5. 前端调用 `POST /api/plan`，页面显示后端返回的 `planId`。
6. 前端打开 `/api/plan/{planId}/stream`。
7. 日志面板出现后端 SSE `state_change`，并显示 `START -> INTENT`。

## UI 要求

- `InputPanel` 必须可编辑自然语言输入，并能提交真实 API 请求。
- `Pinia 状态` 区必须能显示后端返回的 `planId` 和当前 Agent 状态。
- `LogPanel` 必须追加真实 SSE 事件，而不是只展示 fixture 或硬编码成功状态。
- `state_change` 事件必须让页面 Agent 状态进入 `INTENT`。

## API / 后端要求

- `POST /api/plan` 必须返回 HTTP `202`。
- 响应 JSON 必须包含非空 `planId` 和 `status`。
- `GET /api/plan/{planId}/stream` 必须返回 `text/event-stream`。
- SSE stream 必须至少包含一条 `heartbeat` 和一条 `state_change`。
- `state_change` 的 `data.type` 必须等于 `state_change`，事件名也必须是 `state_change`。
- `state_change.data.planId` 必须与本轮 `POST /api/plan` 返回的 `planId` 一致。

## 数据和状态要求

- 线缆字段继续使用 camelCase。
- 不允许新增 snake_case 兼容分支。
- 本轮 real mode 最小后端流只要求 `START -> INTENT`；后续完整状态机由 `B1-004` 继续。
- mock fixture mode 可以继续存在，但不能作为 INT-001 放行的唯一证据。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`frontend/F1-handoff.md`、`backend/HANDOFF.md`、相关前后端代码和 generator handoff。
- 独立运行必要命令和浏览器验证，不使用 generator 自测作为最终放行依据。
- 在 `docs/qa/INT-001-one-input-sse.md` 写入结论。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- Playwright MCP 证据：打开 real mode 前端，输入或保留 Demo 文本，点击“提交规划”，等待页面出现后端 `planId`、`INTENT` 和日志中的 `state_change` / `START -> INTENT`，保存截图或等价证据。
- Chrome DevTools MCP 证据：页面 snapshot / accessibility 覆盖 InputPanel 与 LogPanel；console 无 error / warn；network 包含 `POST http://localhost:8000/api/plan [202]` 和 `/api/plan/{planId}/stream` SSE 请求；视觉复核日志中出现 `state_change`。

手动路径：

- 启动后端和 real mode 前端，按用户路径提交一条 Demo 输入。
- 确认页面上 `Plan ID` 不再是“未创建”，Agent 显示 `INTENT`，日志中出现 `state_change`。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast`
- 后端测试必须覆盖 `POST /api/plan` 后用返回的 `planId` 打开 SSE，并收到相同 `planId` 的 `state_change`。

API / 日志 / 截图证据：

- QA 报告记录后端 / 前端 verify 命令结果。
- QA 报告记录浏览器真实 API 路径、network 证据和截图路径。

## 失败阈值

- 前端无法提交真实 Demo 输入，失败。
- `POST /api/plan` 没有返回非空 `planId`，失败。
- 前端没有用后端返回的 `planId` 建立 SSE，失败。
- 日志面板没有渲染真实后端 `state_change`，失败。
- `state_change.data.planId` 与创建响应的 `planId` 不一致，失败。
- UI 显示成功但 API 或状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只用 mock fixture 证明链路，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
