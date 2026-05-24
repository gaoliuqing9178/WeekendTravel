# Evaluator Report: B2-002 SearchTool and RouteTool

## Evaluator 子代理信息

- 子代理：B2-002 independent evaluator / Codex
- Evaluator ID：B2-002-EVAL-CODEX-20260524T0904+0800
- 验证时间：2026-05-24 09:04:19 +08:00
- 角色边界：只做 QA 验证和证据记录，未修改实现代码或测试代码。

## 测试目标

- 验证 B2-002 `SearchTool and RouteTool`。
- 确认 `SearchTool.searchLocalPlaces()` 从本地 JSON POI 数据返回候选 POI。
- 确认 `RouteTool.calculateRouteTime()` 返回 `distanceMinutes` 和可读 route summary。
- 确认两个工具返回结果都包含 `latencyMs`。
- 运行标准后端验证命令。

## 参考文档与范围

- 已读取/参考 `AGENTS.md`。
- 已读取/参考 `docs/contracts/B2-002-search-route-tools.md`。
- 已读取/参考 `feature_list.json` 中 B2-002 条目：验证时状态仍为 `todo`，evidence 为空；evaluator 按本轮职责不修改该文件。
- 已读取/参考 `docs/backend-contract.md`。
- 已读取/参考 `backend/HANDOFF.md`。
- 本轮是后端 Tool 层任务，不涉及前端 UI；Playwright MCP / Chrome DevTools MCP 不适用。

## 运行命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

补充只读检查命令：

```powershell
Get-Content -Raw -Encoding utf8 backend\src\main\java\com\weekendtravel\backend\b2\tool\SearchTool.java
Get-Content -Raw -Encoding utf8 backend\src\main\java\com\weekendtravel\backend\b2\tool\RouteTool.java
Get-Content -Raw -Encoding utf8 backend\src\main\java\com\weekendtravel\backend\b2\repository\PoiRepository.java
Get-Content -Raw -Encoding utf8 backend\src\test\java\com\weekendtravel\backend\b2\SearchToolTests.java
Get-Content -Raw -Encoding utf8 backend\src\test\java\com\weekendtravel\backend\b2\RouteToolTests.java
rg -n "searchLocalPlaces|calculateRouteTime|latencyMs|mock/poi_data.json|distanceMinutes|summary|supportsScenario|score =|0\.4|RouteResult|SearchResult" backend\src\main\java\com\weekendtravel\backend\b2 backend\src\test\java\com\weekendtravel\backend\b2 docs\contracts\B2-002-search-route-tools.md
```

## 标准验证结果

后端 fast verify 通过。

```text
WeekendTravel verify
Target: backend
Mode: fast

== Backend verify (fast) ==
[run] .\mvnw.cmd test
[INFO] Running com.weekendtravel.backend.b2.RouteToolTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.weekendtravel.backend.b2.SearchToolTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.weekendtravel.backend.BackendApplicationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.weekendtravel.backend.PoiDataTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results:
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[ok] mvnw.cmd test passed.
Verify passed.
```

Surefire 报告摘要：

- `com.weekendtravel.backend.b2.SearchToolTests`：4 tests, 0 failures, 0 errors, 0 skipped。
- `com.weekendtravel.backend.b2.RouteToolTests`：5 tests, 0 failures, 0 errors, 0 skipped。
- 全后端测试合计：12 tests, 0 failures, 0 errors, 0 skipped。

## 关键行为检查

### SearchTool

- `PoiRepository` 从 classpath resource `mock/poi_data.json` 加载 POI 数据，未看到真实地图、真实 POI、数据库或第三方 API 访问。
- `SearchTool.searchLocalPlaces(SearchRequest)` 暴露，`execute(SearchRequest)` 委托到 `searchLocalPlaces()`。
- 搜索过滤包含：
  - `scenario`：只接受 `family` / `friends`，非法值抛出明确异常。
  - `categories`：可选类别过滤。
  - `keyword`：匹配 name、category、subCategory、address、tags 组成的本地可搜索文本。
  - `maxDistanceMinutes`：距离上限过滤。
  - `minAge`：调用 POI 年龄范围约束。
  - `groupSize`：调用 POI 人数范围约束。
  - `limit`：结果上限，非法值抛出明确异常。
