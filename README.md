# WeekendTravel

WeekendTravel 是一个本地短时活动规划与执行 Agent Demo。用户输入一句自然语言，系统围绕半天或几个小时内的本地活动，完成意图理解、方案规划、可行性校验、异常重排，并输出可以确认执行的活动方案。

当前 Demo 只聚焦两个场景：

- `family`：亲子、近距离、低负担餐饮和路线约束。
- `friends`：多人出行、玩吃结合、拍照点和路线效率。

这个仓库是前后端一体的本地开发与验证仓库。所有外部能力先使用本地 Mock 数据和工具层模拟；当前不接入真实支付、真实预约、真实配送、真实地图或真实商家库存。

## 当前状态

当前已验证的状态以 [feature_list.json](feature_list.json)、[progress.md](progress.md) 和 [docs/handoff.md](docs/handoff.md) 为准。README 只做新同学或后续 agent 的入口导航。

截至当前文档更新：

- 工作流规则 `WF-001` / `WF-002` 已建立：generator 完成开发后，最终测试必须交给独立 evaluator 子代理；前端 UI 验收必须包含 Chrome DevTools MCP 证据，不再强制要求 Playwright MCP。
- 后端已具备 Spring Web MVC 骨架、`GET /health`、`POST /api/plan`、`GET /api/plan/{planId}/stream`、CORS、B2 本地 POI / Search / Route / Availability / Booking / Message / ScenarioFlags 工具能力。
- 前端已具备 Vue 3 工作台、API client、mock fixture mode、`InputPanel`、`useSSE` 和实时日志面板；默认以 mock fixture 回放开发，可通过 `VITE_API_MODE=real` 切到真实后端。
- 源码中已经有 B1 状态机相关实现和测试文件；是否可以作为已完成能力，仍以 `feature_list.json` 中对应条目的 evaluator 证据为准。

## 技术栈

后端：

- Java 17
- Spring Boot Web MVC
- Maven Wrapper
- `SseEmitter`
- 本地 JSON Mock 数据
- 默认端口：`8000`

前端：

- Vue 3
- Vite
- Tailwind CSS
- Naive UI
- Pinia
- SSE / EventSource
- 默认端口：`5173`

## 目录结构

```text
.
├── backend/                 # Java 后端：Controller、状态机、B2 工具层、本地 Mock 数据和测试
├── frontend/                # Vue 前端：工作台 UI、API client、fixture loader、SSE composable
├── docs/                    # 产品、架构、API 契约、开发流程、质量规则、contract 和 QA 证据
├── docs/contracts/          # 每个小目标的验收契约
├── docs/fixtures/           # 前端 mock mode 使用的计划和 SSE fixture
├── docs/qa/                 # evaluator 验证报告和截图证据
├── feature_list.json        # 结构化任务清单与 verified 证据
├── progress.md              # 按日期记录的开发进度和验证记录
├── init.ps1                 # 环境检查脚本，不做全局安装
└── verify.ps1               # 仓库级验证入口
```

## 开工前先读

如果你要继续开发，而不是只浏览项目，建议按顺序读：

1. [AGENTS.md](AGENTS.md)
2. [docs/product-spec.md](docs/product-spec.md)
3. [docs/architecture.md](docs/architecture.md)
4. [docs/api-contract.md](docs/api-contract.md)
5. [feature_list.json](feature_list.json)
6. [progress.md](progress.md)
7. 后端任务读 [backend/HANDOFF.md](backend/HANDOFF.md)
8. 前端任务读 [frontend/F1-handoff.md](frontend/F1-handoff.md)
9. 本轮任务对应的 `docs/contracts/*.md`

## 环境要求

- Windows PowerShell
- Java 17
- Node.js `>=20`
- pnpm `9.15.4` 或兼容版本

后端使用 `backend/mvnw.cmd`，通常不需要系统全局安装 Maven。前端依赖需要在 `frontend/` 下安装。

## 初始化检查

在仓库根目录运行：

```powershell
.\init.ps1 -Target all
```

