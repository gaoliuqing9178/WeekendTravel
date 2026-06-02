# Contract: F1-009 Map-centric UI redesign

## 本轮目标

- 按 `docs/NewUi.md` 重做 F1 前端主界面，把旧的“开发者后台式双栏”替换为“地图舞台 + 行程抽屉”。
- 将状态机代码名翻译成用户能感知的行动阶段，例如理解需求、寻找地点、校验可行性、方案出炉、执行预订。
- 保留已 verified 的 F1 / INT 行为链路：输入、CLARIFY、Plan B、微调、确认执行、ExecutionTracker、DONE / DEGRADE / FAILED 可见入口。

## 明确不做

- 不修改 `docs/api-contract.md` 的字段、事件名、枚举或错误结构。
- 不新增后端 API、真实地图 SDK、真实第三方地图 key 或 OpenAI 调用。
- 不新增 snake_case 兼容层。
- 不把 generator 自测当作最终 `verified` 证据。

## UI 要求

- 首屏必须以地图为中心，而不是传统左右分栏。
- 地图区域必须可视化当前位置、候选 POI、路线、搜索半径和 Plan B 替换提示；没有真实地图 SDK 时可以使用本地 CSS / SVG 示意地图。
- 规划内容必须放在抽屉式面板中；桌面端可吸附右侧，移动端必须表现为底部抽屉。
- 方案生成前显示骨架时间轴；方案生成后显示横向滚动时间轴卡片。
- Agent 日志不能只像开发日志列表，必须有“Agent 思考进程”气泡，同时保留 `role=log` 与自动滚动能力。
- 反问必须继续以 2-3 个可点击选项呈现。
- Plan B 必须在地图、方案卡和状态总览中可见。
- 一键执行按钮文案必须面向用户；执行后能看到动作完成进度和确认号。
- 视觉必须响应式，移动端不出现横向页面滚动、文本重叠或主要控件被遮挡。
- 动画必须尊重 `prefers-reduced-motion`，并优先使用 transform / opacity / SVG stroke 等低布局成本属性。

## API / 状态要求

- 前端仍然只消费 `docs/api-contract.md` 中已有字段。
- `plan_ready`、`adjust_result`、`execute_result`、`done`、`error` 的 store 行为不能回退。
- mock mode 不访问真实 `/api/plan/*` 后端端点。
- real mode 的 `POST /api/plan`、`POST /api/plan/{planId}/clarify`、`PATCH /api/plan/{planId}/adjust`、`POST /api/plan/{planId}/execute` 调用入口必须保留。

## 验收方式

Evaluator 子代理：

- 独立读取本 contract、`docs/NewUi.md`、`docs/api-contract.md`、`docs/frontend-contract.md`、`frontend/F1-handoff.md`、`frontend/src/App.vue`、`frontend/src/components/MapStage.vue`、`PlanCard.vue`、`LogPanel.vue`、`InputPanel.vue`、`ConfirmButton.vue`、`ExecutionTracker.vue` 和 `frontend/src/styles/main.css`。
- 独立运行前端 fast verify。
- 确认 `pnpm build` 通过或记录无法运行原因。
- 确认没有新增禁止的 snake_case 字段兼容层。

前端浏览器验收：

- 本轮涉及前端 UI，evaluator 子代理必须使用 Chrome DevTools MCP。
- Chrome DevTools MCP 证据必须覆盖：
  - 打开 `http://127.0.0.1:5173`。
  - mock mode 提交家庭 demo 输入。
  - 回答 ClarifyBubble 后等待 `CONFIRM`。
  - 页面 snapshot / accessibility 中可见地图舞台、规划抽屉、Plan B、横向行程卡、Agent 思考进程、终态轨道。
  - 点击确认执行后可见 `DONE`、执行完成进度和 mock 确认号。
  - console error / warn 为 0。
  - mock mode network 不访问真实 `/api/plan/*` 后端端点。
  - 移动视口下底部抽屉和输入区不遮挡、无横向页面滚动。

手动命令：

```powershell
.\verify.ps1 -Target frontend -Mode fast
```

补充命令：

```powershell
npm run build
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" frontend\src frontend\scripts docs\fixtures
```

## 证据位置

- Generator 补充截图：
  - `docs/qa/F1-redesign-generator-devtools-mock.png`
  - `docs/qa/F1-redesign-generator-mobile-top.png`
- Evaluator QA 报告写入：
  - `docs/qa/F1-009-map-centric-ui-redesign.md`
- Evaluator Chrome DevTools MCP 截图写入：
  - `docs/qa/F1-009-devtools-mock.png`
  - `docs/qa/F1-009-devtools-mobile.png`
- Generator 追加真实后端 smoke：
  - `docs/qa/F1-009-real-backend-smoke.md`
  - `docs/qa/F1-009-devtools-real.png`
  - `docs/qa/F1-009-real-planning-initial.network-response`
  - `docs/qa/F1-009-real-planning-after-clarify.network-response`
  - `docs/qa/F1-009-real-execution.network-response`

## 失败阈值

- 页面仍然以旧双栏开发者后台为主，失败。
- 地图舞台或抽屉式行程面板缺失，失败。
- `CLARIFY`、Plan B、确认执行、DONE 任一已 verified 的前端路径回退，失败。
- 移动端主要输入或执行控件被遮挡，失败。
- mock mode 访问真实 `/api/plan/*`，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 前端 UI 任务缺少独立 evaluator 的 Chrome DevTools MCP 证据，不能标为 `verified`。
