# Progress

## 2026-05-26 B1-002 SSE emitter and heartbeat

### 已完成

- 新增 `docs/contracts/B1-002-sse-emitter.md`，明确本轮只交付 `GET /api/plan/{planId}/stream` 的最小 SSE 链路，不引入 Spring AI、WebFlux 或 Reactor Flux。
- 在 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java` 增加 `GET /api/plan/{planId}/stream`，返回 `text/event-stream`，并显式设置 `Cache-Control: no-cache` 与 `Connection: keep-alive`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`，使用 Spring MVC `SseEmitter` 建立最小 stream，按顺序发送 `heartbeat` 和 mock `state_change`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/plan/sse/HeartbeatEvent.java` 与 `StateChangeEvent.java`，字段使用 camelCase；`state_change` 的最小 mock 转移固定为 `START -> INTENT`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java`，使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JDK `HttpClient` 对真实 HTTP SSE 端点做集成测试。
- 在 `feature_list.json` 将 `B1-002` 标记为 `verified`，并补入 contract、测试、generator 开发侧准备检查和 evaluator 证据。
- 新增 QA 报告 `docs/qa/B1-002-sse-emitter.md`，记录独立 evaluator 放行结论。
- 更新 `backend/HANDOFF.md`，将最小 SSE stream、`heartbeat`、`state_change`、事件名与 `type` 一致性以及 SSE 测试项同步为已完成。

### 验证记录

- Generator 开发侧准备检查：`./backend/mvnw -f "backend/pom.xml" test` 通过，后端测试合计 28 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`PlanControllerStreamTests` 1 个测试、全后端 28 个测试均通过，`Verify passed.`。
- 独立 evaluator：`B1-002-EVAL-gpt-5.4-20260526` 已完成验收并放行。
- Evaluator 以父工作树绝对路径复核 `PlanController`、`PlanStreamService`、`HeartbeatEvent`、`StateChangeEvent`、`PlanControllerStreamTests` 和 `feature_list.json`，避免隔离 worktree 看不到未提交改动的问题。
- Evaluator 独立运行：
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml -Dtest=PlanControllerStreamTests test`
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test`
  均 `BUILD SUCCESS`，确认端点路径、`text/event-stream`、`heartbeat`、mock `state_change`、`START -> INTENT` 和事件名与 `data.type` 一致。
- QA 报告：`docs/qa/B1-002-sse-emitter.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B1-002` 已完成并 verified。
- 后端最小 SSE 占位链路已具备，可供后续 `B1-003` `POST /api/plan` 与 `B1-004` 状态机继续接入。


## 2026-05-24 B2-005 MessageTool / composeShareMessage

### 已完成

