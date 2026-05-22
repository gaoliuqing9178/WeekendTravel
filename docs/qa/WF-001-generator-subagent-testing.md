# Evaluator Report: WF-001 Generator 测试阶段子代理化

## Evaluator 子代理信息

- 子代理：Newton (`019e4da9-79c0-79a3-b7f1-c79f5e594c58`)
- 测试日期：2026-05-22
- Generator handoff：`docs/contracts/WF-001-generator-subagent-testing.md`

## 测试目标

- 验证 harness 文档是否统一要求：所有 generator agent 完成开发后，测试阶段必须委托独立 evaluator 子代理执行。
- 验证 generator 自己运行的 typecheck、build、curl、Playwright、脚本或本地冒烟只作为开发准备记录，不能单独作为 `verified` 证据。
- 验证不存在仍允许 generator 自己完成最终测试、自己直接标记完成或以 generator-only 命令作为 `verified` 证据的活跃表述。
- 验证 `feature_list.json` 可解析。

## 环境和命令

```powershell
rg -n "Self Verify|小任务可以同一个 agent|Generator 负责实现和基础验证" AGENTS.md docs backend frontend
rg -n -C 2 "Self Verify|自行最终验证|小任务可以同一个 agent|小任务.*同一个|Generator 负责实现和基础验证|自测|基础验证" AGENTS.md docs backend frontend progress.md feature_list.json
Get-Content -Raw -Encoding UTF8 -LiteralPath 'feature_list.json' | ConvertFrom-Json | Out-Null; Write-Output 'feature_list.json parse OK'
```

## 用户路径

1. 读取 `AGENTS.md`、`docs/dev-workflow.md`、`docs/quality.md`、contract 模板、QA 模板、前后端 contract、前后端 handoff、长期 handoff、decision log、progress 和 `feature_list.json`。
2. 搜索旧流程和潜在冲突表述。
3. 解析 `feature_list.json`，确认 `WF-001` 条目存在且验收项覆盖新规则。
4. 给出放行或不放行结论。

## 通过证据

- `AGENTS.md` 完成定义要求独立 evaluator 子代理测试，generator 自己运行的命令不能单独作为 `verified` 证据。
- `docs/dev-workflow.md` 已改为 generator handoff 给 evaluator 子代理；小任务也不能由同一个 generator 自测后直接完成。
- `docs/quality.md` 要求 `verified` 必须有独立 evaluator 子代理执行的验收路径和证据。
- `docs/contracts/_template.md` 把“没有 evaluator 子代理参与”列为失败，并禁止只有 generator 命令就标 `verified`。
- `docs/qa/evaluator-template.md` 声明 QA 报告必须由 generator 之外的 evaluator 子代理填写，generator 命令只作参考。
- `docs/backend-contract.md`、`docs/frontend-contract.md`、`backend/HANDOFF.md`、`frontend/F1-handoff.md` 均同步了测试阶段 evaluator 子代理规则。
- `docs/decision-log.md` 记录 D-009 决策。
- `feature_list.json` 可解析，且 `WF-001` 条目存在。

## 失败复现

无。

## 截图 / 日志 / API 输出位置

- 截图：不适用，本轮为 harness 文档改动。
- 日志：本报告记录 evaluator 子代理只读验证摘要。
- API 输出：不适用。
- Playwright trace：不适用。

## 放行结论

- 结论：放行
- 理由：活跃 harness 规则已经统一为“generator 完成开发后，测试阶段必须交给独立 evaluator 子代理”；未发现仍允许 generator-only final testing 的活跃表述；`feature_list.json` 可解析。
- 后续建议：后续 generator 完成任意开发任务后，必须把待测范围、命令、用户路径和风险点交给 evaluator 子代理，并以 evaluator 证据作为 `verified` 前提。

## 独立性声明

- 本报告由 generator 之外的 evaluator 子代理执行测试后填写。
- Generator 自己运行的命令只作为参考，不作为本报告的放行依据。
