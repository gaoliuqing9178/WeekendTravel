# Contract: B2-005 MessageTool / composeShareMessage

## 本轮目标

- 实现 B2 Tool 层 `MessageTool / composeShareMessage`。
- 生成 deterministic fallback 中文转发消息，不依赖 LLM 或网络。
- 明确未来 LLM 只可用于话术生成，且必须遵守 `docs/backend-contract.md` 的 LLM 边界。
- 输出包含 `shareMessage` 的 Plan-compatible payload，供后续 B1 `Plan` 合入。
- 增加后端 JUnit 测试，覆盖家庭、朋友、Plan B、动作备注、边界和 LLM 禁用边界。

## 明确不做

- 不实现 B1 `Plan`、Planner、状态机或 `plan_ready` SSE。
- 不实现 `POST /api/plan` 或任何新的 REST API。
- 不改 `docs/api-contract.md` 正式线缆字段。
- 不接入 OpenAI SDK，不读取 `OPENAI_API_KEY`，不调用真实 LLM。
- 不让 LLM 参与 POI 选择、规划决策、状态转移或 Plan B 判断。

## 用户路径

1. B1 后续在 `PACK` 或执行前调用 `MessageTool.composeShareMessage()`。
2. `MessageTool` 根据已定稿的 plan draft、timeline 和 actions 生成中文转发消息。
3. B1 后续把 `MessageResult.plan().shareMessage()` 或 `MessageResult.shareMessage()` 写入 API Contract 的 `Plan.shareMessage`。
4. 前端在 `plan_ready.plan.shareMessage` 中渲染转发消息预览。

## UI 要求

- 本轮无前端 UI 改动。
- 本轮不涉及 Playwright MCP / Chrome DevTools MCP。

## API / 后端要求

- 新增代码位于 `backend/src/main/java/com/weekendtravel/backend/b2/tool/`。
- Tool 入参和返回值使用 Java record 或 POJO，字段名保持 camelCase。
- `MessageTool` 暴露：
  - `composeShareMessage(MessageRequest request)`
  - `execute(MessageRequest request)` 作为兼容入口。
- `MessageRequest` 至少包含：
  - `planId`
  - `scenario`: `family` 或 `friends`
  - `status`
  - `isPlanB`
  - `planBReason`
  - `summary`
  - `timeline`
  - `actions`
  - `totalDurationHours`
  - `replanCount`
  - `createdAt`
- `MessageResult` 至少包含：
  - `planId`
  - `scenario`
  - `shareMessage`
  - `plan`
  - `llmUsed`
  - `templateVersion`
  - `latencyMs`
- `MessageResult.plan` 必须是 Plan-compatible payload，且包含 `shareMessage` 字段。
- `llmUsed` 本轮必须固定为 `false`。

## 数据和状态要求

- 输出必须稳定：同一输入重复调用返回相同 `shareMessage`。
- `family` 消息应优先呈现亲子、儿童友好、低负担餐饮或少步行信息。
- `friends` 消息应优先呈现玩、吃、拍照、聊天或路线轻松信息。
- 有 `isPlanB=true` 和 `planBReason` 时，消息必须透明说明 Plan B 原因。
- 时间线按 `order` 升序输出；缺失 order 时保持输入顺序。
- `send_message` 动作不应出现在“已预约/已备注”类执行摘要里，避免把生成消息本身写成外部动作。
- 缺失 `planId`、非法 `scenario` 或空 `timeline` 必须抛出明确异常。

## LLM 边界

- 本轮只实现 deterministic fallback 模板。
- 未来如接入 LLM，必须先核验官方 OpenAI developer docs / MCP，并在 `docs/decision-log.md` 记录 SDK、模型、调用方式、超时和兜底策略。
- LLM 只能做分享话术生成；不得选择 POI、决定状态转移、判断 Plan B、执行下单或预约。
- OpenAI key 只能从 `OPENAI_API_KEY` 读取，不能写入源码或配置文件。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- QA 报告请使用中文，命令、类名和字段名保留英文原文。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageTool.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageRequest.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageResult.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessagePlanPayload.java`。
- 查看测试覆盖家庭、朋友、Plan B、`shareMessage` 进入 plan payload、`llmUsed=false` 和错误路径。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- 或在 `backend/` 内运行 `.\mvnw.cmd test`

## 失败阈值

- 不能生成 deterministic fallback `shareMessage`，失败。
- `llmUsed` 不是 `false` 或出现真实 LLM / OpenAI 调用，失败。
- 输出没有 Plan-compatible `shareMessage` 字段，失败。
- 没有独立 evaluator 证据，不能标为 `verified`。
