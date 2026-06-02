# F1-009 Real Backend Smoke

Date: 2026-06-02
Role: F1 generator supplemental verification
Verdict: PASS

## Scope

This is a supplemental real-mode browser smoke after the F1-009 map-centric UI redesign. It verifies that the redesigned UI still works with the real backend API/SSE happy path. It is not a replacement for the independent F1-009 evaluator report in `docs/qa/F1-009-map-centric-ui-redesign.md`.

## Runtime

- Backend started with Spring Boot on `http://127.0.0.1:8000`.
- Backend health check passed: `GET http://127.0.0.1:8000/health` returned `{"status":"ok","service":"WeekendTravel",...}`.
- Frontend restarted on `http://127.0.0.1:5173` with `VITE_API_MODE=real`.
- Initial page snapshot showed mode `real`.

## Chrome DevTools MCP Flow

1. Opened `http://127.0.0.1:5173/`.
2. Confirmed the redesigned F1-009 UI rendered:
   - Map stage `今天下午怎么走`.
   - Planner drawer `规划抽屉`.
   - Input panel heading `告诉我怎么安排 real`.
   - Agent thought panel indicating real API mode.
3. Clicked `生成路线方案`.
4. Real backend returned `planId=plan_13b98173cf56`.
5. Page entered `CLARIFY` with question `请问大概想玩几个小时？`.
6. Clicked `4-6小时`.
7. Page reached `CONFIRM`.
8. Confirmed real backend plan rendered in redesigned UI:
   - Summary: `亲子活动搭配家庭友好晚餐`.
   - Timeline cards: `小小科学工坊`, `四季家庭小厨`.
   - Execution package: `预约 16:30 的家庭座位`, `生成家庭出行消息`.
   - State summary: `CONFIRM`.
   - AdjustPanel visible in `CONFIRM`.
9. Clicked `确认全部并生成行程单`.
10. Page reached `DONE`.
11. Confirmed execution results:
    - ExecutionTracker: `2 / 2 个动作完成 DONE`.
    - Confirmation numbers: `MOCK-TBL-63910`, `MOCK-MSG-13073`.
    - Done summary: `2 个动作已完成，行程执行包已生成`.

## Browser Diagnostics

- Console error / warn: PASS, no messages found.
- Network requests included:
  - `POST http://localhost:8000/api/plan [202]`.
  - `GET http://localhost:8000/api/plan/plan_13b98173cf56/stream [200]`.
  - `POST http://localhost:8000/api/plan/plan_13b98173cf56/clarify [200]`.
  - `GET http://localhost:8000/api/plan/plan_13b98173cf56/stream [200]`.
  - `POST http://localhost:8000/api/plan/plan_13b98173cf56/execute [200]`.
  - `GET http://localhost:8000/api/plan/plan_13b98173cf56/stream [200]`.
- SSE response headers used `content-type:text/event-stream`.
- CORS allowed `access-control-allow-origin:http://127.0.0.1:5173`.

## Evidence

- Screenshot: `docs/qa/F1-009-devtools-real.png`
- Initial planning SSE response: `docs/qa/F1-009-real-planning-initial.network-response`
- Post-clarify planning SSE response: `docs/qa/F1-009-real-planning-after-clarify.network-response`
- Execution SSE response: `docs/qa/F1-009-real-execution.network-response`

## Notes

- This test did not change frontend or backend production code.
- This test did not modify `docs/api-contract.md`.
- The formal F1-009 independent evaluator evidence remains `docs/qa/F1-009-map-centric-ui-redesign.md`.
