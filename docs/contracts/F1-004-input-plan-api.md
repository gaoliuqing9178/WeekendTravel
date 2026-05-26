# Contract: F1-004 InputPanel and POST /api/plan

## 本轮目标

- 将当前首页入口整理成正式 `InputPanel`，保留自然语言输入、场景选择和可选出发位置。
- 用户提交后进入可见 planning 状态。
- real API mode 下必须调用 `POST /api/plan`，读取 camelCase `planId` 和 `status`。
- 成功创建 plan 后立即交给既有 `useSSE` 连接 `/api/plan/{planId}/stream`。
- mock mode 继续使用 fixture 创建 plan 和回放 SSE，不访问后端网络。

## 明确不做

- 不实现完整 PlanCard、ConfirmButton 或 ExecutionTracker；这些属于 `F1-005`。
- 不实现 ClarifyBubble 回复调用；这些属于 `F1-006`。
- 不实现 AdjustPanel；这些属于 `F1-007`。
- 不扩展后端 Planner、状态机或完整 `plan_ready` 真实链路。
- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不引入 snake_case 字段兼容层。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户在 `InputPanel` 输入自然语言需求，选择家庭或朋友场景，并可填写出发位置。
3. 用户点击“提交规划”。
4. 前端显示创建 plan / 连接日志流的 planning 状态。
5. mock mode 下读取 fixture planId 并回放 `docs/fixtures/sse-events.jsonl`。
6. real mode 下调用 `POST /api/plan`，成功后用响应中的 `planId` 连接 `/api/plan/{planId}/stream`。

## UI 要求

- 页面首屏仍是工作台，不做营销落地页。
- `InputPanel` 必须包含：
  - 场景选择：`family | friends`
  - 自然语言输入：必填
  - 出发位置：可选，空白时不发送 `origin`
  - 提交按钮：运行中 loading，空输入或运行中禁用
- 提交后必须有可见状态，例如“正在创建 plan”“正在连接日志流”“已创建 plan”或失败提示。
- mock mode 文案不能暗示真实后端完整规划已经完成。
- 表单控件必须有可访问 label 或等效语义，键盘提交不能绕过校验。

## API / 后端要求

- real mode 只调用 `docs/api-contract.md` 规定的 `POST /api/plan`。
- 请求体字段只包含：
  - `text`: string
  - `scenario`: `family | friends`
  - `origin`: string，可选
- 成功响应读取：
  - `planId`
  - `status`
- API 错误必须显示后端返回的 `message`。
- 成功创建后继续调用既有 `PlannerApiClient.openPlanStream(planId)` 路径建立 SSE。
- mock mode 不访问后端网络。

## 数据和状态要求

- Pinia store 继续作为状态入口，保留 `planId`、`agentState`、`connectionState`、`currentPlan`、`logEvents` 等既有字段。
- 提交开始时清理上一轮 plan、错误、clarification、日志和 SSE 连接。
- 成功创建时写入 `planId`，并追加 client log，内容包含响应 `status`。
- 空白 `origin` 不发送到 API request。
- `setScenario` 切换场景时继续带出对应 demo prompt，但用户仍可自由编辑。
- 不添加任何 snake_case 线缆字段或兼容映射。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/components/InputPanel.vue`、`frontend/src/stores/planner.ts`、`frontend/src/api/client.ts`、`frontend/src/App.vue`。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- Playwright MCP 证据：打开页面，编辑自然语言输入，点击“提交规划”，确认按钮 loading 或 planning 状态可见，mock mode 下日志开始追加并产生 planId。
- Playwright MCP real mode 证据：在后端可用时，以 `VITE_API_MODE=real` 打开前端，提交后确认浏览器发出 `POST /api/plan`，页面显示后端返回的 `planId` 并进入规划状态。
- Chrome DevTools MCP 证据：页面 snapshot / accessibility 结构包含 `InputPanel` 的表单控件，console 无错误；mock mode network 不访问真实后端，real mode network 包含 `POST /api/plan`。

手动路径：

- 在仓库根目录运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 仍应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。
- 可选补充：启动后端和 real mode 前端做一次真实 `POST /api/plan` 浏览器冒烟。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-004-input-plan-api.md`。
- 前端截图证据写入 `docs/qa/`，文件名使用 `F1-004-*` 前缀。
- QA 报告必须分别记录 Playwright MCP 与 Chrome DevTools MCP 证据。

## 失败阈值

- 用户不能输入自然语言并提交，失败。
- real mode 提交没有调用 `POST /api/plan`，失败。
- 成功响应未读取 camelCase `planId` 或 `status`，失败。
- 成功创建后没有进入可见 planning 状态，失败。
- 成功创建后没有把 `planId` 交给既有 SSE 连接路径，失败。
- mock mode 访问真实后端网络，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
