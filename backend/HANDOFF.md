# Backend Handoff

## 当前状态

- [x] Spring Boot + Maven 项目已初始化。
- [x] `pom.xml`、`mvnw`、`src/main`、`src/test` 已存在。
- [x] 当前只有默认应用骨架，业务接口、状态机、SSE、Tool、Mock 数据都还未实现。
- [ ] `GET /health` 已按 `docs/api-contract.md` 返回契约字段。
- [ ] CORS 已放通 `http://localhost:5173`。
- [ ] `POST /api/plan` 已返回 `planId` 和 `status`。
- [ ] `GET /api/plan/{planId}/stream` 已推送心跳和至少一个 `state_change`。
- [ ] `POST /api/debug/scenario` 已支持动态更新 flags。

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
- [ ] 配置 CORS，仅允许 `http://localhost:5173`。
- [ ] 为 `POST /api/plan` 建立最小占位实现，返回 202 + `planId` / `status`。
- [ ] 为 `GET /api/plan/{planId}/stream` 建立最小 SSE 占位实现。
- [ ] SSE 至少推送 1 个 `heartbeat`。
- [ ] SSE 至少推送 1 个 `state_change`。
- [ ] 为 `POST /api/debug/scenario` 建立最小可更新实现。
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
- [ ] SSE 事件名与 `type` 字段保持一致。
- [ ] 支持 `heartbeat`。
- [ ] 支持 `state_change`。
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

- [ ] 准备本地 POI JSON，首批不少于 30 条。
- [ ] 最终 POI JSON 扩充到不少于 50 条。
- [ ] 实现 `MockApiService`。
- [ ] 实现 `ScenarioFlags`。
- [ ] 实现 `SearchTool / searchLocalPlaces`。
- [ ] 实现 `RouteTool / calculateRouteTime`。
- [ ] 实现 `AvailabilityTool / checkAvailability`。
- [ ] 实现 `BookingTool / bookOrOrder`。
- [ ] 实现 `MessageTool / composeShareMessage`。
- [ ] `bookOrOrder` 请求携带 `idempotencyKey`。
- [ ] 支持 `buy_ticket`。
- [ ] 支持 `reserve_table`。
- [ ] 支持 `take_number`。
- [ ] 支持 `schedule_delivery`。
- [ ] 支持 `add_note`。
- [ ] 支持 `cancel_booking`。

## 异常注入与降级

- [ ] `POST /api/debug/scenario` 可动态切换 `restaurantFull`。
- [ ] `POST /api/debug/scenario` 可动态切换 `routeTooFar`。
- [ ] `POST /api/debug/scenario` 可动态切换 `bookingFail`。
- [ ] `POST /api/debug/scenario` 可动态切换 `ageMismatch`。
- [ ] 餐厅满座时优先锁活动地点并重召餐厅。
- [ ] 路线过远时优先换餐厅，其次换活动。
- [ ] 年龄不匹配时直接过滤候选，不足时扩大半径或放松 P2。
- [ ] 下单失败时同类重试 1 次，仍失败则标记 `manual`。
- [ ] 无解冲突时保 P0/P1，放松 P2，并输出透明说明。

## 验证与回归

- [ ] 运行 `./mvnw test`。
- [ ] 运行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ..\verify.ps1 -Target backend -Mode fast`。
- [ ] 为 `GET /health` 添加 API 测试。
- [ ] 为最小 SSE 链路添加测试或可复现验证步骤。
- [ ] 没有验证证据前，不要把相关功能标记为完成或 verified。

## 接手提醒

- [x] 每次改 API 字段前，先更新 `../docs/api-contract.md`。
- [x] 不要擅自把 snake_case 混入正式线缆字段。
- [x] 不要为了让测试简单而删减 SSE 事件。
- [x] 不要接入真实支付、真实预约、真实配送。
- [x] 不要默认 LLM 可以决定 POI、状态转移或规划策略。
