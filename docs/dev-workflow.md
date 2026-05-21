# Dev Workflow

## 每轮 Agent 工作循环

1. 读 `AGENTS.md`。
2. 按任务读取相关 docs，至少确认 `docs/api-contract.md` 是否影响本轮。
3. 查看 `feature_list.json`、`progress.md`、与本轮任务相关的 handoff：后端任务读 `backend/HANDOFF.md`，前端任务读 `frontend/F1-handoff.md`，联调或跨端任务两个都读。
4. 选择一个小目标，优先选 P0 且依赖已满足的任务。
5. 写或更新 `docs/contracts/*.md`，明确本轮目标和不做什么。
6. 实现最小可验证改动。
7. 运行验证命令，优先真实运行路径。
8. 把证据写回 `feature_list.json`、`progress.md`、与本轮任务相关的 handoff。后端任务写 `backend/HANDOFF.md`，前端任务写 `frontend/F1-handoff.md`，联调或跨端任务两个都写。

不要只在聊天里交接。重要结论必须写入仓库文件。

## 前端任务怎么启动

1. 读 `docs/frontend-contract.md`、`docs/api-contract.md` 和 `frontend/F1-handoff.md`。
2. 读 `docs/fixtures/`，确认 UI 能先用 fixture 开发。
3. 从 `feature_list.json` 选择一个 F1 小任务。
4. 写本轮 contract，例如 `docs/contracts/F1-003-sse-log-panel.md`。
5. 在 `frontend/` 内实现。
6. 跑：

```powershell
.\verify.ps1 -Target frontend -Mode fast
```

7. 如果涉及 UI，后续必须用真实浏览器或 Playwright 验证用户路径。

## 后端任务怎么启动

1. 读 `docs/backend-contract.md`、`docs/api-contract.md` 和 `backend/HANDOFF.md`。
2. 从 `feature_list.json` 选择一个 B1 或 B2 小任务。
3. 写本轮 contract，例如 `docs/contracts/B1-001-health-cors.md`。
4. 在 `backend/` 内实现。
5. 跑：

```powershell
.\verify.ps1 -Target backend -Mode fast
```

6. 如果涉及接口，补 API 级验证或 curl 证据。

## 联调任务怎么启动

1. 确认前后端都按同一版 `docs/api-contract.md`，并同时查看 `backend/HANDOFF.md` 和 `frontend/F1-handoff.md`。
2. 跑：

```powershell
.\verify.ps1 -Target backend -Mode fast
.\verify.ps1 -Target frontend -Mode fast
```

3. 启动后端 `http://localhost:8000` 和前端 `http://localhost:5173`。
4. 走真实用户路径：输入一句话 -> 建立 SSE -> 收到日志 -> 收到 `plan_ready` -> 点击确认 -> 收到 `done`。
5. Evaluator 写 `docs/qa/*.md` 报告，包含命令、截图或日志位置、通过/失败结论。

## Generator / Evaluator 方式

推荐流程：

```text
Planner / Contract -> Generator -> Self Verify -> Evaluator -> Fix -> Handoff
```

- Generator 负责实现和基础验证。
- Evaluator 负责像真实用户一样运行、点击、输入、检查 API、查看日志、写 QA 报告。
- 小任务可以同一个 agent 内部分阶段完成。
- 较大前后端联调建议单独起 evaluator。
- Evaluator 不应只读代码，必须尽量跑真实路径。

## 状态更新规则

`feature_list.json` 的状态只能是：

- `todo`
- `in_progress`
- `blocked`
- `verified`

只有有验证证据时才能改成 `verified`。如果只是创建了文件、写了代码、未验证，仍然不能标为 `verified`。
