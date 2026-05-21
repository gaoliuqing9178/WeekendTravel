# Handoff

## 当前状态

Initializer 已把仓库整理成长期 agent 开发 harness。当前仍是初始化阶段，没有 scaffold 后端 Spring Boot 项目，也没有 scaffold 前端 Vue 项目。

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

- `backend/` 还没有 `pom.xml` 和 Spring Boot 源码。
- `frontend/` 还没有 `package.json` 和 Vue 源码。
- 还没有真实 API、SSE、状态机、Tool、POI 数据、UI 组件或 Playwright 测试。
- `feature_list.json` 中所有功能仍是 `todo`，没有业务功能被标为 `verified`。

## 最高优先级下一步

后端优先：

1. 创建 `docs/contracts/B1-001-health-cors.md`。
2. scaffold Java 17 + Spring Boot 3 + Maven 项目。
3. 实现 `GET /health` 和 CORS。
4. 跑 `.\verify.ps1 -Target backend -Mode fast`，补充 API 验证证据。

前端优先：

1. 创建 `docs/contracts/F1-001-vue-skeleton.md`。
2. scaffold Vue 3 + Vite + Pinia + Naive UI 项目。
3. 配置 `pnpm dev` 默认端口 `5173`。
4. 跑 `.\verify.ps1 -Target frontend -Mode fast`。

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

- `init.ps1` 成功运行：Java、Node、pnpm 可见；Maven 缺失；backend/frontend 均提示尚未初始化。
- `verify.ps1` 成功运行脚本逻辑并返回失败：`backend\pom.xml` 缺失、`frontend\package.json` 缺失。这是当前真实未初始化状态，不是通过。
- `git status --short` 可运行，当前文件均为未跟踪；同时 Git 输出 `unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied`，这是宿主 Git 全局 ignore 读取权限警告，未阻塞本轮初始化。

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
