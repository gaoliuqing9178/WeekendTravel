# Contract: B2-002 SearchTool and RouteTool

## 本轮目标

- 基于 B2-001 的 `backend/src/main/resources/mock/poi_data.json` 实现后端 B2 Tool 层最小能力。
- 实现 `SearchTool / searchLocalPlaces`，从本地 POI JSON 返回候选 POI。
- 实现 `RouteTool / calculateRouteTime`，返回确定性的 `distanceMinutes` 和可读路线摘要。
- 两个 Tool 的返回值都必须包含 `latencyMs`。
- 增加后端 JUnit 测试，覆盖正常路径和异常路径。

## 明确不做

- 不实现 `AvailabilityTool`。
- 不实现 `ScenarioFlags` 或 `POST /api/debug/scenario`。
- 不实现 `BookingTool`、`MessageTool` 或 `MockApiService` 的完整业务门面。
- 不改 `docs/api-contract.md` 的 REST / SSE 线缆字段。
- 不接入真实地图、真实 POI、真实库存或真实第三方 API。
- 不让 LLM 参与 POI 选择、路线计算、状态转移或规划策略。

## 用户路径

1. B1 后续状态机进入 `RECALL` 时调用 `SearchTool.searchLocalPlaces()`。
2. `SearchTool` 按 scenario、category、距离、年龄、人数和 keyword 从本地 POI 数据召回候选。
3. B1 后续状态机进入路线校验时调用 `RouteTool.calculateRouteTime()`。
4. `RouteTool` 根据当前位置或两个 POI 的本地坐标返回模拟路线时间和摘要。

## UI 要求

- 本轮无 UI 改动。
- 本轮不涉及 Playwright MCP / Chrome DevTools MCP。

## API / 后端要求

- 新增 Tool 层 Java 代码，位于 `backend/src/main/java/com/weekendtravel/backend/b2/`。
- Tool 入参和返回值使用 Java record 或 POJO，字段名保持 camelCase。
- `SearchTool` 暴露：
  - `searchLocalPlaces(SearchRequest request)`
  - `execute(SearchRequest request)` 作为兼容入口。
- `RouteTool` 暴露：
  - `calculateRouteTime(RouteRequest request)`
  - `execute(RouteRequest request)` 作为兼容入口。
- `SearchResult` 必须包含 `candidates` 和 `latencyMs`。
- `RouteResult` 必须包含 `fromName`、`toName`、`distanceMinutes`、`summary` 和 `latencyMs`。
- 路线计算必须完全本地确定，不访问网络。

## 数据和状态要求

- POI 数据只从 classpath resource `mock/poi_data.json` 加载。
- `SearchTool` 必须至少支持：
  - `scenario`: `family` 或 `friends`。
  - `categories`: 可选类别集合。
  - `keyword`: 可选关键词，匹配 name、category、subCategory、address 或 tags。
  - `maxDistanceMinutes`: 可选最大距离。
  - `minAge`: 可选年龄约束。
  - `groupSize`: 可选人数约束。
  - `limit`: 可选结果上限。
- 候选结果必须来自本地 POI JSON。
- 候选排序必须确定性，至少考虑相关性、距离、评分和 availability 健康度。
- `RouteTool` 必须支持从当前位置到 POI，以及 POI 到 POI 的路线模拟。
- 未知 POI、非法 scenario 或缺失目标 POI 必须返回明确异常，不能静默给假结果。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/SearchTool.java`。
- 查看 `backend/src/main/java/com/weekendtravel/backend/b2/tool/RouteTool.java`。
- 查看测试覆盖正常搜索、无结果、非法 scenario、正常路线、当前位置路线、未知 POI。

自动验证：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- 或在 `backend/` 内运行 `.\mvnw.cmd test`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录后端验证命令、通过结果、关键测试覆盖和放行结论。

## 失败阈值

- 核心用户路径不可用，失败。
- UI 显示成功但 API 或状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只实现占位或 mock 却标记正式完成，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
- SearchTool 不从本地 JSON 返回候选、RouteTool 不返回 `distanceMinutes` / `summary`，或任一 Tool 缺少 `latencyMs`，失败。
