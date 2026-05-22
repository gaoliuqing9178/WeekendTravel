import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '..', '..')
const fixturesDir = path.join(repoRoot, 'docs', 'fixtures')

const planFixtureFiles = [
  'plan-ready-family.json',
  'plan-ready-friends.json',
]

const requiredPlanKeys = [
  'planId',
  'scenario',
  'status',
  'isPlanB',
  'planBReason',
  'summary',
  'timeline',
  'actions',
  'shareMessage',
  'totalDurationHours',
  'replanCount',
  'createdAt',
]

const requiredActionKeys = [
  'actionId',
  'actionType',
  'targetPoiId',
  'description',
  'status',
  'confirmationNo',
]

const requiredPoiKeys = [
  'id',
  'name',
  'category',
  'address',
  'rating',
  'distanceMinutes',
  'tags',
  'availabilityStatus',
  'waitMinutes',
]

const requiredSseTypes = new Set([
  'heartbeat',
  'state_change',
  'tool_call',
  'tool_result',
  'replan',
  'plan_ready',
])

for (const fileName of planFixtureFiles) {
  const filePath = path.join(fixturesDir, fileName)
  const fixture = JSON.parse(await readFile(filePath, 'utf8'))
  assertNoUnderscoredKeys(fixture, fileName)
  assertPlanReadyFixture(fixture, fileName)
  console.log(`[ok] docs/fixtures/${fileName} -> ${fixture.plan.planId}`)
}

const sseFileName = 'sse-events.jsonl'
const sseText = await readFile(path.join(fixturesDir, sseFileName), 'utf8')
const sseLines = sseText
  .split(/\r?\n/)
  .map((line) => line.trim())
  .filter(Boolean)

if (sseLines.length === 0) {
  throw new Error(`${sseFileName} must contain at least one event`)
}

const seenTypes = new Set()

sseLines.forEach((line, index) => {
  const source = `${sseFileName}:${index + 1}`
  const frame = JSON.parse(line)
  assertNoUnderscoredKeys(frame, source)
  assertObject(frame, source)
  assertString(frame.event, `${source}.event`)
  assertObject(frame.data, `${source}.data`)
  assertString(frame.data.type, `${source}.data.type`)

  if (frame.event !== frame.data.type) {
    throw new Error(`${source} event must match data.type`)
  }

  seenTypes.add(frame.data.type)
  assertSsePayload(frame.data, source)
})

for (const requiredType of requiredSseTypes) {
  if (!seenTypes.has(requiredType)) {
    throw new Error(`${sseFileName} is missing ${requiredType}`)
  }
}

console.log(
  `[ok] docs/fixtures/${sseFileName} -> ${sseLines.length} JSONL events`,
)

function assertPlanReadyFixture(value, source) {
  assertObject(value, source)
  assertEqual(value.type, 'plan_ready', `${source}.type`)
  assertString(value.planId, `${source}.planId`)
  assertPlan(value.plan, `${source}.plan`)
  assertEqual(value.plan.planId, value.planId, `${source}.planId`)
}

function assertPlan(value, source) {
  assertObject(value, source)

  for (const key of requiredPlanKeys) {
    if (!(key in value)) {
      throw new Error(`${source}.${key} is required`)
    }
  }

  assertString(value.planId, `${source}.planId`)
  assertScenario(value.scenario, `${source}.scenario`)
  assertString(value.status, `${source}.status`)
  assertBoolean(value.isPlanB, `${source}.isPlanB`)
  assertNullableString(value.planBReason, `${source}.planBReason`)
  assertString(value.summary, `${source}.summary`)
  assertString(value.shareMessage, `${source}.shareMessage`)
  assertNumber(value.totalDurationHours, `${source}.totalDurationHours`)
  assertNumber(value.replanCount, `${source}.replanCount`)
  assertString(value.createdAt, `${source}.createdAt`)
  assertArray(value.timeline, `${source}.timeline`)
  assertArray(value.actions, `${source}.actions`)

  value.timeline.forEach((slot, index) =>
    assertTimeSlot(slot, `${source}.timeline[${index}]`),
  )
  value.actions.forEach((action, index) =>
    assertAction(action, `${source}.actions[${index}]`),
  )
}

