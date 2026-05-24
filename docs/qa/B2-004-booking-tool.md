# B2-004 BookingTool / bookOrOrder QA 验收报告

## 验收结论

PASS。B2-004 `BookingTool / bookOrOrder` 已通过独立 evaluator 验证。

## 验证时间

- 2026-05-24 09:42:45 +08:00

## 验证范围

- 阅读并参考 `AGENTS.md`、`docs/contracts/B2-004-booking-tool.md`、`feature_list.json` 中 B2-004 条目、`docs/api-contract.md` 中 `ActionType` / `ActionStatus` / `execute_result` / `idempotencyKey` 相关段落、`docs/backend-contract.md`、`backend/HANDOFF.md`。
- 只读检查 B2-004 相关源码和测试：
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingTool.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingRequest.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingResult.java`
  - `backend/src/test/java/com/weekendtravel/backend/b2/BookingToolTests.java`
- 运行标准后端验证命令。
- 本次 evaluator 只新增本 QA 报告；未修改实现代码、测试代码、`feature_list.json`、`progress.md` 或 `backend/HANDOFF.md`。

## 运行命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

## 测试结果摘要

```text
AvailabilityToolTests: 4 tests, 0 failures, 0 errors, 0 skipped
BookingToolTests: 5 tests, 0 failures, 0 errors, 0 skipped
DebugScenarioControllerTests: 1 test, 0 failures, 0 errors, 0 skipped
RouteToolTests: 5 tests, 0 failures, 0 errors, 0 skipped
ScenarioFlagsTests: 1 test, 0 failures, 0 errors, 0 skipped
SearchToolTests: 4 tests, 0 failures, 0 errors, 0 skipped
BackendApplicationTests: 2 tests, 0 failures, 0 errors, 0 skipped
PoiDataTests: 1 test, 0 failures, 0 errors, 0 skipped

Total: 23 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESS
Verify passed.
```

## 关键行为检查

- `BookingTool.bookOrOrder()` 已存在，并且 `execute(BookingRequest request)` 兼容入口会委托到 `bookOrOrder()`。
- 成功路径返回 `BookingResult.type=execute_result`，包含 `planId`、`actionId`、`actionType`、`status`、`confirmationNo`、`timestamp`、`message`、`latencyMs`。
- 成功路径生成 deterministic mock `confirmationNo`，并按动作类型使用不同前缀：
  - `buy_ticket` -> `MOCK-TKT-`
  - `reserve_table` -> `MOCK-TBL-`
  - `take_number` -> `MOCK-QNO-`
  - `schedule_delivery` -> `MOCK-DLV-`
  - `add_note` -> `MOCK-NOTE-`
  - `cancel_booking` -> `MOCK-CXL-`
- `bookingFail=true` 时，非 `cancel_booking` 动作返回 `status=failed`，`confirmationNo=null`，不会伪装为成功。
- `idempotencyKey` 通过内存缓存生效；相同 key 重复调用返回同一份 `BookingResult`，不会生成新的 mock 订单或新确认号。
- `cancel_booking` 作为 Tool 层反向操作已支持；即使 `bookingFail=true`，取消动作仍可返回 `status=success` 和 `MOCK-CXL-` 确认号。
- 非取消动作会校验 `targetPoiId` 必填、POI 存在，并确认目标 POI 的 `actionTypes` 支持当前 `actionType`。
- 错误路径覆盖了缺失 `idempotencyKey`、不支持的 `actionType`、目标 POI 不支持该动作等异常。

## 风险与备注

- `idempotencyKey` 缓存当前为 `BookingTool` 内存态 `ConcurrentHashMap`，符合 B2-004 Tool 层 mock 范围；后续如需要跨进程或持久化幂等，需要另开任务设计。
- `bookingFail=true` 当前固定返回 `failed`，契约允许 `failed` 或 `manual`，因此满足验收。
- 标准验证输出包含 Mockito / Java dynamic agent warning，但 Maven 结果为 `BUILD SUCCESS`，不影响本轮 PASS 结论。

