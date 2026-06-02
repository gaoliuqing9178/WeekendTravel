# Evaluator Report: QA-001 21 Golden Cases list and execution record

## Evaluator 子代理信息

- 子代理：QA agent / evaluator
- 测试日期：2026-06-02
- Run ID：`QA-001-EVAL-CODEX-20260602T1647+0800`
- Generator handoff：`feature_list.json` 中 `QA-001` todo 项；本轮不复用 `INT-002` / `INT-003` happy path 证据。

## 测试目标

- 列出并执行 21 条 Golden Cases：7 条 family、7 条 friends、7 条 boundary。
- 通过真实后端 `POST /api/plan`、SSE stream、clarify、adjust、execute 和 debug scenario flags 记录执行结果。
- 记录 SLO，并分开统计正常非注入 Plan B rate 与注入 Plan B trigger accuracy。
- 验证执行记录、contract、feature metadata、progress 和 handoff 可形成闭环。

## 环境和命令

```powershell
Start-Process -FilePath ".\backend\mvnw.cmd" -ArgumentList "-f","backend\pom.xml","spring-boot:run" -WorkingDirectory "H:\WeekendTravel" -RedirectStandardOutput "docs\qa\QA-001-backend-run.out" -RedirectStandardError "docs\qa\QA-001-backend-run.err" -PassThru -WindowStyle Hidden
py -c "exec(open('docs\\qa\\QA-001-run-golden-cases.py', encoding='utf-8').read())"
```

后端启动证据：

- `docs/qa/QA-001-backend-run.out` 记录 Spring Boot 启动，Tomcat 监听 `8000`。
- 启动日志显示 PID `41096`，`Started BackendApplication`。
- 执行脚本在开始时调用 `GET /health`，结果为 `200`，payload 为 `status=ok`、`service=WeekendTravel`。

机器可读证据：

- 执行脚本：`docs/qa/QA-001-run-golden-cases.py`
- 执行结果：`docs/qa/QA-001-golden-cases-results.json`

## 用户路径

1. 每条 case 前重置 `POST /api/debug/scenario` 为全 false。
2. 如 case 有注入条件，使用 `POST /api/debug/scenario` 设置对应 flag。
3. 正常 case 调用 `POST /api/plan` 创建 plan，再调用 `GET /api/plan/{planId}/stream` 消费规划 SSE。
4. 如遇 `clarification_request`，调用 `POST /api/plan/{planId}/clarify`，再继续 stream。
5. 收到 `plan_ready` 后调用 `POST /api/plan/{planId}/execute`，再继续 stream，等待 `execute_result` 与 `done`。
6. `adjust` case 在 `CONFIRM` 后调用 `PATCH /api/plan/{planId}/adjust`，确认收到 `adjust_result` 后再执行。
7. invalid case 只验证 `POST /api/plan` 返回 400 `INVALID_INPUT`，不创建 plan。

## 执行记录

| Case | 类别 | planId | 最终状态 | Clarify | Adjust | 执行动作状态 | 结论 |
|---|---|---|---|---|---|---|---|
| FAM-001 | family | `plan_fa4e84beb3e3` | DONE | yes | no | success, success | PASS |
| FAM-002 | family | `plan_3a1cdb44ba90` | DONE | no | no | success, success | PASS |
| FAM-003 | family | `plan_7d50131e2cbd` | DONE | no | no | success, success | PASS |
| FAM-004 | family | `plan_47b15cd8ee56` | DONE | no | no | success, success | PASS |
| FAM-005 | family | `plan_b9e210a4f3ac` | DONE | no | no | success, success | PASS |
| FAM-006 | family | `plan_ccc18a6e5e87` | DONE | no | no | success, success | PASS |
| FAM-007 | family | `plan_d66ac07e2479` | DONE | yes | no | success, success | PASS |
| FRI-001 | friends | `plan_aa3c19547c7d` | DONE | no | no | success, success | PASS |
| FRI-002 | friends | `plan_590e0598e931` | DONE | no | no | success, success | PASS |
| FRI-003 | friends | `plan_ee16c38d121d` | DONE | no | no | success, success | PASS |
| FRI-004 | friends | `plan_102930bd8835` | DONE | no | no | success, success | PASS |
| FRI-005 | friends | `plan_c5c951e27b1c` | DONE | no | no | success, success | PASS |
| FRI-006 | friends | `plan_2c6eeea7ed94` | DONE | no | no | success, success | PASS |
| FRI-007 | friends | `plan_2195682f799c` | DONE | yes | no | success, success | PASS |
| BND-001 | boundary | `plan_e9499616ad95` | DONE | yes | no | success, success | PASS |
| BND-002 | boundary | `plan_d2063fa0948d` | DEGRADE | no | no | not executed | PASS |
| BND-003 | boundary | `plan_77085535d8d7` | DEGRADE | no | no | not executed | PASS |
| BND-004 | boundary | `plan_1920cc73c593` | DONE | no | no | failed, success | PASS |
| BND-005 | boundary | `plan_365a9d00ce5e` | DEGRADE | no | no | not executed | PASS |
| BND-006 | boundary | `plan_217e6ec32bc9` | DONE | no | yes | success, success | PASS |
| BND-007 | boundary | - | INVALID_INPUT | no | no | not executed | PASS |

