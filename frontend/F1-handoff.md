# F1 Handoff

## 当前状态

F1-001 已完成并验证。`frontend/` 现在是 Vue 3 + Vite + Pinia + Naive UI 前端骨架，`pnpm dev` 默认监听 `127.0.0.1:5173`。

当前前端只做骨架和本地状态预演：
- 已有首页工作台骨架。
- 已有家庭 / 朋友两个 Demo 场景切换。
- 已有自然语言输入、origin 输入、状态摘要和日志占位。
- 已接入 Pinia store。
- 已使用 Naive UI 组件。
- 暂未接入真实 API、fixture mode、SSE、PlanCard、ClarifyBubble、AdjustPanel 或 Playwright 测试。

## 已完成文件

- `docs/contracts/F1-001-vue-skeleton.md`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/index.html`
- `frontend/public/favicon.svg`
- `frontend/src/main.ts`
- `frontend/src/App.vue`
- `frontend/src/stores/planner.ts`
- `frontend/src/styles/main.css`
- `frontend/.gitignore`
- `frontend/README.md`

## 当前入口

- 应用入口：`frontend/src/main.ts`
- 页面入口：`frontend/src/App.vue`
- 状态入口：`frontend/src/stores/planner.ts`
- 样式入口：`frontend/src/styles/main.css`
- Vite 配置：`frontend/vite.config.ts`

## 开发命令

在 `frontend/` 下：

```powershell
pnpm install
pnpm dev
```

访问：

```text
http://127.0.0.1:5173
```

仓库根目录验证：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

## 已验证结果

F1-001 验证已通过：

```text
pnpm typecheck passed
Verify passed.
```

额外构建验证也通过：

```powershell
pnpm build
```

构建结果摘要：
- Vite production build 成功。
- 主 JS chunk 约 `290.51 kB`，gzip 后约 `91.94 kB`。

UI 冒烟验证：
- `pnpm dev` 已监听 `127.0.0.1:5173`。
- Playwright 打开页面可见 WeekendTravel 骨架。
- 点击“开始预演”后，Pinia 状态从 `START/idle` 变为 `INTENT/connecting`。
- console 无 error / warning。

## 环境注意

- 当前沙盒 shell 里直接运行 `.\verify.ps1 -Target frontend -Mode fast` 可能找不到宿主全局 `pnpm`。
- 已验证可用方式是用宿主 PowerShell：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target frontend -Mode fast
```

- Git status 可能提示：

```text
unable to access 'C:\Users\lx8nb/.config/git/ignore': Permission denied
```

这是宿主 Git 全局 ignore 权限警告，不影响前端验证。

## 下一步建议

下一个 F1 小目标应推进 `F1-002`：

1. 创建 `docs/contracts/F1-002-api-client-fixtures.md`。
2. 实现 API client，字段只使用 `docs/api-contract.md` 的 camelCase 契约。
3. 实现 mock fixture mode：
   - `docs/fixtures/plan-ready-family.json`
   - `docs/fixtures/plan-ready-friends.json`
   - `docs/fixtures/sse-events.jsonl`
4. 让验证覆盖 fixture 解析，至少保持 `.\verify.ps1 -Target frontend -Mode fast` 通过。

## F1 边界

- API 字段变更先改 `docs/api-contract.md`。
- 前端不得自定义 snake_case 兼容分支，除非 `docs/api-contract.md` 明确要求。
- 不在前端暴露真实 token、key 或个人路径。
- 后端未完成前，优先用 `docs/fixtures/` 做独立开发。
- Generator 完成开发后，测试阶段必须交给独立 evaluator 子代理执行；generator 自己跑的 typecheck、build、Playwright 或本地冒烟只能作为开发准备记录。
- 前端 evaluator 子代理必须同时使用 Playwright MCP 和 Chrome DevTools MCP：Playwright MCP 覆盖模拟交互、状态等待、截图或 trace；Chrome DevTools MCP 覆盖页面快照、console、network、DOM / accessibility 和视觉复核。
- 没有 evaluator 子代理验证证据，不要把 `feature_list.json` 中的任务标为 `verified`。
