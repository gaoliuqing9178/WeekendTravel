# INT-001 QA: Sprint 1 integration - one input to SSE state_change

## Evaluator

- Evaluator: Codex independent evaluator subagent
- Eval ID: INT-001-EVAL-CODEX-20260527T1915+0800
- Date: 2026-05-27
- Workdir: `H:\WeekendTravel`
- Scope: independent acceptance for `INT-001: Sprint 1 integration: one input to SSE state_change`
- Independence: this report is based on files read, commands run, and browser MCP evidence collected by this evaluator during this session. Generator self-check output was not used as final evidence.

## Verdict

PASS / 放行。

理由：real mode 前端可以提交一条 Demo 输入，后端返回非空 `planId` 与 `status=processing`，前端使用该 `planId` 建立 SSE stream，并在 LogPanel 中渲染真实后端 `heartbeat` 与 `state_change`，可见 `START -> INTENT`，且 network 证据显示 `state_change.data.planId` 与创建响应一致。

## Files Read

- `docs/contracts/INT-001-one-input-sse.md`
- `docs/api-contract.md`
- `docs/product-spec.md`
- `docs/architecture.md`
- `feature_list.json`
- `progress.md`
- `frontend/F1-handoff.md`
- `backend/HANDOFF.md`
- `frontend/src/api/client.ts`
- `frontend/src/api/types.ts`
- `frontend/src/composables/useSSE.ts`
- `frontend/src/stores/planner.ts`
- `frontend/src/components/InputPanel.vue`
- `frontend/src/components/LogPanel.vue`
- `frontend/src/App.vue`
- `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanCreateService.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanStreamService.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanRequest.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanResponse.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/HeartbeatEvent.java`
- `backend/src/main/java/com/weekendtravel/backend/plan/sse/StateChangeEvent.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerIntegrationTests.java`

## Command Evidence

### Backend Fast Verify

Command:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

Result:

- Exit code: 0
- `Verify passed.`
- Maven result: `BUILD SUCCESS`
- Test total: `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`
- INT-001 relevant coverage: `PlanControllerIntegrationTests.createdPlanCanOpenStreamAndReceiveStateChangeForSamePlanId` ran and passed. It creates a plan, opens `/api/plan/{planId}/stream`, and asserts `heartbeat`, `state_change`, same `planId`, `START`, and `INTENT`.
- Non-blocking runtime warnings: Mockito / ByteBuddy dynamic agent warnings under Java 25 were printed, but tests and verify passed.

### Frontend Fast Verify

Command:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

Result:

- Exit code: 0
- `Verify passed.`
- `pnpm verify:fixtures passed`
- Fixture parser output:
  - `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `docs/fixtures/sse-events.jsonl -> 23 JSONL events`
- `pnpm typecheck passed`

## Service Availability

Before browser verification, local listeners were confirmed:

- Port `8000`: `Listen`, owning process `38080`
- Port `5173`: `Listen`, owning process `23840`

## Playwright MCP Evidence

User path:

1. Opened `http://127.0.0.1:5173/`.
2. Confirmed page title `WeekendTravel`.
3. Confirmed visible real mode:
   - `输入需求 real`
   - Pinia state `Mode = real`
4. Kept the default family Demo text:
   `今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。`
5. Clicked `提交规划`.
6. Waited for `state_change` to be visible.

Observed after submit:

- URL: `http://127.0.0.1:5173/`
- `apiModeVisible: true`
- `planId: plan_5c63071dd52c`
- `Agent: INTENT`
- `hasHeartbeat: true`
- `hasStateChange: true`
- `hasStartIntent: true`
- Log sample:
  - `提交规划请求` / `家庭场景正在调用 POST /api/plan。`
  - `plan 已创建` / `planId=plan_5c63071dd52c，status=processing`
  - `heartbeat` / `planId=plan_5c63071dd52c`
  - `state_change` / `START -> INTENT` / `状态已进入 INTENT`

