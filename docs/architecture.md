# Architecture

## 总览

```text
用户
  |
  v
Frontend F1 (Vue 3 / Vite / Pinia / Naive UI)
  |  REST: POST /api/plan, execute, clarify, adjust, debug scenario
  |  SSE: GET /api/plan/{planId}/stream
  v
Backend B1 (Spring Boot Controller / Agent / Planner / SSE)
  |
  | calls typed tools
  v
Backend B2 (Tool Layer / Mock API / POI JSON / ScenarioFlags)
  |
  | reads local mock data, returns deterministic results
  v
Local JSON Mock Data

LLM Boundary:
Backend B1 may call LLM only for intent extraction and message generation.
Planning decisions stay in the state machine and rule code.
```

## 模块职责

### Frontend F1

- Web UI、自然语言输入、场景选择。
- SSE 接入和日志面板。
- 方案卡、确认执行、Plan B 展示。
- `CLARIFY` 反问、`ADJUST` 微调、错误和降级状态展示。
- 在后端未完成前，基于 `docs/fixtures/` 开发和自测。

### Backend B1

- Spring Boot 应用和路由。
- Agent 状态机、Planner、SSE 推送。
- `POST /api/plan`、`GET /api/plan/{planId}/stream`、`POST /api/plan/{planId}/execute`、`POST /api/plan/{planId}/clarify`、`PATCH /api/plan/{planId}/adjust`。
- 意图抽取和话术生成 prompt，但不在初始化阶段锁 SDK 版本或模型名。

### Backend B2

- Tool 实现：SearchTool、RouteTool、AvailabilityTool、BookingTool、MessageTool 等。
- Mock API Service、本地 POI JSON、ScenarioFlags。
- 异常注入和模拟下单 / 预约 / 消息工具。
- `POST /api/debug/scenario` 的数据支撑。

### Mock API

- 所有外部能力都先走本地 Mock。
- Mock API 覆盖美团类 POI、路线、余位、订座、取号、配送和消息能力；真实接入时应逐项替换。
- POI 数据先不少于 30 条，最终不少于 50 条。
- 异常注入至少覆盖：餐厅满座、路线过远、下单失败、年龄不匹配。

## Planning 和排序

```text
INTENT -> SKELETON -> RECALL -> VALIDATE -> PACK
                 ^                   |
                 |                   v
                 +------ REPLAN <----+
```

- 时间骨架由规则生成，当前 Demo 只固定家庭和朋友两个场景。
- 设计文档中出现过“情侣”模板，但当前产品范围不把它作为 Demo 场景或接口枚举。
- 候选排序初始公式：`score = 0.4 * 相关性 + 0.3 * 距离 + 0.2 * 评分 + 0.1 * 余位健康度`。
- 可行性校验优先保 P0/P1 约束，必要时放松 P2；仍失败则进入 `DEGRADE`。

### LLM 调用

- 只允许用于：
  - 意图抽取：自然语言 -> 结构化 Intent。
  - 话术生成：执行结果 -> 自然转发消息。
- 不允许用于：
  - 选择 POI。
  - 决定状态转移。
  - 判断 Plan B 规则。
  - 直接执行下单或预约。
- API key 从 `OPENAI_API_KEY` 读取，不写入仓库。
- 真正实现前必须重新核验官方 OpenAI developer docs / MCP，并把 SDK、模型和调用方式决策写入 `docs/decision-log.md`。

## SSE 数据流

```text
1. 前端 POST /api/plan，后端返回 planId 和 processing 状态。
2. 前端用 planId 建立 GET /api/plan/{planId}/stream。
3. 后端 B1 启动 Agent 状态机。
4. 每次状态转移推送 state_change。
5. 每次 Tool 调用推送 tool_call 和 tool_result。
6. 需要反问时推送 clarification_request，等待前端 POST clarify。
7. 触发异常重排时推送 replan。
8. 方案可确认时推送 plan_ready，状态进入 CONFIRM。
9. 用户 PATCH adjust 时推送 adjust_result，最多 3 次。
10. 用户 POST execute 后推送 execute_result。
11. 全部完成推送 done；无法完成时推送 error 或 DEGRADE 相关事件。
```

## 依赖方向

- 前端只依赖 `docs/api-contract.md` 和 `docs/fixtures/`，不依赖 Java 类。
- 后端只依赖 `docs/api-contract.md` 和本地 Mock 数据，不依赖前端源码。
- B1 可以调用 B2 Tool；B2 不反向依赖 B1 的状态机实现。
- API 字段变更先改 `docs/api-contract.md`，再同步后端模型、前端类型和 fixture。
- Contract、progress、handoff 是跨 agent 交接入口，不能只靠聊天上下文。
