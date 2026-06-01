# Contract: INT-003 Friends scenario complete path

## 本轮目标

- 跑通真实 real mode 朋友场景完整链路：
  `POST /api/plan` -> `GET /api/plan/{planId}/stream` -> `plan_ready` -> `POST /api/plan/{planId}/execute` -> `execute_result` -> `done`。
- 验证前端使用真实后端，而不是 mock fixture，展示 friends plan、确认执行、执行结果和最终完成态。
- friends 方案必须包含活动、中途咖啡 / 甜品或拍照休息点、餐厅，并保留可执行动作包。

## 明确不做

- 不接真实支付、真实订座、真实配送、真实地图或真实第三方服务。
- 不扩展 OpenAI / LLM 接入；规划决策仍由后端确定性状态机和本地 Mock Tool 完成。
- 不修改 `docs/api-contract.md` 既有字段；本轮只使用既有 `timeline`、`actions`、`execute_result` 和 `done` 契约。
- 不把 7 条 friends golden cases、边界 case 或异常 / degrade 链路纳入本轮放行条件。

## 用户路径

1. 前端以 `VITE_API_MODE=real` 启动。
2. 用户选择朋友场景，保留或输入朋友 Demo 文本：
   `今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。`
3. 前端调用 `POST /api/plan`，请求体中 `scenario` 为 `friends`，后端返回非空 `planId` 和 `status=processing`。
4. 前端连接 `GET /api/plan/{planId}/stream`。
5. 后端推送规划链路事件，至少包含 `heartbeat`、`state_change`、`tool_call`、`tool_result`、`plan_ready`。
6. `plan_ready.plan` 必须满足：
   - `scenario="friends"`。
   - `status="CONFIRM"`。
   - `timeline` 至少包含 `activity`、`cafe` 或 `dessert`、`restaurant` 三段。
   - `actions` 至少包含 `reserve_table` 和 `send_message`。
   - 文案不出现家庭 / 亲子专属话术，例如 `适合 5 岁儿童`。
7. 用户点击确认执行，前端调用 `POST /api/plan/{planId}/execute`，请求体为 `{ "confirmed": true }`。
8. 前端重新连接同一个 `planId` 的 SSE stream。
9. 后端推送 `CONFIRM -> EXECUTE`、每个 action 的 `execute_result`、`EXECUTE -> DONE` 和 `done`。
10. 前端 ExecutionTracker 显示 mock 确认号，StateSummaryPanel / LogPanel 显示 `DONE`。

## API / SSE 要求

- `POST /api/plan/{planId}/execute`
  - 只有 `CONFIRM` 状态且已有 packed plan 时允许执行。
  - `confirmed` 必须为 `true`，否则返回 `400 INVALID_INPUT`。
  - 非 `CONFIRM` 状态返回 `409 INVALID_STATE`。
  - 成功响应：

```json
{
  "planId": "plan_abc123",
  "status": "executing",
  "message": "开始执行，请关注右侧日志"
}
```

- `execute_result`
  - SSE event name 必须等于 `execute_result`。
  - payload 必须包含 `type`、`planId`、`actionId`、`actionType`、`status`、`confirmationNo`、`timestamp`。
  - friends happy path 至少覆盖 `reserve_table` 和 `send_message` 两类动作。

- `done`
  - SSE event name 必须等于 `done`。
  - payload 必须包含 `type`、`planId`、`summary`、`timestamp`。
  - 前端收到后进入 `DONE`，关闭或停止重连该执行流。

## 前端要求

- real mode 规划阶段收到 `plan_ready` 后，页面应停在可确认状态，不持续制造重连噪音。
- real mode 点击确认执行后，必须重新连接同一个 `planId` 的 SSE stream，消费真实后端 `execute_result` 和 `done`。
- 前端场景选择必须向后端发送 `scenario="friends"`，不得复用 family 请求体。
- mock mode 行为保持不变，继续使用 fixture 回放。
- UI 不新增调试控件，不暴露内部 key 或本机路径。

## 验收方式

Generator 开发侧准备检查：

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast`
- 需要时补跑 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast`

独立 evaluator 正式验收：

- 必须由独立 evaluator 子代理执行。
- 涉及前端 UI 的验收必须使用 Chrome DevTools MCP。
- evaluator 需要记录：
  - 后端 / 前端 fast verify 结果。
  - real mode 浏览器路径。
  - Chrome DevTools MCP snapshot / accessibility 证据。
  - console error / warn 检查。
  - network 中 `POST /api/plan`、`GET /api/plan/{planId}/stream`、`POST /api/plan/{planId}/execute` 和执行流证据。
  - 页面最终可见 `friends` 方案、咖啡 / 甜品或拍照点、`DONE`、`execute_result`、mock 确认号或执行完成计数。

## 失败阈值

- friends real mode 不能到达 `plan_ready`，失败。
- `plan_ready.plan.scenario` 不是 `friends`，失败。
- 方案缺少活动、中途咖啡 / 甜品或拍照休息点、餐厅任一核心段，失败。
- 点击确认执行没有调用真实 `POST /api/plan/{planId}/execute`，失败。
- 后端未发送 `execute_result` 或 `done`，失败。
- 前端没有消费真实执行 SSE 并更新 ExecutionTracker / DONE 状态，失败。
- 线缆字段出现 snake_case，失败。
- 没有独立 evaluator 证据，不得把 `INT-003` 标记为 `verified`。
