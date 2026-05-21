# API Contract

本文件是 F1、B1、B2 的唯一线缆契约源。任何 API 字段、状态、SSE 事件变更，必须先修改本文件，再同步前端类型、后端模型、fixture 和相关 contract。

## 基础约定

- Base URL：`http://localhost:8000`
- 前端开发端口：`http://localhost:5173`
- JSON 线缆字段：统一使用 camelCase。
- Content-Type：REST 请求和响应使用 `application/json`；SSE 使用 `text/event-stream`。
- 时间戳：`timestamp` 使用 Unix epoch milliseconds。
- `planId` 是前后端主键，所有 plan 相关接口都使用路径参数 `{planId}`。

## 通用枚举

### Scenario

```json
"family" | "friends"
```

### AgentState

```json
"START" | "INTENT" | "CLARIFY" | "SKELETON" | "RECALL" | "VALIDATE" | "REPLAN" | "PACK" | "CONFIRM" | "ADJUST" | "EXECUTE" | "DEGRADE" | "DONE" | "FAILED"
```

### ActionStatus

```json
"pending" | "executing" | "success" | "failed" | "manual"
```

### ActionType

字段名使用 camelCase，但枚举值沿用工具层 lower_snake_case 语义：

```json
"buy_ticket" | "reserve_table" | "take_number" | "schedule_delivery" | "add_note" | "send_message" | "cancel_booking"
```

说明：`cancel_booking` 主要用于 Tool 层反向操作或后续取消能力。当前主链路公开执行动作通常使用前 6 类。

### Error Shape

所有非 2xx 错误响应尽量使用同一结构：

```json
{
  "error": "INVALID_INPUT",
  "message": "scenario must be family or friends",
  "details": {
    "field": "scenario"
  }
}
```

前端必须渲染：`message`。后端必须提供：`error`、`message`。`details` 可选。

## Data Models

### Plan

后端必须提供全部 required 字段；前端必须渲染标记为 required-render 的字段。

```json
{
  "planId": "plan_family_001",
  "scenario": "family",
  "status": "CONFIRM",
  "isPlanB": true,
  "planBReason": "原餐厅排队预计 70 分钟，已换成可订位餐厅",
  "summary": "亲子活动 + 轻食晚餐 + 回家路线",
  "timeline": [],
  "actions": [],
  "shareMessage": "我们今天下午先去亲子乐园，17:30 去餐厅。",
  "totalDurationHours": 4.5,
  "replanCount": 1,
  "createdAt": "2026-05-21T14:00:00+08:00"
}
```

| 字段 | 类型 | 后端必须提供 | 前端必须渲染 | 说明 |
|---|---|---:|---:|---|
| `planId` | string | 是 | 可弱化展示 | 计划 ID |
| `scenario` | string | 是 | 是 | `family` 或 `friends` |
| `status` | AgentState | 是 | 是 | 当前状态 |
| `isPlanB` | boolean | 是 | 是 | Plan B 徽标依据 |
| `planBReason` | string|null | 是 | 是，当 `isPlanB=true` | Plan B 原因 |
| `summary` | string | 是 | 是 | 方案摘要 |
| `timeline` | TimeSlot[] | 是 | 是 | 方案卡主内容 |
| `actions` | ActionItem[] | 是 | 是 | 执行包 |
| `shareMessage` | string | 是 | 是 | 转发消息预览 |
| `totalDurationHours` | number | 是 | 是 | 总时长 |
| `replanCount` | number | 是 | 是 | 重排次数 |
| `createdAt` | string | 是 | 否 | ISO 时间 |

### TimeSlot

```json
{
  "order": 1,
  "type": "activity",
  "title": "亲子乐园",
  "poi": {
    "id": "poi_family_activity_001",
    "name": "奇妙亲子乐园",
    "category": "activity",
    "address": "万象城 L3",
    "rating": 4.6,
    "distanceMinutes": 15,
    "tags": ["适合5岁", "室内", "不太累"],
    "availabilityStatus": "available",
    "waitMinutes": 5
  },
  "startTime": "14:00",
  "endTime": "16:30",
  "notes": ["适合 5 岁儿童", "室内活动，天气影响小"]
}
```

