import familyPlanReadyRaw from '../../../docs/fixtures/plan-ready-family.json?raw'
import friendsPlanReadyRaw from '../../../docs/fixtures/plan-ready-friends.json?raw'
import sseEventsRaw from '../../../docs/fixtures/sse-events.jsonl?raw'

import type {
  Plan,
  PlanReadyEvent,
  Scenario,
  SseFixtureFrame,
  SsePayload,
} from './types'

type JsonObject = Record<string, unknown>

const planReadyByScenario: Record<Scenario, string> = {
  family: familyPlanReadyRaw,
  friends: friendsPlanReadyRaw,
}

export function getPlanReadyFixture(scenario: Scenario): PlanReadyEvent {
  const source = `docs/fixtures/plan-ready-${scenario}.json`
  return parsePlanReadyFixture(planReadyByScenario[scenario], source)
}

export function getSseFixtureFrames(): SseFixtureFrame[] {
  return parseSseFixtureFrames(sseEventsRaw, 'docs/fixtures/sse-events.jsonl')
}

export function parsePlanReadyFixture(
  raw: string,
  source: string,
): PlanReadyEvent {
  const value = parseJson(raw, source)
  assertNoUnderscoredKeys(value, source)
  assertPlanReadyEvent(value, source)
  return value
}

export function parseSseFixtureFrames(
  raw: string,
  source: string,
): SseFixtureFrame[] {
  const lines = raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (lines.length === 0) {
    throw new Error(`${source} must contain at least one JSONL event`)
  }

  return lines.map((line, index) => {
    const lineSource = `${source}:${index + 1}`
    const value = parseJson(line, lineSource)
    assertNoUnderscoredKeys(value, lineSource)
    assertSseFixtureFrame(value, lineSource)
    return value
  })
}

function parseJson(raw: string, source: string): unknown {
  try {
    return JSON.parse(raw) as unknown
  } catch (error) {
    throw new Error(`${source} is not valid JSON: ${String(error)}`)
  }
}

function assertNoUnderscoredKeys(
  value: unknown,
  source: string,
  path = '$',
) {
  if (Array.isArray(value)) {
    value.forEach((item, index) =>
      assertNoUnderscoredKeys(item, source, `${path}[${index}]`),
    )
    return
  }

  if (!isObject(value)) {
    return
  }

  Object.entries(value).forEach(([key, child]) => {
    if (key.includes('_')) {
      throw new Error(`${source} contains non-camelCase key ${path}.${key}`)
    }
    assertNoUnderscoredKeys(child, source, `${path}.${key}`)
  })
}

function assertSseFixtureFrame(
  value: unknown,
  source: string,
): asserts value is SseFixtureFrame {
  const frame = requireObject(value, source)
  const event = requireString(frame, 'event', source)
  const data = requireObject(frame.data, `${source}.data`)
  const type = requireString(data, 'type', `${source}.data`)

  if (event !== type) {
    throw new Error(`${source} event must match data.type`)
  }

  assertSsePayload(data, `${source}.data`)
}

