# Contract: B2-003 AvailabilityTool and ScenarioFlags

## 本轮目标

- 基于 B2-001 的 `backend/src/main/resources/mock/poi_data.json` 实现 `AvailabilityTool / checkAvailability`。
- 实现可热更新的 `ScenarioFlags`，支持 `restaurantFull`、`routeTooFar`、`bookingFail`、`ageMismatch` 四个异常注入开关。
- 实现 `POST /api/debug/scenario`，按 `docs/api-contract.md` 返回 `{ "updated": { ... } }`，更新后无需重启后端。
- 增加后端 JUnit 测试，覆盖默认 availability、异常 flags、debug API 更新和错误路径。

## 明确不做

- 不实现 `BookingTool / bookOrOrder`。
- 不实现 `MessageTool / composeShareMessage`。
- 不实现完整 `MockApiService` 门面。
- 不实现 B1 状态机、Planner、SSE 或 Plan B 决策。
- 不改 `docs/api-contract.md` 的正式线缆字段。
- 不接入真实库存、真实预约、真实配送、真实地图或第三方 API。
- 不让 LLM 参与 availability 判断、异常注入或规划策略。

## 用户路径

1. B1 后续状态机在 `VALIDATE` 时调用 `AvailabilityTool.checkAvailability()`。
2. `AvailabilityTool` 从本地 POI catalog 读取默认 availability 槽位，返回可用性、余位、等待时间和 `latencyMs`。
3. 本地演示或 evaluator 可通过 `POST /api/debug/scenario` 动态切换异常注入 flags。
4. 后续 B1/B2 任务可读取同一个 `ScenarioFlags` 实例，触发 Plan B 或下单失败模拟。

## UI 要求

- 本轮无前端 UI 改动。
- 本轮不涉及 Playwright MCP / Chrome DevTools MCP。

## API / 后端要求

- 新增代码位于 `backend/src/main/java/com/weekendtravel/backend/b2/`。
- Tool 入参和返回值使用 Java record 或 POJO，字段名保持 camelCase。
- `AvailabilityTool` 暴露：
  - `checkAvailability(AvailabilityRequest request)`
  - `execute(AvailabilityRequest request)` 作为兼容入口。
- `AvailabilityRequest` 至少包含：
  - `poiId`: 必填。
  - `slot`: 可选；支持 `weekdayAfternoon` 和 `weekendAfternoon`，为空时默认 `weekendAfternoon`。
  - `minAge`: 可选年龄约束。
  - `groupSize`: 可选人数约束。
- `AvailabilityResult` 至少包含：
  - `poiId`
  - `poiName`
  - `slot`
  - `available`
  - `availabilityStatus`
  - `remaining`
  - `waitMinutes`
  - `ageMatched`
  - `groupSizeMatched`
  - `reasons`
  - `scenarioFlags`
  - `latencyMs`
- `ScenarioFlags` 必须是 Spring singleton component，更新后不依赖重启或重新加载 JSON。
- `POST /api/debug/scenario` 必须接收 camelCase JSON：

```json
{
  "restaurantFull": true,
  "routeTooFar": false,
  "bookingFail": false,
  "ageMismatch": false
}
```

- `POST /api/debug/scenario` 必须返回：

```json
{
  "updated": {
    "restaurantFull": true,
    "routeTooFar": false,
    "bookingFail": false,
    "ageMismatch": false
  }
}
```

## 数据和状态要求

- 默认 availability 只从本地 POI JSON 的 `defaultAvailability` 读取。
- `restaurantFull=true` 时，餐厅类 POI availability 必须被强制为不可用，`remaining=0`，`availabilityStatus=full`，等待时间必须高于普通可接受阈值。
- `ageMismatch=true` 且请求携带 `minAge` 时，`AvailabilityTool` 必须返回 `ageMatched=false` 并使该请求不可用。
- `routeTooFar` 和 `bookingFail` 本轮至少必须被 `ScenarioFlags` 正确存储、更新和返回；实际路线和下单策略由后续任务消费。
- 非法 `poiId`、缺失 `poiId`、非法 `slot` 必须抛出明确异常，不能静默返回假结果。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityTool.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlags.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/controller/DebugScenarioController.java`。
- 查看测试覆盖默认 availability、餐厅满座注入、年龄不匹配注入、flags 动态更新和错误路径。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- 或在 `backend/` 内运行 `.\mvnw.cmd test`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录后端验证命令、通过结果、关键测试覆盖和放行结论。

## 失败阈值

- `AvailabilityTool` 不读取本地 POI 默认 availability，失败。
- 任一 Tool 返回缺少 `latencyMs`，失败。
- `ScenarioFlags` 不能覆盖四个指定 flags，失败。
- `POST /api/debug/scenario` 不能热更新 flags 或响应不符合 `{ "updated": { ... } }`，失败。
- 只实现占位或 mock 却标记正式完成，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
