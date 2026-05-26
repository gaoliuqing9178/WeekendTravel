# Backend Handoff

## 当前状态

- [x] Spring Boot + Maven 项目已初始化。
- [x] `pom.xml`、`mvnw`、`src/main`、`src/test` 已存在。
- [x] B2 POI mock 数据已落地为 `src/main/resources/mock/poi_data.json`，并通过 evaluator 验证。
- [x] 当前已有健康检查 / CORS 骨架、B2 POI 数据、SearchTool、RouteTool、AvailabilityTool、BookingTool、MessageTool、ScenarioFlags、debug scenario API、最小 SSE stream 和最小 `POST /api/plan` 占位接口；状态机、MockApiService 仍未实现。
- [ ] `GET /health` 已按 `docs/api-contract.md` 返回契约字段。
- [x] CORS 已放通 `http://localhost:5173` 和 `http://127.0.0.1:5173`。
- [x] `POST /api/plan` 已返回 `planId` 和 `status`。
- [x] `GET /api/plan/{planId}/stream` 已推送心跳和至少一个 `state_change`。
- [x] `POST /api/debug/scenario` 已支持动态更新 flags。

## 开工前必读

- [x] 阅读 `../docs/backend-contract.md`。
- [x] 阅读 `../docs/api-contract.md`。
- [x] 阅读 `../docs/architecture.md`。
- [x] 阅读 `../docs/handoff.md`。
- [ ] 如要接入 LLM，先补 `../docs/decision-log.md` 中的 SDK / 模型 / 超时 / 兜底决策。

## 后端范围边界

- [x] B1 负责：Controller、状态机、Planner、SSE、意图抽取与话术生成边界。
- [x] B2 负责：SearchTool、RouteTool、AvailabilityTool、BookingTool、MessageTool、MockApiService、POI JSON、ScenarioFlags。
- [x] 规划决策必须留在状态机和规则代码内，不能交给 LLM。
- [x] 后端只依赖 API 契约和本地 Mock 数据，不依赖前端源码。
- [x] API / SSE 字段统一使用 camelCase。

## Sprint 1 最小交付清单

- [ ] 新建 `docs/contracts/B1-001-health-cors.md` 记录健康检查和 CORS 契约。
- [ ] 实现 `GET /health`，返回 `status`、`service`、`timestamp`。
- [ ] 配置 `server.port=8000`。
- [x] 配置 CORS，允许 `http://localhost:5173` 和 `http://127.0.0.1:5173`。
- [x] 为 `POST /api/plan` 建立最小占位实现，返回 202 + `planId` / `status`。
- [x] 为 `GET /api/plan/{planId}/stream` 建立最小 SSE 占位实现。
- [x] SSE 至少推送 1 个 `heartbeat`。
- [x] SSE 至少推送 1 个 `state_change`。
- [x] 为 `POST /api/debug/scenario` 建立最小可更新实现。
- [ ] `mvn test` 可运行通过。

## 状态机与流程约束

- [ ] 状态枚举至少包含 `START / INTENT / CLARIFY / SKELETON / RECALL / VALIDATE / REPLAN / PACK / CONFIRM / ADJUST / EXECUTE / DEGRADE / DONE / FAILED`。
- [ ] 每次状态变化都推送 `state_change`。
- [ ] `CLARIFY` 每次只问一个最关键问题，最多 2 次。
- [ ] `ADJUST` 只重规划受影响槽位，最多 3 次。
- [ ] 单个 plan 最多 3 次 replan。
- [ ] 累计 Tool 调用不超过 30 次。
- [ ] 规划超过 25 秒进入 `DEGRADE`。

## API / SSE 契约实现清单

- [ ] `POST /api/plan/{planId}/execute` 支持确认执行。
- [ ] `POST /api/plan/{planId}/clarify` 支持继续规划。
- [ ] `PATCH /api/plan/{planId}/adjust` 支持局部微调。
- [x] SSE 事件名与 `type` 字段保持一致。
- [x] 支持 `heartbeat`。
- [x] 支持 `state_change`。
- [ ] 支持 `tool_call`。
- [ ] 支持 `tool_result`，并提供 `latencyMs`。
- [ ] 支持 `clarification_request`。
- [ ] 支持 `replan`。
- [ ] 支持 `adjust_result`。
- [ ] 支持 `plan_ready`。
- [ ] 支持 `execute_result`。
- [ ] 支持 `done`。
- [ ] 支持 `error`。

