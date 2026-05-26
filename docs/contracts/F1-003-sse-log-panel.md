# Contract: F1-003 useSSE composable and log panel

## 本轮目标

- 在 `frontend/` 内实现 `useSSE` composable，统一处理 real mode `EventSource` 连接与 mock mode fixture 事件回放。
- 将 `docs/fixtures/sse-events.jsonl` 的逐条 SSE payload 渲染到正式 LogPanel，而不是一次性日志预览。
- LogPanel 必须在新增事件后自动滚动到最新事件，并保留可读的连接状态、事件类型、时间、标题和详情。
- Pinia store 按 SSE 事件类型更新 `planId`、`agentState`、`connectionState`、`currentPlan`、Plan B、clarification、execute result、done 和 error/degrade 状态。

## 明确不做

- 不实现 F1-004 的完整输入提交链路改造；本轮只复用现有“开始预演”入口。
- 不实现 F1-005 的完整 PlanCard、ConfirmButton 或 ExecutionTracker。
- 不实现 F1-006 的正式 ClarifyBubble 回复调用。
- 不实现 F1-007 的正式 AdjustPanel。
- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不引入新的前端框架、SSE 库或 snake_case 字段兼容层。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户选择家庭或朋友场景，并点击“开始预演”。
3. 前端调用现有 API client 创建 plan，占位获得 `planId`。
4. mock mode 下 `useSSE` 逐条消费 `getSseFixtureFrames()`；real mode 下 `useSSE` 调用 `openPlanStream(planId)` 连接 `/api/plan/{planId}/stream`。
5. LogPanel 实时追加事件并自动滚动到最新事件。
6. `plan_ready`、`replan`、`clarification_request`、`adjust_result`、`execute_result`、`done`、`error` 等事件会同步更新 Pinia 状态。

## UI 要求

- 页面首屏仍是工作台，不做营销落地页。
- LogPanel 是正式实时日志面板，不能再标成“fixture 日志预览”。
- LogPanel 必须覆盖并可见展示：
  - `heartbeat`
  - `state_change`
  - `tool_call`
  - `tool_result`
  - `replan`
  - `clarification_request`
  - `adjust_result`
  - `plan_ready`
  - `execute_result`
  - `done`
  - `error`
- `replan` / Plan B 和 `error` / `DEGRADE` 事件必须有明显视觉区分。
- 日志容器必须有固定最大高度、可滚动、追加事件后自动滚到底部。
- UI 文案不得暗示后端完整规划链路已经完成；mock mode 必须清楚表达为 fixture 回放。

## API / 后端要求

- real mode 只连接 `docs/api-contract.md` 规定的 `GET /api/plan/{planId}/stream`。
- `EventSource` URL 必须通过 `PlannerApiClient.openPlanStream(planId)` 生成，继续使用 `VITE_API_BASE_URL` / `VITE_API_MODE=real` 这条既有配置路径。
- mock mode 不访问后端网络，只消费 `docs/fixtures/sse-events.jsonl`。
- 后端当前只保证最小 heartbeat / state_change 占位流；前端不能把完整 `plan_ready` 链路标成真实后端已完成。

## 数据和状态要求

- `useSSE` 至少表达 `idle / connecting / open / retrying / closed / error` 连接状态。
- 所有 SSE `data` JSON 必须按 `data.type` 分发；事件名和 `type` 不一致时应进入错误路径。
- `state_change` 更新当前 `agentState`。
- `plan_ready` 和 `adjust_result` 更新 `currentPlan`。
- `replan` 在日志和状态摘要中提供 Plan B 可见信号。
- `clarification_request` 记录当前 pending clarification，但本轮不实现正式回复 UI。
- `execute_result` 按 `actionId` 尝试更新当前 plan action 状态。
- `done` 进入 `DONE` 状态。
- `error` 按 `code=DEGRADE` 进入 `DEGRADE`，否则进入 `FAILED`，并显示可读错误消息。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、新增 `useSSE`、LogPanel、Pinia store 和 App 接入。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- Playwright MCP 证据：打开页面、点击“开始预演”、等待 fixture SSE 逐条渲染、确认 LogPanel 可见、自动滚动后末尾可见 `done` / `error` 或至少最新事件。
- Chrome DevTools MCP 证据：页面 snapshot / DOM 或 accessibility 结构包含 LogPanel、console 无错误、network 在 mock mode 下只加载前端模块和 fixture raw import，不访问真实后端 stream。

手动路径：

- 在仓库根目录运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 仍应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-003-sse-log-panel.md`。
- 前端截图证据写入 `docs/qa/`，文件名使用 `F1-003-*` 前缀。
- QA 报告必须分别记录 Playwright MCP 与 Chrome DevTools MCP 证据。

## 失败阈值

- `useSSE` 未调用真实 `openPlanStream(planId)` 或 real mode URL 不符合 `/api/plan/{planId}/stream`，失败。
- fixture SSE 事件不能逐条渲染到 LogPanel，失败。
- LogPanel 不会自动滚动到最新事件，失败。
- `plan_ready` 不更新 `currentPlan`，`state_change` 不更新 `agentState`，失败。
- UI 显示成功但状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