- 候选结果通过 `PoiRepository.findAll()` 来自本地 POI catalog。
- 排序确定性可检查：按 `score` 降序、距离升序、POI id 升序。
- 评分公式包含相关性、距离、rating、availability health：`0.4 * relevance + 0.3 * distance + 0.2 * rating + 0.1 * availability`。
- `SearchResult` record 包含 `candidates` 和 `latencyMs`，实现用调用耗时填充 `latencyMs`。
- 自动测试覆盖：
  - family + activity + keyword + distance + age + group size 的正常搜索，断言候选非空、来自 activity、支持 family、支持年龄，并包含已知本地 POI `poi_family_activity_001`。
  - 无结果搜索仍返回 `latencyMs`。
  - 非法 scenario 抛出 `scenario must be family or friends`。
  - 非法 limit 抛出 `limit must be positive`。

### RouteTool

- `RouteTool.calculateRouteTime(RouteRequest)` 暴露，`execute(RouteRequest)` 委托到 `calculateRouteTime()`。
- 缺失 `toPoiId` 抛出明确异常：`toPoiId is required`。
- 未知目标 POI 抛出明确异常：`unknown toPoiId: ...`。
- `fromPoiId` 为空、`origin`、`current` 或 `center` 时使用 POI catalog 的中心点，支持“当前位置到 POI”。
- `fromPoiId` 为 POI id 时从本地 POI catalog 查找，支持 POI 到 POI 路线模拟；未知起点 POI 在源码中也会抛出 `unknown fromPoiId: ...`。
- 从中心点到 POI 时返回目标 POI 的 `distanceMinutesFromCenter`；POI 到 POI 时使用本地经纬度 haversine 公式计算确定性分钟数；同一 POI 返回 0。
- `RouteResult` record 包含 `fromName`、`toName`、`distanceMinutes`、`summary`、`latencyMs`。
- route summary 包含起点、终点、分钟数和移动建议；同一地点摘要说明无需移动。
- 自动测试覆盖：
  - 当前位置到 `poi_family_activity_001`，断言 `distanceMinutes=15`、summary 包含 `约15分钟`、返回 `latencyMs`。
  - POI 到 POI，断言 `distanceMinutes >= 3`、summary 包含起终点名称、返回 `latencyMs`。
  - 同一 POI，断言 `distanceMinutes=0` 且 summary 包含 `无需移动`。
  - 未知目标 POI 抛出明确异常。
  - 缺失目标 POI 抛出明确异常。

## 风险与说明

- B2-002 合同不要求对外 REST / SSE 改动，本轮未验证 B1 状态机如何调用这些 Tool；该集成属于后续 B1/B2 任务范围。
- 自动测试覆盖了未知 `toPoiId`，源码检查确认未知 `fromPoiId` 也会抛出明确异常；当前没有单独的未知起点自动测试，风险较低。
- `latencyMs` 在本地快速执行时可能为 0；测试按非负耗时断言，符合“返回 latencyMs”要求。

## 结论

- 结论：PASS，放行 B2-002。
- 理由：标准后端 fast verify 通过；`SearchTool` 从本地 `mock/poi_data.json` catalog 返回候选并覆盖过滤、排序和 `latencyMs`；`RouteTool` 返回确定性 `distanceMinutes`、可读 summary 和 `latencyMs`；异常路径有明确失败信息。
- 后续建议：generator 按项目流程将本 QA 报告路径回填到 `feature_list.json`、`progress.md` 和 `backend/HANDOFF.md`，再将 B2-002 状态同步为 `verified`。

## 独立性声明

- 本报告由 generator 之外的 independent evaluator 子代理执行验收后填写。
- 本轮 evaluator 只运行验证、做只读源码/测试检查，并写入 `docs/qa/B2-002-search-route-tools.md`。
- 本轮 evaluator 未修改实现代码、测试代码、`feature_list.json`、`progress.md` 或 `backend/HANDOFF.md`。
