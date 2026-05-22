export type Scenario = 'family' | 'friends'

export type AgentState =
  | 'START'
  | 'INTENT'
  | 'CLARIFY'
  | 'SKELETON'
  | 'RECALL'
  | 'VALIDATE'
  | 'REPLAN'
  | 'PACK'
  | 'CONFIRM'
  | 'ADJUST'
  | 'EXECUTE'
  | 'DEGRADE'
  | 'DONE'
  | 'FAILED'

export type ActionStatus =
  | 'pending'
  | 'executing'
  | 'success'
  | 'failed'
  | 'manual'

export type ActionType =
  | 'buy_ticket'
  | 'reserve_table'
  | 'take_number'
  | 'schedule_delivery'
  | 'add_note'
  | 'send_message'
  | 'cancel_booking'

export interface ApiErrorShape {
  error: string
  message: string
  details?: Record<string, unknown>
}

export interface CreatePlanRequest {
  text: string
  scenario: Scenario
  origin?: string
}

export interface CreatePlanResponse {
  planId: string
  status: 'processing'
}

export interface ExecutePlanRequest {
  confirmed: boolean
}

export interface ExecutePlanResponse {
  planId: string
  status: 'executing'
  message: string
}

export interface ClarifyPlanRequest {
  reply: string
}

export interface ClarifyPlanResponse {
  planId: string
  status: 'processing'
  message: string
}

export interface AdjustPlanRequest {
  instruction: string
}

export interface AdjustPlanResponse {
  planId: string
  status: 'adjusting'
  message: string
}

export interface DebugScenarioRequest {
  restaurantFull: boolean
  routeTooFar: boolean
  bookingFail: boolean
  ageMismatch: boolean
}

export interface DebugScenarioResponse {
  updated: DebugScenarioRequest
}

export interface Poi {
  id: string
  name: string
  category: string
  address: string
  rating: number
  distanceMinutes: number
  tags: string[]
  availabilityStatus: string
  waitMinutes: number | null
}

export interface TimeSlot {
  order: number
  type: string
  title: string
  poi: Poi
  startTime: string
  endTime: string
  notes: string[]
}

export interface ActionItem {
  actionId: string
  actionType: ActionType
  targetPoiId: string | null
  description: string
  status: ActionStatus
  confirmationNo: string | null
}

export interface Plan {
  planId: string
  scenario: Scenario
  status: AgentState
  isPlanB: boolean
  planBReason: string | null
  summary: string
  timeline: TimeSlot[]
  actions: ActionItem[]
  shareMessage: string
  totalDurationHours: number
  replanCount: number
  createdAt: string
}

export interface HeartbeatEvent {
  type: 'heartbeat'
  planId: string
  timestamp: number
}

export interface StateChangeEvent {
  type: 'state_change'
  planId: string
  from: AgentState
  to: AgentState
  timestamp: number
}

export interface ToolCallEvent {
  type: 'tool_call'
  planId: string
  tool: string
  status: string
  inputSummary: string
  timestamp: number
}

export interface ToolResultEvent {
  type: 'tool_result'
  planId: string
  tool: string
  status: string
  outputSummary: string
  latencyMs: number
  timestamp: number
}

export interface ClarificationRequestEvent {
  type: 'clarification_request'
  planId: string
  question: string
  field: string
  options: string[]
  timestamp: number
}

export interface ReplanEvent {
  type: 'replan'
  planId: string
  reason: string
  replanCount: number
  timestamp: number
}

export interface AdjustResultEvent {
  type: 'adjust_result'
  planId: string
  affectedSlots: string[]
  summary: string
  plan: Plan
}

export interface PlanReadyEvent {
  type: 'plan_ready'
  planId: string
  plan: Plan
}

export interface ExecuteResultEvent {
  type: 'execute_result'
  planId: string
  actionId: string
  actionType: ActionType
  status: ActionStatus
  confirmationNo: string | null
  timestamp: number
}

export interface DoneEvent {
  type: 'done'
  planId: string
  summary: string
  timestamp: number
}

export interface PlanErrorEvent {
  type: 'error'
  planId: string
  code: string
  message: string
  timestamp: number
}

export type SsePayload =
  | HeartbeatEvent
  | StateChangeEvent
  | ToolCallEvent
  | ToolResultEvent
  | ClarificationRequestEvent
  | ReplanEvent
  | AdjustResultEvent
  | PlanReadyEvent
  | ExecuteResultEvent
  | DoneEvent
  | PlanErrorEvent

export interface SseFixtureFrame<TPayload extends SsePayload = SsePayload> {
  event: TPayload['type']
  data: TPayload
}