- 新增 `docs/contracts/B2-005-message-tool.md`，明确本轮只交付 B2 Tool 层 `MessageTool / composeShareMessage`，不抢做 B1 `Plan`、Planner、状态机、SSE、`POST /api/plan` 或真实 LLM 接入。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageRequest.java`、`MessageResult.java`、`MessagePlanPayload.java`、`MessageTimeSlot.java`、`MessageActionSummary.java` 和 `MessageTool.java`。
- `MessageTool.composeShareMessage()` 会根据已定稿的 `timeline` 和 `actions` 生成 deterministic fallback 中文 `shareMessage`，`execute()` 作为兼容入口委托到同一逻辑。
- `MessageResult` 固定返回 `llmUsed=false` 和 `templateVersion=fallback-v1`，本轮不读取 `OPENAI_API_KEY`、不调用 OpenAI/LLM、不进行网络访问。
- `MessageResult.plan` 返回 Plan-compatible payload，包含 `planId`、`scenario`、`status`、`isPlanB`、`planBReason`、`summary`、`timeline`、`actions`、`shareMessage`、`totalDurationHours`、`replanCount`、`createdAt`，后续 B1 可把其中 `shareMessage` 合入正式 `Plan.shareMessage`。
- 家庭场景文案突出亲子、低负担饮食或少步行；朋友场景文案突出玩、吃、拍照和聊天；`isPlanB=true` 时透明输出 `Plan B 说明`。
- 时间线按正数 `order` 升序输出，缺失或非正 `order` 的槽位保持输入顺序排在后面；`send_message` 动作不会进入“已安排”执行摘要。
- 缺失 `planId`、非法 `scenario`、空 `timeline` 会抛出明确 `IllegalArgumentException`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/MessageToolTests.java`，覆盖家庭/朋友 deterministic fallback、Plan B、动作摘要、`send_message` 过滤、`plan.shareMessage`、`llmUsed=false`、时间线排序和非法请求。
- 在 `feature_list.json` 将 `B2-005` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 27 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`MessageToolTests` 4 个测试、全后端 27 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Mencius (`019e57b3-5e9e-7e92-b1e5-7744b2a4d329`, `B2-005-EVAL-CODEX-20260524T0959+0800`) 已完成中文 QA 验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-005-message-tool.md`、`feature_list.json` 中 B2-005 条目、`docs/api-contract.md` 的 `Plan.shareMessage`、`docs/backend-contract.md` 的 LLM 边界、`backend/HANDOFF.md`、B2 MessageTool 源码和测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 deterministic fallback 中文 `shareMessage`、`execute` 兼容入口、`llmUsed=false`、无 OpenAI/LLM/网络调用、Plan-compatible payload 包含 `shareMessage`，以及家庭/朋友/Plan B/动作摘要/错误路径覆盖。
- QA 报告：`docs/qa/B2-005-message-tool.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-005` 已完成并 verified。
- B2 Sprint 1 当前 `B2-001` 到 `B2-005` 均已 verified；`shareMessage` 已在 Tool 层进入 Plan-compatible payload。
- 真正写入正式 API `Plan`、推送 `plan_ready`、接入 B1 状态机/Planner/SSE 仍等待 B1 后续任务，当前没有伪装完成 B1 链路。

## 2026-05-24 B2-004 BookingTool / bookOrOrder

### 已完成

- 新增 `docs/contracts/B2-004-booking-tool.md`，明确本轮只交付 B2 Tool 层 `BookingTool / bookOrOrder`，不抢做 B1 `execute` API、SSE `execute_result`、完整 `MockApiService` 或 `MessageTool`。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/BookingRequest.java`、`BookingResult.java` 和 `BookingTool.java`。
- `BookingTool.bookOrOrder()` 返回与 `docs/api-contract.md` 中 `execute_result` 对齐的核心字段：`type`、`planId`、`actionId`、`actionType`、`status`、`confirmationNo`、`timestamp`，并补充工具层 `message` 和 `latencyMs`。
- 成功动作会按 `actionType` 生成 deterministic mock `confirmationNo`，覆盖 `MOCK-TKT-`、`MOCK-TBL-`、`MOCK-QNO-`、`MOCK-DLV-`、`MOCK-NOTE-` 和 `MOCK-CXL-` 前缀。
- `bookOrOrder` 要求 `idempotencyKey`，并通过内存 `ConcurrentHashMap` 保证相同 key 重复调用返回同一份 `BookingResult`，不重复生成 mock 订单。
- `bookingFail=true` 时，非 `cancel_booking` 动作返回 `status=failed` 且 `confirmationNo=null`；`cancel_booking` 作为 Tool 层反向操作仍可成功返回 `MOCK-CXL-` 确认号。
- 非取消动作会校验 `targetPoiId` 必填、POI 存在、目标 POI 的 `actionTypes` 支持当前 `actionType`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/BookingToolTests.java`，覆盖成功、幂等重放、`bookingFail`、`cancel_booking` 和非法请求。
- 调整 `backend/pom.xml`，关闭 Maven compiler incremental compilation，避免当前 Windows/Javac 环境在标准 `verify.ps1` 新增源文件后反复出现 `无法关闭编译器资源` 的不稳定失败。
- 在 `feature_list.json` 将 `B2-004` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 23 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`BookingToolTests` 5 个测试、全后端 23 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Confucius (`019e57a4-d8b5-7b10-aba7-5557ecee3057`) 已完成中文 QA 验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-004-booking-tool.md`、`feature_list.json` 中 B2-004 条目、`docs/api-contract.md` 的 ActionType / ActionStatus / `execute_result` / `idempotencyKey` 相关段落、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 BookingTool 源码和测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认成功确认号、`bookingFail` 失败、`execute_result` 核心字段、`latencyMs`、`idempotencyKey` 幂等和 `cancel_booking` 反向操作。
- QA 报告：`docs/qa/B2-004-booking-tool.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-004` 已完成并 verified。
- `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-24 B2-003 AvailabilityTool and ScenarioFlags

### 已完成

- 新增 `docs/contracts/B2-003-availability-scenario-flags.md`，明确本轮只交付 B2 本地 `AvailabilityTool / checkAvailability`、`ScenarioFlags` 和 `POST /api/debug/scenario`，不抢做 BookingTool、MessageTool、MockApiService、状态机、SSE 或 Plan B 决策。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlags.java`、`ScenarioFlagsState.java`、`ScenarioFlagsUpdateRequest.java` 和 `ScenarioFlagsResponse.java`。
- `ScenarioFlags` 作为 Spring singleton component 使用 `AtomicReference` 保存可热更新状态，覆盖 `restaurantFull`、`routeTooFar`、`bookingFail`、`ageMismatch` 四个异常注入开关。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityTool.java`、`AvailabilityRequest.java` 和 `AvailabilityResult.java`。
- `AvailabilityTool.checkAvailability()` 从本地 POI JSON 的 `defaultAvailability` 读取余位，返回 `available`、`availabilityStatus`、`remaining`、`waitMinutes`、`ageMatched`、`groupSizeMatched`、`reasons`、`scenarioFlags` 和 `latencyMs`。
- `restaurantFull=true` 时，餐厅 POI 被强制为不可用、`remaining=0`、`availabilityStatus=full`、等待时间至少 70 分钟。
- `ageMismatch=true` 且请求携带 `minAge` 时，availability 结果会返回 `ageMatched=false` 并不可用；`routeTooFar` 和 `bookingFail` 本轮完成存储、更新和返回，供后续路线/下单任务消费。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/controller/DebugScenarioController.java`，实现 `POST /api/debug/scenario`，返回 `{ "updated": { ... } }`，无需重启服务即可更新 flags。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/AvailabilityToolTests.java`、`ScenarioFlagsTests.java` 和 `DebugScenarioControllerTests.java`，覆盖默认 availability、异常注入、非法入参、flags 热更新和真实 HTTP debug API。
- 在 `feature_list.json` 将 `B2-003` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，后端测试合计 18 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`AvailabilityToolTests` 4 个测试、`DebugScenarioControllerTests` 1 个测试、`ScenarioFlagsTests` 1 个测试、全后端 18 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Dirac (`019e5795-2750-75c0-845e-f3104401f0a0`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-003-availability-scenario-flags.md`、`feature_list.json` 中 B2-003 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 availability / scenario / debug controller 源码和 B2-003 测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 `AvailabilityTool` 读取本地 JSON 默认余位并返回 `latencyMs`，`ScenarioFlags` 覆盖四个开关，`POST /api/debug/scenario` 在同一运行上下文内连续 POST 可热更新 flags。
- QA 报告：`docs/qa/B2-003-availability-scenario-flags.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-003` 已完成并 verified。
- `B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-24 B2-002 SearchTool and RouteTool

### 已完成

- 新增 `docs/contracts/B2-002-search-route-tools.md`，明确本轮只交付 B2 本地 `SearchTool / searchLocalPlaces` 和 `RouteTool / calculateRouteTime`，不抢做 AvailabilityTool、ScenarioFlags、MockApiService、debug API、REST / SSE 或真实地图服务。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/model/*` 和 `backend/src/main/java/com/weekendtravel/backend/b2/repository/PoiRepository.java`，把 B2-001 的 `mock/poi_data.json` 加载为本地 POI catalog。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/SearchTool.java`、`SearchRequest.java`、`SearchResult.java` 和 `SearchCandidate.java`。
- `SearchTool.searchLocalPlaces()` 支持 `family` / `friends` 场景校验，按类别、关键词、距离、年龄和人数过滤本地 POI，并按 `0.4 relevance + 0.3 distance + 0.2 rating + 0.1 availability` 确定性排序。
- 新增 `backend/src/main/java/com/weekendtravel/backend/b2/tool/RouteTool.java`、`RouteRequest.java` 和 `RouteResult.java`。
- `RouteTool.calculateRouteTime()` 支持当前位置到 POI、POI 到 POI、同一 POI 0 分钟、未知 POI 明确异常；返回 `distanceMinutes`、可读 route summary 和 `latencyMs`。
- 新增 `backend/src/test/java/com/weekendtravel/backend/b2/SearchToolTests.java` 和 `backend/src/test/java/com/weekendtravel/backend/b2/RouteToolTests.java`，覆盖搜索候选、无结果、非法入参、路线分钟数、summary、同地路线和异常路径。
- 在 `feature_list.json` 将 `B2-002` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`.\mvnw.cmd test` 通过，后端测试合计 12 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`RouteToolTests` 5 个测试、`SearchToolTests` 4 个测试、全后端 12 个测试均通过，`Verify passed.`。
- 独立 evaluator 子代理 Averroes (`019e5780-ab21-7250-9644-2889eae14565`, `B2-002-EVAL-CODEX-20260524T0904+0800`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-002-search-route-tools.md`、`feature_list.json` 中 B2-002 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、B2 tool / repository 源码和 B2 tool 测试。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，确认 `SearchTool` 从本地 JSON 返回候选 POI，`RouteTool` 返回 `distanceMinutes` 和 route summary，两个工具均返回 `latencyMs`。
- QA 报告：`docs/qa/B2-002-search-route-tools.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-002` 已完成并 verified。
- `B2-003`、`B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-22 B2-001 POI mock data

### 已完成

- 新增 `docs/contracts/B2-001-poi-data.md`，明确本轮只交付 B2 本地 POI mock 数据，不实现 Tool、MockApiService、ScenarioFlags 或 debug API。
- 新增 `backend/src/main/resources/mock/poi_data.json`，作为后端 classpath resource `mock/poi_data.json`，顶层包含 `version`、`updatedAt`、`city`、`center` 和 `pois`。
- POI 数据一次性补到 52 条，满足首批不少于 30 条和最终不少于 50 条要求。
- 数据覆盖 `activity=20`、`restaurant=20`、`cafe=3`、`dessert=3`、`supplier=6`。
- 场景覆盖 `family=28`、`friends=32`，其中 8 条同时支持 family / friends。
- 每条 POI 使用 camelCase 本地字段，包含 `id`、`name`、`category`、`subCategory`、`scenarios`、经纬度、评分、标签、距离、价格、年龄、人数、availability、scenario flags 和 action types。
- 新增 `backend/src/test/java/com/weekendtravel/backend/PoiDataTests.java`，用 Jackson 3 `tools.jackson.databind.ObjectMapper` 解析 JSON，并校验数量、唯一 ID、必填字段、availability 槽位、family / friends 和类别覆盖。
- 修复 `backend/mvnw.cmd` 在普通 `.m2` 目录下访问 `.Target[0]` 的空值问题，使 Maven Wrapper 能在当前 Windows 环境启动。
- 调整 `backend/src/test/java/com/weekendtravel/backend/BackendApplicationTests.java`，用反射校验真实 `HealthController.health()` 返回字段，避开当前 Maven testCompile 类路径对主类 compile-time import 的限制。
- 在 `feature_list.json` 将 `B2-001` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧 JSON 检查：`Get-Content -Raw -Encoding UTF8 backend\src\main\resources\mock\poi_data.json | ConvertFrom-Json` 通过，统计为 `count=52`、`activity=20`、`cafe=3`、`dessert=3`、`restaurant=20`、`supplier=6`、`family=28`、`friends=32`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过；后端测试结果为 3 tests、0 failures、`BUILD SUCCESS`、`Verify passed.`。
- 独立 evaluator 子代理 Newton (`019e4fdf-35fd-7c40-a198-aa35368a6fae`, `B2-001-EVAL-CODEX-20260522T2130+0800`) 已完成验收并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/contracts/B2-001-poi-data.md`、`feature_list.json` 中 B2-001 条目、`docs/backend-contract.md`、`backend/HANDOFF.md`、`backend/src/main/resources/mock/poi_data.json` 和后端测试。
- Evaluator 独立确认 POI JSON 以 UTF-8 可解析，`pois=52`，类别覆盖 `activity=20`、`restaurant=20`、`cafe=3`、`dessert=3`、`supplier=6`，场景覆盖 `family=28`、`friends=32`、`both=8`。
- Evaluator 独立运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，`BackendApplicationTests` 2 个测试和 `PoiDataTests` 1 个测试均通过。
- QA 报告：`docs/qa/B2-001-poi-data.md`。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