## SLO 和指标

| 指标 | 结果 | 口径 |
|---|---:|---|
| Case pass rate | 21 / 21 = 100% | 全部 Golden Cases |
| Planning P95 | 40.00 ms | 所有成功创建 plan 的 20 条 case |
| Normal planning P95 | 43.34 ms | 14 条 family / friends 正常非注入 case |
| Normal feasibility rate | 14 / 14 = 100% | 14 条 family / friends 正常非注入 case 均到达 `plan_ready` 且执行 PASS |
| Normal Plan B rate | 0 / 14 = 0% | 只统计 14 条 family / friends 正常非注入 case |
| Injected Plan B trigger accuracy | 3 / 3 = 100% | `restaurantFull`、`routeTooFar`、`ageMismatch` 均触发 `replan` 并最终 `DEGRADE` |
| Intent / scenario accuracy | 17 / 17 = 100% | 适用 case 的 `plan.scenario` 与请求 scenario 一致 |

说明：

- `bookingFail` 是执行期失败注入，结果为 `failed, success` 后仍 `done`，不计入 injected Plan B trigger accuracy。
- `BND-006` 的用户主动 adjust 过程出现 `replan` 事件，但这是边界微调路径，不计入 normal Plan B rate。
- 当前耗时很低，是因为本轮使用本地 deterministic mock tool，没有外部 LLM、地图、支付或订座网络调用。

## 通过证据

- 21 条 case 均 PASS，机器记录见 `docs/qa/QA-001-golden-cases-results.json`。
- family normal case 均覆盖 `plan_ready`、`execute_result`、`done`，执行动作均为 success。
- friends normal case 均覆盖 `plan_ready`、`execute_result`、`done`，timeline 校验包含 activity + cafe/dessert + restaurant。
- clarify 覆盖：`FAM-001`、`FAM-007`、`FRI-007`、`BND-001`。
- planning injection 覆盖：`BND-002 restaurantFull`、`BND-003 routeTooFar`、`BND-005 ageMismatch`，均出现 `replan`、`REPLAN` 和 `error(code=DEGRADE)`。
- execution injection 覆盖：`BND-004 bookingFail`，执行结果为 `failed, success`，最终仍进入 `DONE`。
- adjust 覆盖：`BND-006` 使用真实 `PATCH /api/plan/{planId}/adjust`，收到 `adjust_result`，随后执行到 `DONE`。
- invalid input 覆盖：`BND-007` 返回 400 `INVALID_INPUT`。

## 回归检查

```powershell
py -c "import json; json.load(open('feature_list.json', encoding='utf-8')); json.load(open('docs/qa/QA-001-golden-cases-results.json', encoding='utf-8')); print('json parse ok')"
rg -n "plan_id|latency_ms|affected_slots|replan_count|action_id|action_type|confirmation_no" backend\src frontend\src frontend\scripts docs\fixtures
.\verify.ps1 -Target backend -Mode fast
.\verify.ps1 -Target frontend -Mode fast
```

结果：

- JSON 解析通过：`feature_list.json` 和 `docs/qa/QA-001-golden-cases-results.json` 均可解析。
- Forbidden snake_case 字段扫描无命中。
- Backend fast verify 通过：48 tests、0 failures、0 errors、`BUILD SUCCESS`、`Verify passed.`。
- Frontend fast verify 通过：`pnpm verify:fixtures`、`docs/fixtures/sse-events.jsonl -> 28 JSONL events`、`pnpm typecheck`、`Verify passed.`。
- QA 执行后已停止本轮启动的后端进程，`8000` 端口已释放。

## 失败复现

无。

## 截图 / 日志 / API 输出位置

- 截图：不适用，本轮没有前端 UI 变更。
- 后端启动日志：`docs/qa/QA-001-backend-run.out`
- 后端 stderr：`docs/qa/QA-001-backend-run.err`
- API / SSE 执行结果：`docs/qa/QA-001-golden-cases-results.json`
- 执行脚本：`docs/qa/QA-001-run-golden-cases.py`

## 前端浏览器 MCP 证据

不适用。本轮是 QA 侧后端 API / SSE Golden Cases 执行记录，不修改前端 UI；若后续把 Golden Cases 升级为真实浏览器回归，需按 `WF-002` 使用 Chrome DevTools MCP。

## 放行结论

- 结论：放行，`QA-001` 可标记为 `verified`。
- 理由：21 / 21 Golden Cases 全部 PASS；SLO 指标完整；Plan B 统计口径已拆分；执行证据已写入 QA 报告和机器可读 JSON。
- 后续建议：真实浏览器层面的 21 case 可另开 UI regression 任务，使用 Chrome DevTools MCP 采集页面、console、network 和可视状态证据。

## 独立性声明

- 本报告由 QA / evaluator agent 执行测试后填写。
- 本轮没有业务 generator 改动；测试不复用 `INT-002` / `INT-003` 的历史 happy path 作为 21 条 Golden Cases 证据。
