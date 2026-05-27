# Contract: F1-005 PlanCard, ConfirmButton, and ExecutionTracker

## 本轮目标

- 基于现有 `plan_ready` fixture 渲染正式 `PlanCard`。
- `PlanCard` 必须展示 `summary`、`timeline`、`actions`、`shareMessage`、总时长、重排次数和 Plan B 信息。
- 在 `CONFIRM` 状态启用 `ConfirmButton`，非 `CONFIRM` 状态禁用。
- 点击确认后调用既有 `PlannerApiClient.executePlan(planId, { confirmed: true })`。
- mock mode 下确认后使用 fixture 里的 `execute_result` / `done` 事件更新执行包，形成 `ExecutionTracker`。
- real API mode 下只调用 `docs/api-contract.md` 规定的 `POST /api/plan/{planId}/execute`，真实执行事件由后续后端状态机和 SSE 继续补齐。

## 明确不做

- 不实现 ClarifyBubble；这些属于 `F1-006`。
- 不实现 AdjustPanel；这些属于 `F1-007`。
- 不扩展后端 Planner、状态机、真实 `plan_ready` 或真实执行事件。
- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不引入 snake_case 字段兼容层。
- 不把 generator 自己的验证作为最终 `verified` 证据。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户提交 Demo 输入。
3. mock mode 下前端回放 fixture planning 事件，停在 `CONFIRM`，页面显示 `PlanCard` 和可点击的确认按钮。
4. 用户点击“确认执行”。
5. 前端调用 `executePlan(planId, { confirmed: true })`。
6. mock mode 下前端回放 fixture execution 事件，`ExecutionTracker` 按 `actionId` 更新动作状态和 `confirmationNo`。
7. real mode 下前端只负责调用 execute endpoint；如果后端尚未实现，必须显示可读错误，而不是伪造真实完成。

## UI 要求

- 页面首屏仍是工作台，不做营销落地页。
- `PlanCard` 必须可见展示：
  - 方案摘要
  - `Plan B` 徽标和 `planBReason`
  - 总时长、重排次数、场景和 Plan ID
  - 按 `order` 排序的 timeline
  - 每个时间段的 `title`、`poi.name`、`startTime`、`endTime`、地址、距离、等待时间、标签和 notes
  - 分享消息预览
- `ConfirmButton` 必须：
  - 只在 `agentState === CONFIRM` 且当前有 plan 时可点击
  - 执行中显示 loading，并避免重复提交
  - 显示 execute endpoint 的返回消息或错误消息
- `ExecutionTracker` 必须：
  - 展示 `actions` 的 `description`、`status` 和 `confirmationNo`
  - 用不同视觉状态区分 `pending`、`executing`、`success`、`failed` 和 `manual`
  - 在 `execute_result` 到达后按 `actionId` 更新对应 action
  - 对空 action 列表显示明确空状态
- UI 控件使用语义按钮和动态 aria 状态；可点击元素必须有 pointer / disabled 状态。

## API / 后端要求

- real mode 确认执行只调用：

```text
POST /api/plan/{planId}/execute
```

- 请求体只发送：

```json
{ "confirmed": true }
```

- 成功响应读取：
  - `planId`
  - `status`
  - `message`
- API 错误必须显示后端返回的 `message`。
- mock mode 不访问后端网络。
- 不新增 API 字段，不做 snake_case 兼容映射。

## 数据和状态要求

- Pinia store 继续作为状态入口。
- `plan_ready` 和 `adjust_result` 仍然更新 `currentPlan`。
- mock planning 回放在 `CONFIRM` 后暂停，等待用户点击确认。
- 点击确认后：
  - 写入执行中状态
  - 追加 client log
  - 调用 `executePlan`
  - mock mode 继续消费 fixture 的 `execute_result` 和 `done`
- `execute_result` 必须按 `actionId` 更新 `currentPlan.actions[*].status` 和 `confirmationNo`。
- `done` 进入 `DONE` 状态。
- `error` 按 `code=DEGRADE` 进入 `DEGRADE`，否则进入 `FAILED`。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`、`frontend/src/components/PlanCard.vue`、`frontend/src/components/ConfirmButton.vue`、`frontend/src/components/ExecutionTracker.vue`。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- Playwright MCP 证据：打开页面，提交 mock plan，等待 `PlanCard` 出现，确认按钮在 `CONFIRM` 状态可点击，点击确认后 `ExecutionTracker` 出现 `success` 和 `confirmationNo`。
- Chrome DevTools MCP 证据：页面 snapshot / accessibility 结构包含 PlanCard、确认按钮和 ExecutionTracker；console 无 error / warn；mock mode network 不访问真实 `/api/plan/*/execute`。

手动路径：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 仍应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-005-plan-card-execution.md`。
- 前端截图证据写入 `docs/qa/`，文件名使用 `F1-005-*` 前缀。
- QA 报告必须分别记录 Playwright MCP 与 Chrome DevTools MCP 证据。

## 失败阈值

- `plan_ready` fixture 不能渲染 PlanCard，失败。
- PlanCard 缺少 required-render 字段，失败。
- ConfirmButton 在非 `CONFIRM` 状态可点击，失败。
- 点击确认没有调用 `executePlan(planId, { confirmed: true })`，失败。
- `execute_result` 没有更新 ExecutionTracker action 状态，失败。
- mock mode 访问真实后端 execute endpoint，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
