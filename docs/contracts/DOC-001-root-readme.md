# Contract: DOC-001 Root README

## 本轮目标

- 在仓库根目录新增 `README.md`，作为 WeekendTravel 的项目入口文档。
- README 需要说明项目目标、Demo 场景边界、技术栈、目录结构、运行命令、验证命令和关键开发规则。
- README 需要指向真实长期文档入口，包括 `docs/api-contract.md`、`feature_list.json`、`progress.md`、`docs/handoff.md`、`backend/HANDOFF.md` 和 `frontend/F1-handoff.md`。
- README 需要明确当前能力状态以 `feature_list.json` 和 evaluator 证据为准，避免把未 verified 的能力写成已完成。

## 明确不做

- 不修改前端或后端业务代码。
- 不修改 API 契约字段、端口、状态机或 fixture。
- 不把任何未 verified 条目标记为已完成。
- 不运行前端浏览器 UI 验收；本轮是文档入口任务，不涉及 UI 行为变更。

## 用户路径

1. 新同学或后续 agent 打开仓库根目录。
2. 先阅读 `README.md`，理解项目用途、技术栈、端口、运行方式和验证方式。
3. 按 README 指引继续阅读 contract、handoff、progress 和 feature metadata。

## UI 要求

- 不适用。本轮不改前端 UI。

## API / 后端要求

- 不适用。本轮不改 API 或后端实现。

## 数据和状态要求

- `feature_list.json` 必须保持可解析。
- README 对当前状态的描述必须与 `feature_list.json` 保持一致。
- README 不应自行声明未 verified 条目已完成；具体能力是否完成以 `feature_list.json` 的当前状态和 evaluator 证据为准。

## 验收方式

Evaluator 子代理：

- 独立 evaluator 子代理只读复核 `README.md` 和 `feature_list.json`。
- 确认 README 包含项目目标、family/friends Demo 范围、前后端技术栈、端口、目录结构、初始化/安装/运行/验证命令。
- 确认 README 引用关键入口文档。
- 确认 README 未把未 verified 的能力写成已验证完成。
- 确认 README 说明 camelCase、端口边界、`OPENAI_API_KEY`、evaluator 子代理和前端 Chrome DevTools MCP 验收规则。
- 确认 `feature_list.json` 仍可解析。

前端浏览器验收：

- 不适用。本轮不涉及前端 UI 变更。

手动路径：

- 读取 `README.md`。
- 搜索关键标题、命令和规则。
- 解析 `feature_list.json`。

自动验证：

```powershell
Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json
rg -n "^# WeekendTravel|^## 当前状态|verify\.ps1|VITE_API_MODE|docs/api-contract\.md|evaluator|Playwright MCP|Chrome DevTools MCP" README.md
```

API / 日志 / 截图证据：

- 不适用。本轮无 API 或 UI 截图要求。

## 失败阈值

- 根目录没有 `README.md`，失败。
- README 缺少项目目标、运行方式或验证方式，失败。
- README 误称任何 `todo` 条目已 verified，失败。
- README 没有指向 `docs/api-contract.md` 或 `feature_list.json`，失败。
- `feature_list.json` 不能解析，失败。
- 没有独立 evaluator 子代理验证证据，不能标记为 `verified`。
