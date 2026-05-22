# Contract: WF-001 Generator 测试阶段子代理化

## 本轮目标

- 优化 WeekendTravel harness 文档，明确所有 generator agent 完成开发后，测试阶段必须委托独立 evaluator 子代理执行。
- 明确 generator 自己运行的本地冒烟、typecheck、build、curl、Playwright 或脚本结果只能作为开发准备记录，不能单独作为 `verified` 证据。
- 将该规则同步到入口、工作流、质量、模板、前后端 handoff 和长期记录，避免后续 agent 只读某一个入口时漏掉。

## 明确不做

- 不修改前后端业务代码。
- 不改变 API 字段、端口、SSE 事件或产品范围。
- 不重算既有已验证功能的历史证据。
- 不把本轮 workflow 文档优化伪装成业务功能完成。

## 用户路径

1. 下一个 generator agent 读取 `AGENTS.md` 和 `docs/dev-workflow.md`。
2. Generator 完成实现后，把改动摘要、待测命令、预期用户路径和风险点交给 evaluator 子代理。
3. Evaluator 子代理执行测试并写证据。
4. Generator 只能基于 evaluator 证据更新 `feature_list.json` 状态、`progress.md` 和相关 handoff。

## UI 要求

- 本轮不涉及 UI 改动。

## API / 后端要求

- 本轮不涉及 API 或后端实现改动。

## 数据和状态要求

- `feature_list.json` 需要新增 workflow 类任务记录，用于追踪 harness 规则优化。
- 该任务只有在 evaluator 子代理验证文档一致性后才能标为 `verified`。

## 验收方式

Evaluator 子代理：

- 由 generator 之外的 evaluator 子代理检查本轮文档 diff。
- 验证 `AGENTS.md`、`docs/dev-workflow.md`、`docs/quality.md`、QA 模板、contract 模板、前后端 handoff 均包含“测试阶段必须使用 evaluator 子代理”的一致规则。
- 验证 `feature_list.json` 仍可被 JSON parser 解析。

手动路径：

- 搜索旧的 generator 自行最终验证表述，以及 `Generator`、`Evaluator`、`子代理` 等关键字，确认不存在与新规则冲突的活跃 harness 表述。

自动验证：

- `Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json`
- `rg -n "小任务可以同一个 agent|Generator 负责实现和基础验证" AGENTS.md docs backend frontend`
- `rg -n "evaluator 子代理|测试阶段必须|Generator 自己" AGENTS.md docs backend frontend`

API / 日志 / 截图证据：

- 本轮不需要 API、日志或截图。
- 需要记录 evaluator 子代理的文档检查结论。

## 失败阈值

- 任一活跃 harness 文档仍允许 generator 自己完成最终测试，失败。
- `feature_list.json` 无法解析，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
