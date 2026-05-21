# WeekendTravel Frontend

Vue 3 + Vite + Pinia + Naive UI 前端骨架。当前完成的是 F1-001，后续 API client、fixture mode、SSE 和业务组件由 F1-002 之后继续接入。

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
- 后端未完成前，优先使用 `../docs/fixtures/` 做独立开发。
- 不在前端暴露真实 token、key 或本机隐私路径。
