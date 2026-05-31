# INT-002 Family Scenario Complete Path QA

## 结论

PASS

## Eval ID

`INT-002-EVAL-CODEX-20260531T1615+0800`

独立 evaluator 子代理验收；本轮只写入本 QA 报告和截图，不修改业务源码、`feature_list.json`、`progress.md` 或 handoff。

## 验收范围

已读取并核对：

- `AGENTS.md`
- `docs/contracts/INT-002-family-complete-path.md`
- `docs/api-contract.md`
- `feature_list.json`
- `progress.md`
- `backend/HANDOFF.md`
- `frontend/F1-handoff.md`

已检查相关实现：

- 后端：`PlanController`、`PlanStateMachineService`、Plan API records、SSE records、`PlanFlowIntegrationTests`
- 前端：`frontend/src/stores/planner.ts`、`frontend/src/composables/useSSE.ts`、`PlanCard.vue`、`ExecutionTracker.vue`

实现核对摘要：

- `PlanController` 暴露 `POST /api/plan`、`GET /api/plan/{planId}/stream`、`POST /clarify`、`POST /execute`。
- `PlanStateMachineService.executePlan` 要求 `confirmed=true` 且当前为 `CONFIRM`、已有 packed plan 后才允许执行；成功返回 `status=executing`。
- `PlanStateMachineService.streamPlan` 在 `CONFIRM + executionRequested` 时进入 `continueExecution`，发送 `CONFIRM -> EXECUTE`、每个 action 的 `execute_result`、`EXECUTE -> DONE` 和 `done`。
- 后端 `ExecuteResultEvent` / `DoneEvent` records 使用 camelCase 字段：`planId`、`actionId`、`actionType`、`confirmationNo`。
- `PlanFlowIntegrationTests.familyCompletePathExecutesActionsAndReachesDone` 覆盖创建 plan、规划流 `plan_ready` / `CONFIRM`、执行 API、执行流 `execute_result`、`reserve_table`、`send_message`、`MOCK-TBL-*`、`MOCK-MSG-*`、`done` / `DONE`。
- 前端 real mode `confirmPlanExecution` 调用 `executePlan(planId, { confirmed: true })` 后使用同一个 `planId` 重新 `sse.connect(response.planId)`。
- 前端 `applySsePayload` 对 `execute_result` 更新 action `status` / `confirmationNo`，对 `done` 设置 `agentState=DONE`、停止执行态并更新当前 plan 状态。
- `PlanCard.vue` 渲染 plan summary、timeline、`poi.name` / address / rating / wait、actions 和 share message；`ExecutionTracker.vue` 渲染完成计数和 mock 确认号。

`feature_list.json` 中 `INT-002` 在验收前仍为 `todo` 且 evidence 为空；本报告只提供 evaluator 证据，不替 generator 更新状态。

## 命令验证

### Fast Verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast
```

结果：PASS

关键输出：

- Backend fast verify：`mvnw.cmd test` 通过。
- 后端测试合计：`Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`。
- `PlanFlowIntegrationTests`：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- Frontend fast verify：`pnpm verify:fixtures` 通过，`docs/fixtures/sse-events.jsonl -> 28 JSONL events`。
- Frontend typecheck：`pnpm typecheck passed`。
- 总结：`Verify passed.`

### Forbidden Snake Case

命令：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" backend\src frontend\src frontend\scripts docs\fixtures
```

结果：PASS，无命中输出。`rg` 无命中返回 exit code 1，符合本检查预期。

### feature_list.json 解析

命令：

```powershell
Get-Content -Raw -LiteralPath 'feature_list.json' -Encoding utf8 | ConvertFrom-Json | Select-Object -ExpandProperty project
```

结果：PASS，输出 `WeekendTravel`。

## Chrome DevTools MCP 浏览器验收

浏览器：Chrome DevTools MCP  
URL：`http://127.0.0.1:5173/`  
模式：real mode  
截图：`docs/qa/INT-002-devtools-real.png`

### 操作步骤

1. 打开 `http://127.0.0.1:5173/`。
2. 保留默认 family 输入：
   `今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。`
3. 点击 `提交规划`。
4. 页面进入 `CLARIFY`，ClarifyBubble 显示 `请问大概想玩几个小时？`。
5. 点击 `回答：4-6小时`。
6. 页面进入 `CONFIRM`，PlanCard 出现，LogPanel 显示 `plan_ready`。
7. 点击 `确认执行`。
8. 页面进入 `DONE`，LogPanel 显示 `execute_result` 和 `done`。

