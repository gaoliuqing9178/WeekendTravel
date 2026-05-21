# WeekendTravel Frontend

前端根目录，当前尚未 scaffold Vue 项目。

## 技术栈

- Vue 3
- Vite
- Tailwind CSS
- Naive UI
- Pinia
- SSE / EventSource
- 默认端口：`5173`

## 开工前读

1. `../AGENTS.md`
2. `../docs/frontend-contract.md`
3. `../docs/api-contract.md`
4. `../docs/fixtures/`
5. `../feature_list.json`
6. `../docs/handoff.md`

## 独立开发方式

后端未完成前，前端必须能用这些 fixture 开发：

- `../docs/fixtures/sse-events.jsonl`
- `../docs/fixtures/plan-ready-family.json`
- `../docs/fixtures/plan-ready-friends.json`

fixture 字段已经按 `docs/api-contract.md` 统一为 camelCase。

## 最小 Sprint 1 验收

- 页面能启动。
- 输入一句话后进入规划状态。
- mock SSE 事件能渲染到日志面板。
- `plan_ready` fixture 能渲染方案卡。
- Plan B、CLARIFY、ADJUST、DONE、ERROR 状态都有可见 UI 状态。

## 验证命令

```powershell
..\verify.ps1 -Target frontend -Mode fast
```

当前没有 `package.json`，所以脚本应明确报告 `frontend not initialized yet`。
