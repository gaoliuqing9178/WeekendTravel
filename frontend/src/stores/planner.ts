import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { createPlannerApiClient } from '@/api/client'
import type { AgentState, Plan, Scenario, SsePayload } from '@/api/types'

type ConnectionState =
  | 'idle'
  | 'connecting'
  | 'open'
  | 'retrying'
  | 'closed'
  | 'error'

interface LogEvent {
  id: string
  type: SsePayload['type'] | 'fixture_loaded'
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
  const connectionState = ref<ConnectionState>('idle')
  const currentPlan = ref<Plan | null>(null)
  const logEvents = ref<LogEvent[]>([
    {
      id: 'evt-ready',
      type: 'fixture_loaded',
      title: 'API client 已就绪',
      detail: '当前默认使用 mock fixture mode，不访问后端网络。',
      timestamp: Date.now(),
    },
  ])

  const scenarioLabel = computed(() =>
    scenario.value === 'family' ? '家庭场景' : '朋友场景',
  )

  const canStart = computed(() => userInput.value.trim().length > 0)

  function setScenario(nextScenario: Scenario) {
    scenario.value = nextScenario
    userInput.value = nextScenario === 'family' ? familyPrompt : friendsPrompt
  }

  async function previewSkeletonFlow() {
    if (!canStart.value) {
      return
    }

    connectionState.value = 'connecting'
    currentPlan.value = null
    logEvents.value = [
      {
        id: 'evt-create-plan',
        type: 'fixture_loaded',
        title: '创建规划请求',
        detail:
          plannerClient.mode === 'mock'
            ? `${scenarioLabel.value}将读取本地 fixture。`
            : `${scenarioLabel.value}将调用真实后端 API。`,
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
      connectionState.value = 'open'

      if (plannerClient.mode === 'real') {
        logEvents.value = [
          ...logEvents.value,
          {
            id: 'evt-real-created',
            type: 'state_change',
            title: '真实 API 已返回',
            detail: `planId=${created.planId}，等待 F1-003 接入 SSE 流。`,
            timestamp: Date.now(),
          },
        ]
        return
      }

      const [planReady, sseFrames] = await Promise.all([
        plannerClient.getPlanReadyFixture(scenario.value),
        plannerClient.getSseFixtureFrames(),
      ])

      currentPlan.value = planReady.plan
      planId.value = planReady.planId
      agentState.value = planReady.plan.status
      connectionState.value = 'closed'
      logEvents.value = [
        ...sseFrames.map((frame, index) =>
          toLogEvent(frame.data, `fixture-${index}`),
        ),
        {
          id: 'evt-plan-fixture-loaded',
          type: 'fixture_loaded',
          title: 'plan_ready fixture 已加载',
          detail: planReady.plan.summary,
          timestamp: Date.now() + sseFrames.length,
        },
      ]
    } catch (error) {
      agentState.value = 'FAILED'
      connectionState.value = 'error'
      logEvents.value = [
        ...logEvents.value,
        {
          id: 'evt-client-error',
          type: 'error',
          title: 'API client 失败',
          detail: error instanceof Error ? error.message : String(error),
          timestamp: Date.now(),
        },
      ]
    }
  }

  function resetSkeletonFlow() {
    planId.value = null
    agentState.value = 'START'
    connectionState.value = 'idle'
    currentPlan.value = null
    logEvents.value = [
      {
        id: 'evt-ready',
        type: 'fixture_loaded',
        title: 'API client 已就绪',
        detail: '当前默认使用 mock fixture mode，不访问后端网络。',
        timestamp: Date.now(),
      },
    ]
  }

  function toLogEvent(payload: SsePayload, fallbackId: string): LogEvent {
    const timestamp = 'timestamp' in payload ? payload.timestamp : Date.now()

    switch (payload.type) {
      case 'heartbeat':
        return {
          id: fallbackId,
          type: payload.type,
          title: 'heartbeat',
          detail: `planId=${payload.planId}`,
          timestamp,
        }
      case 'state_change':
        return {
          id: fallbackId,
          type: payload.type,
          title: `${payload.from} -> ${payload.to}`,
          detail: `状态已进入 ${payload.to}`,
          timestamp,
        }
      case 'tool_call':
        return {
          id: fallbackId,
          type: payload.type,
          title: `${payload.tool} start`,
          detail: payload.inputSummary,
          timestamp,
        }
      case 'tool_result':
        return {
          id: fallbackId,
          type: payload.type,
          title: `${payload.tool} ${payload.status}`,
          detail: `${payload.outputSummary} (${payload.latencyMs}ms)`,
          timestamp,
        }
      case 'clarification_request':
        return {
          id: fallbackId,
          type: payload.type,
          title: 'clarification_request',
          detail: payload.question,
          timestamp,
        }
      case 'replan':
        return {
          id: fallbackId,
          type: payload.type,
          title: `Plan B #${payload.replanCount}`,
          detail: payload.reason,
          timestamp,
        }
      case 'adjust_result':
        return {
          id: fallbackId,
          type: payload.type,
          title: 'adjust_result',
          detail: payload.summary,
          timestamp,
        }
      case 'plan_ready':
        return {
          id: fallbackId,
          type: payload.type,
          title: 'plan_ready',
          detail: payload.plan.summary,
          timestamp,
        }
      case 'execute_result':
        return {
          id: fallbackId,
          type: payload.type,
          title: `${payload.actionType} ${payload.status}`,
          detail: payload.confirmationNo ?? payload.actionId,
          timestamp,
        }
      case 'done':
        return {
          id: fallbackId,
          type: payload.type,
          title: 'done',
          detail: payload.summary,
          timestamp,
        }
      case 'error':
        return {
          id: fallbackId,
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
    logEvents,
    scenarioLabel,
    canStart,
    setScenario,
    previewSkeletonFlow,
    resetSkeletonFlow,
  }
})
