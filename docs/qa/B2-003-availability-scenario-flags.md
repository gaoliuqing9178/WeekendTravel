# B2-003 中文验收摘要

## 结论

PASS，B2-003 `AvailabilityTool and ScenarioFlags` 已通过独立 evaluator 子代理验收。

## 验收时间与角色

- 验收时间：2026-05-24 09:26 +08:00。
- Evaluator：Dirac，独立 QA 子代理。
- 角色边界：evaluator 只做验证和证据记录；未修改实现代码、测试代码、`feature_list.json`、`progress.md` 或 `backend/HANDOFF.md`。

## 验证命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

## 测试结果

- 后端 fast verify 通过。
- 全后端测试合计：18 tests、0 failures、0 errors、0 skipped。
- `AvailabilityToolTests`：4 tests，覆盖默认 POI availability、餐厅满座注入、年龄不匹配注入和非法输入。
- `DebugScenarioControllerTests`：1 test，通过同一 Spring Boot 随机端口服务连续两次 `POST /api/debug/scenario`，验证 flags 可热更新且无需重启。
- `ScenarioFlagsTests`：1 test，覆盖四个 flags 的默认值、全量更新、局部更新和 reset。

## 关键验收点

- `AvailabilityTool.checkAvailability()` 从本地 POI JSON 的 `defaultAvailability` 读取余位和等待时间，并返回 `latencyMs`。
- `ScenarioFlags` 支持 `restaurantFull`、`routeTooFar`、`bookingFail`、`ageMismatch` 四个开关。
- `restaurantFull=true` 会让餐厅 POI 不可用，`remaining=0`，`availabilityStatus=full`，等待时间至少 70 分钟。
- `ageMismatch=true` 且请求携带 `minAge` 时，结果会返回 `ageMatched=false` 并不可用。
- `POST /api/debug/scenario` 返回 `{ "updated": { ... } }`，并已验证同一运行上下文内可动态更新 flags。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

## 说明

以下英文内容为 evaluator 子代理写入的原始 QA 报告，保留作为原始验收证据。

# QA Report: B2-003 AvailabilityTool and ScenarioFlags

## Verdict

PASS

## Verification Time

- 2026-05-24 09:26 +08:00
- Evaluator role: independent QA only

## Scope Checked

- Contract: `docs/contracts/B2-003-availability-scenario-flags.md`
- Feature list entry: `B2-003 AvailabilityTool and ScenarioFlags`
- Backend boundary: `docs/backend-contract.md`
- Handoff context: `backend/HANDOFF.md`
- Source inspected:
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityTool.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityRequest.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityResult.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlags.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlagsState.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlagsUpdateRequest.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlagsResponse.java`
  - `backend/src/main/java/com/weekendtravel/backend/b2/controller/DebugScenarioController.java`
- Tests inspected:
  - `backend/src/test/java/com/weekendtravel/backend/b2/AvailabilityToolTests.java`
  - `backend/src/test/java/com/weekendtravel/backend/b2/ScenarioFlagsTests.java`
  - `backend/src/test/java/com/weekendtravel/backend/b2/DebugScenarioControllerTests.java`

## Command Run

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

## Test Result Summary

```text
AvailabilityToolTests: 4 tests, 0 failures, 0 errors, 0 skipped
DebugScenarioControllerTests: 1 test, 0 failures, 0 errors, 0 skipped
RouteToolTests: 5 tests, 0 failures, 0 errors, 0 skipped
ScenarioFlagsTests: 1 test, 0 failures, 0 errors, 0 skipped
SearchToolTests: 4 tests, 0 failures, 0 errors, 0 skipped
BackendApplicationTests: 2 tests, 0 failures, 0 errors, 0 skipped
PoiDataTests: 1 test, 0 failures, 0 errors, 0 skipped

Total: 18 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESS
Verify passed.
```

## Key Behavior Checks

- `AvailabilityTool.checkAvailability()` loads POIs through `PoiRepository`, reads the requested slot from `Poi.defaultAvailability`, and returns `poiId`, `poiName`, `slot`, `available`, `availabilityStatus`, `remaining`, `waitMinutes`, `ageMatched`, `groupSizeMatched`, `reasons`, `scenarioFlags`, and `latencyMs`.
- Default availability reflection is covered by `AvailabilityToolTests.checkAvailabilityReflectsDefaultPoiAvailability()`, using `poi_family_activity_001` and verifying the local JSON values for `weekdayAfternoon`: `available=true`, `remaining=20`, `waitMinutes=5`, `availabilityStatus=available`, plus `latencyMs >= 0`.
- Invalid availability inputs are covered: missing `poiId`, unknown `poiId`, and invalid `slot` all throw explicit `IllegalArgumentException`s.
- `ScenarioFlags` is a Spring `@Component` backed by an `AtomicReference<ScenarioFlagsState>`, so updates happen in memory without reloading JSON or restarting the backend.
- `ScenarioFlagsState` includes all required flags: `restaurantFull`, `routeTooFar`, `bookingFail`, and `ageMismatch`.
- `ScenarioFlagsTests.scenarioFlagsStartDisabledAndSupportHotUpdates()` verifies disabled defaults, full updates, partial updates that preserve omitted values, and reset behavior.
- `restaurantFull=true` is applied by `AvailabilityTool` to restaurant POIs: result becomes unavailable, `remaining=0`, `availabilityStatus=full`, wait time is raised to at least 70 minutes, and reason includes `scenario:restaurantFull`.
- `ageMismatch=true` with a request `minAge` forces `ageMatched=false`, makes the request unavailable, and reason includes `scenario:ageMismatch`.
- `routeTooFar` and `bookingFail` are stored, updated, and returned by `ScenarioFlags`; current B2-003 contract only requires storage/update/return for these two flags, leaving route and booking consumption to later tasks.
- `POST /api/debug/scenario` is implemented by `DebugScenarioController` at `/api/debug/scenario` and returns `ScenarioFlagsResponse(updated=...)`, which serializes to `{ "updated": { ... } }`.
- `DebugScenarioControllerTests.postDebugScenarioUpdatesFlagsWithoutRestart()` starts one Spring Boot test server, sends two POST requests in the same runtime context, and verifies the second request observes updated flags without restart.

## Risks / Notes

- No frontend UI is involved in B2-003, so Playwright MCP and Chrome DevTools MCP are not applicable.
- The standard verify output includes Mockito / dynamic Java agent warnings from the test runtime. They are warnings only; Maven reports `BUILD SUCCESS` and `Verify passed`.
- I did not modify implementation code, test code, `feature_list.json`, `progress.md`, or `backend/HANDOFF.md`.
