# WeekendTravel Frontend

Vue 3 + Vite + Pinia + Naive UI 前端骨架。当前已完成 F1-001 和 F1-002，具备基础工作台、API client 和 mock fixture mode；完整 SSE 日志面板和业务组件由 F1-003 之后继续接入。

## 技术栈

- Vue 3
- Vite
- Tailwind CSS
- Naive UI
- Pinia
- SSE / EventSource
- 默认端口：`5173`

## 开发命令

```powershell
pnpm install
pnpm verify:fixtures
pnpm dev
```

访问地址：

```text
http://localhost:5173
```

## 验证命令

在仓库根目录运行：

```powershell
.\verify.ps1 -Target frontend -Mode fast
```

如果当前 PowerShell 执行策略拦截 `.ps1`，使用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

## 后续接入边界

- API 字段只以 `../docs/api-contract.md` 为准。
- 后端未完成前，优先使用 `../docs/fixtures/` 做独立开发；默认 API mode 为 `mock`，可用 `VITE_API_MODE=real` 切换真实后端。
- 不在前端暴露真实 token、key 或本机隐私路径。