前端必须渲染：`title`、`poi.name`、`startTime`、`endTime`、`notes`。后端必须提供：上述全部字段，`waitMinutes` 可为 `null`。

### ActionItem

```json
{
  "actionId": "act_001",
  "actionType": "reserve_table",
  "targetPoiId": "poi_family_food_002",
  "description": "预约 17:30 的 2 大 1 小座位",
  "status": "pending",
  "confirmationNo": null
}
```

前端必须渲染：`description`、`status`、`confirmationNo`。后端必须提供：`actionId`、`actionType`、`description`、`status`。

后端 Tool 层执行 `bookOrOrder` 时必须使用 `idempotencyKey` 防止重复模拟下单。当前 `idempotencyKey` 不要求展示给前端，也不要求出现在 Plan 对象中；如未来暴露到 API，必须先更新本契约。

## REST Endpoints

### GET `/health`

用于后端独立验证。

Response 200:

```json
{
  "status": "ok",
  "service": "WeekendTravel",
  "timestamp": 1779362400000
}
```

后端必须提供：`status`。前端通常不渲染。

### POST `/api/plan`

发起规划。

Request:

```json
{
  "text": "今天下午是空的，想和老婆孩子出去玩几个小时",
  "scenario": "family",
  "origin": "当前位置"
}
```

字段要求：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `text` | string | 是 | 用户自然语言 |
| `scenario` | Scenario | 是 | Demo 场景 |
| `origin` | string | 否 | 默认可为“当前位置” |

Response 202:

```json
{
  "planId": "plan_abc123",
  "status": "processing"
}
```

前端必须读取：`planId`、`status`。后端必须提供：`planId`、`status`。

Error 400:

```json
{
  "error": "INVALID_INPUT",
  "message": "text is required",
  "details": {
    "field": "text"
  }
}
```

### GET `/api/plan/{planId}/stream`

建立 SSE 实时日志流。

Response headers:

