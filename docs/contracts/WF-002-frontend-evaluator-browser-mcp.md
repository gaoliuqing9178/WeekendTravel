# Contract: WF-002 前端 evaluator 浏览器 MCP 验收规则

## 本轮目标

- 明确所有前端 UI 任务的 evaluator 子代理只需要使用 Chrome DevTools MCP 做浏览器验收。
- 要求前端 evaluator 用 Chrome DevTools MCP 覆盖模拟交互、截图或快照视觉检查、console / network / DOM 或 accessibility 检查。
- 将该规则同步到 workflow、quality、前端 contract、contract 模板、QA 模板、handoff 和 feature metadata。

## 明确不做

- 不修改前端业务代码。
- 不引入新的 UI 测试框架或改变 `verify.ps1` 行为。
- 不替代后续具体 F1 / INT 任务的真实验收；本轮只规定验收方式。

## 用户路径

1. 前端 generator 完成实现后，交给独立 evaluator 子代理。
2. Evaluator 子代理用 Chrome DevTools MCP 跑用户路径，模拟输入、点击、状态变化和必要截图。
3. Evaluator 子代理继续用 Chrome DevTools MCP 检查页面快照、console、network、DOM 或 accessibility，并按需要补视觉确认。
4. Evaluator 把 Chrome DevTools MCP 证据写入 QA 报告；缺少该证据时，前端任务不能标记为 `verified`。

## UI 要求

- 前端 evaluator 必须至少覆盖一个真实或 fixture 驱动的用户交互路径。
- 涉及 UI 的任务必须有截图或明确的视觉检查记录。
- 视觉检查必须关注文本溢出、遮挡、错位、空白页面、按钮状态和关键业务区域是否可见。

## API / 后端要求

- 本轮不涉及 API 或后端实现改动。
- 联调任务仍需按 `docs/api-contract.md` 检查真实 API / SSE 行为。

## 数据和状态要求

- `feature_list.json` 需要新增 workflow 类任务记录，用于追踪该前端 evaluator 规则。
- 后续前端 QA 报告必须记录 Chrome DevTools MCP 证据。

## 验收方式

Evaluator 子代理：

- 由 generator 之外的 evaluator 子代理检查本轮文档 diff。
- 验证 `docs/dev-workflow.md`、`docs/frontend-contract.md`、`docs/quality.md`、`docs/contracts/_template.md`、`docs/qa/evaluator-template.md`、`frontend/F1-handoff.md`、`docs/handoff.md` 均包含前端 evaluator 必须使用 Chrome DevTools MCP 的要求，且不再强制要求 Playwright MCP。
- 验证 `feature_list.json` 仍可被 JSON parser 解析。

手动路径：

- 搜索 `Chrome DevTools MCP`、`截图`、`视觉`、`console`、`network` 等关键字，确认规则覆盖前端测试入口和报告模板。

自动验证：

- `Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json`
- `rg -n "Chrome DevTools MCP|截图|视觉|console|network" AGENTS.md docs/dev-workflow.md docs/frontend-contract.md docs/quality.md docs/contracts/_template.md docs/qa/evaluator-template.md frontend/F1-handoff.md docs/handoff.md feature_list.json README.md`

API / 日志 / 截图证据：

- 本轮不需要真实 UI 截图，因为没有前端界面改动。
- 需要记录 evaluator 子代理的文档检查结论。

## 失败阈值

- 任一前端验收入口仍只要求泛泛的浏览器，而没有要求 Chrome DevTools MCP，失败。
- 任一活跃前端验收规则仍强制要求 Playwright MCP，失败。
- QA 模板无法记录 Chrome DevTools MCP 证据，失败。
- `feature_list.json` 无法解析，失败。
- 测试阶段没有独立 evaluator 子代理参与，失败。
- 只有 generator 自己运行验证命令或没有证据，不能标为 `verified`。