### 当前状态

- `B2-001` 已完成并 verified。
- `B2-002`、`B2-003`、`B2-004` 和 `B2-005` 已于 2026-05-24 完成并 verified；B2 Sprint 1 Tool 层当前可交由 B1 后续接入状态机、Plan 和 SSE。

## 2026-05-22 F1-002 API client and mock fixture mode

### 已完成

- 新增 `docs/contracts/F1-002-api-client-fixtures.md`，明确本轮只交付 API client、mock fixture mode 和 fixture 解析验证，不抢做完整 SSE composable 或 PlanCard。
- 新增 `frontend/src/api/types.ts`，按 `docs/api-contract.md` 建立 `Scenario`、`AgentState`、`Plan`、`TimeSlot`、`ActionItem`、REST request / response 和 SSE payload 类型。
- 新增 `frontend/src/api/client.ts`，支持默认 `mock` mode 和 `VITE_API_MODE=real` 的真实 API mode；真实路径只使用 `docs/api-contract.md` 里的 REST / SSE 端点。
- 新增 `frontend/src/api/fixtures.ts`，通过 Vite raw import 读取 `docs/fixtures/plan-ready-family.json`、`docs/fixtures/plan-ready-friends.json` 和 `docs/fixtures/sse-events.jsonl`，并做 camelCase key、plan_ready 和 SSE frame 基础校验。
- 新增 `frontend/scripts/verify-fixtures.mjs` 和 `frontend/package.json` 的 `verify:fixtures`，把 fixture JSON / JSONL 解析纳入前端 fast verify。
- 更新 `verify.ps1`，当前 `.\verify.ps1 -Target frontend -Mode fast` 会先跑 `pnpm verify:fixtures`，再跑 `pnpm typecheck`。
- 更新 `frontend/src/stores/planner.ts` 和 `frontend/src/App.vue`，点击“开始预演”后默认使用 mock client 加载 fixture，显示 `CONFIRM` / `closed` 状态、Plan ID、Plan B 摘要和 fixture 日志预览。
- 在 `feature_list.json` 将 `F1-002` 标记为 `verified` 并写入 evaluator 证据。

