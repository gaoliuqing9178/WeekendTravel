# Contract: F1-001 Vue Skeleton

## 本轮目标

- 在 `frontend/` 下创建 Vue 3 + Vite 前端项目骨架。
- 配置 Pinia 作为全局状态入口。
- 配置 Naive UI 作为基础组件库入口。
- 配置 `pnpm dev` 默认运行在 `http://localhost:5173`。
- 通过 `.\verify.ps1 -Target frontend -Mode fast` 的前端快速验证。

## 明确不做

- 不实现真实 `POST /api/plan`、SSE 连接或执行动作。
- 不新增 `docs/api-contract.md` 之外的 API 字段。
- 不接入 OpenAI SDK、第三方地图、真实预约或支付能力。
- 不把本地演示状态标记为正式业务完成。

## 用户路径

1. 开发者进入 `frontend/`。
2. 运行 `pnpm dev`。
3. 浏览器访问 `http://localhost:5173`。
4. 页面显示 WeekendTravel 前端骨架、场景选择、自然语言输入区、Pinia 状态摘要和日志占位区。

## UI 要求

- 首屏不是空白页，必须能看到 WeekendTravel 的前端工作台骨架。
- 页面需要体现家庭场景和朋友场景两个 Demo 入口。
- Naive UI 组件必须真实挂载并用于页面基础控件。
- 交互控件需要有可见禁用态、焦点态和基础响应式布局。

## API / 后端要求

- 本轮不调用后端。
- 本轮只保留与 `docs/api-contract.md` 对齐的前端边界说明。
- 后续 F1-002 开始再实现 API client 和 fixture mode。

## 数据和状态要求

- Pinia store 至少表达：
  - 当前 `planId` 占位状态。
  - 当前 `agentState`。
  - 用户输入、场景和 origin。
  - SSE 连接状态占位。
  - 日志事件数组占位。
- 状态字段命名使用 camelCase。

## 验收方式

手动路径：
- `cd frontend`
- `pnpm dev`
- 打开 `http://localhost:5173`，确认页面可见且端口为 `5173`。

自动验证：
- `.\verify.ps1 -Target frontend -Mode fast`

API / 日志 / 截图证据：
- 本轮证据以 `pnpm typecheck` 或 `pnpm build` 通过为准。
- 若验证脚本因本机缺少 `pnpm` 失败，不能将该任务标记为 `verified`。

## 失败阈值

- `frontend/package.json` 不存在，失败。
- `pnpm dev` 不是默认 `5173`，失败。
- Pinia 或 Naive UI 未配置，失败。
- `.\verify.ps1 -Target frontend -Mode fast` 未运行或无通过证据，不能标记为 `verified`。
