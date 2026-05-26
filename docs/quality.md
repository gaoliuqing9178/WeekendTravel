# Quality

## 验证优先级

1. 真实运行路径：像真实用户一样输入、点击、确认、查看 UI 和日志。
2. API 链路：真实 HTTP 请求、SSE 流、错误响应、异常注入。
3. 单测：状态机、Tool、Mock service、fixture 解析、组件逻辑。
4. 静态检查：typecheck、lint、格式、依赖边界。

不能只读代码后宣布完成。

## 完成定义

一个功能只有同时满足以下条件，才能标记为 `verified`：

- 实现已经落地到对应目录。
- 验收路径已经由独立 evaluator 子代理执行。
- 有命令输出、API 输出、截图、日志或 QA 报告作为证据。
- `feature_list.json` 的 `evidence` 已更新。
- `progress.md` 和对应 handoff 已更新。后端任务更新 `backend/HANDOFF.md`，前端任务更新 `frontend/F1-handoff.md`，联调或跨端任务两个都更新。

没有 evaluator 子代理验证证据不能标记完成。Generator 自己运行的本地冒烟、typecheck、build 或脚本结果只能证明开发准备情况，不能单独作为 `verified` 证据。

## 技术债扫描命令

```powershell
rg -n "TODO|FIXME|HACK|XXX|TEMP|console\.log|debugger|ts-ignore|eslint-disable|any\b|skip\(|only\(" .
Get-ChildItem -Recurse -File | Sort-Object Length -Descending | Select-Object -First 20 FullName,Length
git log --stat --since="30 days ago"
```

如果 `rg` 不可用，使用 PowerShell 原生命令替代搜索，但不要因此跳过扫描。

## 常见失败模式

- UI 显示成功，但没有真实 API 调用。
- 后端单测通过，但 SSE 没有推送必须事件。
- 前端为了赶进度写死 fixture 字段，联调时字段不一致。
- 后端把原始产品文档里的 snake_case 示例当成正式契约。
- 未核验官方文档就写死 OpenAI SDK 版本或模型名。
- 未初始化的子项目被脚本误报为通过。

## Evaluator 要求

Evaluator 必须由 generator 之外的子代理担任。Evaluator 报告必须尽量包含真实运行证据：

- evaluator 子代理身份或执行入口。
- generator 提供的改动摘要和待测范围。
- 环境和命令。
- 用户路径。
- 通过证据。
- 失败复现。
- 截图、日志或 API 输出位置。
- 放行或不放行结论。

如果测试阶段没有独立 evaluator 子代理参与，即使命令在 generator 手里通过，也不能放行。

## 前端 Evaluator 浏览器要求

涉及前端 UI 的任务，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP：

- Playwright MCP 用于真实用户路径、模拟交互、状态等待、截图或 trace。
- Chrome DevTools MCP 用于页面快照、DOM / accessibility 检查、console 错误、network 请求和视觉复核。
- QA 报告必须分别记录两类 MCP 的证据。缺少任一类 MCP 证据时，前端任务不能标记为 `verified`。
- 截图视觉检查至少关注文本溢出、遮挡、错位、空白页面、关键按钮状态和核心业务内容是否可见。
