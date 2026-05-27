import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { createPlannerApiClient } from '@/api/client'
import { useSSE, type SseConnectionState } from '@/composables/useSSE'
import type {
  AgentState,
  ClarificationRequestEvent,
  ExecuteResultEvent,
  Plan,
  Scenario,
  SsePayload,
} from '@/api/types'

export interface LogEvent {
  id: string
  type: SsePayload['type'] | 'client_event'
  title: string
  detail: string
  timestamp: number
}

const familyPrompt =
  '今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。'

const friendsPrompt =
  '今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。'

const plannerClient = createPlannerApiClient()

export const usePlannerStore = defineStore('planner', () => {
  const planId = ref<string | null>(null)
  const agentState = ref<AgentState>('START')
  const scenario = ref<Scenario>('family')
  const origin = ref('当前位置')
  const userInput = ref(familyPrompt)
  const connectionState = ref<SseConnectionState>('idle')
  const currentPlan = ref<Plan | null>(null)
  const pendingClarification = ref<ClarificationRequestEvent | null>(null)
  const errorMessage = ref<string | null>(null)
  const submitMessage = ref<string | null>(null)
  const executeMessage = ref<string | null>(null)
  const isExecuting = ref(false)
  const logEvents = ref<LogEvent[]>([createReadyEvent()])

  let eventSequence = 0
  let executionSequence = 0

  const scenarioLabel = computed(() =>
    scenario.value === 'family' ? '家庭场景' : '朋友场景',
  )

  const isSubmitting = computed(() =>
    ['connecting', 'open', 'retrying'].includes(connectionState.value),
  )

  const canSubmit = computed(
    () => userInput.value.trim().length > 0 && !isSubmitting.value,
  )

  const canConfirmPlan = computed(
    () =>
      Boolean(planId.value && currentPlan.value) &&
      agentState.value === 'CONFIRM' &&
      !isExecuting.value,
  )

  const isRunning = computed(() => isSubmitting.value || isExecuting.value)

  const planBReason = computed(() => {
    if (currentPlan.value?.isPlanB) {
      return currentPlan.value.planBReason
    }

    const replanEvent = [...logEvents.value]
      .reverse()
      .find((event) => event.type === 'replan')

    return replanEvent?.detail ?? null
  })

  const sse = useSSE({
    client: plannerClient,
    mockIntervalMs: 90,
    onConnectionStateChange(nextState) {
      connectionState.value = nextState
      updateSubmitMessageForConnection(nextState)
    },
    onEvent(payload, meta) {
      handleSsePayload(payload, meta.id)
      closeMockPlanningStreamAtConfirm(payload)
    },
    onError(message) {
      errorMessage.value = message
      appendLogEvent({
        id: nextLogId('evt-sse-error'),
        type: 'error',
        title: 'SSE 连接提示',
        detail: message,
        timestamp: Date.now(),
      })
    },
  })

  function setScenario(nextScenario: Scenario) {
    scenario.value = nextScenario
    userInput.value = nextScenario === 'family' ? familyPrompt : friendsPrompt
  }

  async function previewSkeletonFlow() {
    await submitPlan()
  }

  async function submitPlan() {
    if (!canSubmit.value) {
      return
    }

    sse.close('closed')
    eventSequence = 0
    planId.value = null
    agentState.value = 'START'
    connectionState.value = 'connecting'
    currentPlan.value = null
    pendingClarification.value = null
    errorMessage.value = null
    submitMessage.value = '正在创建 plan...'
    executeMessage.value = null
    isExecuting.value = false
    executionSequence += 1
    logEvents.value = [
      {
        id: nextLogId('evt-create-plan'),
        type: 'client_event',
        title: '提交规划请求',
        detail:
          plannerClient.mode === 'mock'
            ? `${scenarioLabel.value}使用本地 fixture 创建 plan，并回放日志流。`
            : `${scenarioLabel.value}正在调用 POST /api/plan。`,
        timestamp: Date.now(),
      },
    ]

    try {
      const created = await plannerClient.createPlan({
        text: userInput.value.trim(),
        scenario: scenario.value,
        origin: origin.value.trim() || undefined,
      })

      planId.value = created.planId
      agentState.value = 'INTENT'
      submitMessage.value = `已创建 ${created.planId}，正在连接日志流。`
      appendLogEvent({
        id: nextLogId('evt-plan-created'),
        type: 'client_event',
        title: 'plan 已创建',
        detail: `planId=${created.planId}，status=${created.status}`,
        timestamp: Date.now(),
      })

      await sse.connect(created.planId)
    } catch (error) {
      agentState.value = 'FAILED'
      connectionState.value = 'error'
      errorMessage.value = error instanceof Error ? error.message : String(error)
      submitMessage.value = null
      appendLogEvent({
        id: nextLogId('evt-client-error'),
        type: 'error',
        title: '规划启动失败',
        detail: errorMessage.value,
        timestamp: Date.now(),
      })
    }
  }

  function resetSkeletonFlow() {
    sse.close('closed')
    eventSequence = 0
    executionSequence += 1
    planId.value = null
    agentState.value = 'START'
    connectionState.value = 'idle'
    currentPlan.value = null
    pendingClarification.value = null
    errorMessage.value = null
    submitMessage.value = null
    executeMessage.value = null
    isExecuting.value = false
    logEvents.value = [createReadyEvent()]
  }

  async function confirmPlanExecution() {
    if (!canConfirmPlan.value || !planId.value || !currentPlan.value) {
      return
    }

    const token = executionSequence + 1
    executionSequence = token
    isExecuting.value = true
    errorMessage.value = null
    executeMessage.value = '正在提交执行确认...'
    agentState.value = 'EXECUTE'
    markPendingActionsExecuting()
    appendLogEvent({
      id: nextLogId('evt-confirm-execute'),
      type: 'client_event',
      title: '确认执行',
      detail: `planId=${planId.value}，confirmed=true`,
      timestamp: Date.now(),
    })

    try {
      const response = await plannerClient.executePlan(planId.value, {
        confirmed: true,
      })
      executeMessage.value = response.message
      appendLogEvent({
        id: nextLogId('evt-execute-started'),
        type: 'client_event',
        title: '执行已提交',
        detail: `planId=${response.planId}，status=${response.status}`,
        timestamp: Date.now(),
      })

      if (plannerClient.mode === 'mock') {
        await playMockExecutionEvents(token)
      }
    } catch (error) {
      if (token !== executionSequence) {
        return
      }

      isExecuting.value = false
      agentState.value = 'FAILED'
      errorMessage.value = error instanceof Error ? error.message : String(error)
      executeMessage.value = null
      setCurrentPlanStatus('FAILED')
      appendLogEvent({
        id: nextLogId('evt-execute-error'),
        type: 'error',
        title: '执行确认失败',
        detail: errorMessage.value,
        timestamp: Date.now(),
      })
    }
  }

  function handleSsePayload(payload: SsePayload, fallbackId: string) {
    appendLogEvent(toLogEvent(payload, fallbackId))
    applySsePayload(payload)
  }

  function applySsePayload(payload: SsePayload) {
    planId.value = payload.planId

    switch (payload.type) {
      case 'heartbeat':
      case 'tool_call':
      case 'tool_result':
        return
      case 'state_change':
        agentState.value = payload.to
        setCurrentPlanStatus(payload.to)
        return
      case 'clarification_request':
        pendingClarification.value = payload
        agentState.value = 'CLARIFY'
        return
      case 'replan':
        agentState.value = 'REPLAN'
        setCurrentPlanStatus('REPLAN')
        return
      case 'adjust_result':
        currentPlan.value = payload.plan
        pendingClarification.value = null
        agentState.value = payload.plan.status
        return
      case 'plan_ready':
        currentPlan.value = payload.plan
        pendingClarification.value = null
        errorMessage.value = null
        agentState.value = payload.plan.status
        return
      case 'execute_result':
        updateActionStatus(payload)
        agentState.value = 'EXECUTE'
        setCurrentPlanStatus('EXECUTE')
        return
      case 'done':
        pendingClarification.value = null
        agentState.value = 'DONE'
        isExecuting.value = false
        executeMessage.value = payload.summary
        setCurrentPlanStatus('DONE')
        return
      case 'error':
        errorMessage.value = payload.message
        agentState.value = payload.code === 'DEGRADE' ? 'DEGRADE' : 'FAILED'
        isExecuting.value = false
        setCurrentPlanStatus(agentState.value)
        return
    }
  }

  function updateActionStatus(payload: ExecuteResultEvent) {
    if (!currentPlan.value) {
      return
    }

    currentPlan.value = {
      ...currentPlan.value,
      actions: currentPlan.value.actions.map((action) =>
        action.actionId === payload.actionId
          ? {
              ...action,
              status: payload.status,
              confirmationNo: payload.confirmationNo,
            }
          : action,
      ),
    }
  }

  function markPendingActionsExecuting() {
    if (!currentPlan.value) {
      return
    }

    currentPlan.value = {
      ...currentPlan.value,
      status: 'EXECUTE',
      actions: currentPlan.value.actions.map((action) =>
        action.status === 'pending'
          ? {
              ...action,
              status: 'executing',
            }
          : action,
      ),
    }
  }

  function setCurrentPlanStatus(status: AgentState) {
    if (!currentPlan.value) {
      return
    }

    currentPlan.value = {
      ...currentPlan.value,
      status,
    }
  }

  function closeMockPlanningStreamAtConfirm(payload: SsePayload) {
    if (plannerClient.mode !== 'mock') {
      return
    }

    if (payload.type === 'state_change' && payload.to === 'CONFIRM') {
      sse.close('closed')
    }
  }

  async function playMockExecutionEvents(token: number) {
    const frames = await plannerClient.getSseFixtureFrames()
    const executionFrames = frames.filter(
      (frame) => frame.data.type === 'execute_result' || frame.data.type === 'done',
    )

    for (const frame of executionFrames) {
      if (token !== executionSequence) {
        return
      }

      await wait(180)

      if (token !== executionSequence) {
        return
      }

      handleSsePayload(frame.data, `fixture-execute-${frame.data.type}`)
    }

    if (token === executionSequence) {
      isExecuting.value = false
    }
  }

  function appendLogEvent(event: LogEvent) {
    logEvents.value = [...logEvents.value, event]
  }

  function updateSubmitMessageForConnection(nextState: SseConnectionState) {
    if (errorMessage.value) {
      return
    }

    if (!planId.value) {
      return
    }

    switch (nextState) {
      case 'connecting':
        submitMessage.value = '正在连接日志流...'
        return
      case 'open':
        submitMessage.value = `规划中：${planId.value}`
        return
      case 'retrying':
        submitMessage.value = `日志流重连中：${planId.value}`
        return
      case 'closed':
        submitMessage.value = `本轮日志流已结束：${planId.value}`
        return
      case 'idle':
      case 'error':
        return
    }
  }

  function nextLogId(prefix: string) {
    eventSequence += 1
    return `${prefix}-${eventSequence}`
  }

  function createReadyEvent(): LogEvent {
    return {
      id: 'evt-ready',
      type: 'client_event',
      title: 'useSSE 已就绪',
      detail:
        plannerClient.mode === 'mock'
          ? '当前使用 mock fixture mode，不访问后端网络。'
          : '当前使用 real API mode，提交后会连接后端 SSE。',
      timestamp: Date.now(),
    }
  }

  function toLogEvent(payload: SsePayload, fallbackId: string): LogEvent {
    const timestamp = 'timestamp' in payload ? payload.timestamp : Date.now()

    switch (payload.type) {
      case 'heartbeat':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: 'heartbeat',
          detail: `planId=${payload.planId}`,
          timestamp,
        }
      case 'state_change':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: `${payload.from} -> ${payload.to}`,
          detail: `状态已进入 ${payload.to}`,
          timestamp,
        }
      case 'tool_call':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: `${payload.tool} start`,
          detail: payload.inputSummary,
          timestamp,
        }
      case 'tool_result':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: `${payload.tool} ${payload.status}`,
          detail: `${payload.outputSummary} (${payload.latencyMs}ms)`,
          timestamp,
        }
      case 'clarification_request':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: 'clarification_request',
          detail: payload.question,
          timestamp,
        }
      case 'replan':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: `Plan B #${payload.replanCount}`,
          detail: payload.reason,
          timestamp,
        }
      case 'adjust_result':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: 'adjust_result',
          detail: payload.summary,
          timestamp,
        }
      case 'plan_ready':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: 'plan_ready',
          detail: payload.plan.summary,
          timestamp,
        }
      case 'execute_result':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: `${payload.actionType} ${payload.status}`,
          detail: payload.confirmationNo ?? payload.actionId,
          timestamp,
        }
      case 'done':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: 'done',
          detail: payload.summary,
          timestamp,
        }
      case 'error':
        return {
          id: nextLogId(fallbackId),
          type: payload.type,
          title: payload.code,
          detail: payload.message,
          timestamp,
        }
    }
  }

  return {
    planId,
    agentState,
    scenario,
    origin,
    userInput,
    connectionState,
    currentPlan,
    pendingClarification,
    errorMessage,
    submitMessage,
    logEvents,
    scenarioLabel,
    isSubmitting,
    canSubmit,
    isExecuting,
    canConfirmPlan,
    isRunning,
    canStart: canSubmit,
    planBReason,
    executeMessage,
    apiMode: plannerClient.mode,
    setScenario,
    submitPlan,
    confirmPlanExecution,
    previewSkeletonFlow,
    resetSkeletonFlow,
  }
})

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}
