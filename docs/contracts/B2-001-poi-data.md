# Contract: B2-001 POI Mock Data

## 本轮目标

- 建立后端 B2 本地 POI mock 数据集，供后续 SearchTool、RouteTool、AvailabilityTool、BookingTool 和 MessageTool 使用。
- 数据文件落地为后端 classpath resource：`backend/src/main/resources/mock/poi_data.json`。
- 数据集一次性补到最终规模不少于 50 条，同时满足 Sprint 1 的首批不少于 30 条要求。
- 覆盖 family、friends、restaurant、dessert/cafe、supplier 场景和类别。
- 增加后端测试，确保 JSON 可被 Jackson 解析，并校验数量、唯一 ID、必填字段和类别覆盖。

## 明确不做

- 不实现 SearchTool、RouteTool、AvailabilityTool、BookingTool、MessageTool。
- 不实现 MockApiService。
- 不实现 ScenarioFlags 服务或 `POST /api/debug/scenario`。
- 不改变 `docs/api-contract.md` 的 REST / SSE 线缆字段。
- 不接入真实外部 POI、地图、订座、配送或支付服务。

## 用户路径

1. B1/B2 后续代码从 classpath 加载 `mock/poi_data.json`。
2. Tool 层按场景、类别、距离、评分、余位和年龄约束召回候选 POI。
3. Planner 使用这些候选生成家庭或朋友 Demo 方案。

## UI 要求

- 本轮无 UI 改动。

## API / 后端要求

- POI 数据文件必须位于 `backend/src/main/resources/mock/poi_data.json`。
- 顶层 JSON 使用对象结构，包含 `version`、`updatedAt`、`city`、`center` 和 `pois`。
- `pois` 必须是数组，且条目数不少于 50。
- 每条 POI 至少包含：
  - `id`
  - `name`
  - `category`
  - `subCategory`
  - `scenarios`
  - `address`
  - `lat`
  - `lng`
  - `rating`
  - `tags`
  - `distanceMinutesFromCenter`
  - `pricePerPerson`
  - `ageRequirement`
  - `groupSize`
  - `availabilityStatus`
  - `waitMinutes`
  - `defaultAvailability`
  - `scenarioFlags`
  - `actionTypes`
- 本地数据字段使用 camelCase，避免把历史产品文档里的 snake_case 示例扩散到后续正式线缆契约。

## 数据和状态要求

- `id` 在全数据集中唯一。
- `scenarioFlags.family` 和 `scenarioFlags.friends` 均为 boolean。
- 数据集必须至少包含：
  - family 场景 POI。
  - friends 场景 POI。
  - `restaurant` 类别。
  - `cafe` 和 `dessert` 类别。
  - `supplier` 类别。
- `defaultAvailability` 至少提供 `weekdayAfternoon` 和 `weekendAfternoon` 两个槽位。
- 每个 availability 槽位提供 `available`、`remaining`、`waitMinutes`。
- `availabilityStatus` 使用 `available`、`limited`、`full` 之一。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 打开 `backend/src/main/resources/mock/poi_data.json`，确认 `pois` 条目数不少于 50。
- 检查类别和场景覆盖 family、friends、restaurant、cafe、dessert、supplier。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- 或在 `backend/` 内运行 `.\mvnw.cmd test`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录后端验证命令、通过结果、POI 条目数量和覆盖结论。

## 失败阈值

- 核心用户路径不可用，失败。
- UI 显示成功但 API 或状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只实现占位或 mock 却标记正式完成，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
- POI JSON 无法解析、少于 50 条、ID 重复或缺少必要类别覆盖，失败。
