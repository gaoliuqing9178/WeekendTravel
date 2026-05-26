import { onScopeDispose, ref } from 'vue'

import type {
  PlannerApiClient,
} from '@/api/client'
import type { SseFixtureFrame, SsePayload } from '@/api/types'

export type SseConnectionState =
  | 'idle'
  | 'connecting'
  | 'open'
  | 'retrying'
  | 'closed'
  | 'error'

export interface SseEventMeta {
  id: string
  source: 'fixture' | 'eventsource'
}

export interface UseSseOptions {
  client: PlannerApiClient
  mockIntervalMs?: number
  onEvent: (payload: SsePayload, meta: SseEventMeta) => void
  onConnectionStateChange?: (state: SseConnectionState) => void
  onError?: (message: string) => void
}

const sseEventTypes: SsePayload['type'][] = [
  'heartbeat',
  'state_change',
  'tool_call',
  'tool_result',
  'clarification_request',
  'replan',
  'adjust_result',
  'plan_ready',
  'execute_result',
  'done',
  'error',
]

export function useSSE(options: UseSseOptions) {
  const connectionState = ref<SseConnectionState>('idle')
  const errorMessage = ref<string | null>(null)
  const activePlanId = ref<string | null>(null)

  let eventSource: EventSource | null = null
  let mockTimer: number | null = null
  let runToken = 0
  let closedByClient = false

  async function connect(planId: string) {
    cleanup()

    activePlanId.value = planId
    errorMessage.value = null
    closedByClient = false
    runToken += 1
    setConnectionState('connecting')

    try {
      if (options.client.mode === 'mock') {
        await connectFixtureStream(runToken)
        return
      }

      connectEventSource(planId)
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      errorMessage.value = message
      setConnectionState('error')
      options.onError?.(message)
    }
  }

  function close(nextState: SseConnectionState = 'closed') {
    closedByClient = true
    runToken += 1
    cleanup()
    activePlanId.value = null
    setConnectionState(nextState)
  }

  async function connectFixtureStream(token: number) {
    const frames = await options.client.getSseFixtureFrames()
    const intervalMs = options.mockIntervalMs ?? 120

    if (token !== runToken) {
      return
    }

    setConnectionState('open')
    playFixtureFrames(frames, token, intervalMs)
  }

  function playFixtureFrames(
    frames: SseFixtureFrame[],
    token: number,
    intervalMs: number,
  ) {
    let index = 0

    const tick = () => {
      if (token !== runToken) {
        return
      }

      const frame = frames[index]
      if (!frame) {
        setConnectionState('closed')
        return
      }

      options.onEvent(frame.data, {
        id: `fixture-${index}-${frame.data.type}`,
        source: 'fixture',
      })

      index += 1
      mockTimer = window.setTimeout(tick, intervalMs)
    }

    mockTimer = window.setTimeout(tick, 0)
  }

  function connectEventSource(planId: string) {
    const source = options.client.openPlanStream(planId)
    eventSource = source

    source.onopen = () => {
      setConnectionState('open')
    }

    source.onerror = () => {
      if (closedByClient) {
        return
      }

      setConnectionState('retrying')
    }

    sseEventTypes.forEach((type) => {
      source.addEventListener(type, (event) => {
        handleMessageEvent(event as MessageEvent<string>, type)
      })
    })

    source.onmessage = (event) => {
      handleMessageEvent(event)
    }
  }

  function handleMessageEvent(
    event: MessageEvent<string>,
    expectedType?: SsePayload['type'],
  ) {
    if (
      typeof event.data !== 'string' ||
      event.data.trim().length === 0 ||
      event.data === 'undefined'
    ) {
      return
    }

    try {
      const payload = parsePayload(event.data, expectedType)
      options.onEvent(payload, {
        id: `stream-${payload.type}-${payload.planId}-${event.lastEventId || Date.now()}`,
        source: 'eventsource',
      })

      if (payload.type === 'done' || payload.type === 'error') {
        close('closed')
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      errorMessage.value = message
      setConnectionState('error')
      options.onError?.(message)
      close('error')
    }
  }

  function parsePayload(
    raw: string,
    expectedType?: SsePayload['type'],
  ): SsePayload {
    const value = JSON.parse(raw) as unknown

    if (!isObject(value) || typeof value.type !== 'string') {
      throw new Error('SSE payload must be an object with type')
    }

    if (expectedType && value.type !== expectedType) {
      throw new Error(`SSE event ${expectedType} does not match ${value.type}`)
    }

    return value as unknown as SsePayload
  }

  function cleanup() {
    if (mockTimer !== null) {
      window.clearTimeout(mockTimer)
      mockTimer = null
    }

    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  function setConnectionState(nextState: SseConnectionState) {
    connectionState.value = nextState
    options.onConnectionStateChange?.(nextState)
  }

  onScopeDispose(() => {
    close('closed')
  })

  return {
    activePlanId,
    connectionState,
    errorMessage,
    connect,
    close,
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
