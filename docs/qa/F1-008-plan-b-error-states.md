# Evaluator Report: F1-008 Plan B highlight and error/degrade states

## Evaluator 子代理信息

- 子代理：独立 evaluator 子代理，非 generator。
- 测试日期：2026-05-30。
- Generator handoff：已独立读取 `frontend/F1-handoff.md`，并以本轮实际命令和 Chrome DevTools MCP 证据为准。
- 验收边界：只写入本报告与 `docs/qa/F1-008-devtools-mock.png`；未修改业务代码、`feature_list.json`、`progress.md` 或 `frontend/F1-handoff.md`。

## 测试目标

- 验证 F1-008 对 `replan` / Plan B 的可视化：LogPanel 高亮 `replan`，PlanCard 显示 `Plan B` 徽标和原因。
- 验证状态总览提供稳定入口：当前状态、SSE 状态、`DONE / DEGRADE / FAILED` 终态轨道、Plan B 原因和 DONE 摘要可见。
- 验证 mock mode 下不访问真实 `/api/plan/*` 后端接口。
- 验证 fixture、类型检查、禁止 snake_case 字段和 `feature_list.json` 可解析。

## 独立读取文件

- `docs/contracts/F1-008-plan-b-error-states.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `frontend/F1-handoff.md`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/components/StateSummaryPanel.vue`
- `frontend/src/components/PlanCard.vue`
- `frontend/src/components/LogPanel.vue`
- `frontend/scripts/verify-fixtures.mjs`
- `docs/fixtures/sse-events.jsonl`
- `docs/qa/evaluator-template.md`

## 环境和命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：PASS。

```text
[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture
[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture
[ok] docs/fixtures/sse-events.jsonl -> 28 JSONL events
[ok] pnpm verify:fixtures passed.
[ok] pnpm typecheck passed.
Verify passed.
```

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

结果：PASS。`rg` exit code 为 1，输出为空，符合“预期无命中”。

```powershell
Get-Content -LiteralPath 'feature_list.json' -Raw -Encoding UTF8 | ConvertFrom-Json | Out-Null
```

结果：PASS。`feature_list.json ConvertFrom-Json PASS`。

## 用户路径

1. 使用 Chrome DevTools MCP 打开 `http://127.0.0.1:5173/`。
2. 点击“提交规划”。
3. 等待 ClarifyBubble 出现，点击“回答：4-6小时”。
4. 等待进入 `CONFIRM`。
5. 验证 snapshot / accessibility 中可见 `F1-008`、`Plan B`、`Plan B 已启用`、Plan B 原因、`状态总览`、`DONE / DEGRADE / FAILED` 终态轨道，以及 LogPanel 的 `replan` / `Plan B #1`。
6. 点击“确认执行”，等待进入 `DONE`。
7. 验证 DONE 状态、DONE 摘要、done 日志、执行追踪 `3 / 3` 和 3 个确认号可见。
8. 检查 console error / warn 为 0。
9. 检查 network 没有真实 `/api/plan/*` 请求。
10. 保存截图到 `docs/qa/F1-008-devtools-mock.png`。

## 通过证据

- Chrome DevTools MCP 初始 snapshot 可见：
  - `F1-008`
  - `WeekendTravel`
  - `START / idle`
  - `状态总览`
  - 终态轨道 `DONE / DEGRADE / FAILED`
  - LogPanel ready 文案：`当前使用 mock fixture mode，不访问后端网络。`
- 点击“提交规划”后 snapshot 可见：
  - `CLARIFY`
  - ClarifyBubble：`需要确认`、`请问大概想玩几个小时？`、`durationHours`
  - 三个回答按钮：`回答：3-4小时`、`回答：4-6小时`、`回答：6小时以上`
- 点击“回答：4-6小时”后 snapshot 可见：
  - Agent 状态为 `CONFIRM`
  - PlanCard header：`方案卡 亲子室内活动、低负担晚餐和回家路线 Plan B CONFIRM`
  - Plan B alert：`Plan B 已启用`
  - Plan B 原因：`原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅`
  - 状态总览区域继续显示 `DONE / DEGRADE / FAILED`
  - LogPanel 中出现 `replan`、`Plan B #1` 和同一 Plan B 原因
- 点击“确认执行”并等待后 snapshot 可见：
  - Agent 状态为 `DONE`
  - 状态总览 DONE alert：`DONE`、`3 个动作已完成，1 条分享消息已生成`
  - 确认执行区域显示 `当前状态 DONE`
  - 执行追踪显示 `3 / 3 个动作完成 DONE`
  - 三个确认号可见：`MOCK-TBL-88421`、`MOCK-NOTE-122`、`MOCK-MSG-309`
  - LogPanel 中出现 `done` 和完成摘要

## DOM / accessibility 复核

Chrome DevTools MCP `evaluate_script` 复核结果：

