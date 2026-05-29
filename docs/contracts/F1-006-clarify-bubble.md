# Contract: F1-006 ClarifyBubble

## 本轮目标

- 基于 `clarification_request` SSE payload 渲染正式 `ClarifyBubble`。
- 页面同一时间只能出现一个反问气泡。
- 气泡必须展示 `question`、`field` 和 2-3 个快捷选项。
- 用户点击选项后调用既有 `PlannerApiClient.clarifyPlan(planId, { reply })`。
- real API mode 下调用 `docs/api-contract.md` 规定的 `POST /api/plan/{planId}/clarify`。
- mock mode 下使用 `docs/fixtures/sse-events.jsonl` 先停在 `clarification_request`，用户回答后继续回放到 `plan_ready` / `CONFIRM`。

## 明确不做

- 不实现 AdjustPanel；这些属于 `F1-007`。
- 不扩展后端 Planner、CLARIFY 规则、状态机或 SSE payload。
- 不修改 `docs/api-contract.md` 的字段、事件名或枚举。
- 不引入 snake_case 字段兼容层。
- 不伪造 real API mode 成功；后端错误必须显示其 `message`。
- 不把 generator 自己的验证作为最终 `verified` 证据。

## 用户路径

1. 用户打开 `http://127.0.0.1:5173`。
2. 用户提交 Demo 输入。
3. 前端收到 `clarification_request` 后渲染一个 `ClarifyBubble`，并暂停继续规划。
4. 用户点击 2-3 个快捷选项中的一个。
5. 前端调用 `clarifyPlan(planId, { reply })`。
6. mock mode 下继续回放 fixture planning 事件，最终显示 `PlanCard` 并进入 `CONFIRM`。
7. real mode 下重新连接 `/api/plan/{planId}/stream`，等待后端继续推送后续 SSE 事件。

## UI 要求

- 页面首屏仍是工作台，不做营销落地页。
- `ClarifyBubble` 放在输入面板和方案卡之间，保持用户回答路径清晰。
- 只要 `planner.pendingClarification` 存在，页面只渲染一个反问气泡。
- 气泡必须展示：
  - `question`
  - `field`
  - `options` 中的 2-3 个快捷选项按钮
- 点击选项时按钮进入 loading / disabled，避免重复提交。
- 如果后端或 mock client 返回成功消息，显示可读状态。
- 如果 clarify API 返回错误，显示错误消息，并保留当前反问供用户重试。
- 控件必须有语义按钮、可见 disabled 状态和键盘可访问文本。

## API / 后端要求

- real mode 只调用：

```text
POST /api/plan/{planId}/clarify
```

- 请求体只发送：

```json
{ "reply": "<用户选择的选项文本>" }
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
- `clarification_request` 仍写入 `pendingClarification`，并将 `agentState` 置为 `CLARIFY`。
- `replyToClarification(reply)` 必须：
  - 忽略空 reply
  - 防止重复提交
  - 追加 client log
  - 调用 `plannerClient.clarifyPlan(planId, { reply })`
  - 成功后清理 `pendingClarification`
  - mock mode 下继续播放 clarification 之后、`CONFIRM` 之前的 fixture 事件
  - real mode 下重新连接同一 `planId` 的 SSE stream
- `plan_ready`、`adjust_result`、`done`、`error` 的既有 reducer 行为不能被破坏。
- `docs/fixtures/sse-events.jsonl` 必须包含一个有效 `clarification_request`，且 options 数量为 2-3。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/stores/planner.ts`、`frontend/src/App.vue`、`frontend/src/components/ClarifyBubble.vue`、`frontend/src/api/client.ts`、`frontend/scripts/verify-fixtures.mjs` 和 `docs/fixtures/sse-events.jsonl`。
- 独立运行前端 fast verify。
- 确认 `feature_list.json` 在 evaluator 证据写入后仍可解析。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- Playwright MCP 证据：打开页面，提交 mock plan，等待 `ClarifyBubble` 出现，确认页面只有一个气泡、气泡包含 1 个问题和 3 个选项，点击一个选项后继续到 `PlanCard` / `CONFIRM`。
- Chrome DevTools MCP 证据：页面 snapshot / accessibility 结构包含 `ClarifyBubble`、问题和选项按钮；console 无 error / warn；mock mode network 不访问真实 `/api/plan/*/clarify`。
- real mode API 证据：在后端可用时提交触发 `CLARIFY` 的输入，点击选项后 network 包含 `POST /api/plan/{planId}/clarify`，请求体为 `{ "reply": "<选项文本>" }`，响应读取 `status=processing`。

手动路径：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

自动验证：

- `pnpm verify:fixtures` 仍应解析 2 个 plan_ready JSON 和 1 个 SSE JSONL，并校验 `clarification_request`。
- `pnpm typecheck` 必须通过。
- 可选补充：`pnpm build` 通过。

API / 日志 / 截图证据：

- evaluator 报告写入 `docs/qa/F1-006-clarify-bubble.md`。
- 前端截图证据写入 `docs/qa/`，文件名使用 `F1-006-*` 前缀。
- QA 报告必须分别记录 Playwright MCP 与 Chrome DevTools MCP 证据。

## 失败阈值

- `clarification_request` 不能渲染 ClarifyBubble，失败。
- 同一页面同时出现多个反问气泡，失败。
- 气泡缺少 `question` 或 2-3 个快捷选项，失败。
- 点击选项没有调用 `clarifyPlan(planId, { reply })`，失败。
- real mode 没有调用 `POST /api/plan/{planId}/clarify`，失败。
- mock mode 回答后不能继续到 `plan_ready` / `CONFIRM`，失败。
- mock mode 访问真实后端 clarify endpoint，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 前端 UI 任务缺少 Playwright MCP 或 Chrome DevTools MCP 任一类证据，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