## B2 Tool / Mock 数据清单

- [x] 准备本地 POI JSON，首批不少于 30 条。
- [x] 最终 POI JSON 扩充到不少于 50 条。
- [ ] 实现 `MockApiService`。
- [x] 实现 `ScenarioFlags`。
- [x] 实现 `SearchTool / searchLocalPlaces`。
- [x] 实现 `RouteTool / calculateRouteTime`。
- [x] 实现 `AvailabilityTool / checkAvailability`。
- [x] 实现 `BookingTool / bookOrOrder`。
- [x] 实现 `MessageTool / composeShareMessage`。
- [x] `bookOrOrder` 请求携带 `idempotencyKey`。
- [x] 支持 `buy_ticket`。
- [x] 支持 `reserve_table`。
- [x] 支持 `take_number`。
- [x] 支持 `schedule_delivery`。
- [x] 支持 `add_note`。
- [x] 支持 `cancel_booking`。

## 异常注入与降级

- [x] `POST /api/debug/scenario` 可动态切换 `restaurantFull`。
- [x] `POST /api/debug/scenario` 可动态切换 `routeTooFar`。
- [x] `POST /api/debug/scenario` 可动态切换 `bookingFail`。
- [x] `POST /api/debug/scenario` 可动态切换 `ageMismatch`。
- [ ] 餐厅满座时优先锁活动地点并重召餐厅。
- [ ] 路线过远时优先换餐厅，其次换活动。
- [ ] 年龄不匹配时直接过滤候选，不足时扩大半径或放松 P2。
- [ ] 下单失败时同类重试 1 次，仍失败则标记 `manual`。
- [ ] 无解冲突时保 P0/P1，放松 P2，并输出透明说明。

## 验证与回归

