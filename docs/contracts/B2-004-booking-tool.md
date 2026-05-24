# Contract: B2-004 BookingTool / bookOrOrder

## 本轮目标

- 实现 B2 Tool 层 `BookingTool / bookOrOrder`，用于模拟买票、订座、取号、配送、备注和取消。
- 成功执行时返回 deterministic mock `confirmationNo`。
- `bookingFail=true` 异常注入时，非取消动作返回 `failed` 或 `manual`，不能伪装成功。
- `bookOrOrder` 必须使用 `idempotencyKey` 防止重复模拟下单。
- Tool 层支持 `cancel_booking` 反向操作。
- 增加后端 JUnit 测试，覆盖成功、失败注入、幂等、取消和错误路径。

## 明确不做

- 不实现 B1 的 `POST /api/plan/{planId}/execute`。
- 不推送 SSE `execute_result` 事件。
- 不实现完整 `MockApiService` 门面。
- 不实现 `MessageTool / composeShareMessage`。
- 不接入真实支付、真实预约、真实配送、真实商家库存或第三方 API。
- 不让 LLM 参与执行动作、重试策略或状态转移。

## 用户路径

1. B1 后续进入 `EXECUTE` 时，为每个 `ActionItem` 调用 `BookingTool.bookOrOrder()`。
2. `BookingTool` 根据 `actionType`、`targetPoiId` 和 `idempotencyKey` 返回模拟执行结果。
3. B1 后续将工具结果映射为 `execute_result` SSE 事件或更新 Plan actions。
4. 如果用户重复确认或网络重试导致同一 `idempotencyKey` 再次提交，工具必须返回同一份模拟结果，不生成新的确认号。
5. 后续取消或回滚能力可用 `cancel_booking` 调用 Tool 层反向操作。

## UI 要求

- 本轮无前端 UI 改动。
- 本轮不涉及 Playwright MCP / Chrome DevTools MCP。

## API / 后端要求

- 新增代码位于 `backend/src/main/java/com/weekendtravel/backend/b2/tool/`。
- Tool 入参和返回值使用 Java record 或 POJO，字段名保持 camelCase。
- `BookingTool` 暴露：
  - `bookOrOrder(BookingRequest request)`
  - `execute(BookingRequest request)` 作为兼容入口。
- `BookingRequest` 至少包含：
  - `planId`: 必填，用于对齐 `execute_result.planId`。
  - `actionId`: 必填，用于对齐 `execute_result.actionId`。
  - `actionType`: 必填，支持 `buy_ticket`、`reserve_table`、`take_number`、`schedule_delivery`、`add_note`、`cancel_booking`。
  - `targetPoiId`: 非 `cancel_booking` 动作必填，且动作必须被目标 POI 的 `actionTypes` 支持。
  - `description`: 可选，可用于可读执行消息。
  - `idempotencyKey`: 必填；相同 key 重复调用必须返回相同结果。
  - `previousConfirmationNo`: `cancel_booking` 推荐传入，用于记录取消对象。
- `BookingResult` 必须包含 `docs/api-contract.md` 中 `execute_result` 的核心字段：
  - `type`: 固定为 `execute_result`。
  - `planId`
  - `actionId`
  - `actionType`
  - `status`: `success`、`failed` 或 `manual`。
  - `confirmationNo`
  - `timestamp`
- `BookingResult` 还必须包含工具层字段：
  - `message`
  - `latencyMs`
- `idempotencyKey` 不暴露到 `BookingResult`，也不要求出现在 Plan 对象中。

## 数据和状态要求

- `BookingTool` 只使用本地 POI catalog 和内存幂等缓存，不访问网络。
- 成功确认号必须确定性生成，至少按动作类型使用不同前缀：
  - `buy_ticket`: `MOCK-TKT-xxxxx`
  - `reserve_table`: `MOCK-TBL-xxxxx`
  - `take_number`: `MOCK-QNO-xxxxx`
  - `schedule_delivery`: `MOCK-DLV-xxxxx`
  - `add_note`: `MOCK-NOTE-xxxxx`
  - `cancel_booking`: `MOCK-CXL-xxxxx`
- `bookingFail=true` 时，非 `cancel_booking` 动作必须返回 `failed` 或 `manual`，`confirmationNo` 为空。
- `cancel_booking` 不被 `bookingFail` 阻断；它是 Tool 层反向操作，成功时返回 `success` 和 `MOCK-CXL-xxxxx`。
- 缺失 `idempotencyKey`、缺失必填字段、未知 POI、目标 POI 不支持该动作或未知 `actionType` 必须抛出明确异常。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- QA 报告请使用中文，命令、类名和字段名保留英文原文。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingTool.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingRequest.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingResult.java`。
- 查看测试覆盖成功、`bookingFail`、幂等、取消和错误路径。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- 或在 `backend/` 内运行 `.\mvnw.cmd test`

## 失败阈值

- 成功动作不返回 mock `confirmationNo`，失败。
- `bookingFail=true` 时非取消动作仍返回成功，失败。
- 缺少或忽略 `idempotencyKey`，失败。
- `BookingResult` 不包含 `execute_result` 核心字段，失败。
- 不支持 `cancel_booking`，失败。
- 只有 generator 自己运行验证命令或没有独立 evaluator 证据，不能标为 `verified`。