function assertSsePayload(
  value: unknown,
  source: string,
): asserts value is SsePayload {
  const payload = requireObject(value, source)
  const type = requireString(payload, 'type', source)
  requireString(payload, 'planId', source)

  switch (type) {
    case 'heartbeat':
      requireNumber(payload, 'timestamp', source)
      return
    case 'state_change':
      requireString(payload, 'from', source)
      requireString(payload, 'to', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'tool_call':
      requireString(payload, 'tool', source)
      requireString(payload, 'status', source)
      requireString(payload, 'inputSummary', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'tool_result':
      requireString(payload, 'tool', source)
      requireString(payload, 'status', source)
      requireString(payload, 'outputSummary', source)
      requireNumber(payload, 'latencyMs', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'clarification_request':
      requireString(payload, 'question', source)
      requireString(payload, 'field', source)
      requireStringArray(payload, 'options', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'replan':
      requireString(payload, 'reason', source)
      requireNumber(payload, 'replanCount', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'adjust_result':
      requireStringArray(payload, 'affectedSlots', source)
      requireString(payload, 'summary', source)
      assertPlan(payload.plan, `${source}.plan`)
      return
    case 'plan_ready':
      assertPlanReadyEvent(payload, source)
      return
    case 'execute_result':
      requireString(payload, 'actionId', source)
      requireString(payload, 'actionType', source)
      requireString(payload, 'status', source)
      requireNullableString(payload, 'confirmationNo', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'done':
      requireString(payload, 'summary', source)
      requireNumber(payload, 'timestamp', source)
      return
    case 'error':
      requireString(payload, 'code', source)
      requireString(payload, 'message', source)
      requireNumber(payload, 'timestamp', source)
      return
    default:
      throw new Error(`${source} has unsupported SSE type: ${type}`)
  }
}

function assertPlanReadyEvent(
  value: unknown,
  source: string,
): asserts value is PlanReadyEvent {
  const event = requireObject(value, source)
  const type = requireString(event, 'type', source)

  if (type !== 'plan_ready') {
    throw new Error(`${source} must be a plan_ready payload`)
  }

  const planId = requireString(event, 'planId', source)
  assertPlan(event.plan, `${source}.plan`)

  if (event.plan.planId !== planId) {
    throw new Error(`${source} planId must match plan.planId`)
  }
}

function assertPlan(value: unknown, source: string): asserts value is Plan {
  const plan = requireObject(value, source)

  requireString(plan, 'planId', source)
  requireScenario(plan, 'scenario', source)
  requireString(plan, 'status', source)
  requireBoolean(plan, 'isPlanB', source)
  requireNullableString(plan, 'planBReason', source)
  requireString(plan, 'summary', source)
  requireString(plan, 'shareMessage', source)
  requireNumber(plan, 'totalDurationHours', source)
  requireNumber(plan, 'replanCount', source)
  requireString(plan, 'createdAt', source)

  const timeline = requireArray(plan, 'timeline', source)
  timeline.forEach((slot, index) => assertTimeSlot(slot, `${source}.timeline[${index}]`))

  const actions = requireArray(plan, 'actions', source)
  actions.forEach((action, index) =>
    assertActionItem(action, `${source}.actions[${index}]`),
  )
}

function assertTimeSlot(value: unknown, source: string) {
  const slot = requireObject(value, source)
  requireNumber(slot, 'order', source)
  requireString(slot, 'type', source)
  requireString(slot, 'title', source)
  requireString(slot, 'startTime', source)
  requireString(slot, 'endTime', source)
  requireStringArray(slot, 'notes', source)

  const poi = requireObject(slot.poi, `${source}.poi`)
  requireString(poi, 'id', `${source}.poi`)
  requireString(poi, 'name', `${source}.poi`)
  requireString(poi, 'category', `${source}.poi`)
  requireString(poi, 'address', `${source}.poi`)
  requireNumber(poi, 'rating', `${source}.poi`)
  requireNumber(poi, 'distanceMinutes', `${source}.poi`)
  requireStringArray(poi, 'tags', `${source}.poi`)
  requireString(poi, 'availabilityStatus', `${source}.poi`)
  requireNullableNumber(poi, 'waitMinutes', `${source}.poi`)
}

function assertActionItem(value: unknown, source: string) {
  const action = requireObject(value, source)
  requireString(action, 'actionId', source)
  requireString(action, 'actionType', source)
  requireNullableString(action, 'targetPoiId', source)
  requireString(action, 'description', source)
  requireString(action, 'status', source)
  requireNullableString(action, 'confirmationNo', source)
}

function requireObject(value: unknown, source: string): JsonObject {
  if (!isObject(value)) {
    throw new Error(`${source} must be an object`)
  }
  return value
}

function requireArray(
  record: JsonObject,
  key: string,
  source: string,
): unknown[] {
  const value = record[key]
  if (!Array.isArray(value)) {
    throw new Error(`${source}.${key} must be an array`)
  }
  return value
}

function requireString(record: JsonObject, key: string, source: string): string {
  const value = record[key]
  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`${source}.${key} must be a non-empty string`)
  }
  return value
}

function requireScenario(record: JsonObject, key: string, source: string) {
  const value = requireString(record, key, source)
  if (value !== 'family' && value !== 'friends') {
    throw new Error(`${source}.${key} must be family or friends`)
  }
}

function requireBoolean(record: JsonObject, key: string, source: string) {
  if (typeof record[key] !== 'boolean') {
    throw new Error(`${source}.${key} must be a boolean`)
  }
}

function requireNumber(record: JsonObject, key: string, source: string): number {
  const value = record[key]
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw new Error(`${source}.${key} must be a number`)
  }
  return value
}

function requireNullableNumber(record: JsonObject, key: string, source: string) {
  const value = record[key]
  if (value !== null && (typeof value !== 'number' || Number.isNaN(value))) {
    throw new Error(`${source}.${key} must be a number or null`)
  }
}

function requireNullableString(record: JsonObject, key: string, source: string) {
  const value = record[key]
  if (value !== null && typeof value !== 'string') {
    throw new Error(`${source}.${key} must be a string or null`)
  }
}

function requireStringArray(record: JsonObject, key: string, source: string) {
  const values = requireArray(record, key, source)
  if (!values.every((item) => typeof item === 'string')) {
    throw new Error(`${source}.${key} must contain only strings`)
  }
}

function isObject(value: unknown): value is JsonObject {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