- [x] Generator 完成开发后，测试阶段必须交给独立 evaluator 子代理执行。
- [x] Generator 自己运行的 `./mvnw test`、`verify.ps1`、curl 或本地冒烟只能作为开发准备记录，不能单独作为 `verified` 证据。
- [x] Evaluator 子代理需要记录命令、API 输出、日志或 QA 报告；没有 evaluator 证据不要标记完成。
- [x] 运行 `./mvnw test`。
- [x] 运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ..\verify.ps1 -Target backend -Mode fast`。
- [ ] 为 `GET /health` 添加 API 测试。
- [x] 为最小 SSE 链路添加测试或可复现验证步骤。
- [ ] 没有验证证据前，不要把相关功能标记为完成或 verified。

### 2026-05-22 B2-001 验证记录

- `docs/contracts/B2-001-poi-data.md` 已新增。
- `src/main/resources/mock/poi_data.json` 包含 52 条 POI：`activity=20`、`restaurant=20`、`cafe=3`、`dessert=3`、`supplier=6`。
- 场景覆盖：`family=28`、`friends=32`、`both=8`。
- `src/test/java/com/weekendtravel/backend/PoiDataTests.java` 覆盖 JSON 解析、数量、唯一 ID、必填字段、availability 槽位、family / friends 和类别覆盖。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，3 tests、0 failures、`BUILD SUCCESS`。
- 独立 evaluator 子代理 Newton (`019e4fdf-35fd-7c40-a198-aa35368a6fae`, `B2-001-EVAL-CODEX-20260522T2130+0800`) 已放行。
- QA 报告：`docs/qa/B2-001-poi-data.md`。

### 2026-05-24 B2-002 验证记录

- `docs/contracts/B2-002-search-route-tools.md` 已新增。
- `src/main/java/com/weekendtravel/backend/b2/repository/PoiRepository.java` 从 classpath resource `mock/poi_data.json` 加载本地 POI catalog。
- `src/main/java/com/weekendtravel/backend/b2/tool/SearchTool.java` 已实现 `searchLocalPlaces`，支持场景、类别、关键词、距离、年龄、人数、limit 过滤，返回候选 POI 和 `latencyMs`。
- `src/main/java/com/weekendtravel/backend/b2/tool/RouteTool.java` 已实现 `calculateRouteTime`，支持当前位置到 POI、POI 到 POI、同一 POI 0 分钟、未知/缺失 POI 异常，返回 `distanceMinutes`、summary 和 `latencyMs`。
- `src/test/java/com/weekendtravel/backend/b2/SearchToolTests.java` 覆盖搜索候选、无结果、非法 scenario 和非法 limit。
- `src/test/java/com/weekendtravel/backend/b2/RouteToolTests.java` 覆盖中心点路线、POI 间路线、同一 POI、未知目标和缺失目标。
- Generator 开发侧准备检查：`.\mvnw.cmd test` 通过，12 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，12 tests、0 failures、`Verify passed.`。
- 独立 evaluator 子代理 Averroes (`019e5780-ab21-7250-9644-2889eae14565`, `B2-002-EVAL-CODEX-20260524T0904+0800`) 已放行。
- QA 报告：`docs/qa/B2-002-search-route-tools.md`。

### 2026-05-24 B2-003 验证记录

- `docs/contracts/B2-003-availability-scenario-flags.md` 已新增。
- `src/main/java/com/weekendtravel/backend/b2/scenario/ScenarioFlags.java` 已实现可热更新 flags，支持 `restaurantFull`、`routeTooFar`、`bookingFail`、`ageMismatch`。
- `src/main/java/com/weekendtravel/backend/b2/tool/AvailabilityTool.java` 已实现 `checkAvailability`，从本地 POI JSON 的 `defaultAvailability` 返回默认余位、等待时间、可用性、匹配原因、当前 flags 和 `latencyMs`。
- `restaurantFull=true` 会让餐厅 POI 不可用、`remaining=0`、`availabilityStatus=full`、等待时间至少 70 分钟；`ageMismatch=true` 且请求携带 `minAge` 时会让结果不可用并标记 `ageMatched=false`。
- `src/main/java/com/weekendtravel/backend/b2/controller/DebugScenarioController.java` 已实现 `POST /api/debug/scenario`，返回 `{ "updated": { ... } }` 并无需重启即可更新 flags。
- `src/test/java/com/weekendtravel/backend/b2/AvailabilityToolTests.java` 覆盖默认 availability、餐厅满座注入、年龄不匹配注入和非法输入。
- `src/test/java/com/weekendtravel/backend/b2/ScenarioFlagsTests.java` 覆盖四个 flags 的默认、全量更新、局部更新和 reset。
- `src/test/java/com/weekendtravel/backend/b2/DebugScenarioControllerTests.java` 使用随机端口 Spring Boot 服务和 JDK `HttpClient` 连续两次 POST 验证 debug API 热更新。
- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，18 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，18 tests、0 failures、`Verify passed.`。
- 独立 evaluator 子代理 Dirac (`019e5795-2750-75c0-845e-f3104401f0a0`) 已放行。
- QA 报告：`docs/qa/B2-003-availability-scenario-flags.md`。

### 2026-05-24 B2-004 验证记录

- `docs/contracts/B2-004-booking-tool.md` 已新增。
- `src/main/java/com/weekendtravel/backend/b2/tool/BookingTool.java` 已实现 `bookOrOrder` 和兼容入口 `execute`。
- `src/main/java/com/weekendtravel/backend/b2/tool/BookingRequest.java` 包含 `planId`、`actionId`、`actionType`、`targetPoiId`、`description`、`idempotencyKey` 和 `previousConfirmationNo`。
- `src/main/java/com/weekendtravel/backend/b2/tool/BookingResult.java` 返回 `execute_result` 核心字段：`type`、`planId`、`actionId`、`actionType`、`status`、`confirmationNo`、`timestamp`，并补充 `message` 和 `latencyMs`。
- 成功路径按动作类型生成 deterministic mock 确认号：`MOCK-TKT-`、`MOCK-TBL-`、`MOCK-QNO-`、`MOCK-DLV-`、`MOCK-NOTE-`、`MOCK-CXL-`。
- `bookOrOrder` 通过内存 `ConcurrentHashMap` 对 `idempotencyKey` 做幂等缓存；同一 key 重复调用返回同一份 `BookingResult`。
- `bookingFail=true` 时，非 `cancel_booking` 动作返回 `status=failed` 且 `confirmationNo=null`；`cancel_booking` 作为 Tool 层反向操作不被该注入阻断。
- 非取消动作会校验 `targetPoiId` 必填、目标 POI 存在且 `actionTypes` 支持当前动作。
- `src/test/java/com/weekendtravel/backend/b2/BookingToolTests.java` 覆盖成功确认号、幂等重放、`bookingFail`、`cancel_booking` 和非法请求。
- `backend/pom.xml` 已关闭 Maven compiler incremental compilation，稳定标准 `verify.ps1` 在当前 Windows/Javac 环境下的新增源文件编译。
- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，23 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，23 tests、0 failures、`Verify passed.`。
- 独立 evaluator 子代理 Confucius (`019e57a4-d8b5-7b10-aba7-5557ecee3057`) 已放行。
- 中文 QA 报告：`docs/qa/B2-004-booking-tool.md`。

### 2026-05-24 B2-005 验证记录

- `docs/contracts/B2-005-message-tool.md` 已新增。
- `src/main/java/com/weekendtravel/backend/b2/tool/MessageTool.java` 已实现 `composeShareMessage` 和兼容入口 `execute`。
- `src/main/java/com/weekendtravel/backend/b2/tool/MessageRequest.java`、`MessageResult.java`、`MessagePlanPayload.java`、`MessageTimeSlot.java`、`MessageActionSummary.java` 已新增。
- `MessageTool` 生成 deterministic fallback 中文 `shareMessage`，返回 `llmUsed=false`、`templateVersion=fallback-v1` 和 `latencyMs`。
- `MessageResult.plan` 是 Plan-compatible payload，包含 `shareMessage`，供后续 B1 合入正式 `Plan.shareMessage`；本轮未实现 B1 `Plan`、Planner、状态机、SSE 或 `POST /api/plan`。
- 家庭/朋友场景文案、Plan B 原因、动作摘要、`send_message` 过滤、时间线排序和非法请求均有测试覆盖。
- 本轮没有 OpenAI/LLM/网络调用，没有读取或硬编码 `OPENAI_API_KEY`；未来真实 LLM 接入仍必须先核验官方文档并更新 `../docs/decision-log.md`。
- `src/test/java/com/weekendtravel/backend/b2/MessageToolTests.java` 覆盖 deterministic fallback、`plan.shareMessage`、`llmUsed=false`、Plan B、朋友场景排序和错误路径。
- Generator 开发侧准备检查：`.\mvnw.cmd test "-Dmaven.compiler.useIncrementalCompilation=false"` 通过，27 tests、0 failures、`BUILD SUCCESS`。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，27 tests、0 failures、`Verify passed.`。
- 独立 evaluator 子代理 Mencius (`019e57b3-5e9e-7e92-b1e5-7744b2a4d329`, `B2-005-EVAL-CODEX-20260524T0959+0800`) 已放行。
- 中文 QA 报告：`docs/qa/B2-005-message-tool.md`。

### 2026-05-26 F1-004 real mode CORS 补充

- 为支持 F1-004 real mode 从前端默认开发地址提交，`src/main/java/com/weekendtravel/backend/config/WebConfig.java` 已允许 `http://localhost:5173` 和 `http://127.0.0.1:5173`。
- `src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java` 已新增 `createAllowsConfiguredFrontendOrigins`，覆盖两个 Origin 对 `POST /api/plan` 的 CORS 回显。
- Generator 开发侧准备检查：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast` 通过，35 tests、0 failures、`BUILD SUCCESS`。
- 独立 evaluator 子代理 Gauss (`019e64d9-95ea-76a2-9f4d-53478e6a64a6`) 已在 F1-004 验收中覆盖该 CORS 修正。
- QA 报告：`docs/qa/F1-004-input-plan-api.md`。

## 接手提醒

- [x] 每次改 API 字段前，先更新 `../docs/api-contract.md`。
- [x] 不要擅自把 snake_case 混入正式线缆字段。
- [x] 不要为了让测试简单而删减 SSE 事件。
- [x] 不要接入真实支付、真实预约、真实配送。
- [x] 不要默认 LLM 可以决定 POI、状态转移或规划策略。
