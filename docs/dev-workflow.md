# Dev Workflow

## 每轮 Agent 工作循环

1. 读 `AGENTS.md`。
2. 按任务读取相关 docs，至少确认 `docs/api-contract.md` 是否影响本轮。
3. 查看 `feature_list.json`、`progress.md`、与本轮任务相关的 handoff：后端任务读 `backend/HANDOFF.md`，前端任务读 `frontend/F1-handoff.md`，联调或跨端任务两个都读。
4. 选择一个小目标，优先选 P0 且依赖已满足的任务。
5. 写或更新 `docs/contracts/*.md`，明确本轮目标和不做什么。
6. 实现最小可验证改动。
7. Generator 完成开发后，把改动摘要、待测命令、预期用户路径和风险点交给独立 evaluator 子代理。
8. Evaluator 子代理运行测试，优先真实运行路径，并写出命令、日志、截图或 QA 报告证据。
9. Generator 只能基于 evaluator 证据更新 `feature_list.json`、`progress.md`、与本轮任务相关的 handoff。后端任务写 `backend/HANDOFF.md`，前端任务写 `frontend/F1-handoff.md`，联调或跨端任务两个都写。

不要只在聊天里交接。重要结论必须写入仓库文件。

## 前端任务怎么启动

1. 读 `docs/frontend-contract.md`、`docs/api-contract.md` 和 `frontend/F1-handoff.md`。
2. 读 `docs/fixtures/`，确认 UI 能先用 fixture 开发。
3. 从 `feature_list.json` 选择一个 F1 小任务。
4. 写本轮 contract，例如 `docs/contracts/F1-003-sse-log-panel.md`。
5. 在 `frontend/` 内实现。
6. Generator 完成实现后，交给 evaluator 子代理跑：

```powershell
.\verify.ps1 -Target frontend -Mode fast
```

7. 如果涉及 UI，evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP 验证用户路径、模拟交互、截图视觉状态，并检查 console / network / DOM 或 accessibility 线索。

## 后端任务怎么启动

1. 读 `docs/backend-contract.md`、`docs/api-contract.md` 和 `backend/HANDOFF.md`。
2. 从 `feature_list.json` 选择一个 B1 或 B2 小任务。
3. 写本轮 contract，例如 `docs/contracts/B1-001-health-cors.md`。
4. 在 `backend/` 内实现。
5. Generator 完成实现后，交给 evaluator 子代理跑：

```powershell
.\verify.ps1 -Target backend -Mode fast
```

6. 如果涉及接口，evaluator 子代理补 API 级验证或 curl 证据。

## 联调任务怎么启动

1. 确认前后端都按同一版 `docs/api-contract.md`，并同时查看 `backend/HANDOFF.md` 和 `frontend/F1-handoff.md`。
2. 跑：

```powershell
.\verify.ps1 -Target backend -Mode fast
.\verify.ps1 -Target frontend -Mode fast
```

3. 启动后端 `http://localhost:8000` 和前端 `http://localhost:5173`。
4. 走真实用户路径：输入一句话 -> 建立 SSE -> 收到日志 -> 收到 `plan_ready` -> 点击确认 -> 收到 `done`。
5. Evaluator 子代理写 `docs/qa/*.md` 报告，包含命令、截图或日志位置、通过/失败结论；如涉及前端 UI，报告必须分别记录 Playwright MCP 和 Chrome DevTools MCP 证据。

## Generator / Evaluator 方式

推荐流程：

```text
Planner / Contract -> Generator -> Handoff to Evaluator Subagent -> Evaluator Test -> Fix -> Evaluator Retest -> Handoff
```

- Generator 负责实现、记录改动范围、列出建议测试命令和风险点。
- Generator 可以做开发内准备检查，例如编译、typecheck、格式检查或启动服务，目的是尽早发现明显破损；这些结果不能单独作为 `verified` 证据。
- 所有 generator agent 在完成开发后，测试阶段必须委托独立 evaluator 子代理执行，不能自己兼任最终测试者。
- Evaluator 子代理负责像真实用户一样运行、点击、输入、检查 API、查看日志、写 QA 报告。
- 前端 evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP：Playwright MCP 负责用户路径、模拟交互和截图，Chrome DevTools MCP 负责页面快照、console、network、DOM / accessibility 和必要的视觉复核。
- 前端 evaluator 截图后必须视觉检查截图检查是否出错，不能只看截图位置。
- 小任务也不能由同一个 generator agent 自测后直接标记完成；如果无法启动 evaluator 子代理，任务状态只能保持 `todo` / `in_progress` / `blocked`，不能标为 `verified`。
- 修复循环中，Generator 根据 evaluator 反馈修复；修完后仍必须交回 evaluator 子代理复测。
- Evaluator 不应只读代码，必须尽量跑真实路径。

## 状态更新规则

`feature_list.json` 的状态只能是：

- `todo`
- `in_progress`
- `blocked`
- `verified`

只有有 evaluator 子代理验证证据时才能改成 `verified`。如果只是创建了文件、写了代码，或者只有 generator 自己运行过命令，仍然不能标为 `verified`。
