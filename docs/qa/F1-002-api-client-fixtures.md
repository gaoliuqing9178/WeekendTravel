# Evaluator Report: F1-002 API client and mock fixture mode

## Evaluator 子代理信息

- 子代理：WeekendTravel F1-002 独立 evaluator 子代理（本轮只做验证，未修改实现代码）
- 测试日期：2026-05-22（Asia/Shanghai）
- Generator handoff：`docs/contracts/F1-002-api-client-fixtures.md`、`frontend/F1-handoff.md`

## 测试目标

- 验证前端 API client 是否按 `docs/api-contract.md` 的 camelCase 线缆字段实现。
- 验证 mock fixture mode 是否能解析 `docs/fixtures/plan-ready-family.json`、`docs/fixtures/plan-ready-friends.json`、`docs/fixtures/sse-events.jsonl`。
- 验证 `.\verify.ps1 -Target frontend -Mode fast` 是否覆盖 fixture 解析并通过 `pnpm typecheck`。
- 验证前端页面点击“开始预演”后能进入 fixture-loaded/`CONFIRM` 等价状态，并展示 Plan B 与 summary。
- 验证 `frontend/src`、`frontend/scripts`、`docs/fixtures` 未新增禁止的 snake_case 字段名。

## 环境和命令

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend/src frontend/scripts docs/fixtures
rg -n "reserve_table|add_note|send_message" frontend/src frontend/scripts docs/fixtures
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "Start-Process -FilePath 'C:\nvm4w\nodejs\npm.cmd' -ArgumentList @('run','dev','--','--port','5173') -WorkingDirectory 'H:\WeekendTravel\frontend' -WindowStyle Hidden -PassThru"
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173' -TimeoutSec 5
```

## 用户路径

1. 打开 `http://127.0.0.1:5173`。
2. 使用默认家庭场景与默认输入，点击“开始预演”。
3. 等待页面进入 `CONFIRM` / `closed` 状态，确认 Plan ID、Plan B、summary 和 fixture 日志可见。

## 通过证据

- 已按要求读取并核对：`docs/contracts/F1-002-api-client-fixtures.md`、`docs/api-contract.md`、`frontend/src/api/*`、`frontend/scripts/verify-fixtures.mjs`、`verify.ps1`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast` 通过。
- verify 输出明确覆盖：
  - `[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture`
  - `[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture`
  - `[ok] docs/fixtures/sse-events.jsonl -> 23 JSONL events`
  - `[ok] pnpm verify:fixtures passed.`
  - `[ok] pnpm typecheck passed.`
  - `Verify passed.`
- 禁止字段名搜索 `plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no` 在 `frontend/src`、`frontend/scripts`、`docs/fixtures` 中无命中。
- `reserve_table`、`add_note`、`send_message` 仅作为 `docs/api-contract.md` 允许的 `ActionType` 枚举值出现，不按字段名违规处理。
- Chrome DevTools network 显示三份 fixture 均通过 Vite raw import 加载：
  - `GET /@fs/H:/WeekendTravel/docs/fixtures/plan-ready-family.json?import&raw [200]`
  - `GET /@fs/H:/WeekendTravel/docs/fixtures/plan-ready-friends.json?import&raw [200]`
  - `GET /@fs/H:/WeekendTravel/docs/fixtures/sse-events.jsonl?import&raw [200]`

## 失败复现

无。

## 截图 / 日志 / API 输出位置

- 截图：`docs/qa/F1-002-devtools-confirm.png`
- 截图：`docs/qa/F1-002-devtools-confirm-root.png`
- Playwright MCP 截图 artifact：`docs/qa/F1-002-playwright-confirm.png`
- 日志：本报告记录 `verify.ps1`、Playwright MCP、Chrome DevTools MCP 的关键输出摘要。
- API 输出：本轮为 mock fixture mode，无真实后端 API 调用；Chrome DevTools network 仅观察到前端模块和 fixture raw import 请求。
- Playwright trace：未生成 trace；本轮使用 Playwright 状态等待、交互快照和截图作为证据。

## 前端浏览器 MCP 证据

- Playwright MCP：打开 `http://127.0.0.1:5173`，页面 title 为 `WeekendTravel`；点击“开始预演”；等待 `CONFIRM` 可见；页面快照显示 `CONFIRM`、`closed`、`plan_family_fixture`、`Plan B：原餐厅排队预计 70 分钟，已切换到可订位低卡餐厅`、`亲子室内活动、低负担晚餐和回家路线`；console 检查结果为 Errors 0、Warnings 0。
- Chrome DevTools MCP：页面 snapshot 初始显示 `START` / `idle`；点击“开始预演”后 snapshot 显示 `CONFIRM` / `closed`、`plan_family_fixture`、Plan B alert、fixture 日志；console error/warn 列表为空；network 显示 `client.ts`、`fixtures.ts` 和三份 fixture raw import 均为 200；DOM evaluate 返回 `hasConfirm=true`、`hasClosed=true`、`hasPlanB=true`、`hasSummary=true`、`hasFixturePlanId=true`。

## 放行结论

- 结论：放行
- 理由：F1-002 的 API client、mock fixture parser、fast verify、camelCase 字段约束和基础 fixture UI 路径均通过独立验证；未发现阻塞问题。
- 后续建议：由 generator 或收尾代理按项目流程同步 `feature_list.json`、`progress.md` 和 `frontend/F1-handoff.md` 的 F1-002 verified 证据；本 evaluator 未修改这些状态文件。

## 独立性声明

- 本报告由 generator 之外的 evaluator 子代理执行测试后填写。
- Generator 自己运行的命令只作为参考，不作为本报告的放行依据。