### 验证记录

- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过，输出覆盖：
  - `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `docs/fixtures/sse-events.jsonl -> 23 JSONL events`
  - `pnpm typecheck passed`
  - `Verify passed.`
- Generator 额外构建检查：`npm run build` 通过，Vite production build 成功。
- Generator 字段检查：`rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures` 无命中。
- 独立 evaluator 子代理 Hooke (`019e4dd8-6378-79b2-a8d6-9f89bcadb13d`) 已完成验证并放行。
- Evaluator 验证范围包含：`docs/contracts/F1-002-api-client-fixtures.md`、`docs/api-contract.md`、`frontend/src/api/*`、`frontend/scripts/verify-fixtures.mjs`、`verify.ps1`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`。
- Evaluator 运行前端 fast verify 通过，并确认 fixture 解析、`pnpm typecheck` 和 `Verify passed.`。
- Evaluator 使用 Playwright MCP 打开 `http://127.0.0.1:5173` 并点击“开始预演”，确认页面进入 `CONFIRM` / `closed`，可见 `plan_family_fixture`、Plan B 和 summary，console 错误 / 警告为 0。
- Evaluator 使用 Chrome DevTools MCP 复核 snapshot、console、network、DOM：三份 fixture raw import 均为 200，页面含 `CONFIRM`、`closed`、Plan B alert 和 fixture 日志。
- QA 报告：`docs/qa/F1-002-api-client-fixtures.md`。
- 截图证据：`docs/qa/F1-002-devtools-confirm.png`、`docs/qa/F1-002-devtools-confirm-root.png`、`docs/qa/F1-002-playwright-confirm.png`。

### 当前状态

- `F1-002` 已完成并 verified。
- 下一步 F1 小目标应推进 `F1-003`：`useSSE` composable and log panel，把当前 fixture 解析能力接到真实日志面板和流式事件分发。

## 2026-05-22 WF-002 前端 evaluator 浏览器 MCP 规则

### 已完成

- 新增 `docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`，明确前端 evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- 更新 `docs/dev-workflow.md`，把前端 UI 验收从泛泛的浏览器 / Playwright 要求升级为双 MCP 要求。
- 更新 `docs/frontend-contract.md`，明确 Playwright MCP 负责模拟交互、状态等待、截图或 trace，Chrome DevTools MCP 负责页面快照、console、network、DOM / accessibility 和视觉复核。
- 更新 `docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`frontend/F1-handoff.md` 和 `docs/decision-log.md`，统一前端 evaluator 证据要求。
- 在 `feature_list.json` 新增 `WF-002`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Playwright MCP|Chrome DevTools MCP|截图|视觉|console|network|DOM|accessibility|trace`，确认入口、前端 contract、workflow、quality、模板、handoff、decision log、progress 和 feature metadata 均有覆盖。
- 独立 evaluator 子代理 Euler (`019e4db4-865e-7fe2-85ed-4dd5e5b6f3c2`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/frontend-contract.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`docs/initiallizer-agent-prompt.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：前端验收入口、QA 模板、handoff、decision log、progress 和 feature metadata 都已覆盖 Playwright MCP + Chrome DevTools MCP 双工具要求；`feature_list.json` 可解析；未发现冲突规则。
- QA 报告已写入 `docs/qa/WF-002-frontend-evaluator-browser-mcp.md`。

## 2026-05-22 WF-001 harness 测试职责调整

### 已完成

- 新增 `docs/contracts/WF-001-generator-subagent-testing.md`，把“generator 测试阶段必须使用 evaluator 子代理”作为本轮 workflow contract。
- 更新 `AGENTS.md`，把完成定义改为必须有独立 evaluator 子代理验证证据。
- 更新 `docs/dev-workflow.md`，将旧的 generator 自行最终验证流程改为 `Generator -> Handoff to Evaluator Subagent -> Evaluator Test -> Fix -> Evaluator Retest -> Handoff`。
- 更新 `docs/quality.md`，明确 generator 自己运行的 typecheck、build、脚本、curl 或冒烟只能作为开发准备记录，不能单独作为 `verified` 证据。
- 更新 `docs/contracts/_template.md` 和 `docs/qa/evaluator-template.md`，要求后续 contract 与 QA 报告记录 evaluator 子代理。
- 更新 `docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md` 和 `docs/initiallizer-agent-prompt.md`，统一新规则。
- 在 `feature_list.json` 新增 `WF-001`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Self Verify|小任务可以同一个 agent|Generator 负责实现和基础验证|Generator -> Self Verify`，只剩 `docs/contracts/WF-001-generator-subagent-testing.md` 的负例搜索命令命中，不是活跃规则。
- 独立 evaluator 子代理 Newton (`019e4da9-79c0-79a3-b7f1-c79f5e594c58`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-001-generator-subagent-testing.md`、`docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：活跃规则已统一为“generator 完成开发后，测试阶段必须交给独立 evaluator 子代理”；没有发现仍允许 generator-only final testing 的活跃表述；`feature_list.json` 可解析。
- QA 报告已写入 `docs/qa/WF-001-generator-subagent-testing.md`。

## 2026-05-21 Initializer

### 已完成

- 读取根目录产品文档、外部 harness 总结材料、当前仓库状态和已有 `docs/initiallizer-agent-prompt.md`。
- 创建短入口 `AGENTS.md`。
- 创建长期 docs：
  - `docs/product-spec.md`
  - `docs/architecture.md`
  - `docs/api-contract.md`
  - `docs/frontend-contract.md`
  - `docs/backend-contract.md`
  - `docs/dev-workflow.md`
  - `docs/quality.md`
  - `docs/decision-log.md`
  - `docs/contracts/_template.md`
  - `docs/qa/evaluator-template.md`
- 创建前端 fixture：
  - `docs/fixtures/sse-events.jsonl`
  - `docs/fixtures/plan-ready-family.json`
  - `docs/fixtures/plan-ready-friends.json`
- 创建结构化任务清单 `feature_list.json`。
- 创建 `backend/README.md` 和 `frontend/README.md`，明确尚未 scaffold。
- 创建 `init.ps1` 和 `verify.ps1` 的 Windows harness 脚本。

### 关键决策

- 后端默认端口统一为 `8000`。
- API 和 SSE 线缆字段统一使用 camelCase。
- 前端先用 fixture 开发，后端先用本地 Mock API 开发。
- OpenAI 接入暂不锁 SDK 版本或模型名；真正实现前必须重新核验官方 OpenAI developer docs / MCP。
- 本轮不 scaffold Spring Boot 或 Vue 项目，不把空目录伪装为已完成项目。

### 当前状态

- 仓库已经具备长期 agent 开发 harness。
- 前后端业务代码尚未初始化。
- `feature_list.json` 中功能状态仍为 `todo`，因为没有业务功能通过验证。

### 验证记录

- 回读 `AGENTS.md` 和 `docs/api-contract.md`，中文显示正常。
- `feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-family.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-friends.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/sse-events.jsonl` 共 23 行，逐行可通过 `ConvertFrom-Json` 解析。
- 直接运行 `.\init.ps1 -Target all` 和 `.\verify.ps1 -Target all -Mode fast` 被当前 PowerShell 执行策略拦截，错误类型为 `PSSecurityException / UnauthorizedAccess`。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all` 成功运行：Java、Node、pnpm 可见，Maven 缺失；backend/frontend 均明确提示尚未初始化。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast` 成功运行脚本逻辑并返回失败：`backend\pom.xml` 缺失、`frontend\package.json` 缺失。这是当前真实未初始化状态，不是通过。
- `git status --short` 可运行，当前文件均为未跟踪；Git 同时输出 `unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied` 权限警告，未阻塞本轮初始化。

## 2026-05-21 B1-001 health and CORS

### 已完成

- 在 `backend/` 内完成 Spring Boot + Maven 最小骨架落地。
- 新增全局 CORS 配置 `backend/src/main/java/com/weekendtravel/backend/config/WebConfig.java`，允许 `http://localhost:5173`。
- 新增 `GET /health` controller：`backend/src/main/java/com/weekendtravel/backend/controller/HealthController.java`。
- 新增健康检查响应 DTO：`backend/src/main/java/com/weekendtravel/backend/dto/HealthResponse.java`。
- 在 `backend/src/main/resources/application.properties` 中补充 `server.port=8000`。
- 恢复并保留测试目录 `backend/src/test/`，测试文件验证健康检查返回约定字段。
- 新增本轮 contract：`docs/contracts/B1-001-health-cors.md`。

### 验证记录

- `./backend/mvnw -f "backend/pom.xml" test` 通过，结果为 `BUILD SUCCESS`。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 现已通过；脚本已改为调用 `backend/mvnw.cmd`，不再依赖全局 `mvn`。
- `./backend/mvnw -f "backend/pom.xml" spring-boot:run` 成功启动服务，Tomcat 监听 `8000`。
- `curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 `HTTP/1.1 200`，并包含 `Access-Control-Allow-Origin: http://localhost:5173`，响应体包含 `status`、`service`、`timestamp`。

### 当前状态

- `feature_list.json` 中 `B1-001` 已按真实运行证据标记为 `verified`。
- 后端已不再是“未初始化”状态；下一优先任务应转到 `B1-002` SSE emitter and heartbeat 或 `B1-003` POST `/api/plan`。
- `verify.ps1` 已改为使用 Maven Wrapper，与当前 backend 工作流一致。


### 已补充

- 补读 2 页设计文档后，将候选排序公式、P0/P1/P2 约束分级、Mock API 覆盖范围、`bookOrOrder` 幂等键、`cancel_booking` 反向操作、正常场景 Plan B 触发率指标补入 docs。
- 在 `docs/decision-log.md` 记录了 20 vs 21 条 golden case、Plan B 指标口径、情侣模板是否纳入 Demo 的冲突处理。

### 保持不变

- Golden Case 仍以 21 条为准。
- Demo 场景仍只有 `family` 和 `friends`。
- API 字段名仍统一 camelCase；部分动作枚举值保留工具语义 lower_snake_case。

## 2026-05-21 F1-sprint1-generator

### 已完成

- 创建 `docs/contracts/F1-001-vue-skeleton.md`，明确本轮只交付 Vue 前端骨架，不接入真实 API / SSE / OpenAI。
- 在 `frontend/` scaffold Vue 3 + Vite + Pinia + Naive UI 项目，并保留 Tailwind CSS 入口以匹配前端技术栈。
- 配置 `vite.config.ts`：`server.port = 5173`、`strictPort = true`，`pnpm dev` 默认访问 `http://localhost:5173`。
- 创建 Pinia store `src/stores/planner.ts`，包含 `planId`、`agentState`、用户输入、场景、origin、SSE 状态占位和日志事件数组占位。
- 页面 `src/App.vue` 使用 Naive UI 组件渲染输入区、场景切换、状态摘要和日志占位，不调用后端。
- 通过宿主 npm 安装全局 `pnpm`，并在 `frontend/` 生成 `pnpm-lock.yaml`。
- 新增 `frontend/.gitignore`，忽略 `node_modules/`、`dist/` 和本地生成物。
- 新增根 `.gitignore`，忽略 Playwright MCP 本地快照目录 `.playwright-mcp/` 以及后端/前端常见生成物。
- 将 `feature_list.json` 中 `F1-001` 标记为 `verified` 并写入验证证据。

### 验证记录

- 直接运行 `.\verify.ps1 -Target frontend -Mode fast` 时，当前沙盒 shell 找不到宿主全局 `pnpm`，脚本按预期失败在环境检查。
- 使用宿主 PowerShell 运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：
- `pnpm typecheck` passed。
- `Verify passed.`
- 额外运行 `pnpm build` 通过，Vite 生产构建成功，主 JS chunk 约 290.51 kB / gzip 91.94 kB。
- 已启动 `pnpm dev`，监听 `127.0.0.1:5173`；Playwright 打开页面可见 WeekendTravel 骨架，点击“开始预演”后 Pinia 状态从 `START/idle` 变为 `INTENT/connecting`，console 无 error/warning。

### 下一步

- F1 下一步建议推进 `F1-002`：API client 和 mock fixture mode。
- 后端下一步推进 `B1-002`。
