import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

type Scenario = 'family' | 'friends'
type AgentState = 'START' | 'INTENT' | 'SKELETON'
type ConnectionState = 'idle' | 'connecting' | 'open' | 'closed' | 'error'

interface LogEvent {
  id: string
  type: 'state_change' | 'tool_call' | 'tool_result'
  title: string
  detail: string
  timestamp: number
}

const familyPrompt =
  '今天下午是空的，想和老婆孩子出去玩几个小时，别离家太远。孩子5岁，老婆最近在减肥，帮我安排一下。'

const friendsPrompt =
  '今天下午4个人出去玩，2个男生2个女生，不想离家太远，帮我安排一个能玩、能吃、能拍照的下午。'

export const usePlannerStore = defineStore('planner', () => {
  const planId = ref<string | null>(null)
  const agentState = ref<AgentState>('START')
  const scenario = ref<Scenario>('family')
  const origin = ref('当前位置')
  const userInput = ref(familyPrompt)
  const connectionState = ref<ConnectionState>('idle')
  const logEvents = ref<LogEvent[]>([
    {
      id: 'evt-ready',
      type: 'state_change',
      title: '前端骨架已就绪',
      detail: '等待 F1-002 接入 API client 与 fixture mode。',
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

  function previewSkeletonFlow() {
    if (!canStart.value) {
      return
    }

    planId.value = 'frontend_skeleton_preview'
    agentState.value = 'INTENT'
    connectionState.value = 'connecting'
    logEvents.value = [
      {
        id: 'evt-intent',
        type: 'state_change',
        title: '进入 INTENT',
        detail: `${scenarioLabel.value}输入已进入本地骨架预演。`,
        timestamp: Date.now(),
      },
      {
        id: 'evt-tool',
        type: 'tool_call',
        title: '等待后续工具层',
        detail: 'F1-001 不调用后端；真实 API 与 SSE 将在后续任务接入。',
        timestamp: Date.now() + 1,
      },
    ]
  }

  function resetSkeletonFlow() {
    planId.value = null
    agentState.value = 'START'
    connectionState.value = 'idle'
    logEvents.value = [
      {
        id: 'evt-ready',
        type: 'state_change',
        title: '前端骨架已就绪',
        detail: '等待 F1-002 接入 API client 与 fixture mode。',
        timestamp: Date.now(),
      },
    ]
  }

  return {
    planId,
    agentState,
    scenario,
    origin,
    userInput,
    connectionState,
    logEvents,
    scenarioLabel,
    canStart,
    setScenario,
    previewSkeletonFlow,
    resetSkeletonFlow,
  }
})