function assertTimeSlot(value, source) {
  assertObject(value, source)
  assertNumber(value.order, `${source}.order`)
  assertString(value.type, `${source}.type`)
  assertString(value.title, `${source}.title`)
  assertString(value.startTime, `${source}.startTime`)
  assertString(value.endTime, `${source}.endTime`)
  assertStringArray(value.notes, `${source}.notes`)

  assertObject(value.poi, `${source}.poi`)
  for (const key of requiredPoiKeys) {
    if (!(key in value.poi)) {
      throw new Error(`${source}.poi.${key} is required`)
    }
  }
  assertString(value.poi.id, `${source}.poi.id`)
  assertString(value.poi.name, `${source}.poi.name`)
  assertString(value.poi.category, `${source}.poi.category`)
  assertString(value.poi.address, `${source}.poi.address`)
  assertNumber(value.poi.rating, `${source}.poi.rating`)
  assertNumber(value.poi.distanceMinutes, `${source}.poi.distanceMinutes`)
  assertStringArray(value.poi.tags, `${source}.poi.tags`)
  assertString(value.poi.availabilityStatus, `${source}.poi.availabilityStatus`)
  assertNullableNumber(value.poi.waitMinutes, `${source}.poi.waitMinutes`)
}

function assertAction(value, source) {
  assertObject(value, source)
  for (const key of requiredActionKeys) {
    if (!(key in value)) {
      throw new Error(`${source}.${key} is required`)
    }
  }
  assertString(value.actionId, `${source}.actionId`)
  assertString(value.actionType, `${source}.actionType`)
  assertNullableString(value.targetPoiId, `${source}.targetPoiId`)
  assertString(value.description, `${source}.description`)
  assertString(value.status, `${source}.status`)
  assertNullableString(value.confirmationNo, `${source}.confirmationNo`)
}

function assertSsePayload(value, source) {
  assertObject(value, `${source}.data`)
  assertString(value.type, `${source}.data.type`)
  assertString(value.planId, `${source}.data.planId`)

  if (value.type === 'plan_ready') {
    assertPlan(value.plan, `${source}.data.plan`)
    return
  }

  if (value.type === 'tool_result') {
    assertNumber(value.latencyMs, `${source}.data.latencyMs`)
  }

  if (value.type === 'adjust_result') {
    assertStringArray(value.affectedSlots, `${source}.data.affectedSlots`)
    assertPlan(value.plan, `${source}.data.plan`)
  }

  if ('timestamp' in value) {
    assertNumber(value.timestamp, `${source}.data.timestamp`)
  }
}

function assertNoUnderscoredKeys(value, source, keyPath = '$') {
  if (Array.isArray(value)) {
    value.forEach((item, index) =>
      assertNoUnderscoredKeys(item, source, `${keyPath}[${index}]`),
    )
    return
  }

  if (!isObject(value)) {
    return
  }

  for (const [key, child] of Object.entries(value)) {
    if (key.includes('_')) {
      throw new Error(`${source} contains non-camelCase key ${keyPath}.${key}`)
    }
    assertNoUnderscoredKeys(child, source, `${keyPath}.${key}`)
  }
}

function assertObject(value, source) {
  if (!isObject(value)) {
    throw new Error(`${source} must be an object`)
  }
}

function assertArray(value, source) {
  if (!Array.isArray(value)) {
    throw new Error(`${source} must be an array`)
  }
}

function assertString(value, source) {
  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`${source} must be a non-empty string`)
  }
}

function assertStringArray(value, source) {
  assertArray(value, source)
  if (!value.every((item) => typeof item === 'string')) {
    throw new Error(`${source} must contain only strings`)
  }
}

function assertNullableString(value, source) {
  if (value !== null && typeof value !== 'string') {
    throw new Error(`${source} must be a string or null`)
  }
}

function assertNumber(value, source) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw new Error(`${source} must be a number`)
  }
}

function assertNullableNumber(value, source) {
  if (value !== null && (typeof value !== 'number' || Number.isNaN(value))) {
    throw new Error(`${source} must be a number or null`)
  }
}

function assertBoolean(value, source) {
  if (typeof value !== 'boolean') {
    throw new Error(`${source} must be a boolean`)
  }
}

function assertScenario(value, source) {
  if (value !== 'family' && value !== 'friends') {
    throw new Error(`${source} must be family or friends`)
  }
}

function assertEqual(actual, expected, source) {
  if (actual !== expected) {
    throw new Error(`${source} must be ${expected}`)
  }
}

function isObject(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
