# B1-003 Create plan QA 报告

## 结论

PASS

Evaluator：`B1-003-EVAL-gpt-5.4-20260526`

验收时间：2026-05-26

## 检查范围

- 已检查 `docs/contracts/B1-003-create-plan.md`，确认本轮目标是实现最小 `POST /api/plan` 占位接口。
- 已检查 `docs/api-contract.md` 中 `POST /api/plan` 的正式线缆契约。
- 已检查 `feature_list.json` 中 `B1-003` 条目，验收项为：
  - `POST /api/plan` accepts `text`, `scenario`, and optional `origin`
  - Response uses camelCase `planId` and `status`
  - Invalid input returns the documented error shape
- 已检查 `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`、`backend/src/main/java/com/weekendtravel/backend/plan/PlanCreateService.java`、`backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanRequest.java`、`CreatePlanResponse.java`、`backend/src/main/java/com/weekendtravel/backend/api/ApiErrorResponse.java`、`ApiExceptionHandler.java`。
- 已检查 `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java` 的真实 HTTP 集成测试覆盖。

## 源码与测试检查

- `backend/src/main/java/com/weekendtravel/backend/controller/PlanController.java`
  - 在现有 `/api/plan` controller 上新增 `@PostMapping`。
  - `create(@RequestBody CreatePlanRequest request)` 返回 `ResponseEntity.accepted()`，保证 HTTP `202`。
- `backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanRequest.java`
  - 请求字段为 `text`、`scenario`、`origin`，与契约一致。
- `backend/src/main/java/com/weekendtravel/backend/plan/api/CreatePlanResponse.java`
  - 响应字段为 camelCase 的 `planId`、`status`，与契约一致。
- `backend/src/main/java/com/weekendtravel/backend/plan/PlanCreateService.java`
  - 校验 `request` 非空。
  - 校验 `text` 非空白，否则抛出 `text is required`。
  - 校验 `scenario` 必填且只能为 `family|friends`，否则抛出 `scenario is required` 或 `scenario must be family or friends`。
  - `origin` 为空白时归一化为 `null`，不会额外报错。
  - 成功返回 `status=processing`，`planId` 带 `plan_` 前缀。
- `backend/src/main/java/com/weekendtravel/backend/api/ApiErrorResponse.java`
  - 定义统一错误结构 `error`、`message`、`details`。
- `backend/src/main/java/com/weekendtravel/backend/api/ApiExceptionHandler.java`
  - `IllegalArgumentException` 映射为 HTTP `400` + `error=INVALID_INPUT`。
  - 对 `"... is required"` 文案自动提取 `details.field`。
  - `HttpMessageNotReadableException` 映射为 HTTP `400` + `request body is required`。
- `backend/src/test/java/com/weekendtravel/backend/controller/PlanControllerCreateTests.java`
  - 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + JDK `HttpClient` 发真实 HTTP 请求。
  - 覆盖成功创建、缺失 `text`、空白 `text`、缺失 `scenario`、非法 `scenario`、空 body 共 6 个场景。

## 命令与测试结果

父仓库定向测试：

```powershell
D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml -Dtest=PlanControllerCreateTests test
```

结果：`BUILD SUCCESS`

父仓库全量后端测试：

```powershell
D:/Users/lenovo/Desktop/WeekendTravel/backend/mvnw -f D:/Users/lenovo/Desktop/WeekendTravel/backend/pom.xml test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 34`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

Generator 开发侧准备检查：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果：`Verify passed.`

## 关键证据

- `POST /api/plan` 成功响应为 HTTP `202`。
- 成功响应 body 使用 camelCase 的 `planId`、`status`。
- `status` 固定为 `processing`。
- `planId` 由后端生成，带 `plan_` 前缀。
- 非法输入统一返回 `400` + `INVALID_INPUT` + `message`。
- 对缺失必填字段的场景，响应 `details.field` 可正确指向 `text` 或 `scenario`。
- `origin` 是可选字段，未被错误地强制要求。

## 风险 / 备注

- evaluator 通过父工作树绝对路径执行了独立 Maven 测试，这是本次放行主证据。
- `verify.ps1` 虽然通过，但它在 agent 隔离 worktree 中运行，不作为本次独立放行的核心依据。
- 当前自动化已覆盖空 body，但没有单独新增“非法 JSON”用例；不过 `ApiExceptionHandler` 已对 `HttpMessageNotReadableException` 做统一 `400 INVALID_INPUT` 映射，代码证据充分。
- 本轮不涉及前端 UI，Playwright MCP / Chrome DevTools MCP 不适用。