### DOM / Snapshot 证据

最终页面可见：

- `Mode` 为 `real`。
- 真实后端 planId：`plan_79b2807ae0b3`。
- Agent 状态：`DONE`。
- SSE 状态：`closed`。
- PlanCard 标题：`亲子活动搭配家庭友好晚餐`。
- PlanCard timeline 有 POI 信息：
  - `小小科学工坊 · 少年宫东楼 2F`
  - `四季家庭小厨 · 武林广场西侧 2F`
- 执行包为 2 个动作：
  - `预约 16:30 的家庭座位`
  - `生成家庭出行消息`
- ExecutionTracker：`2 / 2 个动作完成`。
- Mock 确认号：
  - `MOCK-TBL-50306`
  - `MOCK-MSG-99469`
- LogPanel 包含：
  - `plan_ready`
  - `CONFIRM -> EXECUTE`
  - `execute_result`
  - `reserve_table success`
  - `send_message success`
  - `EXECUTE -> DONE`
  - `done`

DevTools DOM evaluate 结果：

```json
{
  "hasModeReal": true,
  "planId": "plan_79b2807ae0b3",
  "hasDone": true,
  "hasConfirm": true,
  "hasPoiActivity": true,
  "hasPoiRestaurant": true,
  "hasExecutionCount": true,
  "hasMockTbl": true,
  "hasMockMsg": true,
  "hasExecuteResultLog": true,
  "hasDoneLog": true,
  "bodyTextLength": 3085
}
```

### Console 证据

Chrome DevTools MCP `list_console_messages` 过滤 `error` / `warn`：

```text
<no console messages found>
```

结论：console error / warn 为 0。

### Network 证据

关键请求：

| reqid | 请求 | 状态 | 证据 |
|---:|---|---:|---|
| 29 | `POST http://localhost:8000/api/plan` | 202 | Request body 使用 `scenario=family`、`origin=当前位置`；Response body 为 `{"planId":"plan_79b2807ae0b3","status":"processing"}` |
| 30 | `GET http://localhost:8000/api/plan/plan_79b2807ae0b3/stream` | 200 | 初始规划流包含 `heartbeat`、`START -> INTENT`、`INTENT -> CLARIFY`、`clarification_request` |
| 32 | `POST http://localhost:8000/api/plan/plan_79b2807ae0b3/clarify` | 200 | Request body 为 `{"reply":"4-6小时"}`；Response body 为 `status=processing` |
| 33 | `GET http://localhost:8000/api/plan/plan_79b2807ae0b3/stream` | 200 | 后续规划流包含 `CLARIFY -> INTENT`、`SKELETON`、`RECALL`、`VALIDATE`、`PACK -> CONFIRM`、`plan_ready` |
| 35 | `POST http://localhost:8000/api/plan/plan_79b2807ae0b3/execute` | 200 | Request body 为 `{"confirmed":true}`；Response body 为 `{"planId":"plan_79b2807ae0b3","status":"executing","message":"开始执行，请关注右侧日志"}` |
| 36 | `GET http://localhost:8000/api/plan/plan_79b2807ae0b3/stream` | 200 | 执行流包含 `CONFIRM -> EXECUTE`、两条 `execute_result`、`reserve_table`、`send_message`、`MOCK-TBL-50306`、`MOCK-MSG-99469`、`EXECUTE -> DONE`、`done` |

Network 还包含 CORS preflight：

- `OPTIONS /api/plan [200]`
- `OPTIONS /api/plan/{planId}/clarify [200]`
- `OPTIONS /api/plan/{planId}/execute [200]`

## 截图

Chrome DevTools MCP full-page screenshot 已保存：

```text
docs/qa/INT-002-devtools-real.png
```

## 失败阈值核对

- family real mode 到达 `plan_ready`：PASS。
- 点击确认执行调用真实 `POST /api/plan/{planId}/execute`：PASS。
- 后端发送 `execute_result`：PASS。
- 后端发送 `done`：PASS。
- 前端消费真实执行 SSE 并更新 ExecutionTracker / DONE 状态：PASS。
- 禁止 snake_case 字段搜索：PASS。
- 独立 evaluator Chrome DevTools MCP 证据：PASS。

最终结论：PASS。
