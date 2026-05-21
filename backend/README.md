# WeekendTravel Backend

后端根目录，当前尚未 scaffold Spring Boot 项目。

## 技术栈

- Java 17
- Spring Boot 3
- Maven
- SseEmitter
- 本地 JSON Mock 数据
- 默认端口：`8000`

## 开工前读

1. `../AGENTS.md`
2. `../docs/backend-contract.md`
3. `../docs/api-contract.md`
4. `../feature_list.json`
5. `../docs/handoff.md`

## B1 / B2 边界

- B1：controller、agent、planner、sse、intent/message prompt。
- B2：tools、mock service、POI JSON、scenario flags、booking/order 模拟。

## 最小 Sprint 1 验收

- `GET /health` 返回 200。
- `POST /api/plan` 返回 `planId` 和 `status`。
- `GET /api/plan/{planId}/stream` 能推送 SSE 心跳和至少一个 mock `state_change`。
- `POST /api/debug/scenario` 能更新异常开关。

## 验证命令

```powershell
..\verify.ps1 -Target backend -Mode fast
```

当前没有 `pom.xml`，所以脚本应明确报告 `backend not initialized yet`。
