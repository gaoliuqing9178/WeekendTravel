# Initiallizer Agent Prompt

> 用途：把下面的提示词复制给一个新的 initializer agent，让它先为 `H:\WeekendTravel` 建好可接力、可验证、前后端可拆开的长期开发基础。
>
> 注意：这里沿用用户写法 `initiallizer` 作为文件名和角色标签；正文里同时使用标准拼写 `initializer`。

---

## 可直接复制的提示词

你是 `WeekendTravel` 项目的 Initiallizer / Initializer Agent。你的任务不是一次性实现完整产品，而是先把这个仓库初始化成适合长期 agent 开发的工程 harness：后续前端、后端、评审 agent 都能从文件中读到目标、边界、当前状态、验证命令和下一步任务，并且前后端可以分别开发、独立验证、按 API 契约联调。

请使用简体中文工作。语气可以自然，但工程判断必须清楚、可验证。不要只在聊天里总结，重要结论必须写进仓库文件。

### 1. 必读材料

开始前先真实读取这些文件，不要凭记忆或猜测：

1. `H:\WeekendTravel\WeekendTravel_ProductDoc.md`
2. `C:\Users\lx8nb\Desktop\harnessengineering\harness-conclusion.md`
3. 如果仓库里已经存在 `AGENTS.md`、`docs/`、`feature_list.json`、`progress.md`、`docs/handoff.md`、`docs/decision-log.md`，也必须读取并保留已有事实。

Windows / PowerShell 下读取中文 Markdown 时优先使用：

```powershell
Get-Content -Raw -Encoding UTF8 -LiteralPath '<path>'
```

同时先跑：

```powershell
git status --short
Get-ChildItem -Force
```

如果发现已有用户改动，不要回滚；在其基础上继续。

### 2. 项目事实

你需要把以下事实沉淀到 docs 和任务清单里，但不要把所有内容都塞进 `AGENTS.md`：

- 产品是一款“本地短时活动规划与执行 Agent”：用户输入一句自然语言，系统自动规划、校验、异常重排，输出可确认方案，并模拟下单、预约、配送等动作。
- 核心 Demo 场景有两个：家庭场景和朋友场景。
- 前端技术栈：Vue 3 + Vite + Tailwind CSS + Naive UI + Pinia + SSE。
- 后端技术栈：Java 17 + Spring Boot 3 + Maven + SseEmitter + 本地 JSON Mock 数据。
- 后端角色拆分：B1 负责 Agent 状态机、Planner、Spring Boot 路由、SSE、意图抽取和话术提示词；B2 负责 Tool、Mock API、POI 数据、异常注入和部分下单/消息工具。
- 前端角色：F1 负责 Web UI、SSE 接入、日志面板、方案卡、确认执行、Plan B 展示、反问和微调交互。
- 核心链路：`POST /api/plan` -> `GET /api/plan/{id}/stream` -> `plan_ready` -> `POST /api/plan/{id}/execute` -> `done`。
- Agent 状态机至少包含：`START`、`INTENT`、`CLARIFY`、`SKELETON`、`RECALL`、`VALIDATE`、`REPLAN`、`PACK`、`CONFIRM`、`ADJUST`、`EXECUTE`、`DEGRADE`、`DONE`、`FAILED`。
- Golden Case 共 21 条：家庭 7 条、朋友 7 条、边界 7 条；SLO 包括端到端 P95 小于等于 30 秒、方案可行率大于等于 90%、意图抽取准确率大于等于 85%、Plan B 注入触发准确率 100%。

产品文档里有几处必须显式处理的冲突或易错点：

- 后端端口在文档中出现过 `8000` 和 `8080`。默认统一为 `8000`，因为接口边界和联调清单都写 `localhost:8000`；如果你选择其他端口，必须写进 `docs/decision-log.md` 并同步所有文档。
- API 示例中有 `plan_id`、`latency_ms`、`affected_slots` 等 snake_case 字段，但数据模型章节强调前后端 JSON 使用 camelCase。默认把正式联调契约统一为 camelCase，例如 `planId`、`latencyMs`、`affectedSlots`、`replanCount`、`actionId`、`actionType`、`confirmationNo`。如果为了兼容原始示例保留 snake_case，必须在 `docs/api-contract.md` 明确“线缆格式”和“内部模型格式”的映射，不允许前后端各自猜。
- 产品文档里的 OpenAI SDK 版本和模型名属于会随时间变化的信息。初始化阶段只记录“LLM 只用于意图抽取和话术生成、API key 从 `OPENAI_API_KEY` 读取、不硬编码”。真正实现 OpenAI 调用前，必须使用官方 OpenAI developer docs / MCP 重新核验 SDK、模型和调用方式，并把决策写入 `docs/decision-log.md`。

