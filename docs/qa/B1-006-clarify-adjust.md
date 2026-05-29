# B1-006 QA: CLARIFY and ADJUST

## 结论

开发验证 PASS；独立 evaluator 验收 PASS。

本文件同时记录两类证据：

- generator 开发阶段验证结果
- 独立 evaluator 子代理的只读 / 运行态验收结果

是否可以将条目标记为 `verified`，以独立 evaluator 结论为准，而不是以 generator 自测为准。

## 验收对象

- Contract: `docs/contracts/B1-006-clarify-adjust.md`
- API contract: `docs/api-contract.md`
- 相关代码：
  - `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
  - `backend/src/main/java/com/weekendtravel/backend/api/ApiExceptionHandler.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanState.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanContext.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PendingClarification.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanSelectionSnapshot.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/PlanStateMachineService.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/sse/ClarificationRequestEvent.java`
  - `backend/src/main/java/com/weekendtravel/backend/plan/sse/AdjustResultEvent.java`
  - `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`
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
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Backend fast verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./verify.ps1 -Target backend -Mode fast
```

结果：PASS。

关键输出：

```text
[ok] mvnw.cmd test passed.
Verify passed.
```

## 关键行为证据

### happy path

- `PlanFlowIntegrationTests.createThenStreamReachesPlanReady` 断言：
  - stream 包含 `plan_ready`
  - 状态可达 `PACK` 与 `CONFIRM`
  - `isPlanB=false`
  - `replanCount=0`
- 运行态 create + stream 观察到：
  - `POST /api/plan` 返回 `202` + 非空 `planId`
  - SSE 出现 `PACK -> CONFIRM`
  - `plan_ready.plan.status = CONFIRM`

### ambiguous input -> CLARIFY -> clarify -> resume

- `PlanFlowIntegrationTests.ambiguousInputTriggersClarifyThenClarifyReplyResumesPlan` 断言：
  - 首次 stream 含 `CLARIFY`
  - 含 `clarification_request`
  - 初次 stream 不含 `plan_ready`
  - `POST /clarify` 返回 `status=processing`
  - 再次 stream 含 `CLARIFY -> INTENT`
  - 最终恢复到 `plan_ready` / `CONFIRM`
- 运行态观察到 `clarification_request` payload：

```json
{
  "type": "clarification_request",
  "planId": "plan_xxx",
  "question": "请问大概想玩几个小时？",
  "field": "durationHours",
  "options": ["3-4小时", "4-6小时", "6小时以上"]
}
```

### adjust path

- `PlanFlowIntegrationTests.adjustRequestEmitsAdjustResultAndKeepsPlanInConfirm` 断言：
  - 初始 stream 含 `plan_ready` 与 `CONFIRM`
  - `PATCH /adjust` 返回 `202` + `status=adjusting`
  - 二次 stream 含 `ADJUST`
  - 含 `adjust_result`
  - `affectedSlots=["restaurant"]`
  - 最终回到 `CONFIRM`
- 运行态 fresh 实例 `8002` 观察到：
  - `CONFIRM -> ADJUST -> VALIDATE`
  - `adjust_result.summary = 已将餐厅换为 轻食家庭餐厅，其余安排保持不变`
  - `adjust_result.plan.timeline[1].title = 轻食家庭餐厅`
  - `summary` 与最终 plan 一致

### adjust limit

- `PlanControllerCreateTests.adjustRejectsFourthAttempt` 断言：
  - 同一 plan 前 3 次 adjust 返回 `202`
  - 第 4 次 adjust 返回 `429`
  - 错误码为 `ADJUST_LIMIT_EXCEEDED`
  - message 为 `已达最大微调次数，请直接确认或重新发起规划`

## 独立 evaluator 验收补充

- Independent evaluator verdict: `PASS`
- Independent evaluator commands and runtime evidence:
  - `D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test`
  - `cd "D:/Users/lenovo/Desktop/WeekendTravel" && powershell.exe -NoProfile -ExecutionPolicy Bypass -File "D:/Users/lenovo/Desktop/WeekendTravel/verify.ps1" -Target backend -Mode fast`
  - `http://127.0.0.1:8002/health`
  - create + stream happy path on `8002`
  - create + stream ambiguous input on `8002`
  - `POST /clarify` on `8002`
  - `PATCH /adjust` + stream on `8002`
- Independent evaluator confirms:
  - parent-tree backend tests and fast verify both pass
  - ambiguous input enters `CLARIFY` and emits one `clarification_request` with 3 options
  - `POST /clarify` returns `processing` and flow resumes to `plan_ready` / `CONFIRM`
  - `PATCH /adjust` emits `adjust_result` with `affectedSlots` and latest full `plan`
  - `adjust_result.summary` is readable and consistent with final `plan.timeline`
  - event naming and payload `type` are consistent; fields remain camelCase
- 最终 feature 状态应以这部分独立 evaluator 结果作为放行依据。

## 实现核对

- `PlanState` 已补齐 `CLARIFY`、`CONFIRM`、`ADJUST` 等状态。
- `PlanContext` 已补齐：
  - `currentState`
  - `clarifyCount`
  - `adjustCount`
  - `pendingClarification`
  - `selectionSnapshot`
  - `pendingAdjustInstruction`
- `PlanController` 已新增：
  - `POST /api/plan/{planId}/clarify`
  - `PATCH /api/plan/{planId}/adjust`
- `ApiExceptionHandler` 已支持：
  - `409 INVALID_STATE`
  - `429 ADJUST_LIMIT_EXCEEDED`
- `PlanStateMachineService` 已实现：
  - low-confidence -> `CLARIFY`
  - `clarification_request` 事件发送
  - `POST /clarify` 后恢复推进
  - `CONFIRM -> ADJUST -> VALIDATE`
  - `adjust_result` 事件发送
  - adjust limit 3 次
  - `summary` 与最终 plan 对齐
- `tool_result` 仍保留 `latencyMs` 字段。
- SSE 事件名保持与 payload `type` 一致。
- 未引入 snake_case 线缆字段。

## 残留说明

- 当前 clarify 启发式只覆盖 duration 不明确这一类低置信度输入，并非通用意图澄清器。
- 当前 adjust 仍是 deterministic 局部微调，主要覆盖 activity / restaurant 槽位，不是通用自由文本重规划。
- 前端 ClarifyBubble / AdjustPanel UI 仍留给后续 F1 任务；本轮只完成后端正式链路和契约落地。
