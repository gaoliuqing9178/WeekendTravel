# Contract: B1-002 SSE emitter and heartbeat

## 本轮目标

- 实现 `GET /api/plan/{planId}/stream` 的最小 SSE 链路。
- 返回 `text/event-stream`。
- 在 stream 建立后至少发送一个 `heartbeat` 事件。
- 在 stream 建立后至少发送一个 mock `state_change` 事件。
- 事件名与 `data.type` 保持一致，字段使用 camelCase。
- 增加后端集成测试，验证响应头和最小事件序列。

## 明确不做

- 不引入 Spring AI。
- 不引入 Spring WebFlux 或 Reactor Flux 作为本轮 SSE 实现基础。
- 不实现完整状态机。
- 不实现 `POST /api/plan`。
- 不实现真实 Tool 编排、真实规划流程或持久化 plan。
- 不改 `docs/api-contract.md` 已确认的正式线缆字段。

## 用户路径

1. 启动后端服务。
2. 前端或测试客户端请求 `GET /api/plan/{planId}/stream`。
3. 服务返回 `text/event-stream` 响应。
4. 客户端先收到一条 `heartbeat`。
5. 客户端再收到一条 mock `state_change`，用于 Sprint 1 联调和日志面板接入。

## UI 要求

- 本轮无 UI 改动。
- 前端后续只需能消费该 stream，本轮不实现前端日志面板。

## API / 后端要求

- 端点路径为 `GET /api/plan/{planId}/stream`。
- 响应头至少满足：
  - `Content-Type: text/event-stream`
  - `Cache-Control: no-cache`
- 返回事件至少包含：
  - `heartbeat`
  - `state_change`
- `heartbeat` data JSON 至少包含：
  - `type`
  - `planId`
  - `timestamp`
- `state_change` data JSON 至少包含：
  - `type`
  - `planId`
  - `from`
  - `to`
  - `timestamp`
- `state_change` 的最小 mock 转移固定为 `START -> INTENT`。
- 事件名与 `type` 字段必须一致。
- 后端实现基于 Spring MVC `SseEmitter`，与当前项目栈保持一致。

## 数据和状态要求

- `planId` 直接使用路径参数。
- `heartbeat.type` 固定为 `heartbeat`。
- `state_change.type` 固定为 `state_change`。
- `timestamp` 返回 Unix epoch milliseconds。
- 当前不校验 plan 是否真实存在；本轮以最小 mock stream 为准。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理运行后端验证，不使用 generator 自测结果作为最终放行依据。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。

手动路径：

- 启动后端后请求 `GET /api/plan/test-plan/stream`。
- 确认响应头包含 `text/event-stream`。
- 确认响应中至少出现一条 `event: heartbeat` 和一条 `event: state_change`。
- 确认 `data` JSON 中 `type` 与事件名一致。

自动验证：

- `./backend/mvnw -f "backend/pom.xml" test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`

API / 日志 / 截图证据：

- evaluator 需要在 QA 报告中记录后端验证命令、SSE 响应头、最小事件输出和放行结论。

## 失败阈值

- 核心用户路径不可用，失败。
- 响应不是 `text/event-stream`，失败。
- 缺少 `heartbeat` 或缺少 `state_change`，失败。
- 事件名与 `data.type` 不一致，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只实现占位却没有可验证 SSE 输出，失败。
- 测试阶段没有独立 evaluator 子代理参与，不能标为 `verified`。