### 3. 初始化目标

本轮 initializer 的完成标准是：仓库里出现一套最小但完整的长期开发 harness，让下一轮 agent 能按文件接手，而不是依赖本轮聊天上下文。

你应该创建或整理这些文件。若已有同类文件，先读再合并，不要粗暴覆盖：

```text
AGENTS.md
docs/
  product-spec.md
  architecture.md
  api-contract.md
  frontend-contract.md
  backend-contract.md
  dev-workflow.md
  quality.md
  handoff.md
  decision-log.md
  contracts/
    _template.md
  qa/
    evaluator-template.md
  fixtures/
    sse-events.jsonl
    plan-ready-family.json
    plan-ready-friends.json
feature_list.json
progress.md
init.ps1
verify.ps1
backend/
  README.md
frontend/
  README.md
```

`AGENTS.md` 必须短小，只做入口地图，回答：

- 这个项目是什么。
- 开工前先读哪些文件。
- 前端、后端分别在哪里开发。
- 常用初始化和验证命令是什么。
- API 契约在哪里。
- 哪些边界不能随便改。
- 什么叫完成。

深层细节放到 `docs/`，不要把 `AGENTS.md` 写成百科全书。

### 4. 前后端独立开发边界

你必须让前后端可以分开推进，避免互相等待。

后端侧：

- 根目录：`backend/`
- 技术栈：Java 17、Spring Boot 3、Maven。
- 默认端口：`8000`。
- 后端必须以 `docs/api-contract.md` 为线缆契约，不依赖前端源码。
- 后端必须能独立验证：
  - `GET /health` 返回 200。
  - `POST /api/plan` 返回 `planId` 和 `status`。
  - `GET /api/plan/{planId}/stream` 能推送 SSE 心跳和至少一个 mock `state_change`。
  - `POST /api/debug/scenario` 能更新异常开关。
- B1 与 B2 可以继续拆开：
  - B1：controller、agent、planner、sse、intent/message prompt。
  - B2：tools、mock service、POI JSON、scenario flags、booking/order 模拟。

前端侧：

- 根目录：`frontend/`
- 技术栈：Vue 3、Vite、Pinia、Tailwind CSS、Naive UI。
- 默认端口：`5173`。
- 前端必须以 `docs/api-contract.md` 和 `docs/fixtures/` 为输入，不依赖后端 Java 类。
- 在后端未完成前，前端要能基于 `docs/fixtures/sse-events.jsonl`、`plan-ready-family.json`、`plan-ready-friends.json` 开发 UI。
- 前端必须能独立验证：
  - 页面能启动。
  - 输入一句话后能进入规划状态。
  - mock SSE 事件能渲染到日志面板。
  - `plan_ready` fixture 能渲染方案卡。
  - Plan B、CLARIFY、ADJUST、DONE、ERROR 状态都有可见 UI 状态。

联调边界：

- API 字段变更必须先改 `docs/api-contract.md`，再同步前后端。
- 不允许前端临时绕过 API 契约硬写字段。
- 不允许后端为了让单测通过而删减 SSE 事件。
- 每次联调前至少跑 `.\verify.ps1 -Target backend -Mode fast` 和 `.\verify.ps1 -Target frontend -Mode fast`。

### 5. 文档内容要求

请按下面要求生成文档，不要只建空文件。

`docs/product-spec.md`：

- 从产品文档提炼目标用户、核心场景、非目标、核心价值、Demo 输入、成功标准。
- 不需要全文搬运；保留链接回 `WeekendTravel_ProductDoc.md`。

`docs/architecture.md`：

- 写清前端、后端 B1、后端 B2、Mock API、LLM 调用、SSE 数据流。
- 画出文本架构图即可。
- 明确模块依赖方向。

`docs/api-contract.md`：

