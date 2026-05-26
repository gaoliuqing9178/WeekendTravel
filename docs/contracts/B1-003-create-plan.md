# Contract: B1-003 Create plan

## 本轮目标

- 实现 `POST /api/plan` 的最小占位创建接口。
- 接收 `text`、`scenario`、可选 `origin`。
- 成功时返回 HTTP `202` 和 camelCase 的 `planId`、`status`。
- 非法输入时返回统一错误结构 `{ error, message, details? }`。
- 为后续 B1-004 状态机和 `plan_ready` SSE 链路保留扩展边界。

## 明确不做

- 不实现真实 Planner。
- 不触发完整状态机。
- 不返回完整 `Plan`。
- 不接真实 Tool 编排。
- 不改 `docs/api-contract.md` 已确认的正式线缆字段。

## 用户路径

1. 客户端向 `POST /api/plan` 发送自然语言需求。
2. 服务校验 `text`、`scenario` 和可选 `origin`。
3. 校验通过后返回 HTTP `202`。
4. 响应体包含新生成的 `planId` 和固定 `status=processing`。
5. 非法输入返回 HTTP `400` 和统一错误结构。

## UI 要求

- 本轮无 UI 改动。
- 前端只需能读取 `planId` 和 `status`，并在失败时展示 `message`。

## API / 后端要求

- 端点路径为 `POST /api/plan`。
- 请求头使用 `Content-Type: application/json`。
- 请求体字段：
  - `text`: 必填 string
  - `scenario`: 必填，且只能为 `family | friends`
  - `origin`: 可选 string
- 成功响应：
  - HTTP `202`
  - body 为 `{ "planId": "...", "status": "processing" }`
- 错误响应：
  - HTTP `400`
  - body 为 `{ "error": "INVALID_INPUT", "message": "...", "details": { ... } }`
- 线缆字段统一使用 camelCase。

## 数据和状态要求

- `planId` 由后端生成，格式至少满足可读前缀 `plan_`。
- `status` 固定为 `processing`。
- `scenario` 内部可归一化为 lower-case，但线缆语义仍为 `family` / `friends`。
- `origin` 为可选；空白字符串不应触发额外错误。
- 非法输入至少覆盖：
  - `text` 缺失或 blank
  - `scenario` 缺失
  - `scenario` 非 `family|friends`
  - 空 body 或非法 JSON

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 向 `POST /api/plan` 发送合法请求，确认返回 `202`、`planId`、`status=processing`。
- 向 `POST /api/plan` 发送缺失 `text`、缺失 `scenario`、非法 `scenario` 的请求，确认返回 `400` 和统一错误结构。

自动验证：

- `./backend/mvnw -f "backend/pom.xml" test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录后端验证命令、成功与失败请求结果，以及放行结论。

## 失败阈值

- `POST /api/plan` 不可用，失败。
- 成功响应不是 HTTP `202`，失败。
- 成功响应缺少 `planId` 或 `status`，失败。
- 返回 snake_case 线缆字段，失败。
- 非法输入没有统一错误结构，失败。
- 只完成本地自测、没有独立 evaluator 证据，不能标为 `verified`.
