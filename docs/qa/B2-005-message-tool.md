# B2-005 MessageTool / composeShareMessage QA 报告

## 结论

PASS

Evaluator：Mencius (`B2-005-EVAL-CODEX-20260524T0959+0800`)

验收时间：2026-05-24 09:59:32 +08:00

## 检查范围

- 已阅读 `AGENTS.md`，确认本轮为后端 B2 Tool 层验收，evaluator 只写入本 QA 报告。
- 已阅读 `docs/contracts/B2-005-message-tool.md`，确认本轮目标是 deterministic fallback 中文 `shareMessage`，不实现 B1 `Plan`、Planner、SSE 或 API。
- 已检查 `feature_list.json` 中 B2-005 条目：`status=todo`，验收项为 deterministic fallback、LLM 边界、`shareMessage appears in Plan`。
- 已检查 `docs/api-contract.md` 的 `Plan` 模型，确认 `shareMessage` 是 required 且前端 required-render 字段。
- 已检查 `docs/backend-contract.md` 的 LLM 边界，确认 LLM 只允许做意图抽取和分享消息/执行话术生成，不允许做规划决策、POI 选择、状态转移，也不能硬编码或写入 OpenAI key。
- 已检查 `backend/HANDOFF.md`，当前仍显示 `MessageTool / composeShareMessage` 未勾选；这是 generator 在 evaluator PASS 后需要同步的状态项，本报告未修改该文件。

## 源码与测试检查

- `backend/src/main/java/com/weekendtravel/backend/b2/tool/MessageTool.java`
  - `execute(MessageRequest request)` 直接委托 `composeShareMessage(request)`，兼容入口存在。
  - `composeShareMessage` 构造 deterministic fallback 中文消息，并返回 `MessageResult`。
  - `llmUsed` 固定返回 `false`，`templateVersion` 为 `fallback-v1`。
  - `MessagePlanPayload` 中包含 `shareMessage`，并保留 `planId`、`scenario`、`status`、`isPlanB`、`planBReason`、`summary`、`timeline`、`actions`、`totalDurationHours`、`replanCount`、`createdAt` 等 Plan-compatible 字段。
  - 时间线按正数 `order` 升序输出；缺失或非正 `order` 排在后面并保持输入顺序。
  - `send_message` 动作被过滤，不会进入“已安排”摘要。
  - `family` 场景会生成亲子、低负担饮食或少步行相关尾句；`friends` 场景会生成玩、吃、拍照、聊天等尾句。
  - `isPlanB=true` 且存在 `planBReason` 时会透明输出 `Plan B 说明`。
  - 缺失 `planId`、非法 `scenario`、空 `timeline` 会抛出明确 `IllegalArgumentException`。
- `MessageRequest.java`、`MessageResult.java`、`MessagePlanPayload.java`、`MessageTimeSlot.java`、`MessageActionSummary.java`
  - 使用 Java record，字段名保持 camelCase。
  - `timeline`、`actions`、`notes` 对 null 做空列表兜底，避免空指针。
- `backend/src/test/java/com/weekendtravel/backend/b2/MessageToolTests.java`
  - 覆盖家庭场景 deterministic fallback、`shareMessage` 进入 `plan.shareMessage`、动作摘要和 `send_message` 过滤。
  - 覆盖朋友场景 timeline 排序和拍照/聊天文案。
  - 覆盖 Plan B reason、`isPlanB`、`replanCount`。
  - 覆盖缺失 `planId`、非法 `scenario`、空 `timeline` 错误路径。
  - 覆盖 `llmUsed=false`、`templateVersion=fallback-v1`、`latencyMs>=0`。

## 命令与测试结果

标准后端验证命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\verify.ps1 -Target backend -Mode fast
```

结果：

- `Verify passed.`
- Maven `BUILD SUCCESS`
- 总测试：27
- Failures：0
- Errors：0
- Skipped：0
- `MessageToolTests`：4 tests, 0 failures, 0 errors

边界检索命令：

```powershell
rg -n "OpenAI|OPENAI_API_KEY|LLM|HttpClient|WebClient|RestTemplate|URLConnection|Socket|java\.net|System\.getenv|System\.getProperty|completion|chat|model" backend\src\main\java\com\weekendtravel\backend\b2\tool\MessageTool.java backend\src\main\java\com\weekendtravel\backend\b2\tool\MessageRequest.java backend\src\main\java\com\weekendtravel\backend\b2\tool\MessageResult.java backend\src\main\java\com\weekendtravel\backend\b2\tool\MessagePlanPayload.java backend\src\main\java\com\weekendtravel\backend\b2\tool\MessageTimeSlot.java backend\src\main\java\com\weekendtravel\backend\b2\tool\MessageActionSummary.java backend\src\test\java\com\weekendtravel\backend\b2\MessageToolTests.java
```

结果：未命中，未发现 OpenAI/LLM/网络调用、`OPENAI_API_KEY` 读取或硬编码、`System.getenv`、HTTP client 相关使用。

API / SSE 边界检索命令：

```powershell
rg -n "PostMapping|GetMapping|SseEmitter|/api/plan|plan_ready|state_change|MockApiService|MessageTool" backend\src\main\java backend\src\test\java\com\weekendtravel\backend\b2\MessageToolTests.java
```

结果：本轮新增范围只出现 `MessageTool` 及其测试；未发现 B2-005 假装实现 `POST /api/plan`、Planner、SSE 或 `plan_ready`。

## 关键证据

- deterministic fallback：测试中同一 `MessageRequest` 连续调用 `composeShareMessage`，断言两次 `shareMessage` 相同。
- 中文 `shareMessage`：测试断言包含 `14:00 先去奇妙亲子乐园`、`17:00 去轻食家庭餐厅`、`低负担饮食`、`玩、吃、拍照和聊天` 等中文文案。
- Plan-compatible payload：测试断言 `result.plan().shareMessage()` 等于顶层 `result.shareMessage()`。
- LLM 禁用：源码返回 `llmUsed=false`，边界检索未发现 OpenAI、LLM、网络调用或 key 读取。
- 动作摘要：测试确认 `reserve_table`、`add_note` 描述进入消息，`send_message` 描述不进入消息。
- Plan B：测试确认消息包含 `Plan B 说明`，payload 中 `isPlanB=true`、`replanCount=1`。
- 错误路径：测试覆盖缺失 `planId`、非法 `scenario`、空 `timeline`，异常消息明确。

## 风险 / 备注

- `execute()` 兼容入口已通过源码确认是一行委托到 `composeShareMessage`，但当前没有单独的 `execute()` 调用测试。风险低，建议后续补一个轻量测试以防未来改动破坏兼容入口。
- `latencyMs` 会随运行时间变化；当前 deterministic 验收关注的是 `shareMessage`，测试也只比较 `shareMessage` 稳定性，这与契约一致。
- `backend/HANDOFF.md` 和 `feature_list.json` 尚未由 generator 同步 B2-005 验收状态；本 evaluator 按要求未修改这些文件。
