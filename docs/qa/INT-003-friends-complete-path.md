# QA: INT-003 Friends scenario complete path

## Evaluator

- Evaluator: Codex independent evaluator subagent
- Evaluator ID: `INT-003-EVAL-CODEX-20260601T2235+0800`
- Date: 2026-06-01
- Scope: `docs/contracts/INT-003-friends-complete-path.md`
- Mode: real frontend API mode, `VITE_API_MODE=real`, `VITE_API_BASE_URL=http://localhost:8000`
- Verdict: PASS

## Commands

- `.\verify.ps1 -Target backend -Mode fast`
  - Result: PASS
  - Evidence: Maven test suite completed with `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, and `Verify passed.`
- `.\verify.ps1 -Target frontend -Mode fast`
  - Result: PASS
  - Evidence: `pnpm verify:fixtures` passed, `docs/fixtures/sse-events.jsonl -> 28 JSONL events`, `pnpm typecheck` passed, and `Verify passed.`

## Chrome DevTools MCP Path

- Opened `http://127.0.0.1:5173/`.
- Confirmed page was in real mode: visible `Mode` value was `real`.
- Selected the `朋友` scenario.
- Submitted the friends demo input:
  `今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。`
- Page reached `plan_ready` / `CONFIRM` for `plan_5ca109e3a00c`.
- Visible plan evidence:
  - Scenario: `朋友`
  - Summary: `朋友活动、咖啡/甜品和轻松聚餐`
  - Timeline contained `activity` / `城市影像展`
  - Timeline contained `cafe` / `街角咖啡`
  - Timeline contained `restaurant` / `暮色烤肉`
  - Action package contained `reserve_table` and `send_message`
  - DOM check found no family-only phrases: no `适合 5 岁儿童`, no `亲子`, no `老婆孩子`
- Clicked `确认执行`.
- Page reached final `DONE`.
- Final visible execution evidence:
  - `执行追踪 2 / 2 个动作完成`
  - `reserve_table` success with `MOCK-TBL-94672`
  - `send_message` success with `MOCK-MSG-43835`
  - LogPanel contained `execute_result` and `done`
  - State summary showed `DONE`

Screenshot:

- `docs/qa/INT-003-devtools-real.png`

## Network Evidence

Chrome DevTools Network captured real backend requests:

- `POST http://localhost:8000/api/plan` -> 202
  - Request body: `{"text":"今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。","scenario":"friends","origin":"当前位置"}`
  - Response body: `{"planId":"plan_5ca109e3a00c","status":"processing"}`
- `GET http://localhost:8000/api/plan/plan_5ca109e3a00c/stream` -> 200
  - Response headers included `content-type:text/event-stream`.
  - Planning stream included `heartbeat`, `state_change`, `tool_call`, `tool_result`, and `plan_ready`.
  - `plan_ready.plan.scenario` was `friends`.
  - `plan_ready.plan.status` was `CONFIRM`.
  - `plan_ready.plan.timeline` included `activity`, `cafe`, and `restaurant`.
  - `plan_ready.plan.actions` included `reserve_table` and `send_message`.
  - Saved raw response evidence: `docs/qa/INT-003-planning-stream.network-response`
- `POST http://localhost:8000/api/plan/plan_5ca109e3a00c/execute` -> 200
  - Request body: `{"confirmed":true}`
  - Response body: `{"planId":"plan_5ca109e3a00c","status":"executing","message":"开始执行，请关注右侧日志"}`
- `GET http://localhost:8000/api/plan/plan_5ca109e3a00c/stream` -> 200
  - Response headers included `content-type:text/event-stream`.
  - Execution stream included `CONFIRM -> EXECUTE`, two `execute_result` events, `EXECUTE -> DONE`, and `done`.
  - `execute_result` covered `reserve_table` with `MOCK-TBL-94672`.
  - `execute_result` covered `send_message` with `MOCK-MSG-43835`.
  - Saved raw response evidence: `docs/qa/INT-003-execution-stream.network-response`

## Console

- Chrome DevTools console error / warn check: PASS
- Result: no console messages found.

## Conclusion

PASS. INT-003 friends real mode complete path met the contract acceptance criteria in the current working tree:

- Backend and frontend fast verify both passed.
- The browser used real mode against `localhost:8000`, not mock fixtures.
- Friends scenario submitted `scenario:"friends"` and reached `plan_ready` / `CONFIRM`.
- The plan contained activity, cafe, and restaurant timeline slots and did not show family-only copy.
- Confirm execution called the real execute endpoint.
- The execution SSE stream emitted `execute_result` and `done`.
- The frontend consumed the execution stream and reached visible `DONE` with mock confirmation numbers.
