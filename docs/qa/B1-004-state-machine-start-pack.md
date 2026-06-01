# B1-004 QA: State machine START to PACK

## Verdict

PASS.

B1-004 的当前后端实现满足合同 `docs/contracts/B1-004-state-machine-start-pack.md`。独立 evaluator Volta (`019e837b-353a-7990-ae11-a71a58741421`) 做了只读复核、后端 fast verify 和真实 HTTP 抽样，确认 `POST /api/plan` 返回的 `planId` 可以继续用于 `GET /api/plan/{planId}/stream`，family happy path 可观察到 `START / INTENT / SKELETON / RECALL / VALIDATE / PACK`，并发送 `heartbeat`、`state_change`、`tool_call`、`tool_result`、`plan_ready`。

当前实现随后进入 `CONFIRM`，这是 B1-006 / INT-002 等后续确认链路扩展，不阻塞 B1-004 的 START 到 PACK 验收。

## Commands

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
  - Result: PASS. Backend tests: 48 run, 0 failures, 0 errors. Maven `BUILD SUCCESS`; harness output `Verify passed.`
- Real HTTP sample against temporary backend on `127.0.0.1:8000`:
  - `POST http://127.0.0.1:8000/api/plan`
  - `GET http://127.0.0.1:8000/api/plan/{planId}/stream`
  - Result: PASS. Sample `planId=plan_c7092ab341f0`; stream returned 200 and completed successfully.
- `feature_list.json` metadata parse:
  - Result before this generator close-out: `B1-004.status=todo`; `B1-004.evidence=[]`.

## Runtime Evidence

Sample stream event names:

```text
heartbeat
state_change
state_change
state_change
state_change
tool_call
tool_result
tool_call
tool_result
tool_call
tool_result
tool_call
tool_result
tool_call
tool_result
state_change
state_change
plan_ready
```

Observed transitions:

```text
START -> INTENT
INTENT -> SKELETON
SKELETON -> RECALL
RECALL -> VALIDATE
VALIDATE -> PACK
PACK -> CONFIRM
```

Observed states:

```text
START
INTENT
SKELETON
RECALL
VALIDATE
PACK
CONFIRM
```

`tool_result` latency evidence:

- `searchLocalPlaces`: `latencyMs=1`
- `searchLocalPlaces`: `latencyMs=0`
- `calculateRouteTime`: `latencyMs=1`
- `checkAvailability`: `latencyMs=0`
- `checkAvailability`: `latencyMs=0`

`plan_ready.plan` sample:

- `planId=plan_c7092ab341f0`
- `scenario=family`
- `status=CONFIRM`
- `summary=亲子活动搭配家庭友好晚餐`
- `timelineCount=2`
- `actionsCount=2`
- `replanCount=0`
- keys: `planId`, `scenario`, `status`, `isPlanB`, `planBReason`, `summary`, `timeline`, `actions`, `shareMessage`, `totalDurationHours`, `replanCount`, `createdAt`

All observed SSE event names matched `payload.type`.

## Code Evidence

- `PlanController.create` exposes `POST /api/plan`; `PlanController.stream` exposes `GET /api/plan/{planId}/stream`.
- `PlanCreateService.createPlan` generates `planId`, stores the request in `PlanStateMachineService`, and returns `status=processing`.
- `PlanStreamService.openStream` validates the `planId`, sends `heartbeat`, then calls `PlanStateMachineService.streamPlan`.
- `PlanStateMachineService.continuePlanning` advances `START -> INTENT -> SKELETON -> RECALL -> VALIDATE`.
- `PlanStateMachineService.validateAndPack` advances `VALIDATE -> PACK`, builds the plan payload, then emits `plan_ready`. The later `PACK -> CONFIRM` transition is an already-integrated downstream extension.
- `PlanStateMachineService.transition` emits every transition as `state_change`.
- `callSearch`, `callRoute`, and `callAvailability` emit paired `tool_call` / `tool_result` events and preserve `latencyMs`.
- `SearchTool.score` implements `0.4 * relevance + 0.3 * distance + 0.2 * rating + 0.1 * availability`.
- `PlanControllerStreamTests.streamReturnsEventStreamWithStateMachineEvents` covers `heartbeat`, `state_change`, `tool_call`, `tool_result`, `plan_ready`, and START-to-PACK visibility.
- `PlanFlowIntegrationTests.createThenStreamReachesPlanReady` covers POST-to-stream chaining with the same `planId`, `plan_ready`, `PACK`, `CONFIRM`, `isPlanB=false`, and `replanCount=0`.

## Metadata Finding

Before this close-out pass, `feature_list.json` had not been closed out for B1-004:

- `status`: `todo`
- `evidence`: `[]`

This report is the independent evaluator evidence used by the generator to close B1-004 as `verified`.