```text
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

SSE frame:

```text
event: state_change
data: {"type":"state_change","from":"START","to":"INTENT","timestamp":1779362400000}
```

后端必须推送心跳和至少一个 `state_change`，用于 Sprint 1 联调。前端必须能把所有事件追加到日志面板，并按事件类型更新状态。

### POST `/api/plan/{planId}/execute`

用户确认并开始执行。

Request:

```json
{
  "confirmed": true
}
```

Response 200:

```json
{
  "planId": "plan_abc123",
  "status": "executing",
  "message": "开始执行，请关注右侧日志"
}
```

Error 409:

```json
{
  "error": "INVALID_STATE",
  "message": "plan must be in CONFIRM state before execute"
}
```

### POST `/api/plan/{planId}/clarify`

用户回答 `CLARIFY` 反问。

Request:

```json
{
  "reply": "4-6小时"
}
```

Response 200:

```json
{
  "planId": "plan_abc123",
  "status": "processing",
  "message": "已收到，继续规划中"
}
```

Error 409:

```json
{
  "error": "INVALID_STATE",
  "message": "plan is not waiting for clarification"
}
```

### PATCH `/api/plan/{planId}/adjust`

用户在 `CONFIRM` 状态下局部微调方案。

Request:

```json
{
  "instruction": "换一家餐厅，要能订位的"
}
```

Response 202:

```json
{
  "planId": "plan_abc123",
  "status": "adjusting",
  "message": "正在调整餐厅，请关注右侧日志"
}
```

Error 429:

```json
{
  "error": "ADJUST_LIMIT_EXCEEDED",
  "message": "已达最大微调次数，请直接确认或重新发起规划"
}
```

前后端共同约束：每个 plan 最多允许 3 次微调。

### POST `/api/debug/scenario`

更新 Demo 异常注入开关。只用于本地开发和演示。

Request:

```json
{
  "restaurantFull": true,
  "routeTooFar": false,
  "bookingFail": false,
  "ageMismatch": false
}
```

Response 200:

```json
{
  "updated": {
    "restaurantFull": true,
    "routeTooFar": false,
    "bookingFail": false,
    "ageMismatch": false
  }
}
```

后端必须保证更新无需重启服务。前端可选渲染 debug 控件，但不能把它当正式用户路径。

## SSE Event Types

所有 SSE `data` JSON 必须包含 `type`。事件名和 `type` 保持一致。

### `heartbeat`

```json
{
  "type": "heartbeat",
  "planId": "plan_abc123",
  "timestamp": 1779362400000
}
```

前端可不渲染，但必须不报错。后端必须定期推送或在连接建立时至少推送一次。

### `state_change`

```json
{
  "type": "state_change",
  "planId": "plan_abc123",
  "from": "START",
  "to": "INTENT",
  "timestamp": 1779362400000
}
```

前端必须渲染到日志面板，并更新当前状态。后端每次状态转移必须推送。

### `tool_call`

```json
{
  "type": "tool_call",
  "planId": "plan_abc123",
  "tool": "searchLocalPlaces",
  "status": "start",
  "inputSummary": "搜索附近亲子活动，半径 3km",
  "timestamp": 1779362400100
}
```

前端必须渲染：`tool`、`status`、`inputSummary`。

### `tool_result`

```json
{
  "type": "tool_result",
  "planId": "plan_abc123",
  "tool": "searchLocalPlaces",
  "status": "success",
  "outputSummary": "找到 5 个候选 POI",
  "latencyMs": 234,
  "timestamp": 1779362400334
}
```

前端必须渲染：`tool`、`status`、`outputSummary`、`latencyMs`。后端必须提供 `latencyMs`。

### `clarification_request`

```json
{
  "type": "clarification_request",
  "planId": "plan_abc123",
  "question": "请问大概想玩几个小时？",
  "field": "durationHours",
  "options": ["3-4小时", "4-6小时", "6小时以上"],
  "timestamp": 1779362400500
}
```

前端必须只显示一个反问气泡。后端每次 `CLARIFY` 只推送一个最关键问题，最多 2 次。

### `replan`

```json
{
  "type": "replan",
  "planId": "plan_abc123",
  "reason": "餐厅青禾花园排队预计 70 分钟，超出阈值",
  "replanCount": 1,
  "timestamp": 1779362403000
}
```

前端必须在日志和方案卡上高亮 Plan B。后端必须保证 `replanCount` 递增且不超过规则上限。

### `adjust_result`

```json
{
  "type": "adjust_result",
  "planId": "plan_abc123",
  "affectedSlots": ["restaurant"],
  "summary": "已将餐厅换为林间日式小食，可订位，排队约 5 分钟",
  "plan": {}
}
```

`plan` 必须是最新 Plan 对象。前端必须局部更新受影响槽位并恢复 `CONFIRM` 交互。

### `plan_ready`

```json
{
  "type": "plan_ready",
  "planId": "plan_abc123",
  "plan": {}
}
```

`plan` 必须符合 Plan 模型。前端必须渲染方案卡、执行包、分享消息、确认按钮。

### `execute_result`

```json
{
  "type": "execute_result",
  "planId": "plan_abc123",
  "actionId": "act_001",
  "actionType": "reserve_table",
  "status": "success",
  "confirmationNo": "MOCK-TBL-88421",
  "timestamp": 1779362420000
}
```

前端必须按 `actionId` 更新执行包状态。后端必须为每个执行动作推送结果。

### `done`

```json
{
  "type": "done",
  "planId": "plan_abc123",
  "summary": "3 个动作全部完成，1 个需人工处理",
  "timestamp": 1779362425000
}
```

前端必须进入完成态并关闭 SSE 或停止重连。后端在全部动作结束后推送。

### `error`

```json
{
  "type": "error",
  "planId": "plan_abc123",
  "code": "DEGRADE",
  "message": "3 次重排后仍无可行方案，建议放宽距离限制或调整时间",
  "timestamp": 1779362429000
}
```

前端必须展示错误或降级状态。后端必须提供可读 `message`。

## 字段兼容说明

原始产品文档中出现过 `plan_id`、`latency_ms`、`affected_slots`、`replan_count`、`action_id`、`action_type`、`confirmation_no` 等 snake_case 示例。正式联调不使用 snake_case。若后续为了兼容历史示例引入映射层，必须在本节追加清晰映射表，并同步 fixture。