如果当前 PowerShell 执行策略拦截 `.ps1`：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\init.ps1 -Target all
```

`init.ps1` 只检查环境和给出下一步提示，不会自动 scaffold，也不会安装全局依赖。

## 安装依赖

后端依赖由 Maven Wrapper 在测试或启动时解析：

```powershell
cd backend
.\mvnw.cmd test
```

前端依赖在 `frontend/` 下安装：

```powershell
cd frontend
pnpm install
```

## 本地运行

启动后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

后端默认监听：

```text
http://localhost:8000
```

启动前端 mock mode：

```powershell
cd frontend
pnpm dev
```

前端默认访问：

```text
http://127.0.0.1:5173
```

切换前端到真实后端 API mode：

```powershell
cd frontend
$env:VITE_API_MODE = 'real'
$env:VITE_API_BASE_URL = 'http://localhost:8000'
pnpm dev
```

mock mode 会读取 `docs/fixtures/`；real mode 会调用 `http://localhost:8000` 下的真实 REST / SSE 端点。

## 验证

仓库级快速验证：

```powershell
.\verify.ps1 -Target all -Mode fast
```

如果执行策略拦截：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast
```

只验证后端：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

只验证前端：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

当前 `fast` 模式会覆盖：

- 后端：`backend/mvnw.cmd test`
- 前端：`pnpm verify:fixtures` 和 `pnpm typecheck`

前端额外构建检查可在 `frontend/` 下运行：

```powershell
pnpm build
```

## API 主链路

正式 API 契约只以 [docs/api-contract.md](docs/api-contract.md) 为准。当前主链路是：

```text
POST /api/plan
  -> GET /api/plan/{planId}/stream
  -> plan_ready
```

当前已存在或规划中的端点包括：

- `GET /health`
- `POST /api/plan`
- `GET /api/plan/{planId}/stream`
- `POST /api/debug/scenario`
- `POST /api/plan/{planId}/execute`
- `POST /api/plan/{planId}/clarify`
- `PATCH /api/plan/{planId}/adjust`

注意：前端 client 已为 `execute`、`clarify`、`adjust` 预留调用入口，但后端公开端点是否已经完成，必须看 `feature_list.json`、对应 contract 和源码实现，不能只看 client 方法推断。

## 开发规则

- API、SSE、字段和状态变更必须先改 [docs/api-contract.md](docs/api-contract.md)，再同步前端类型、后端模型和 fixture。
- JSON 线缆字段统一使用 camelCase，例如 `planId`、`latencyMs`、`affectedSlots`。
- 默认端口不要随意改：后端 `8000`，前端 `5173`。
- OpenAI key 只能从 `OPENAI_API_KEY` 读取，不能硬编码进仓库。
- 初始化阶段不锁死 OpenAI SDK 版本或模型名；真正接入前必须重新核验官方 OpenAI developer docs / MCP，并写入 [docs/decision-log.md](docs/decision-log.md)。
- 后端规划决策必须留在状态机和规则代码里；LLM 只允许用于意图抽取和话术生成边界，不能决定 POI、状态转移或 Plan B 规则。
- 不为了测试简单而删减 SSE 事件。

## 交付规则

这个仓库按小目标推进，每一轮只做一个清楚的目标。完成一个功能时，需要同步：

- 实现代码或文档
- 本轮 contract
- generator 开发检查记录
- 独立 evaluator 子代理验证证据
- `feature_list.json`
- `progress.md`
- 对应 handoff

没有独立 evaluator 证据时，不要把 `feature_list.json` 中的任务标为 `verified`。

涉及前端 UI 的任务，evaluator 必须提供：

- Chrome DevTools MCP：交互、状态等待、页面快照、console、network、DOM / accessibility 或等价浏览器诊断

## 重要文档入口

- 产品说明：[docs/product-spec.md](docs/product-spec.md)
- 架构说明：[docs/architecture.md](docs/architecture.md)
- API 契约：[docs/api-contract.md](docs/api-contract.md)
- 后端边界：[docs/backend-contract.md](docs/backend-contract.md)
- 前端边界：[docs/frontend-contract.md](docs/frontend-contract.md)
- 开发流程：[docs/dev-workflow.md](docs/dev-workflow.md)
- 质量规则：[docs/quality.md](docs/quality.md)
- 决策记录：[docs/decision-log.md](docs/decision-log.md)
