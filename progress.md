# Progress

## 2026-05-22 WF-002 前端 evaluator 浏览器 MCP 规则

### 已完成

- 新增 `docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`，明确前端 evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP。
- 更新 `docs/dev-workflow.md`，把前端 UI 验收从泛泛的浏览器 / Playwright 要求升级为双 MCP 要求。
- 更新 `docs/frontend-contract.md`，明确 Playwright MCP 负责模拟交互、状态等待、截图或 trace，Chrome DevTools MCP 负责页面快照、console、network、DOM / accessibility 和视觉复核。
- 更新 `docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`frontend/F1-handoff.md` 和 `docs/decision-log.md`，统一前端 evaluator 证据要求。
- 在 `feature_list.json` 新增 `WF-002`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Playwright MCP|Chrome DevTools MCP|截图|视觉|console|network|DOM|accessibility|trace`，确认入口、前端 contract、workflow、quality、模板、handoff、decision log、progress 和 feature metadata 均有覆盖。
- 独立 evaluator 子代理 Euler (`019e4db4-865e-7fe2-85ed-4dd5e5b6f3c2`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/frontend-contract.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-002-frontend-evaluator-browser-mcp.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`docs/initiallizer-agent-prompt.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：前端验收入口、QA 模板、handoff、decision log、progress 和 feature metadata 都已覆盖 Playwright MCP + Chrome DevTools MCP 双工具要求；`feature_list.json` 可解析；未发现冲突规则。
- QA 报告已写入 `docs/qa/WF-002-frontend-evaluator-browser-mcp.md`。

## 2026-05-22 WF-001 harness 测试职责调整

### 已完成

- 新增 `docs/contracts/WF-001-generator-subagent-testing.md`，把“generator 测试阶段必须使用 evaluator 子代理”作为本轮 workflow contract。
- 更新 `AGENTS.md`，把完成定义改为必须有独立 evaluator 子代理验证证据。
- 更新 `docs/dev-workflow.md`，将旧的 generator 自行最终验证流程改为 `Generator -> Handoff to Evaluator Subagent -> Evaluator Test -> Fix -> Evaluator Retest -> Handoff`。
- 更新 `docs/quality.md`，明确 generator 自己运行的 typecheck、build、脚本、curl 或冒烟只能作为开发准备记录，不能单独作为 `verified` 证据。
- 更新 `docs/contracts/_template.md` 和 `docs/qa/evaluator-template.md`，要求后续 contract 与 QA 报告记录 evaluator 子代理。
- 更新 `docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md` 和 `docs/initiallizer-agent-prompt.md`，统一新规则。
- 在 `feature_list.json` 新增 `WF-001`，并在 evaluator 子代理放行后标记为 `verified`。

### 验证记录

- Generator 开发侧准备检查：`feature_list.json` 可通过 `ConvertFrom-Json` 解析。
- Generator 开发侧准备检查：搜索 `Self Verify|小任务可以同一个 agent|Generator 负责实现和基础验证|Generator -> Self Verify`，只剩 `docs/contracts/WF-001-generator-subagent-testing.md` 的负例搜索命令命中，不是活跃规则。
- 独立 evaluator 子代理 Newton (`019e4da9-79c0-79a3-b7f1-c79f5e594c58`) 已完成只读验证并放行。
- Evaluator 验证范围包含：`AGENTS.md`、`docs/dev-workflow.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`docs/contracts/WF-001-generator-subagent-testing.md`、`docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md`、`docs/handoff.md`、`docs/decision-log.md`、`progress.md`、`feature_list.json`。
- Evaluator 结论：活跃规则已统一为“generator 完成开发后，测试阶段必须交给独立 evaluator 子代理”；没有发现仍允许 generator-only final testing 的活跃表述；`feature_list.json` 可解析。
- QA 报告已写入 `docs/qa/WF-001-generator-subagent-testing.md`。

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

## 2026-05-21 B1-001 health and CORS

### 已完成

- 在 `backend/` 内完成 Spring Boot + Maven 最小骨架落地。
- 新增全局 CORS 配置 `backend/src/main/java/com/weekendtravel/backend/config/WebConfig.java`，允许 `http://localhost:5173`。
- 新增 `GET /health` controller：`backend/src/main/java/com/weekendtravel/backend/controller/HealthController.java`。
- 新增健康检查响应 DTO：`backend/src/main/java/com/weekendtravel/backend/dto/HealthResponse.java`。
- 在 `backend/src/main/resources/application.properties` 中补充 `server.port=8000`。
- 恢复并保留测试目录 `backend/src/test/`，测试文件验证健康检查返回约定字段。
- 新增本轮 contract：`docs/contracts/B1-001-health-cors.md`。

### 验证记录

- `./backend/mvnw -f "backend/pom.xml" test` 通过，结果为 `BUILD SUCCESS`。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 现已通过；脚本已改为调用 `backend/mvnw.cmd`，不再依赖全局 `mvn`。
- `./backend/mvnw -f "backend/pom.xml" spring-boot:run` 成功启动服务，Tomcat 监听 `8000`。
- `curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 `HTTP/1.1 200`，并包含 `Access-Control-Allow-Origin: http://localhost:5173`，响应体包含 `status`、`service`、`timestamp`。

### 当前状态

- `feature_list.json` 中 `B1-001` 已按真实运行证据标记为 `verified`。
- 后端已不再是“未初始化”状态；下一优先任务应转到 `B1-002` SSE emitter and heartbeat 或 `B1-003` POST `/api/plan`。
- `verify.ps1` 已改为使用 Maven Wrapper，与当前 backend 工作流一致。


### 已补充

- 补读 2 页设计文档后，将候选排序公式、P0/P1/P2 约束分级、Mock API 覆盖范围、`bookOrOrder` 幂等键、`cancel_booking` 反向操作、正常场景 Plan B 触发率指标补入 docs。
- 在 `docs/decision-log.md` 记录了 20 vs 21 条 golden case、Plan B 指标口径、情侣模板是否纳入 Demo 的冲突处理。

### 保持不变

- Golden Case 仍以 21 条为准。
- Demo 场景仍只有 `family` 和 `friends`。
- API 字段名仍统一 camelCase；部分动作枚举值保留工具语义 lower_snake_case。

## 2026-05-21 F1-sprint1-generator

### 已完成

- 创建 `docs/contracts/F1-001-vue-skeleton.md`，明确本轮只交付 Vue 前端骨架，不接入真实 API / SSE / OpenAI。
- 在 `frontend/` scaffold Vue 3 + Vite + Pinia + Naive UI 项目，并保留 Tailwind CSS 入口以匹配前端技术栈。
- 配置 `vite.config.ts`：`server.port = 5173`、`strictPort = true`，`pnpm dev` 默认访问 `http://localhost:5173`。
- 创建 Pinia store `src/stores/planner.ts`，包含 `planId`、`agentState`、用户输入、场景、origin、SSE 状态占位和日志事件数组占位。
- 页面 `src/App.vue` 使用 Naive UI 组件渲染输入区、场景切换、状态摘要和日志占位，不调用后端。
- 通过宿主 npm 安装全局 `pnpm`，并在 `frontend/` 生成 `pnpm-lock.yaml`。
- 新增 `frontend/.gitignore`，忽略 `node_modules/`、`dist/` 和本地生成物。
- 新增根 `.gitignore`，忽略 Playwright MCP 本地快照目录 `.playwright-mcp/`。
- 将 `feature_list.json` 中 `F1-001` 标记为 `verified` 并写入验证证据。

### 验证记录

- 直接运行 `.\verify.ps1 -Target frontend -Mode fast` 时，当前沙盒 shell 找不到宿主全局 `pnpm`，脚本按预期失败在环境检查。
- 使用宿主 PowerShell 运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

结果：
- `pnpm typecheck` passed。
- `Verify passed.`
- 额外运行 `pnpm build` 通过，Vite 生产构建成功，主 JS chunk 约 290.51 kB / gzip 91.94 kB。
- 已启动 `pnpm dev`，监听 `127.0.0.1:5173`；Playwright 打开页面可见 WeekendTravel 骨架，点击“开始预演”后 Pinia 状态从 `START/idle` 变为 `INTENT/connecting`，console 无 error/warning。

### 下一步

- F1 下一步建议推进 `F1-002`：API client 和 mock fixture mode。
- 后端仍未 scaffold，`B1-001` 仍是后端方向的下一步。
