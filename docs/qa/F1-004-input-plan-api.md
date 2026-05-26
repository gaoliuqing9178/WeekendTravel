# F1-004 InputPanel and POST /api/plan QA

## 结论

PASS。

独立 evaluator 已验收 F1-004：`InputPanel` 可编辑自然语言输入、场景与出发位置；mock mode 不访问真实后端并回放 fixture SSE；real mode 会调用 `POST http://localhost:8000/api/plan`，读取 camelCase `planId` / `status`，并将 `planId` 交给既有 SSE 链路。补充 CORS 修复也已纳入验收，`http://localhost:5173` 与 `http://127.0.0.1:5173` 均能完成浏览器 POST 202。

验证时间：2026-05-26 23:27:50 +08:00

## 验收范围

已读取并核对：

- `AGENTS.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `frontend/F1-handoff.md`
- `docs/contracts/F1-004-input-plan-api.md`
- `frontend/src/components/InputPanel.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/api/client.ts`
- `frontend/src/composables/useSSE.ts`
- `frontend/src/App.vue`
- `backend/src/main/java/com/weekendtravel/backend/config/WebConfig.java`
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`

重点核对结果：

- `InputPanel` 包含场景选择、自然语言 textarea、可选出发位置和提交按钮。
- `planner.submitPlan()` 会清理上一轮状态，调用 `plannerClient.createPlan()`，成功后写入 `planId`，追加 client log，并调用 `sse.connect(created.planId)`。
- `client.createPlan()` 在 real mode 调用 `/api/plan`，请求体字段为 `text`、`scenario`、可选 `origin`；mock mode 使用 fixture planId。
- `useSSE` 在 real mode 通过 `openPlanStream(planId)` 连接 `/api/plan/{planId}/stream`，mock mode 通过 `docs/fixtures/sse-events.jsonl` 回放。
- backend `WebConfig` 允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 两个前端 Origin。
- backend `PlanControllerCreateTests.createAllowsConfiguredFrontendOrigins` 覆盖两个 Origin，断言 202 和 `Access-Control-Allow-Origin` 回显。

## 命令结果

### frontend fast verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：

```text
[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture
[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture
[ok] docs/fixtures/sse-events.jsonl -> 23 JSONL events
[ok] pnpm verify:fixtures passed.
[ok] pnpm typecheck passed.
Verify passed.
```

### backend fast verify

补充 CORS 修复纳入验收后，独立运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果：

```text
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
[ok] mvnw.cmd test passed.
Verify passed.
```

说明：输出中存在 JDK 动态 agent warning，未导致测试失败。

### 禁止字段搜索

命令：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

结果：无命中，退出码 1，符合预期。

### feature_list.json 解析

命令：

```powershell
Get-Content -LiteralPath 'feature_list.json' -Raw | ConvertFrom-Json
```

结果：解析成功。`F1-004` 当前仍为 `todo`，本 evaluator 未把它标为 `verified`，等待 generator 根据本报告更新 metadata。

## Playwright MCP 证据

### mock mode

访问：`http://127.0.0.1:5173`

证据：

- 页面显示 `输入需求 mock` 和 `Mode mock`。
- `InputPanel` 可见，包含“场景”“自然语言输入”“出发位置”“提交规划”。
- 编辑自然语言输入与出发位置后点击“提交规划”。
- 页面出现 `plan_family_fixture`。
- 日志面板显示 `25 条事件`，包含 `heartbeat`、`START -> INTENT`、`plan_ready`、`done`、`error`。
- Playwright DOM 检查显示 `role=log` 自动滚动到底部：

```json
{
  "hasInputPanel": true,
  "hasPlanFamilyFixture": true,
  "hasStatus": true,
  "hasDoneAndError": true,
  "eventCountText": "25 条事件",
  "logScroll": {
    "atBottom": true
  }
}
```

截图：

- `docs/qa/F1-004-playwright-mock.png`

### real mode

后端健康检查：`GET http://localhost:8000/health -> 200`

`localhost` origin 证据：

- 访问 `http://localhost:5173`
- 点击“提交规划”
- Playwright network 捕获：

```text
POST http://localhost:8000/api/plan => 202
referer: http://localhost:5173/
access-control-allow-origin: http://localhost:5173
request body: {"text":"今天下午想带家人去附近玩两三个小时，孩子五岁，希望室内为主，晚饭清淡一点。","scenario":"family","origin":"望京SOHO"}
response body: {"planId":"plan_f130d794ba79","status":"processing"}
```

`127.0.0.1` origin 证据：

- 访问 `http://127.0.0.1:5173`
- 点击“提交规划”
- Playwright network 捕获：

