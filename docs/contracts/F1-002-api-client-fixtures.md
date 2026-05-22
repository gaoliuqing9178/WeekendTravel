# Contract: F1-002 API client and mock fixture mode

## 本轮目标

- 在 `frontend/` 内新增类型化 API client，字段命名只跟随 `docs/api-contract.md` 的 camelCase 契约。
- 建立 mock fixture mode，让前端可以在后端业务接口完成前读取 `docs/fixtures/plan-ready-family.json`、`docs/fixtures/plan-ready-friends.json` 和 `docs/fixtures/sse-events.jsonl`。
- 把 fixture 解析纳入前端 fast verify，避免 JSON / JSONL 或字段命名漂移。

## 明确不做

- 不实现完整 `useSSE` composable、自动重连、日志自动滚动或真实流式 UI；这些属于 `F1-003`。
- 不实现完整 PlanCard、ConfirmButton、ExecutionTracker；这些属于 `F1-005`。
- 不修改 `docs/api-contract.md` 的字段或枚举。
- 不引入 snake_case 字段兼容层；动作枚举值继续按 API 契约保留 lower_snake_case 语义。
- 不接入真实 OpenAI API。

## 用户路径

1. 用户打开前端骨架，选择家庭或朋友场景。
2. 用户点击“开始预演”。
3. mock client 根据场景创建本地 `planId`，加载 plan_ready fixture，并解析 SSE JSONL fixture 为日志预览。
4. 前端状态更新为 fixture 中的 `CONFIRM` 方案状态。

## UI 要求

- 保持 F1-001 工作台结构，不做大幅视觉重构。
- 用户点击后能看见 fixture 已加载的方案摘要、Plan B 信息和日志事件。
- UI 文案不得暗示真实后端规划已经完成。

## API / 后端要求

- real mode API client 只调用 `docs/api-contract.md` 中已有路径：
  - `POST /api/plan`
  - `POST /api/plan/{planId}/execute`
  - `POST /api/plan/{planId}/clarify`
  - `PATCH /api/plan/{planId}/adjust`
  - `POST /api/debug/scenario`
  - `GET /api/plan/{planId}/stream`
- mock mode 不访问后端网络。
- API client 默认 base URL 为 `http://localhost:8000`，可通过 `VITE_API_BASE_URL` 覆盖。
- API mode 默认 `mock`，可通过 `VITE_API_MODE=real` 切换真实 API client。

## 数据和状态要求

- 前端类型必须覆盖 `Scenario`、`AgentState`、`Plan`、`TimeSlot`、`ActionItem` 和当前 API / SSE payload。
- fixture parser 必须拒绝 JSON key 中的 underscored 字段名。
- `plan-ready-family.json` 和 `plan-ready-friends.json` 必须解析为 `plan_ready` payload，并校验 `planId`、`scenario`、`status`、`timeline`、`actions` 等基础字段。
- `sse-events.jsonl` 必须逐行解析，并校验 `event` 与 `data.type` 一致。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、新增前端 API client、fixture parser 和验证脚本。
- 独立运行前端 fast verify。
- 确认没有新增 snake_case 字段兼容层。

前端浏览器验收：

- 本轮只做 API client 和 fixture mode，不要求完整 UI 浏览器验收；如果 evaluator 额外打开页面，只作为补充证据。

手动路径：

- 在 `frontend/` 下运行 `pnpm verify:fixtures`，应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL。

自动验证：

- 在仓库根目录运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

API / 日志 / 截图证据：

- `pnpm verify:fixtures` 输出 fixture 解析通过。
- `pnpm typecheck` 通过。
- evaluator 子代理报告写入 `docs/qa/F1-002-api-client-fixtures.md`。

## 失败阈值

- 核心用户路径不可用，失败。
- UI 显示成功但 API 或状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只实现占位或 mock 却标记正式完成，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
