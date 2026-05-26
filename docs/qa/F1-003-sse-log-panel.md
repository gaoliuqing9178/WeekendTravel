# F1-003 useSSE composable and log panel QA

## 结论

- 验收结论：PASS，建议放行。
- evaluator 身份：独立 evaluator 子代理，仅做验证。
- 工作树约束：未修改实现源码，未修改 `feature_list.json`、`progress.md`、`frontend/F1-handoff.md`。
- QA 产物：
  - `docs/qa/F1-003-sse-log-panel.md`
  - `docs/qa/F1-003-playwright-log-panel.png`
  - `docs/qa/F1-003-devtools-log-panel.png`

## 独立读取文件

已读取并核对：

- `docs/contracts/F1-003-sse-log-panel.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `frontend/F1-handoff.md`
- `frontend/src/composables/useSSE.ts`
- `frontend/src/components/LogPanel.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/App.vue`

## 命令验证

### Frontend fast verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

关键输出：

```text
WeekendTravel verify
Target: frontend
Mode: fast

== Frontend verify (fast) ==
[run] pnpm verify:fixtures
[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture
[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture
[ok] docs/fixtures/sse-events.jsonl -> 23 JSONL events
[ok] pnpm verify:fixtures passed.
[run] pnpm typecheck
[ok] pnpm typecheck passed.

Verify passed.
```

### 禁止字段搜索

命令：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

结果：无输出，`rg` exit code 为 1，符合“无命中”预期。

### feature_list.json 解析

命令：

```powershell
Get-Content -Encoding UTF8 -LiteralPath feature_list.json | ConvertFrom-Json | Out-Null; Write-Output "feature_list.json parse OK"
```

关键输出：

```text
feature_list.json parse OK
```

## 静态契约核对

- `useSSE` 表达 `idle / connecting / open / retrying / closed / error` 连接状态。
- mock mode 通过 `client.getSseFixtureFrames()` 逐条回放 fixture，未访问后端网络。
- real mode 通过 `client.openPlanStream(planId)` 创建 `EventSource`，符合既有 API client 配置路径。
- `handleMessageEvent` 按 SSE event name 和 `payload.type` 校验；不一致进入错误路径。
- Pinia store 按事件更新：
  - `state_change` -> `agentState`
  - `plan_ready` / `adjust_result` -> `currentPlan`
  - `clarification_request` -> `pendingClarification`
  - `execute_result` -> action 状态与 `confirmationNo`
  - `done` -> `DONE`
  - `error(code=DEGRADE)` -> `DEGRADE`，其他 error -> `FAILED`
- `LogPanel` 使用 `role="log"`、`aria-live="polite"`、固定滚动容器，并在事件数量变化后滚动到底部。

## Playwright MCP 证据

验证路径：

1. 打开 `http://127.0.0.1:5173/`。
2. 点击「开始预演」。
3. 等待 mock fixture SSE 完成。
4. 检查 LogPanel 事件、末尾状态和自动滚动。

关键结果：

```json
{
  "hasLog": true,
  "role": "log",
  "ariaLive": "polite",
  "eventCount": 25,
  "includesDone": true,
  "includesError": true,
  "includesDegrade": true,
  "scrollTop": 2360,
  "clientHeight": 460,
  "scrollHeight": 2820,
  "atBottom": true,
  "lastItemText": "error19:20:14DEGRADE3 次重排后仍无可行方案，建议放宽距离限制或调整时间"
}
```

事件类型序列包含：

```text
client_event, client_event, heartbeat, state_change, tool_call, tool_result,
state_change, state_change, tool_call, tool_result, state_change, tool_call,
tool_result, replan, state_change, state_change, plan_ready, state_change,
clarification_request, adjust_result, state_change, execute_result,
execute_result, done, error
```

Playwright console 补充检查：

```text
Total messages: 2 (Errors: 0, Warnings: 0)
Returning 0 messages for level "warning"
```

截图：

- `docs/qa/F1-003-playwright-log-panel.png`

## Chrome DevTools MCP 证据

验证路径：

1. 打开 `http://127.0.0.1:5173/`。
2. 使用 snapshot / accessibility 检查 LogPanel。
3. 点击「开始预演」并等待 `done` / `DEGRADE` / `error`。
4. 检查 console 和 network。

Accessibility / snapshot 关键证据：

```text
log "实时日志面板" live="polite" relevant="additions text"
```

完成态 snapshot 关键内容：

```text
StaticText "DEGRADE"
StaticText "closed"
StaticText "plan_family_fixture"
StaticText "done"
heading "done" level="3"
StaticText "error"
heading "DEGRADE" level="3"
StaticText "3 次重排后仍无可行方案，建议放宽距离限制或调整时间"
```

Console：

```text
<no console messages found>
```

Network：

- 20 条请求，均为前端页面、Vite 模块、依赖模块、Vue SFC、API client/composable/store 模块和 fixture raw import。
- fixture raw import：
  - `@fs/H:/WeekendTravel/docs/fixtures/plan-ready-family.json?import&raw`
  - `@fs/H:/WeekendTravel/docs/fixtures/plan-ready-friends.json?import&raw`
  - `@fs/H:/WeekendTravel/docs/fixtures/sse-events.jsonl?import&raw`
- 未发现 `/api/plan/*/stream` 或其他真实后端 plan stream 请求。

截图：

- `docs/qa/F1-003-devtools-log-panel.png`

## 注意事项

- 本轮浏览器验证前，两套 MCP 的专用 Chrome profile 被残留进程占用；已仅关闭命令行中明确带有 `ms-playwright\mcp-chrome-*` 或 `chrome-devtools-mcp\chrome-profile` 的 MCP 专用 Chrome 进程后重试。
- 后续 generator 可基于本报告同步 `feature_list.json`、`progress.md` 和 `frontend/F1-handoff.md`，但本 evaluator 未修改这些文件。
