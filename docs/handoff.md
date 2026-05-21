# Handoff

## 2026-05-21 F1-sprint1-generator 更新

F1-001 已完成并验证。`frontend/` 现在已经是 Vue 3 + Vite + Pinia + Naive UI 项目，`pnpm dev` 默认端口为 `5173`，`feature_list.json` 中 `F1-001` 已标记为 `verified`。

本轮新增或更新：
- `docs/contracts/F1-001-vue-skeleton.md`
- `.gitignore`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/index.html`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/styles/main.css`
- `frontend/public/favicon.svg`
- `frontend/.gitignore`
- `frontend/README.md`

验证命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

验证结果：`pnpm typecheck` passed，`Verify passed.` 直接在当前沙盒 shell 运行 `.\verify.ps1 -Target frontend -Mode fast` 会因为找不到宿主全局 `pnpm` 失败；宿主 PowerShell 路径已验证通过。

额外构建验证：`pnpm build` 通过，Vite 生产构建成功，主 JS chunk 约 290.51 kB / gzip 91.94 kB。

UI 冒烟验证：`pnpm dev` 已启动并监听 `127.0.0.1:5173`；Playwright 打开页面可见 WeekendTravel 骨架，点击“开始预演”后 Pinia 状态从 `START/idle` 变为 `INTENT/connecting`，console 无 error/warning。

下一步建议：
- F1：推进 `F1-002`，实现 API client 和 mock fixture mode。
- B1：推进 `B1-002`，实现 SSE emitter and heartbeat。

## 当前状态

Initializer 已把仓库整理成长期 agent 开发 harness。前端 F1-001 Vue 骨架已 scaffold 并通过验证；后端已经完成 B1-001 的最小初始化：Spring Boot + Maven 项目已存在，`GET /health`、全局 CORS 和基础测试已落地，默认端口已固定为 `8000`。

## 已创建文件

- `AGENTS.md`
- `docs/product-spec.md`
- `docs/architecture.md`
- `docs/api-contract.md`
- `docs/frontend-contract.md`
- `docs/backend-contract.md`
- `docs/dev-workflow.md`
- `docs/quality.md`
- `docs/handoff.md`
- `docs/decision-log.md`
- `docs/contracts/_template.md`
- `docs/contracts/B1-001-health-cors.md`
- `docs/contracts/F1-001-vue-skeleton.md`
- `docs/qa/evaluator-template.md`
- `docs/fixtures/sse-events.jsonl`
- `docs/fixtures/plan-ready-family.json`
- `docs/fixtures/plan-ready-friends.json`
- `feature_list.json`
- `progress.md`
- `init.ps1`
- `verify.ps1`
- `backend/README.md`
- `frontend/README.md`

已有文件已保留：

- `WeekendTravel_ProductDoc.md`
- `docs/initiallizer-agent-prompt.md`

## 尚未实现内容

- 后端还没有 `POST /api/plan`、SSE stream、`POST /api/debug/scenario`、状态机、Tool、POI 数据或真实业务链路。
- `frontend/` 已有 Vue/Vite/Pinia/Naive UI 骨架，但还没有 API client、fixture mode、真实 SSE、业务组件或完整 Playwright 自动化覆盖。
- 还没有真实 API、SSE、状态机、Tool、POI 数据或端到端联调。
- 当前 `B1-001` 和 `F1-001` 已具备验证证据并标记为 `verified`；其余功能仍按各自验证证据推进。

## 最高优先级下一步

后端优先：

1. 进入 `B1-002`，实现 `GET /api/plan/{planId}/stream` 的最小 SSE emitter 和 heartbeat。
2. 进入 `B1-003`，实现 `POST /api/plan` 返回 `planId` 和 `status`。
3. 按 `docs/dev-workflow.md` 继续为每轮任务补 contract、验证证据和 handoff。

前端优先：

