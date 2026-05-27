# F1-005 PlanCard, ConfirmButton, and ExecutionTracker QA

## 结论

PASS。

本轮作为独立 evaluator 子代理完成验收。验收过程中未修改产品代码，未修改 `feature_list.json`、`progress.md` 或 `frontend/F1-handoff.md`；只写入本报告和截图证据文件。

## 验收对象

- Contract: `docs/contracts/F1-005-plan-card-execution.md`
- API contract: `docs/api-contract.md`
- Frontend contract: `docs/frontend-contract.md`
- Handoff: `frontend/F1-handoff.md`
- 相关代码：
  - `frontend/src/stores/planner.ts`
  - `frontend/src/App.vue`
  - `frontend/src/components/PlanCard.vue`
  - `frontend/src/components/ConfirmButton.vue`
  - `frontend/src/components/ExecutionTracker.vue`

## 命令检查

### Frontend fast verify

命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：PASS。

关键输出：

```text
[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture
[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture
[ok] docs/fixtures/sse-events.jsonl -> 24 JSONL events
[ok] pnpm verify:fixtures passed.
[ok] pnpm typecheck passed.
Verify passed.
```

### 禁止 snake_case 线缆字段检查

命令：

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

结果：PASS。命令无输出；`rg` 以无命中状态返回，符合预期。未发现新增禁止的 snake_case 线缆字段兼容层。

### feature_list.json 解析

命令：

```powershell
ConvertFrom-Json -InputObject (Get-Content -Raw -LiteralPath "feature_list.json")
```

结果：PASS。`feature_list.json` 可解析，返回对象包含 `project=WeekendTravel`、`statusValues={todo,in_progress,blocked,verified}` 和 `items`。

## Playwright MCP 验收

访问地址：`http://127.0.0.1:5174/`

截图证据：`docs/qa/F1-005-playwright-mock.png`

执行路径：

1. Playwright MCP 打开 mock 前端页面，初始状态为 `START / idle`。
2. 初始 snapshot 显示 `确认执行` 按钮为 disabled，原因文案为“等待方案生成”。
3. 点击“提交规划”，等待页面进入 `CONFIRM / closed`。
4. `CONFIRM` 状态下，PlanCard 可见并展示：
   - summary: “亲子室内活动、低负担晚餐和回家路线”
   - `Plan B` 和 `planBReason`
   - 总时长 `4.2 小时`
   - 重排 `1 次`
   - `Plan ID=plan_family_fixture`
   - 时间线 `2 段`
   - 执行动作 `3 个动作`
   - 分享消息预览
5. `CONFIRM` 状态下，“确认执行”按钮可点击。
6. 点击“确认执行”后，等待 ExecutionTracker 进入完成态。
7. Playwright MCP 确认页面包含：
   - `3 / 3`
   - `MOCK-TBL-88421`
   - `MOCK-NOTE-122`
   - `MOCK-MSG-309`
8. Playwright MCP DOM 文本检查返回：

```json
{
  "hasPlanCard": true,
  "hasTimeline": true,
  "hasActions": true,
  "hasShareMessage": true,
  "hasPlanB": true,
  "hasDuration": true,
  "hasCompleted": true,
  "hasTable": true,
  "hasNote": true,
  "hasMsg": true,
  "confirmDisabledAfterDone": true
}
```

结论：PASS。

## Chrome DevTools MCP 验收

访问地址：`http://127.0.0.1:5174/`

截图证据：`docs/qa/F1-005-devtools-mock.png`

检查内容：

1. Chrome DevTools MCP 打开 mock 前端页面，点击“提交规划”，等待 `CONFIRM`。
2. snapshot / accessibility 显示：
   - `方案卡 亲子室内活动、低负担晚餐和回家路线 CONFIRM`
   - `Plan B`
   - `总时长 4.2 小时`
   - `时间线`
   - `执行包 3 个动作`
   - `分享消息`
   - `确认执行 CONFIRM`
   - `执行追踪 0 / 3 个动作完成 CONFIRM`
3. 点击“确认执行”后，snapshot / accessibility 显示：
   - `方案卡 ... DONE`
   - `确认执行 DONE`，按钮 disabled
   - `执行追踪 3 / 3 个动作完成 DONE`
   - `确认号：MOCK-TBL-88421`
   - `确认号：MOCK-NOTE-122`
   - `确认号：MOCK-MSG-309`
4. console 检查结果：

```text
<no console messages found>
```

即 error / warn 为 0。

5. network 检查结果：
   - fetch / xhr 列表为空。
   - 全量 network 共 24 条，只有 Vite 页面、模块加载和 fixture raw import。
   - 未发现真实 `POST /api/plan/{planId}/execute` 或任何 `/api/plan/*/execute` 请求。
6. DevTools DOM / resource 检查返回：

```json
{
  "hasPlanCard": true,
  "hasConfirmButton": true,
  "confirmButtonDisabledAfterDone": true,
  "hasExecutionTracker": true,
  "hasAllConfirmationNos": true,
  "executeNetworkRequests": [],
  "bodyContains": {
    "planB": true,
    "timeline": true,
    "actions": true,
    "shareMessage": true,
    "duration": true
  }
}
```

结论：PASS。

## 截图证据

- Playwright MCP: `docs/qa/F1-005-playwright-mock.png`
- Chrome DevTools MCP: `docs/qa/F1-005-devtools-mock.png`

## 残留风险

- 本轮按 F1-005 contract 验收 mock mode 用户路径；real API mode 的真实 execute endpoint 未在本轮浏览器验收中触发。
- 本轮浏览器验收覆盖默认 family fixture 路径；friends fixture 通过 fixture parser 和 typecheck 覆盖，未单独做浏览器点击截图。
- Chrome DevTools MCP 和 Playwright MCP 初次连接时各自存在一个历史自动化 profile 占用进程；已仅清理对应 MCP 自动化会话后重试，不影响产品代码与验收结论。