Screenshot evidence:

- Playwright MCP full-page screenshot was captured during this evaluation and referenced in the tool output.
- Note: no screenshot file is retained in the repo, to respect the instruction that only this QA markdown file may be written.

## Chrome DevTools MCP Evidence

Page opened:

- `http://localhost:5173/`
- Page title: `WeekendTravel`

Accessibility / snapshot evidence:

- Snapshot covered InputPanel:
  - heading `输入需求 real`
  - radio `家庭` checked
  - radio `朋友`
  - textbox `自然语言输入` with the default family Demo text
  - textbox `当前位置`
  - button `提交规划`
- Snapshot covered Pinia state:
  - `Mode = real`
  - before submit: `Plan ID = 未创建`, `Agent = START`, `SSE = idle`
  - after submit: `Plan ID = plan_0cfb9dd63714`, `Agent = INTENT`, `SSE = retrying`
- Snapshot covered LogPanel:
  - `log "实时日志面板" live="polite" relevant="additions text"`
  - after submit, visible entries included `heartbeat`, `state_change`, and heading `START -> INTENT`

Console evidence:

- `list_console_messages` with `error` and `warn`: `<no console messages found>`

Network evidence:

- `POST http://localhost:8000/api/plan [202]`
  - Request body:
    ```json
    {"text":"今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。","scenario":"family","origin":"当前位置"}
    ```
  - Response body:
    ```json
    {"planId":"plan_0cfb9dd63714","status":"processing"}
    ```
  - Response header included `access-control-allow-origin: http://localhost:5173`
- `GET http://localhost:8000/api/plan/plan_0cfb9dd63714/stream [200]`
  - Request header included `accept: text/event-stream`
  - Response header included `content-type: text/event-stream`
  - Response body:
    ```text
    event:heartbeat
    data:{"type":"heartbeat","planId":"plan_0cfb9dd63714","timestamp":1779880454848}

    event:state_change
    data:{"type":"state_change","planId":"plan_0cfb9dd63714","from":"START","to":"INTENT","timestamp":1779880454848}
    ```

Visual evidence:

- Chrome DevTools full-page screenshot was captured inline during this evaluation.
- The visible page showed `Mode real`, `Plan ID plan_0cfb9dd63714`, `Agent INTENT`, `SSE retrying`, and LogPanel entries for `heartbeat` plus `state_change / START -> INTENT`.

## Acceptance Mapping

- Frontend submits one demo input: PASS. Playwright and DevTools both submitted the default family Demo input through the real mode UI.
- Backend returns `planId`: PASS. Network showed `POST /api/plan [202]` with `{"planId":"plan_0cfb9dd63714","status":"processing"}`; Playwright observed `plan_5c63071dd52c`.
- Frontend uses returned `planId` to open SSE: PASS. DevTools network showed `/api/plan/plan_0cfb9dd63714/stream`; response body used the same `planId`.
- Frontend receives and renders `state_change`: PASS. Playwright and DevTools snapshots both showed `state_change` and `START -> INTENT` in LogPanel.
- `state_change.data.planId` matches create response: PASS. DevTools network response body used `plan_0cfb9dd63714`, matching the POST response.
- Real mode, not mock-only: PASS. UI showed `real`, network showed real backend POST and SSE requests.
- Console has no error/warn: PASS.
- Required dual browser MCP evidence: PASS. Playwright MCP and Chrome DevTools MCP were both used.

## Notes

- The current minimal backend stream completes after sending the required events, and browser EventSource then reconnects; the UI therefore shows `retrying` and repeated `heartbeat` / `state_change` entries. This matches the current minimal stream behavior documented in prior F1 handoff/progress and does not block INT-001, whose contract only requires proving one input creates a plan and receives/rendered `state_change START -> INTENT`.
- No implementation files, handoff files, `feature_list.json`, `progress.md`, or `docs/handoff.md` were modified by this evaluator.