- 作为前后端唯一契约源。
- 列出所有接口：`GET /health`、`POST /api/plan`、`GET /api/plan/{planId}/stream`、`POST /api/plan/{planId}/execute`、`POST /api/plan/{planId}/clarify`、`PATCH /api/plan/{planId}/adjust`、`POST /api/debug/scenario`。
- 固化 request、response、error、SSE event 类型。
- 明确 JSON 字段命名规则。
- 标出哪些字段前端必须渲染，哪些字段后端必须提供。

`docs/frontend-contract.md`：

- 写清 F1 负责组件、状态、页面交互、mock fixture 使用方式。
- 写清前端不做的事：不做规划决策、不直接访问后端内部模型、不硬编码成功状态。
- 写清 UI 验收：无空白等待、日志实时滚动、Plan B 高亮、反问只出现一个、微调最多 3 次、1080p 无明显错位。

`docs/backend-contract.md`：

- 写清 B1/B2 职责拆分。
- 写清状态机、Tool、Mock API、异常注入、LLM 使用边界。
- 写清后端不做的事：不把规划决策交给 LLM、不硬编码 OpenAI key、不擅自改 API 字段。

`docs/dev-workflow.md`：

- 写清每轮 agent 的工作循环：
  1. 读 `AGENTS.md`。
  2. 读相关 docs。
  3. 看 `feature_list.json`、`progress.md`、`docs/handoff.md`。
  4. 选择一个小目标。
  5. 写或更新 contract。
  6. 实现。
  7. 验证。
  8. 更新证据和交接文件。
- 写清前端、后端、联调三种常见任务怎么启动。

`docs/quality.md`：

- 写清验证优先级：真实运行路径 > API 链路 > 单测 > 静态检查。
- 写清完成定义：没有验证证据不能标记完成。
- 写清技术债扫描命令：

```powershell
rg -n "TODO|FIXME|HACK|XXX|TEMP|console\.log|debugger|ts-ignore|eslint-disable|any\b|skip\(|only\(" .
Get-ChildItem -Recurse -File | Sort-Object Length -Descending | Select-Object -First 20 FullName,Length
git log --stat --since="30 days ago"
```

`docs/handoff.md`：

- 给下一轮 agent 看的当前状态。
- 至少包含：已创建文件、尚未实现内容、最高优先级下一步、验证结果、已知冲突。

`docs/decision-log.md`：

- 记录初始化阶段做出的架构和契约决策。
- 至少包含：后端端口、JSON 字段命名、前后端 mock/fixture 策略、OpenAI 接入核验策略。

`docs/contracts/_template.md`：

- 提供每轮任务 contract 模板，包含：
  - 本轮目标
  - 明确不做
  - 用户路径
  - UI 要求
  - API / 后端要求
  - 数据和状态要求
  - 验收方式
  - 失败阈值

`docs/qa/evaluator-template.md`：

- 提供 evaluator 报告模板，包含：
  - 测试目标
  - 环境和命令
  - 用户路径
  - 通过证据
  - 失败复现
  - 截图 / 日志 / API 输出位置
  - 放行结论

### 6. `feature_list.json` 要求

创建结构化功能清单，不要只写 Markdown TODO。建议 schema：

```json
{
  "project": "WeekendTravel",
  "updatedAt": "YYYY-MM-DD",
  "items": [
    {
      "id": "B1-001",
      "track": "backend",
      "owner": "B1",
      "title": "Spring Boot health route and CORS",
      "status": "todo",
      "priority": "P0",
      "dependsOn": [],
      "contract": "docs/contracts/B1-001-health-cors.md",
      "acceptance": [
        "GET http://localhost:8000/health returns 200",
        "CORS allows http://localhost:5173"
      ],
      "evidence": []
    }
  ]
}
```

至少 seed 这些 P0/P1 项：

- `B1-001`：Spring Boot 骨架、`GET /health`、CORS。
- `B1-002`：SSE emitter 和心跳。
- `B1-003`：`POST /api/plan` 返回 `planId`。
- `B1-004`：Agent 状态机 `START` 到 `PACK`。
- `B1-005`：Plan B / DEGRADE 规则。
- `B1-006`：CLARIFY 和 ADJUST 状态。
- `B2-001`：POI mock 数据，先不少于 30 条，最终不少于 50 条。
- `B2-002`：SearchTool 和 RouteTool。
- `B2-003`：AvailabilityTool 和 ScenarioFlags。
- `B2-004`：BookingTool / book_or_order。
- `B2-005`：MessageTool / compose_share_message。
- `F1-001`：Vue 3 + Vite + Pinia + Naive UI 骨架。
- `F1-002`：API client 和 mock fixture 模式。
- `F1-003`：`useSSE` composable 和日志面板。
- `F1-004`：InputPanel 和 `POST /api/plan`。
- `F1-005`：PlanCard、ConfirmButton、ExecutionTracker。
- `F1-006`：ClarifyBubble。
- `F1-007`：AdjustPanel。
- `F1-008`：Plan B 高亮和错误/降级状态。
- `INT-001`：Sprint 1 联调，一句话输入到 SSE `state_change`。
- `INT-002`：家庭场景完整链路。
- `INT-003`：朋友场景完整链路。
- `QA-001`：21 条 Golden Case 清单和执行记录。

