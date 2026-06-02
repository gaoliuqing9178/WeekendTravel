# F1-009 Map-centric UI redesign QA

Date: 2026-06-02
Role: Independent F1 evaluator
Verdict: PASS

## Scope

本轮按 `docs/contracts/F1-009-map-centric-ui-redesign.md` 验收当前工作树的 F1-009 UI 重设计。验收只读生产代码和契约，只写入本 QA 报告与 Chrome DevTools MCP 截图证据。

## Files Reviewed

- `docs/contracts/F1-009-map-centric-ui-redesign.md`
- `docs/NewUi.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `frontend/F1-handoff.md`
- `frontend/src/App.vue`
- `frontend/src/components/MapStage.vue`
- `frontend/src/components/PlanCard.vue`
- `frontend/src/components/LogPanel.vue`
- `frontend/src/components/InputPanel.vue`
- `frontend/src/components/ConfirmButton.vue`
- `frontend/src/components/ExecutionTracker.vue`
- `frontend/src/styles/main.css`

## Command Verification

### `.\verify.ps1 -Target frontend -Mode fast`

Result: PASS

Summary:

- `pnpm verify:fixtures` passed.
- `docs/fixtures/plan-ready-family.json -> plan_family_fixture`
- `docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
- `docs/fixtures/sse-events.jsonl -> 28 JSONL events`
- `pnpm typecheck` passed.
- Final output: `Verify passed.`

### `npm run build` in `frontend/`

Result: PASS

Summary:

- `vue-tsc --noEmit` passed.
- `vite build` passed.
- Built assets:
  - `dist/index.html` 0.60 kB, gzip 0.36 kB
  - `dist/assets/index-DiJ8ZzjM.css` 20.00 kB, gzip 5.26 kB
  - `dist/assets/index-B4e_68K0.js` 376.87 kB, gzip 117.02 kB

### Forbidden snake_case field scan

Command:

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

Result: PASS

Summary:

- Exit code was `1` with no output, which means no matches were found.
- No forbidden snake_case compatibility fields were detected in the checked frontend source, frontend scripts, or fixtures.

### `feature_list.json` parse check

Result: PASS

Summary:

- PowerShell pipeline attempts were interrupted by sandbox spawn refresh, so I reran the same JSON parse check with an approved read-only parser command:

```powershell
py -c "import json; json.load(open('feature_list.json', encoding='utf-8')); print('feature_list.json parse passed')"
```

- Output: `feature_list.json parse passed`

## Chrome DevTools MCP Browser Verification

URL: `http://127.0.0.1:5173/`

Initial page:

- Accessibility snapshot showed `region "今天下午怎么走"` as the map stage.
- Accessibility snapshot showed `region "规划抽屉"` as the planner drawer.
- Initial page was not blank and was not the old developer two-column workbench.
- Initial skeleton timeline showed `活动待定` / `晚餐待定` / `收尾待定`.
- Initial LogPanel kept `role=log` and `aria-live=polite`.

Mock flow:

1. Opened `http://127.0.0.1:5173/`.
2. Clicked `生成路线方案`.
3. Page entered `CLARIFY`.
4. Confirmed one ClarifyBubble region, question `请问大概想玩几个小时？`, and three options:
   - `3-4小时`
   - `4-6小时`
   - `6小时以上`
5. Clicked `4-6小时`.
6. Page reached `CONFIRM`.
7. Confirmed visible map stage with user-facing phase `方案出炉`, route pins, and map Plan B replacement card.
8. Confirmed visible planner drawer with:
   - `Plan B`
   - Plan B reason
   - horizontal itinerary cards for `奇妙亲子乐园` and `轻食家庭餐厅`
   - plan metrics
   - execution package
   - share message
9. Confirmed `Agent 思考进程` was visible and still exposed a `role=log` log region.
10. Confirmed terminal state rail remained visible through `状态总览` with `DONE / DEGRADE / FAILED`.
11. Clicked `确认全部并生成行程单`.
12. Page entered `EXECUTE`, then reached `DONE`.
13. Confirmed execution completion:
   - `执行追踪 3 / 3 个动作完成 DONE`
   - `确认号：MOCK-TBL-88421`
   - `确认号：MOCK-NOTE-122`
   - `确认号：MOCK-MSG-309`
   - done summary `3 个动作已完成，1 条分享消息已生成`

Diagnostics:

- Console `error` / `warn`: PASS, no messages found.
- Network fetch/xhr: PASS, empty list in mock mode.
- Real `/api/plan/*` access in mock mode: PASS, no fetch/xhr request to real plan endpoints observed.

## Mobile Viewport Check

Chrome DevTools resize request: `390x844`

Observed viewport after resize: `500x844` because the page keeps a 320px minimum page width and DevTools reported an effective wider viewport.

DOM measurements after resetting page scroll and drawer content scroll:

```json
{
  "viewport": { "width": 500, "height": 844 },
  "scrollWidth": 485,
  "clientWidth": 485,
  "horizontalOverflow": false,
  "drawer": { "top": 236, "bottom": 844, "height": 608, "visible": true },
  "inputPanel": { "top": 385, "bottom": 912, "visible": true },
  "textInput": { "top": 481, "bottom": 513, "visible": true },
  "submit": { "top": 797, "bottom": 831, "visible": true },
  "drawerContentScrollTop": 0
}
```

Result: PASS

- Bottom drawer is visible.
- Input panel, text input, and submit button are reachable in the bottom drawer.
- No horizontal page overflow was detected.
- Drawer content is scrollable; the full input panel may extend below the first mobile viewport, but the text input and submit control are visible and not blocked after the drawer is at top.

## Evidence

- Main Chrome DevTools MCP screenshot: `docs/qa/F1-009-devtools-mock.png`
- Mobile supplementary Chrome DevTools MCP screenshot: `docs/qa/F1-009-devtools-mobile.png`
- QA report: `docs/qa/F1-009-map-centric-ui-redesign.md`

## Notes

- I did not modify production code.
- I did not modify `feature_list.json`, `progress.md`, or `frontend/F1-handoff.md`.
- I did not update F1-009 status metadata; this report is the evaluator evidence generator can use for the truth-chain update.
