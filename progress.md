# Progress

## 2026-05-21 Initializer

### 已完成

- 读取根目录产品文档、外部 harness 总结材料、当前仓库状态和已有 `docs/initiallizer-agent-prompt.md`。
- 创建短入口 `AGENTS.md`。
- 创建长期 docs：
  - `docs/product-spec.md`
  - `docs/architecture.md`
  - `docs/api-contract.md`
  - `docs/frontend-contract.md`
  - `docs/backend-contract.md`
  - `docs/dev-workflow.md`
  - `docs/quality.md`
  - `docs/decision-log.md`
  - `docs/contracts/_template.md`
  - `docs/qa/evaluator-template.md`
- 创建前端 fixture：
  - `docs/fixtures/sse-events.jsonl`
  - `docs/fixtures/plan-ready-family.json`
  - `docs/fixtures/plan-ready-friends.json`
- 创建结构化任务清单 `feature_list.json`。
- 创建 `backend/README.md` 和 `frontend/README.md`，明确尚未 scaffold。
- 创建 `init.ps1` 和 `verify.ps1` 的 Windows harness 脚本。

### 关键决策

- 后端默认端口统一为 `8000`。
- API 和 SSE 线缆字段统一使用 camelCase。
- 前端先用 fixture 开发，后端先用本地 Mock API 开发。
- OpenAI 接入暂不锁 SDK 版本或模型名；真正实现前必须重新核验官方 OpenAI developer docs / MCP。
- 本轮不 scaffold Spring Boot 或 Vue 项目，不把空目录伪装为已完成项目。

### 当前状态

- 仓库已经具备长期 agent 开发 harness。
- 前后端业务代码尚未初始化。
- `feature_list.json` 中功能状态仍为 `todo`，因为没有业务功能通过验证。

### 验证记录

- 回读 `AGENTS.md` 和 `docs/api-contract.md`，中文显示正常。
- `feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-family.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/plan-ready-friends.json` 可通过 `ConvertFrom-Json` 解析。
- `docs/fixtures/sse-events.jsonl` 共 23 行，逐行可通过 `ConvertFrom-Json` 解析。
- 直接运行 `.\init.ps1 -Target all` 和 `.\verify.ps1 -Target all -Mode fast` 被当前 PowerShell 执行策略拦截，错误类型为 `PSSecurityException / UnauthorizedAccess`。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all` 成功运行：Java、Node、pnpm 可见，Maven 缺失；backend/frontend 均明确提示尚未初始化。
- 使用 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast` 成功运行脚本逻辑并返回失败：`backend\pom.xml` 缺失、`frontend\package.json` 缺失。这是当前真实未初始化状态，不是通过。
- `git status --short` 可运行，当前文件均为未跟踪；Git 同时输出 `unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied` 权限警告，未阻塞本轮初始化。

## 2026-05-21 设计文档补读

### 已补充

- 补读 2 页设计文档后，将候选排序公式、P0/P1/P2 约束分级、Mock API 覆盖范围、`bookOrOrder` 幂等键、`cancel_booking` 反向操作、正常场景 Plan B 触发率指标补入 docs。
- 在 `docs/decision-log.md` 记录了 20 vs 21 条 golden case、Plan B 指标口径、情侣模板是否纳入 Demo 的冲突处理。

### 保持不变

- Golden Case 仍以 21 条为准。
- Demo 场景仍只有 `family` 和 `friends`。
- API 字段名仍统一 camelCase；部分动作枚举值保留工具语义 lower_snake_case。