```json
{
  "hasF1008": true,
  "hasPlanBBadge": true,
  "hasPlanBEnabled": true,
  "hasPlanBReason": true,
  "hasStateSummary": true,
  "terminalText": "DONE已完成DEGRADE已降级FAILED失败",
  "hasDoneStatus": true,
  "hasDoneLog": true,
  "replanLogCount": 1,
  "replanItems": [
    "replan19:20:01Plan B #1原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅"
  ],
  "planCardClass": "n-card plan-card is-plan-b",
  "realApiRequestCount": 0,
  "apiRequests": []
}
```

补充执行追踪复核：

```json
{
  "normalizedHas3of3": true,
  "completedLabels": 7,
  "confirmationsVisible": true,
  "doneAlert": [
    "Plan B 已启用原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅",
    "Plan B原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅",
    "DONE3 个动作已完成，1 条分享消息已生成",
    "3 个动作已完成，1 条分享消息已生成"
  ]
}
```

## Console / Network 证据

- Console：Chrome DevTools MCP `list_console_messages` 使用 `types=["error","warn"]`，结果为 `<no console messages found>`。
- Network：Chrome DevTools MCP `list_network_requests` 共 26 条 document / script / fetch / xhr 请求，均为 Vite 页面、模块和 fixture raw import：
  - `GET http://127.0.0.1:5173/`
  - `GET http://127.0.0.1:5173/@vite/client`
  - `GET http://127.0.0.1:5173/src/main.ts`
  - `GET http://127.0.0.1:5173/src/components/PlanCard.vue`
  - `GET http://127.0.0.1:5173/src/components/StateSummaryPanel.vue`
  - `GET http://127.0.0.1:5173/src/stores/planner.ts`
  - `GET http://127.0.0.1:5173/@fs/H:/WeekendTravel/docs/fixtures/plan-ready-family.json?import&raw`
  - `GET http://127.0.0.1:5173/@fs/H:/WeekendTravel/docs/fixtures/plan-ready-friends.json?import&raw`
  - `GET http://127.0.0.1:5173/@fs/H:/WeekendTravel/docs/fixtures/sse-events.jsonl?import&raw`
- 未出现真实 `/api/plan/*` 请求。DOM 复核中 `realApiRequestCount=0`。

## Error / DEGRADE / FAILED 覆盖说明

- `frontend/scripts/verify-fixtures.mjs` 要求 `error` 事件存在，并要求 `error(code=DEGRADE)` 可解析；本轮 frontend fast verify 已通过。
- `docs/fixtures/sse-events.jsonl` 包含：
  - `{"type":"error","code":"DEGRADE","message":"3 次重排后仍无可行方案，建议放宽距离限制或调整时间"}`
- `frontend/src/stores/planner.ts` 中 `error` 分支按 `payload.code === "DEGRADE" ? "DEGRADE" : "FAILED"` 映射，并保存 `payload.message`。
- `frontend/src/components/StateSummaryPanel.vue` 中 `DEGRADE` 和 `FAILED` 均有 `terminalNotice` 文案入口，且终态轨道始终展示 `DONE / DEGRADE / FAILED`。
- 本轮指定浏览器路径以 mock 正常执行流结束于 `DONE`，未额外注入非用户路径的 error 事件；DEGRADE / FAILED 的浏览器可见入口由终态轨道和源码映射证据覆盖。

## 失败复现

无。

## 截图 / 日志 / API 输出位置

- 截图：`docs/qa/F1-008-devtools-mock.png`
- 截图文件状态：已保存，大小 330298 bytes。
- 日志：本报告内记录命令输出、Chrome DevTools MCP snapshot / DOM / console / network 证据。
- API 输出：不适用。本轮为 mock mode，未访问真实 `/api/plan/*`。
- 浏览器诊断：Chrome DevTools MCP，console error / warn 为 0，network 无真实 `/api/plan/*`。

## 前端浏览器 MCP 证据

- Chrome DevTools MCP：已覆盖打开页面、点击“提交规划”、回答 ClarifyBubble、等待 `CONFIRM`、检查 Plan B / 状态总览 / LogPanel、点击“确认执行”、等待 `DONE`、检查执行追踪 `3 / 3`、console、network、DOM / accessibility 和截图。
- Playwright MCP：未使用，也不是本轮硬性要求。

## 放行结论

- 结论：PASS，放行。
- 理由：指定命令全部通过；禁止 snake_case 字段无命中；`feature_list.json` 可解析；Chrome DevTools MCP 证据覆盖 F1-008、Plan B 双位置可见、状态总览终态轨道、DONE 执行完成、LogPanel `replan / Plan B #1`、console 无 error/warn、mock mode 无真实 `/api/plan/*` 请求。
- 后续建议：后续 integration 任务如需要真实后端异常链路，可再补 real mode 下 `error(code=DEGRADE)` 和非 DEGRADE error 的浏览器网络证据。

## 独立性声明

- 本报告由 generator 之外的独立 evaluator 子代理执行测试后填写。
- Generator 自己运行的命令只作为参考，不作为本报告的放行依据。
- 本轮未修改业务代码、`feature_list.json`、`progress.md` 或 `frontend/F1-handoff.md`。
