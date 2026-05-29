# Decision Log

## 2026-05-21 初始化阶段

### D-001 后端默认端口统一为 8000

- 决策：后端默认使用 `8000`。
- 原因：产品文档中同时出现过 `8000` 和 `8080`，但接口边界和联调清单都写 `localhost:8000`。
- 影响：`docs/api-contract.md`、`backend/README.md`、`init.ps1`、`verify.ps1`、联调说明都按 `8000` 编写。
- 如果未来变更：必须先更新本文件和 `docs/api-contract.md`，再同步前后端配置。

### D-002 JSON 线缆字段统一使用 camelCase

- 决策：正式 API 和 SSE 线缆字段统一使用 camelCase。
- 示例：`planId`、`latencyMs`、`affectedSlots`、`replanCount`、`actionId`、`actionType`、`confirmationNo`。
- 原因：产品文档 API 示例里出现 snake_case，但数据模型章节强调前后端 JSON 使用 camelCase。为避免前后端各猜一套，初始化阶段把正式联调契约统一为 camelCase。
- 影响：`docs/api-contract.md` 和 `docs/fixtures/` 均使用 camelCase。原始产品文档里的 snake_case 只作为历史示例，不作为实现依据。

### D-003 前端先用 fixture，后端先用 Mock API

- 决策：前端可脱离后端，以 `docs/fixtures/sse-events.jsonl`、`plan-ready-family.json`、`plan-ready-friends.json` 开发 UI；后端可脱离前端，以 `docs/api-contract.md` 和本地 JSON Mock 数据开发 API。
- 原因：F1、B1、B2 可以并行推进，减少互相等待。
- 影响：前端不得临时硬写 API 字段，后端不得为了单测删减 SSE 事件。

### D-004 OpenAI 接入暂不锁 SDK 版本和模型名

- 决策：初始化阶段只记录 LLM 使用边界：只用于意图抽取和话术生成，API key 从 `OPENAI_API_KEY` 读取，不硬编码。
- 原因：产品文档里的 OpenAI SDK 版本和模型名属于会随时间变化的信息，不能在未核验官方文档前写死。
- 影响：真正实现 OpenAI 调用前，必须使用官方 OpenAI developer docs / MCP 重新核验 SDK、模型和调用方式，并把决策追加到本文件。

### D-005 本轮不 scaffold 前后端项目

- 决策：本轮只创建长期开发 harness、契约、fixture、README 和脚本，不创建 Spring Boot 或 Vue 业务代码。
- 原因：Initializer 的目标是让仓库可接力、可验证、可拆分开发，不是一次性实现完整产品。
- 影响：`verify.ps1` 会明确报告 `backend not initialized yet` 或 `frontend not initialized yet`，不会伪装成通过。

### D-006 Plan B 指标拆成两个口径

- 决策：保留“异常注入场景 Plan B 触发准确率 100%”，同时补充“正常非注入场景 Plan B 触发率小于等于 30%”。
- 原因：完整产品文档和 initializer 要求强调异常注入必须触发 Plan B；补充设计文档强调 Plan B 触发率过高说明 Plan A 召回质量不足。两者不是同一个指标。
- 影响：QA 统计时必须区分 injected Plan B accuracy 和 normal Plan B rate。

### D-007 Golden Case 数量以 21 条为准

- 决策：正式 QA 清单仍按 21 条：家庭 7 条、朋友 7 条、边界 7 条。
- 原因：完整产品文档和 initializer 要求均明确 21 条；补充设计文档中的“20 条 golden case”视为较早或压缩口径。
- 影响：`feature_list.json` 的 `QA-001` 不改为 20 条。

### D-008 当前 Demo 场景仍只有家庭和朋友

- 决策：当前接口 `scenario` 仍只允许 `family` 和 `friends`。
- 原因：补充设计文档的时间骨架里提到“情侣”模板，但产品核心 Demo 场景和 initializer 要求都只包含家庭、朋友。
- 影响：不新增 `couple` 场景，不扩展前后端任务清单；如后续产品范围扩大，必须先更新 `docs/product-spec.md`、`docs/api-contract.md` 和 golden case。

## 2026-05-22 Harness 测试职责调整

### D-009 Generator 测试阶段必须委托 evaluator 子代理

- 决策：所有 generator agent 完成开发后，测试阶段必须委托独立 evaluator 子代理执行。Generator 自己运行的本地冒烟、typecheck、build 或脚本结果只能作为开发准备记录，不能单独作为 `verified` 证据。
- 原因：WeekendTravel 是长期接力项目，测试者和实现者必须分离，避免 generator 为了完成任务而弱化验收、漏测用户路径或把“能编译”当成“已验证”。
- 影响：`docs/dev-workflow.md`、`docs/quality.md`、QA 模板、contract 模板、AGENTS 入口和 handoff 均按该规则更新。后续任何功能若没有 evaluator 子代理证据，只能保持 `todo` / `in_progress` / `blocked`，不能标为 `verified`。

### D-010 前端 evaluator 浏览器验收使用 Chrome DevTools MCP

- 决策：所有涉及前端 UI 的任务，evaluator 子代理只需要使用 Chrome DevTools MCP 做浏览器验收；不再强制要求 Playwright MCP。Chrome DevTools MCP 负责用户路径、模拟交互、状态等待、页面快照、DOM / accessibility、console、network 和视觉复核。
- 原因：Playwright MCP 与 Chrome DevTools MCP 在用户路径、交互和截图验证上存在较多重叠；保留 Chrome DevTools MCP 可以同时覆盖交互与浏览器诊断，降低验收重复成本。
- 影响：前端任务的 QA 报告必须记录 Chrome DevTools MCP 证据。缺少 Chrome DevTools MCP 证据时，前端任务不能标记为 `verified`；Playwright MCP 可作为补充证据，但不是必需项。
