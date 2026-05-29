# B1-005 QA: Plan B and DEGRADE rules

## 结论

开发验证 PASS；独立 evaluator 验收 PASS with caveat。

本文件同时记录两类证据：

- generator 开发阶段验证结果
- 独立 evaluator 子代理的只读验收结果

是否可以将条目标记为 `verified`，以独立 evaluator 结论为准，而不是以 generator 自测为准。

## 验收对象

- Contract: `docs/contracts/B1-005-plan-b-degrade.md`
- API contract: `docs/api-contract.md`
- 相关代码：
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanState.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/sse/ErrorEvent.java`
  - `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerStreamTests.java`
  - `backend/src/test/java/com/weekendtravel/backend/controller/PlanFlowIntegrationTests.java`

## 命令检查

### Backend Maven tests

命令：

```powershell
./backend/mvnw -f backend/pom.xml test
```

结果：PASS。

关键输出：

```text
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Backend fast verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果：PASS。

关键输出：

```text
[ok] mvnw.cmd test passed.
Verify passed.
```

## 关键行为证据

### happy path

- `PlanControllerStreamTests` 断言 stream 中存在：
  - `heartbeat`
  - `state_change`
  - `tool_call`
  - `tool_result`
  - `plan_ready`
  - `"isPlanB":`
  - `"replanCount":`
- `PlanFlowIntegrationTests.createThenStreamReachesPlanReady` 断言：
  - 到达 `PACK`
  - 无 `event:replan`
  - `"isPlanB":false`
  - `"replanCount":0`

### restaurantFull 注入

- `PlanFlowIntegrationTests.restaurantFullScenarioTriggersReplanThenDegrade` 断言 stream 包含：
  - `event:replan`
  - `REPLAN`
  - `原餐厅排队预计`
  - `DEGRADE`
  - `event:error`
  - `"code":"DEGRADE"`
  - `2 次重排后仍无可行方案`
  - 且不包含 `plan_ready`

### routeTooFar 注入

- `PlanFlowIntegrationTests.routeTooFarScenarioTriggersReplanThenDegrade` 断言 stream 包含：
  - `event:replan`
  - `REPLAN`
  - `原路线过远，已切换到更近餐厅`
  - `DEGRADE`
  - `event:error`
  - `"code":"DEGRADE"`
  - 且不包含 `plan_ready`

## 独立 evaluator 验收补充

- Independent evaluator verdict: `PASS with caveat`
- 关键 caveat：
  - happy path / `restaurantFull` / `routeTooFar` / `DEGRADE` / `latencyMs` 证据齐全；
  - 但 evaluator 观察到 `plan_ready.plan.status` 当前为 `CONFIRM`，而不是 `PACK`，这是当前实现的既有行为，不阻塞本轮 B1-005 contract。
- Independent evaluator commands and runtime evidence:
  - `"/d/Users/lenovo/Desktop/WeekendTravel/backend/mvnw" -f "/d/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml" spring-boot:run`
  - `curl -sS -D - http://127.0.0.1:8000/health`
  - `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "/d/Users/lenovo/Desktop/WeekendTravel/verify.ps1" -Target backend -Mode fast`
  - `"/d/Users/lenovo/Desktop/WeekendTravel/backend/mvnw" -f "/d/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml" test`
  - `curl` create + stream happy path
  - `curl` create + stream with `restaurantFull=true`
  - `curl` create + stream with `routeTooFar=true`
- Independent evaluator runtime evidence confirms:
  - happy path reaches `PACK` then `plan_ready`
  - `tool_result` payload contains `latencyMs`
  - `restaurantFull` produces `replan`, `REPLAN`, `DEGRADE`, `error(code=DEGRADE)`
  - `routeTooFar` produces `replan`, `REPLAN`, `DEGRADE`, `error(code=DEGRADE)`
- 本轮 feature 状态与证据更新应以这部分独立 evaluator 结果为最终放行依据。

## 实现核对

- `PlanState` 已补充 `DEGRADE`。
- `PlanContext` 已补充：
  - `replanCount`
  - `injectedPlanB`
  - `latestReason`
  - `packedPlan`
- `PlanStateMachineService` 已实现：
  - bounded replan loop
  - `replan` 事件发送
  - `DEGRADE` 状态转移
  - `error(code=DEGRADE)` 事件
  - happy path `plan_ready`
  - `friends` 场景的最小搜索参数 / 文案分流
- `PlanStreamService` 已在发送 heartbeat 前先校验 `planId` 是否存在，避免非法 stream 请求先写出 SSE 再异常中断。
- `tool_result` 仍保留 `latencyMs` 字段。
- SSE 事件名保持与 payload `type` 一致。
- 未引入 snake_case 线缆字段。

## 残留说明

- 当前 fallback 策略只实现“优先换餐厅”，尚未扩展到更复杂的活动替换或 P2 放宽策略。
- 独立 evaluator 的 caveat 为：happy path 运行时到达 `PACK`，但 `plan_ready.plan.status` 当前仍为 `CONFIRM`，这符合当前 payload 生成逻辑，不阻塞本轮 contract 放行。
