# Contract: B1-001 Health and CORS

## 本轮目标

- 完成 `backend/` 的 Spring Boot + Maven 最小骨架。
- 实现 `GET /health`，返回 `docs/api-contract.md` 约定的 `status`、`service`、`timestamp`。
- 通过全局 `WebMvcConfigurer` 放通 `http://localhost:5173`。
- 2026-05-26 F1-004 real mode 补充放通 `http://127.0.0.1:5173`，与当前前端 dev server 默认监听地址保持一致。
- 将后端默认端口固定为 `8000`。

## 明确不做

- 不实现 `POST /api/plan`。
- 不实现 SSE stream、heartbeat、`state_change`。
- 不实现 `POST /api/debug/scenario`。
- 不实现状态机、Planner、Tool 层、Mock POI 数据或 LLM 接入。

## 用户路径

1. 启动后端服务。
2. 访问 `GET http://localhost:8000/health`。
3. 收到 200 响应和契约字段。
4. 前端从 `http://localhost:5173` 或 `http://127.0.0.1:5173` 发请求时不会被后端 CORS 拦截。

## UI 要求

- 本轮无 UI 改动。

## API / 后端要求

- `backend/pom.xml` 存在，并使用 Java 17 + Spring Boot 3。
- `GET /health` 返回 200。
- 响应 JSON 使用 camelCase 字段：`status`、`service`、`timestamp`。
- 全局 CORS 配置允许 `http://localhost:5173` 和 `http://127.0.0.1:5173`。
- 服务监听端口为 `8000`。

## 数据和状态要求

- `status` 固定返回 `ok`。
- `service` 固定返回 `WeekendTravel`。
- `timestamp` 返回 Unix epoch milliseconds。

## 验收方式

手动路径：

- 启动后端后请求 `GET /health`，确认返回 200 和约定 JSON 字段。
- 带 `Origin: http://localhost:5173` 请求 `/health`，确认响应头允许该 origin。
- 带 `Origin: http://127.0.0.1:5173` 请求 `/health`，确认响应头允许该 origin。

自动验证：

- `./backend/mvnw -f "backend/pom.xml" test`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast`

API / 日志 / 截图证据：

- 2026-05-21：`./backend/mvnw -f "backend/pom.xml" test` 通过，结果为 `BUILD SUCCESS`。
- 2026-05-21：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "./verify.ps1" -Target backend -Mode fast` 通过，脚本已改为调用 `backend/mvnw.cmd`。
- 2026-05-21：`curl -i -H "Origin: http://localhost:5173" http://localhost:8000/health` 返回 `HTTP/1.1 200`，响应头包含 `Access-Control-Allow-Origin: http://localhost:5173`，响应体包含 `status`、`service`、`timestamp`。
- 2026-05-26：F1-004 real mode 验收补充确认 `POST /api/plan` 支持 `http://localhost:5173` 与 `http://127.0.0.1:5173` 两个 Origin；`PlanControllerCreateTests.createAllowsConfiguredFrontendOrigins` 已覆盖两个 Origin 的 CORS 回显。

## 失败阈值

- 核心用户路径不可用，失败。
- UI 显示成功但 API 或状态没有真实变化，失败。
- 前后端字段与 `docs/api-contract.md` 不一致，失败。
- 只实现占位或 mock 却标记正式完成，失败。
- 验证命令未运行或无证据，不能标为 `verified`。