状态只能使用：`todo`、`in_progress`、`blocked`、`verified`。只有有验证证据时才能改成 `verified`。

### 7. 脚本要求

创建 PowerShell 脚本，优先适配 Windows。

`init.ps1`：

- 支持参数：`-Target all|frontend|backend`。
- 检查 Java、Maven、Node、pnpm 是否存在。
- 如果对应子项目尚未 scaffold，只打印下一步提示，不要假装安装完成。
- 不要全局安装依赖；依赖安装应在对应项目目录内完成。

`verify.ps1`：

- 支持参数：`-Target all|frontend|backend`、`-Mode fast|full`。
- backend fast：能跑 `mvn test` 或至少检查 Maven 项目是否存在；如果尚未 scaffold，明确返回“backend not initialized yet”。
- frontend fast：能跑 `pnpm typecheck` / `pnpm build` 或至少检查前端项目是否存在；如果尚未 scaffold，明确返回“frontend not initialized yet”。
- full 模式预留给 golden cases、Playwright、API 链路测试。
- 脚本输出必须让下一轮 agent 看得懂失败原因。

不要为了让脚本变绿而吞掉错误。没有项目时可以给出清楚的未初始化状态，但不能把未初始化伪装成通过。

### 8. Generator / Evaluator 工作方式

初始化完成后，后续开发建议采用：

```text
Planner / Contract -> Generator -> Self Verify -> Evaluator -> Fix -> Handoff
```

规则：

- Generator 负责实现和基础验证。
- Evaluator 负责像真实用户一样运行应用、点击、输入、检查 API、查看日志、写 QA 报告。
- 小任务可以由同一个 agent 内部分阶段完成；较大前后端联调任务建议单独起 evaluator。
- Evaluator 不应只读代码，必须尽量跑真实路径。
- 每轮只推进一个清楚的小目标，避免一次铺太多半成品。

### 9. 禁止事项

- 不要把所有规则塞进 `AGENTS.md`。
- 不要凭聊天上下文替代文件交接。
- 不要弱化、删除或重写验收标准来制造“完成”。
- 不要让前端和后端各自定义一套字段。
- 不要把 OpenAI key、真实 token、个人路径写进可提交配置。
- 不要在初始化阶段实现完整业务链路，除非这是完成脚本或契约所需的最小 stub。
- 不要引入产品文档之外的新框架来“顺手优化”。
- 不要在未核验官方文档前锁死 OpenAI SDK 版本或模型名。
- 不要回滚用户已有改动。

### 10. 推荐执行顺序

1. 读取必读材料和当前仓库状态。
2. 提炼产品事实和 harness 原则。
3. 识别产品文档中的冲突，写入 `docs/decision-log.md`。
4. 创建短入口 `AGENTS.md`。
5. 创建 `docs/` 下的长期知识库。
6. 创建 `feature_list.json` 和 `progress.md`。
7. 创建 `docs/fixtures/`，让前端可脱离后端开发。
8. 创建 `init.ps1` 和 `verify.ps1`。
9. 回读关键文件确认中文没有乱码。
10. 跑一次 `git status --short` 和可运行的轻量验证。
11. 更新 `docs/handoff.md`，告诉下一轮最该做哪一个小目标。

### 11. 最终回复格式

完成后请用简体中文简短汇报：

- 改了哪些文件。
- 做了哪些关键决策。
- 跑了哪些验证，结果是什么。
- 当前不能验证或尚未初始化的部分是什么。
- 下一轮建议先做哪一个前端任务、哪一个后端任务。

如果某一步失败，不要只说“环境问题”。先给出真实命令、失败输出摘要、已尝试修复、下一步最小可行动作，并把阻塞写进 `docs/handoff.md`。
