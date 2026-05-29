# WeekendTravel Agent 入口

## 项目是什么

WeekendTravel 是一个“本地短时活动规划与执行 Agent”。用户输入一句自然语言，系统规划短时活动、校验可行性、异常时自动重排，输出可确认方案，并模拟下单、预约、配送、转发消息等执行动作。

核心 Demo 场景只有两个：家庭场景、朋友场景。

## 开工前先读

1. `docs/product-spec.md`
2. `docs/architecture.md`
3. `docs/api-contract.md`
4. `feature_list.json`
5. `progress.md`
6. 与本轮任务相关的 handoff：后端读 `backend/HANDOFF.md`，前端读 `frontend/F1-handoff.md`，联调或跨端任务两个都读
7. 与本轮任务相关的 `docs/*-contract.md` 或 `docs/contracts/*.md`

## 开发位置

- 前端：`frontend/`，技术栈为 Vue 3 + Vite + Tailwind CSS + Naive UI + Pinia + SSE，默认端口 `5173`。
- 后端：`backend/`，技术栈为 Java 17 + Spring Boot 3 + Maven + SseEmitter + 本地 JSON Mock 数据，默认端口 `8000`。
- 契约和长期知识库：`docs/`。

## 常用命令

```powershell
.\init.ps1 -Target all
.\verify.ps1 -Target backend -Mode fast
.\verify.ps1 -Target frontend -Mode fast
.\verify.ps1 -Target all -Mode full
```

如果当前 PowerShell 执行策略拦截 `.ps1`，用一次性方式运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target all -Mode fast
```

## API 契约

前后端唯一线缆契约是 `docs/api-contract.md`。字段变更必须先改契约，再同步前后端和 fixture。

## 不能随便改的边界

- 后端默认端口是 `8000`，前端默认端口是 `5173`。
- JSON 线缆字段统一使用 camelCase，例如 `planId`、`latencyMs`、`affectedSlots`。
- OpenAI key 只能从 `OPENAI_API_KEY` 读取，不能硬编码。
- 初始化阶段不锁死 OpenAI SDK 版本或模型名；真正接入前必须重新核验官方 OpenAI developer docs / MCP，并写入 `docs/decision-log.md`。
- 前端不得绕过 `docs/api-contract.md` 自定义字段；后端不得为了测试删减 SSE 事件。

## 什么叫完成

每轮只推进一个清楚的小目标。完成必须同时满足：实现落地、由独立 evaluator 子代理完成测试并留下证据、`feature_list.json` 状态和证据同步、`progress.md` 与对应 handoff 更新。后端任务更新 `backend/HANDOFF.md`，前端任务更新 `frontend/F1-handoff.md`，联调或跨端任务两个都更新。

所有 generator agent 在完成开发后，测试阶段必须委托独立 evaluator 子代理执行；generator 自己运行的本地冒烟、typecheck、build 或脚本结果只能作为开发准备记录，不能单独作为 `verified` 证据。没有 evaluator 子代理验证证据的功能不能标为 `verified`。

涉及前端 UI 的任务，evaluator 子代理只需要使用 Chrome DevTools MCP 做浏览器验收。Chrome DevTools MCP 负责模拟交互、状态等待、页面快照、console、network、DOM / accessibility 和视觉复核；不再强制要求 Playwright MCP 证据。缺少 Chrome DevTools MCP 证据时，前端任务不能标为 `verified`。
