# F1-007 AdjustPanel QA 报告

## 测试目标

- 验收 `docs/contracts/F1-007-adjust-panel.md` 定义的 AdjustPanel 用户路径。
- 确认前端 fast verify、fixture 校验、typecheck、禁用 snake_case 字段搜索、`feature_list.json` 解析都通过。
- 使用 Chrome DevTools MCP 完成 mock mode 浏览器验收：提交 plan、回答 ClarifyBubble、进入 `CONFIRM`、提交 3 次微调、确认 `adjust_result` 更新方案并锁定第 4 次微调入口。
- 本轮作为独立 evaluator 验收，只写 QA 报告和截图，不修改业务代码、`feature_list.json`、`progress.md` 或 handoff。

## 读取范围

已独立读取并核对以下文件：

- `docs/contracts/F1-007-adjust-panel.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `frontend/F1-handoff.md`
- `frontend/src/components/AdjustPanel.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/App.vue`
- `frontend/src/api/client.ts`
- `frontend/scripts/verify-fixtures.mjs`
- `docs/fixtures/sse-events.jsonl`

静态核对结论：

- `AdjustPanel` 在 `App.vue` 中通过 `v-if="planner.agentState === 'CONFIRM'"` 渲染，符合只在 `CONFIRM` 可见的要求。
- `PlannerApiClient.adjustPlan(planId, { instruction })` 在 real mode 下调用 `PATCH /api/plan/{planId}/adjust`，请求体只发送 `instruction`。
- Pinia store 暴露 `adjustCount`、`adjustLimit`、`isAdjusting`、`canAdjustPlan`、`adjustMessage`、`adjustErrorMessage` 和 `adjustPlan(instruction)`。
- `submitPlan()` 与 `resetSkeletonFlow()` 会重置微调次数、消息、错误和 loading 状态。
- `adjust_result` reducer 使用 `payload.plan` 更新 `currentPlan`，清理 `isAdjusting`，展示 `payload.summary`，并把 `agentState` 恢复为 `payload.plan.status`。
- `docs/fixtures/sse-events.jsonl` 包含 `adjust_result`，并在 mock mode 下把餐厅局部更新为“林间日式小食”。

## 命令

### 前端 fast verify

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：PASS。

关键输出：

```text
[ok] docs/fixtures/plan-ready-family.json -> plan_family_fixture
[ok] docs/fixtures/plan-ready-friends.json -> plan_friends_fixture
[ok] docs/fixtures/sse-events.jsonl -> 28 JSONL events
[ok] pnpm verify:fixtures passed.
[ok] pnpm typecheck passed.
Verify passed.
```

### 禁止字段搜索

```powershell
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

结果：PASS，无命中。

### feature_list.json 解析

```powershell
ConvertFrom-Json -InputObject (Get-Content -LiteralPath 'feature_list.json' -Raw)
```

结果：PASS，PowerShell 成功解析并返回 `project=WeekendTravel`、`updatedAt=2026-05-29`、`statusValues={todo,in_progress,blocked,verified}`。

## 用户路径

浏览器：Chrome DevTools MCP  
地址：`http://127.0.0.1:5173/`  
模式：mock mode

步骤与结果：

1. 打开页面，初始状态为 `START` / `idle`，Mode 为 `mock`。
2. 点击“提交规划”，页面进入 `CLARIFY`，只出现一个 ClarifyBubble。
3. ClarifyBubble 显示问题“请问大概想玩几个小时？”和 3 个选项：`3-4小时`、`4-6小时`、`6小时以上`。
4. 点击“回答：4-6小时”，fixture 继续回放到 `plan_ready` / `CONFIRM`。
5. `CONFIRM` 下出现 PlanCard、ConfirmButton、ExecutionTracker 和 AdjustPanel。
6. AdjustPanel 初始显示 `0 / 3`，快捷项、自由文本框和“提交微调”入口可用；确认执行按钮也可用。
7. 第一次选择“换一家餐厅，要能订位”并提交微调，等待 `adjust_result`。
8. 页面显示“林间日式小食”，微调计数变为 `1 / 3`，Agent 保持 `CONFIRM`，日志出现 `CONFIRM -> ADJUST`、`ADJUST -> VALIDATE`、`adjust_result`。
9. 再提交“把总时长压缩 30 分钟”，计数变为 `2 / 3`。
10. 再提交“减少步行，优先室内”，计数变为 `3 / 3`。
11. 达到上限后，三个快捷项、自由文本框和“提交微调”按钮全部 disabled，并显示“已达最大微调次数”。
12. 达到上限后，“确认执行”按钮仍然可用。

## Chrome DevTools MCP 证据

### Snapshot / accessibility 关键文本

Chrome DevTools MCP snapshot 覆盖以下关键节点：

