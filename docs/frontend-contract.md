# Frontend Contract

F1 负责 `frontend/` 下全部前端实现。前端可以先脱离后端，基于 `docs/api-contract.md` 和 `docs/fixtures/` 开发。

## 技术栈

- Vue 3
- Vite
- Tailwind CSS
- Naive UI
- Pinia
- SSE / EventSource
- 默认端口：`5173`

不得引入产品文档之外的新前端框架来“顺手优化”。

## F1 职责

- Web UI 总体布局。
- 输入面板和场景选择。
- API client，统一读取 `docs/api-contract.md` 定义的字段。
- Mock fixture 模式：后端未完成时，使用 `docs/fixtures/sse-events.jsonl`、`plan-ready-family.json`、`plan-ready-friends.json`。
- `useSSE` composable：连接、事件分发、自动滚动日志、错误和重连状态。
- 日志面板：展示 `state_change`、`tool_call`、`tool_result`、`replan`、`clarification_request`、`adjust_result`、`execute_result`、`done`、`error`。
- 方案卡：展示 timeline、Plan B 标记、执行动作、分享消息。
- ConfirmButton：只在 `CONFIRM` 且有可执行方案时可点击。
- ClarifyBubble：`CLARIFY` 状态只出现一个问题和 2-3 个快捷选项。
- AdjustPanel：`CONFIRM` 状态可微调，最多 3 次。
- Plan B、DONE、DEGRADE、FAILED、ERROR 的可见 UI 状态。

## 前端不做的事

- 不做规划决策。
- 不直接访问后端 Java 类、内部模型或 Mock service。
- 不硬编码“成功状态”来绕过 SSE。
- 不为了展示方便自定义一套字段名。
- 不把 debug scenario 当正式用户路径。
- 不在 UI 中暴露真实 token、key、个人路径。

## 状态要求

Pinia store 至少能表达：

- 当前 `planId`。
- 当前 `agentState`。
- 用户输入、场景和 origin。
- SSE 连接状态：idle / connecting / open / retrying / closed / error。
- 日志事件数组。
- 当前 Plan。
- pending clarification。
- adjust count 和 adjusting 状态。
- execution actions 状态。
- error / degrade message。

## Mock Fixture 使用方式

- UI 组件开发优先读取 `docs/fixtures/plan-ready-family.json` 和 `docs/fixtures/plan-ready-friends.json`。
- 日志流开发读取 `docs/fixtures/sse-events.jsonl`，逐行解析为 SSE data payload。
- fixture 字段必须保持 camelCase，不允许在前端写 snake_case 兼容分支，除非 `docs/api-contract.md` 明确要求。

## UI 验收

- 页面启动后不能有空白等待区域；规划中必须有可见加载状态和日志容器。
- 输入一句话后必须进入规划状态，按钮状态要可见变化。
- mock SSE 事件必须能实时渲染到日志面板，并自动滚动到最新事件。
- `plan_ready` fixture 必须能渲染方案卡、timeline、执行包、确认按钮和分享消息。
- Plan B 必须在日志和方案卡上高亮。
- `CLARIFY` 只允许出现一个反问气泡，且包含 2-3 个快捷选项。
- `ADJUST` 输入最多 3 次；第 4 次要有明确提示。
- `DONE`、`DEGRADE`、`FAILED`、`ERROR` 都必须有可见状态。
- 1080p 下无明显错位、遮挡、按钮文字溢出。

## 前端独立验证

后续 scaffold 后，`.\verify.ps1 -Target frontend -Mode fast` 至少应覆盖：

- 依赖存在性检查。
- typecheck 或 build。
- fixture 解析。

完整模式后续应覆盖：

- Playwright 打开页面。
- 输入 Demo 文本。
- mock SSE 渲染到日志。
- `plan_ready` 方案卡可见。
- Plan B、CLARIFY、ADJUST、DONE、ERROR 状态截图或 trace。
