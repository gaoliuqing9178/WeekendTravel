# B1-002 SSE emitter and heartbeat QA 报告

## 结论

PASS

Evaluator：`B1-002-EVAL-gpt-5.4-20260526`

验收时间：2026-05-26

## 检查范围

- 已检查 `docs/contracts/B1-002-sse-emitter.md`，确认本轮目标是为 `GET /api/plan/{planId}/stream` 提供最小 SSE 链路。
- 已检查 `feature_list.json` 中 `B1-002` 条目，验收项为：
  - `GET /api/plan/{planId}/stream returns text/event-stream`
  - `SSE stream sends heartbeat`
  - `SSE stream sends at least one mock state_change event`
- 已检查 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`、`backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`、`backend/src/main/java/com/weekendtravel/backend/plan/sse/HeartbeatEvent.java`、`backend/src/main/java/com/weekendtravel/backend/plan/sse/StateChangeEvent.java`。
- 已检查 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java` 的真实 HTTP 集成测试覆盖。

## 源码与测试检查

- `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
  - 使用 `@RequestMapping("/api/plan")` 和 `@GetMapping(path = "/{planId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)` 暴露 `GET /api/plan/{planId}/stream`。
  - 显式设置 `Cache-Control: no-cache` 和 `Connection: keep-alive`。
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`
  - `openStream(planId)` 创建 `SseEmitter`。
  - 建连后先发送 `heartbeat`，再发送 mock `state_change`。
  - mock 转移固定为 `START -> INTENT`。
  - 通过 `SseEmitter.event().name(event.type()).data(event)` 发送，事件名与 `data.type` 使用同一来源，保持一致。
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/HeartbeatEvent.java`
  - 字段为 `type`、`planId`、`timestamp`。
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/StateChangeEvent.java`
  - 字段为 `type`、`planId`、`from`、`to`、`timestamp`。
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java`
  - 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JDK `HttpClient` 发起真实 HTTP 请求。
  - 断言 `statusCode == 200`。
  - 断言 `content-type` 包含 `text/event-stream`。
  - 断言 `cache-control == no-cache`。
  - 断言响应体包含 `heartbeat`、`state_change`、`test-plan`、`START`、`INTENT`。

## 命令与测试结果

父仓库定向测试：

```powershell
D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml -Dtest=PlanControllerStreamTests test
```

结果：`BUILD SUCCESS`

父仓库全量后端测试：

```powershell
D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 28`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

Generator 开发侧准备检查：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果：`Verify passed.`

## 关键证据

- 端点路径符合合同：`GET /api/plan/{planId}/stream`。
- Controller `produces = MediaType.TEXT_EVENT_STREAM_VALUE`，满足 `text/event-stream` 响应类型要求。
- Stream service 明确发送至少 1 条 `heartbeat`。
- Stream service 明确发送至少 1 条 mock `state_change`。
- `state_change` 固定为 `START -> INTENT`，符合合同中的最小 mock 转移要求。
- 事件名与 `data.type` 通过 `.name(event.type()).data(event)` 绑定为同一来源。
- 真实 HTTP 集成测试验证了响应头与最小事件内容。

## 风险 / 备注

- evaluator 首次在隔离 worktree 中无法看到父工作树未提交改动，因此重新以父工作树绝对路径执行了独立验收；最终放行结论基于父工作树源码和父仓库测试结果。
- 现有测试没有逐字断言原始 SSE 线缆中 `event:` 行文本，但代码中 `.name(event.type()).data(event)` 已充分证明事件名与 `type` 一致。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。
