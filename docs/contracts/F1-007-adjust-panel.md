# Contract: F1-007 AdjustPanel

## 本轮目标

- 新增正式 `AdjustPanel`，让用户在方案进入 `CONFIRM` 后提交局部微调要求。
- `AdjustPanel` 只在 `CONFIRM` 状态渲染。
- 用户提交微调时调用既有 `PlannerApiClient.adjustPlan(planId, { instruction })`。
- real API mode 下调用 `docs/api-contract.md` 规定的 `PATCH /api/plan/{planId}/adjust`。
- 单个前端 plan 最多允许 3 次微调；第 4 次前端必须阻止提交并给出明确提示。
- mock mode 下使用 `docs/fixtures/sse-events.jsonl` 中的 `adjust_result` 更新当前 plan，并回到 `CONFIRM` 交互。

## 明确不做

- 不扩展后端 Planner、ADJUST 规则、状态机或 SSE payload。
- 不实现 F1-008 的 Plan B / error / degrade 状态完整视觉治理。
- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不引入 snake_case 字段兼容层。
- 不伪造 real API mode 成功；后端错误必须显示其 `message`。
- 不把 generator 自己的验证作为最终 `verified` 证据。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户提交 Demo 输入，并在 `ClarifyBubble` 中回答反问。
3. 前端继续回放到 `plan_ready` / `CONFIRM`，页面显示方案卡、`AdjustPanel` 和确认执行入口。
4. 用户输入或选择一个微调要求，点击“提交微调”。
5. 前端调用 `adjustPlan(planId, { instruction })`。
6. mock mode 下前端消费 `adjust_result`，更新方案卡并将交互恢复到 `CONFIRM`。
7. real mode 下前端连接同一 `planId` 的 SSE stream，等待后端推送 `adjust_result`。
8. 用户最多可重复 3 次微调；达到上限后仍可确认执行，但不能继续提交微调。

## UI 要求

- 页面首屏仍是工作台，不做营销落地页。
- `AdjustPanel` 放在侧栏确认执行入口之前，作为确认前的最后调整动作。
- `AdjustPanel` 必须只在 `planner.agentState === 'CONFIRM'` 时出现。
- 面板必须展示：
  - 当前已用微调次数和总上限 `3`
  - 剩余可用次数
  - 2-3 个快捷微调项
  - 自由文本输入框
  - 提交按钮
- 提交中按钮进入 loading / disabled，避免重复提交。
- 达到 3 次后，快捷项、输入框和提交按钮不可继续提交，并显示明确原因。
- 如果 adjust API 返回成功消息或 `adjust_result.summary`，显示可读状态。
- 如果 adjust API 返回错误，显示错误消息，并保留当前方案供用户继续确认或重试。
- 控件必须使用语义按钮、动态 disabled 状态和键盘可访问文本。

## API / 后端要求

- real mode 只调用：

```text
PATCH /api/plan/{planId}/adjust
```

- 请求体只发送：

```json
{ "instruction": "<用户输入的微调要求>" }
```

- 成功响应读取：
  - `planId`
  - `status`
  - `message`
- 后续 `adjust_result` 仍由 SSE reducer 更新当前 plan。
- API 错误必须显示后端返回的 `message`。
- mock mode 不访问后端网络。
- 不新增 API 字段，不做 snake_case 兼容映射。

## 数据和状态要求

- Pinia store 继续作为状态入口。
- store 必须暴露：
  - `adjustCount`
  - `adjustLimit`
  - `isAdjusting`
  - `canAdjustPlan`
  - `adjustMessage`
  - `adjustErrorMessage`
  - `adjustPlan(instruction)`
- `submitPlan()` 和 `resetSkeletonFlow()` 必须重置 adjust 状态与次数。
- `adjustPlan(instruction)` 必须：
  - 忽略空 instruction
  - 防止重复提交
  - 在第 4 次前端阻止提交
  - 追加 client log
  - 调用 `plannerClient.adjustPlan(planId, { instruction })`
  - 成功后将本地 `adjustCount` 增加 1
  - mock mode 下播放 `CONFIRM` 之后到 `adjust_result` 的 fixture 事件
  - real mode 下重新连接同一 `planId` 的 SSE stream
- `adjust_result` reducer 必须：
  - 用 `payload.plan` 更新 `currentPlan`
  - 清理 adjusting 状态
  - 将 `agentState` 恢复为 `payload.plan.status`
  - 展示 `payload.summary`
- `plan_ready`、`clarification_request`、`execute_result`、`done`、`error` 的既有 reducer 行为不能被破坏。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`、`frontend/src/components/AdjustPanel.vue`、`frontend/src/api/client.ts`、`frontend/scripts/verify-fixtures.mjs` 和 `docs/fixtures/sse-events.jsonl`。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须使用 Chrome DevTools MCP。
- Chrome DevTools MCP 证据：打开页面，提交 mock plan，回答 ClarifyBubble，等待 `CONFIRM`；确认 `AdjustPanel` 可见；提交一次微调；等待 `adjust_result` 后确认方案摘要或餐厅内容更新、次数变为 `1 / 3`、状态回到 `CONFIRM`；console 无 error / warn；mock mode network 不访问真实 `/api/plan/*/adjust`。
- Chrome DevTools MCP 还需覆盖页面 snapshot、DOM / accessibility 或视觉复核证据。
- Playwright MCP 可作为补充证据，但不是本轮 `verified` 的硬门槛。

手动路径：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 仍应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL，并要求 `adjust_result` 存在。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-007-adjust-panel.md`。
- 前端截图证据写入 `docs/qa/`，文件名使用 `F1-007-*` 前缀。
- QA 报告必须记录 Chrome DevTools MCP 证据。

## 失败阈值

- `CONFIRM` 状态不能渲染 AdjustPanel，失败。
- 非 `CONFIRM` 状态渲染 AdjustPanel，失败。
- 点击提交没有调用 `adjustPlan(planId, { instruction })`，失败。
- real mode 没有调用 `PATCH /api/plan/{planId}/adjust`，失败。
- 第 4 次微调仍可提交，失败。
- `adjust_result` 没有更新当前方案或没有回到 `CONFIRM`，失败。
- mock mode 访问真实后端 adjust endpoint，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Chrome DevTools MCP 证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