```text
POST http://localhost:8000/api/plan => 202
referer: http://127.0.0.1:5173/
access-control-allow-origin: http://127.0.0.1:5173
request body: {"text":"今天下午想和朋友在附近找个轻松的活动，最好能吃饭聊天，别跑太远。","scenario":"family"}
response body: {"planId":"plan_5d24518d8102","status":"processing"}
```

页面证据：

- 页面显示后端返回的 `planId`。
- 日志面板可见 `heartbeat` 和 `START -> INTENT`。
- 空出发位置时，request body 未包含 `origin`。

截图：

- `docs/qa/F1-004-playwright-real.png`
- `docs/qa/F1-004-playwright-real-127.png`

## Chrome DevTools MCP 证据

### mock mode

访问：`http://127.0.0.1:5173`

snapshot / accessibility：

- RootWebArea 标题为 `WeekendTravel`。
- 可访问结构包含 `heading "输入需求 mock"`。
- 表单控件包含：
  - radio `家庭`
  - radio `朋友`
  - textbox `说一句你想怎么安排这个下午`
  - textbox `当前位置`
  - button `提交规划`
- LogPanel 为 `log "实时日志面板" live="polite"`。
- 提交后可见 `plan_family_fixture`、`DEGRADE`、`closed`、`25 条事件`、`heartbeat`、`START -> INTENT`、`done`、`error`。

console：

```text
<no console messages found>
```

network：

```text
仅访问 Vite 页面、模块和 fixture raw import：
GET /@fs/H:/WeekendTravel/docs/fixtures/plan-ready-family.json?import&raw
GET /@fs/H:/WeekendTravel/docs/fixtures/plan-ready-friends.json?import&raw
GET /@fs/H:/WeekendTravel/docs/fixtures/sse-events.jsonl?import&raw
```

DevTools evaluate 结果：

```json
{
  "hasMockMode": true,
  "hasInputPanel": true,
  "hasPlanFamilyFixture": true,
  "hasEventCount25": true,
  "logAtBottom": true,
  "apiPlanMentionInPerf": []
}
```

结论：mock mode 未访问真实 `/api/plan`。

截图：

- `docs/qa/F1-004-devtools-mock.png`

### real mode

访问：`http://127.0.0.1:5173`

snapshot / accessibility：

- 可访问结构包含 `heading "输入需求 real"`。
- 表单控件包含场景 radio、自然语言输入、出发位置、提交按钮。
- LogPanel 为 `log "实时日志面板" live="polite"`。
- 提交后页面显示 `plan_4f8421a01fb8`、`INTENT`、`retrying`，日志包含 `heartbeat` 与 `START -> INTENT`。

console：

```text
<no console messages found>
```

network：

```text
POST http://localhost:8000/api/plan [202]
GET http://localhost:8000/api/plan/plan_4f8421a01fb8/stream [200]
```

`POST /api/plan` 详情：

```text
origin: http://127.0.0.1:5173
access-control-allow-origin: http://127.0.0.1:5173
request body: {"text":"今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。今天下午想带家人去附近活动，想要室内、有饭吃、路线别太复杂。","scenario":"family","origin":"当前位置回龙观"}
response body: {"planId":"plan_4f8421a01fb8","status":"processing"}
```

截图：

- `docs/qa/F1-004-devtools-real.png`

## 网络证据汇总

- mock mode：DevTools network 与 Performance resource 均无 `/api/plan`；只加载前端模块和 fixture raw imports。
- real mode `localhost`：POST `/api/plan` 返回 202，CORS 回显 `http://localhost:5173`。
- real mode `127.0.0.1`：POST `/api/plan` 返回 202，CORS 回显 `http://127.0.0.1:5173`。
- real mode 成功响应均使用 camelCase：`planId`、`status`。
- real mode 成功创建后，页面继续请求 `/api/plan/{planId}/stream` 并渲染 `heartbeat` / `state_change`。

## 风险与观察

未发现阻断 F1-004 的风险。

观察项：

- real mode 中当前后端 SSE 占位流会让前端进入 `retrying` 并继续收到重复的 `heartbeat` / `START -> INTENT`。这不阻断 F1-004，因为本轮验收重点是 InputPanel 提交、POST 202、读取 `planId` / `status`、以及能看到 SSE 事件；完整状态机流转可在后续 INT/F1/B1 任务继续收敛。
- Chrome DevTools `fill_form` 对已有默认输入表现为追加文本，不影响产品验收；Playwright 的 `fill()` 已覆盖真实编辑路径。

## 最终判定

F1-004 满足本轮 contract 和验收要求。建议 generator 以本报告作为独立 evaluator 证据，后续再更新 `feature_list.json`、`progress.md` 和 `frontend/F1-handoff.md`。