- 初始页面：`WeekendTravel`、`START`、`idle`、`输入需求 mock`、`提交规划`。
- ClarifyBubble：region `需要确认`、问题 `请问大概想玩几个小时？`、按钮 `回答：3-4小时`、`回答：4-6小时`、`回答：6小时以上`。
- CONFIRM 初始：PlanCard 显示 `亲子室内活动、低负担晚餐和回家路线`，Pinia 状态 `Agent CONFIRM`，AdjustPanel region `微调方案`，标题 `微调方案 3 次可用 0 / 3`。
- 第一次微调后：PlanCard 显示 `亲子室内活动、日式轻食晚餐和回家路线`、餐厅 `林间日式小食`，AdjustPanel 标题 `微调方案 2 次可用 1 / 3`，日志包含 `adjust_result`。
- 第三次微调后：AdjustPanel 标题 `微调方案 0 次可用 3 / 3`，快捷项和输入框显示 disabled，文本 `已达最大微调次数` 可见，ConfirmButton `确认执行` 仍未 disabled。

### DOM 状态复核

Chrome DevTools MCP `evaluate_script` 返回：

```json
{
  "hasRestaurant": true,
  "hasAdjustCount": true,
  "hasLimitReason": true,
  "agentConfirmOccurrences": 10,
  "adjustButtons": [
    { "text": "换一家餐厅，要能订位", "disabled": true },
    { "text": "把总时长压缩 30 分钟", "disabled": true },
    { "text": "减少步行，优先室内", "disabled": true },
    { "text": "提交微调", "disabled": true, "ariaDisabled": "true" },
    { "text": "确认执行", "disabled": false, "ariaDisabled": "false" }
  ],
  "textareas": [
    { "placeholder": "说一句你想怎么安排这个下午", "disabled": false },
    { "placeholder": "例如：换一家餐厅，要能订位", "disabled": true, "value": "" }
  ]
}
```

### Console

Chrome DevTools MCP `list_console_messages(types=["error","warn"])`：

```text
<no console messages found>
```

结论：console error / warn 为 0。

### Network

Chrome DevTools MCP `list_network_requests(resourceTypes=["xhr","fetch"])`：无 fetch / xhr 请求。

完整 network 列表仅包含 Vite 页面、模块、组件和 fixture raw import：

- `GET http://127.0.0.1:5173/`
- `GET http://127.0.0.1:5173/src/components/AdjustPanel.vue`
- `GET http://127.0.0.1:5173/src/stores/planner.ts`
- `GET http://127.0.0.1:5173/@fs/H:/WeekendTravel/docs/fixtures/sse-events.jsonl?import&raw`
- 以及其他前端模块静态资源

结论：mock mode 没有真实 `/api/plan/*/adjust` fetch / xhr。

### 截图

- Chrome DevTools MCP 截图：`docs/qa/F1-007-devtools-mock.png`
- 截图状态：第三次微调后，页面显示 `3 / 3`、`已达最大微调次数`、`林间日式小食`、Agent `CONFIRM`，并保留可用的“确认执行”按钮。

## 截图、日志和 API 输出位置

- QA 报告：`docs/qa/F1-007-adjust-panel.md`
- Chrome DevTools MCP 截图：`docs/qa/F1-007-devtools-mock.png`
- 命令输出：记录在本报告“命令”章节。
- Chrome DevTools MCP snapshot / DOM / console / network 输出：记录在本报告“Chrome DevTools MCP 证据”章节。
- 本轮未生成单独 API 日志文件；mock mode 下没有真实 adjust API 请求。
- 仓库中已有 `docs/qa/F1-007-generator-devtools-mock.png`，这是 generator 侧遗留截图，本轮未作为正式 evaluator 放行证据。

## 失败复现

功能验收未发现失败。

本轮浏览器启动时遇到一次非业务卡点：Chrome DevTools MCP 的测试 profile `C:\Users\lx8nb\.cache\chrome-devtools-mcp\chrome-profile` 被既有自动化 Chrome 进程占用，`list_pages` / `new_page` 报错 `The browser is already running for ... chrome-profile`。确认这些进程属于 DevTools MCP 自动化 profile 后，仅关闭该测试 Chrome 主进程，再重新打开 `http://127.0.0.1:5173/`，浏览器验收继续完成。

## 放行结论

PASS。

F1-007 AdjustPanel 满足本轮 contract 的核心验收项：

- `CONFIRM` 下可见，非 `CONFIRM` 初始状态未渲染。
- mock plan -> ClarifyBubble -> `CONFIRM` -> AdjustPanel -> 三次微调的用户路径走通。
- 第一次 `adjust_result` 后方案更新为“林间日式小食”，计数为 `1 / 3`，Agent 回到 `CONFIRM`。
- 第三次后计数为 `3 / 3`，快捷项、输入框、提交按钮 disabled，并显示明确原因。
- 达到微调上限后，确认执行按钮仍可用。
- 前端 fast verify 通过，禁用字段搜索无命中，`feature_list.json` 可解析。
- Chrome DevTools MCP 提供了 snapshot/accessibility、DOM 状态、console、network 和截图证据。

## 独立性声明

本报告由独立 evaluator 子代理完成。验收过程中未修改业务代码，未修改 `feature_list.json`、`progress.md`、`frontend/F1-handoff.md` 或其他 handoff 文件。最终放行依据为本轮独立读取、命令执行和 Chrome DevTools MCP 浏览器证据，不使用 generator 自证结果替代 evaluator 结论。
