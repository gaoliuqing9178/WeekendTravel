# Contract: F1-008 Plan B highlight and error/degrade states

## 本轮目标

- 强化前端对 `replan` / Plan B 的可视化：日志面板必须明显高亮 Plan B 事件，方案卡必须显示 Plan B 徽标和原因。
- 强化前端对终态和异常态的可见性：`DONE`、`DEGRADE`、`FAILED` 和 `error` 消息必须在工作台上有稳定入口，而不只藏在日志行里。
- 保持现有 Vue 3 + Pinia + Naive UI 架构，继续以 `docs/api-contract.md` 和 `docs/fixtures/` 为唯一前端契约来源。

## 明确不做

- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不新增后端 API、状态机规则或真实异常注入能力。
- 不实现完整 `INT-002` / `INT-003` 端到端联调。
- 不新增 snake_case 兼容层。
- 不把 generator 自己运行的检查当作最终 `verified` 证据。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户提交 mock demo 输入，并回答 ClarifyBubble。
3. 前端继续回放到 `replan`、`plan_ready` 和 `CONFIRM`。
4. LogPanel 高亮 `replan` 事件，PlanCard 显示 Plan B 徽标和原因。
5. 用户点击确认执行后，`done` 事件会进入 `DONE` 可见状态。
6. 如果 SSE 收到 `error(code=DEGRADE)`，前端进入 `DEGRADE` 并显示可读错误消息；其他 `error` 或客户端异常进入 `FAILED` 并显示错误消息。

## UI 要求

- 页面仍然是工作台，不做营销落地页。
- Plan B 必须在两个位置可见：
  - LogPanel 的 `replan` 事件行使用独立视觉样式。
  - PlanCard header 显示 `Plan B` 徽标，正文显示 `planBReason`。
- 状态总览必须始终展示当前 Agent 状态、SSE 连接状态、终态轨道 `DONE / DEGRADE / FAILED`，并在对应状态发生时高亮。
- `errorMessage` 必须以可读文本显示，不能只通过颜色或日志类型表达。
- 视觉区分必须同时依赖文本、边框 / 背景 / 标签，不能只依赖颜色。
- 组件必须保持响应式，移动端不能出现横向滚动或文字溢出。

## API / 后端要求

- 继续只消费 `docs/api-contract.md` 中已有字段：
  - `replan.reason`
  - `replan.replanCount`
  - `plan.isPlanB`
  - `plan.planBReason`
  - `done.summary`
  - `error.code`
  - `error.message`
- `error.code === "DEGRADE"` 映射到 `DEGRADE`。
- 其他 SSE `error` 和客户端请求异常映射到 `FAILED`。
- mock mode 不访问真实后端网络。

## 数据和状态要求

- Pinia store 继续作为状态入口。
- `plan_ready` 和 `adjust_result` 仍然使用 `payload.plan` 更新当前方案。
- `replan` 不能破坏当前方案，只更新日志和状态可见信号。
- `done` 必须进入 `DONE`，并保留执行完成摘要。
- `error` 必须保存 `payload.message`，并按 code 映射到 `DEGRADE` 或 `FAILED`。
- fixture 验证必须要求 `replan`、`plan_ready`、`execute_result`、`done` 和 `error` 存在，并确认 Plan B 原因和 `DEGRADE` error 可解析。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`、`frontend/src/components/PlanCard.vue`、`frontend/src/components/LogPanel.vue`、状态总览组件、fixture verifier 和 `docs/fixtures/sse-events.jsonl`。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须使用 Chrome DevTools MCP。
- Chrome DevTools MCP 证据必须覆盖：
  - 打开页面并提交 mock plan。
  - 回答 ClarifyBubble 后等待 `CONFIRM`。
  - 页面 snapshot / accessibility 中可见 `Plan B` 徽标、Plan B 原因、终态轨道 `DONE / DEGRADE / FAILED`。
  - LogPanel 中可见并高亮 `replan` / Plan B 事件。
  - 点击确认执行后可见 `DONE` 状态或 done 日志。
  - console 无 error / warn。
  - mock mode network 不访问真实 `/api/plan/*` 后端端点。

手动路径：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 必须通过，并覆盖 Plan B、done 和 error/degrade fixture。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-008-plan-b-error-states.md`。
- Chrome DevTools MCP 截图证据写入 `docs/qa/F1-008-devtools-mock.png`。
- QA 报告必须记录 Chrome DevTools MCP 的 snapshot / console / network / DOM 或 accessibility 证据。

## 失败阈值

- `replan` 事件没有明显 Plan B 高亮，失败。
- PlanCard 没有 Plan B 徽标或 `planBReason`，失败。
- `DONE`、`DEGRADE`、`FAILED` 或 `error` 只能从隐藏状态推断，页面没有稳定可见入口，失败。
- SSE `error(code=DEGRADE)` 没有进入 `DEGRADE`，失败。
- 非 DEGRADE 错误或客户端异常没有进入 `FAILED`，失败。
- mock mode 访问真实后端 `/api/plan/*` 端点，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Chrome DevTools MCP 证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