1. 创建 `docs/contracts/F1-002-api-client-fixtures.md`。
2. 实现 API client，字段只使用 `docs/api-contract.md` 的 camelCase 契约。
3. 实现 mock fixture mode，读取 `docs/fixtures/*.json` 和 `docs/fixtures/*.jsonl`。
4. 跑 `./verify.ps1 -Target frontend -Mode fast` 或等效 PowerShell Bypass 命令，补充 fixture 解析或 build/typecheck 证据。

## 验证结果

已运行回读和轻量验证：

```powershell
Get-Content -Raw -Encoding UTF8 -LiteralPath 'AGENTS.md'
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\api-contract.md'
Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\fixtures\plan-ready-family.json' | ConvertFrom-Json
Get-Content -Raw -Encoding UTF8 -LiteralPath 'docs\fixtures\plan-ready-friends.json' | ConvertFrom-Json
Get-Content -Encoding UTF8 -LiteralPath 'docs\fixtures\sse-events.jsonl' | ForEach-Object { $_ | ConvertFrom-Json | Out-Null }
```

结果：

- `AGENTS.md`、`docs/api-contract.md` 中文回读正常。
- `feature_list.json` 可解析。
- 两个 `plan-ready` fixture 可解析。
- `sse-events.jsonl` 共 23 行，逐行可解析。

脚本验证：

```powershell
.\init.ps1 -Target all
.\verify.ps1 -Target all -Mode fast
```

当前 PowerShell 执行策略直接拦截 `.ps1`，错误类型为 `PSSecurityException / UnauthorizedAccess`。已用一次性方式验证脚本逻辑：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast
```

结果：

- `init.ps1` 成功运行：Java、Node、pnpm 可见；初始化阶段 Maven 缺失，backend/frontend 均提示尚未初始化。
- `verify.ps1` 在 initializer 阶段曾返回失败：当时 `backend\pom.xml` 缺失、`frontend\package.json` 缺失。
- 2026-05-21 已完成 B1-001 后续验证：`./backend/mvnw -f "backend/pom.xml" test` 通过；`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 通过；`./backend/mvnw -f "backend/pom.xml" spring-boot:run` 可启动后端；`curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 200，且包含 `Access-Control-Allow-Origin: http://localhost:5173`。
- `verify.ps1` 已改为调用 `backend/mvnw.cmd`，backend verify 不再依赖系统 `mvn` 在 PATH 中。
- F1-001 已通过 frontend fast verify、`pnpm build` 和 Playwright 冒烟验证。
- `git status --short` 可运行；Git 仍可能输出全局 ignore 权限警告，但未阻塞本地开发。

## 已知冲突和处理

- 产品文档中后端端口同时出现 `8000` 和 `8080`。本仓库契约统一为 `8000`，见 `docs/decision-log.md`。
- 产品文档 API 示例出现 snake_case，但数据模型强调 camelCase。本仓库正式 API 和 SSE 线缆字段统一为 camelCase，见 `docs/api-contract.md`。
- 产品文档里的 OpenAI SDK 版本和模型名可能随时间变化。本轮不锁死；真正接入前必须核验官方 OpenAI developer docs / MCP，并追加 `docs/decision-log.md`。
- 补充设计文档提到 20 条 golden case，但本仓库正式 QA 仍以 21 条为准，见 `docs/decision-log.md`。
- 补充设计文档提到正常场景 Plan B 触发率小于等于 30%，本仓库已与“异常注入 Plan B 触发准确率 100%”拆成两个指标。
- 补充设计文档提到“情侣”时间骨架模板，但当前 Demo 场景仍只有 `family` 和 `friends`，不扩接口枚举。

## 接手提醒

- 每轮只推进一个清楚小目标。
- API 字段变更先改 `docs/api-contract.md`。
- 没有验证证据，不要把 `feature_list.json` 状态改成 `verified`。
- 当前已完成并验证的最小任务是 `B1-001` 和 `F1-001`；继续推进时优先按依赖顺序做 `B1-002`、`B1-003`、`F1-002`。
- `verify.ps1` 已与 Maven Wrapper 工作流对齐，可直接用于 backend fast verify。
