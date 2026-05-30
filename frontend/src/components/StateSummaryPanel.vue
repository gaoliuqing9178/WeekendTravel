<script setup lang="ts">
import { computed } from 'vue'
import { NAlert, NCard, NTag } from 'naive-ui'

import type { AgentState } from '@/api/types'
import type { SseConnectionState } from '@/composables/useSSE'

const terminalStates: Array<{
  state: Extract<AgentState, 'DONE' | 'DEGRADE' | 'FAILED'>
  label: string
}> = [
  { state: 'DONE', label: '已完成' },
  { state: 'DEGRADE', label: '已降级' },
  { state: 'FAILED', label: '失败' },
]

const props = defineProps<{
  agentState: AgentState
  connectionState: SseConnectionState
  planId: string | null
  planBReason: string | null
  errorMessage: string | null
  executeMessage: string | null
}>()

const stateTagType = computed(() => {
  if (props.agentState === 'DONE') {
    return 'success'
  }

  if (props.agentState === 'DEGRADE' || props.agentState === 'REPLAN') {
    return 'warning'
  }

  if (props.agentState === 'FAILED') {
    return 'error'
  }

  if (props.agentState === 'CONFIRM') {
    return 'success'
  }

  return 'info'
})

const terminalNotice = computed(() => {
  if (props.agentState === 'DONE') {
    return {
      type: 'success' as const,
      title: 'DONE',
      detail: props.executeMessage ?? '执行动作已完成。',
    }
  }

  if (props.agentState === 'DEGRADE') {
    return {
      type: 'warning' as const,
      title: 'DEGRADE',
      detail: props.errorMessage ?? '当前方案进入降级状态。',
    }
  }

  if (props.agentState === 'FAILED') {
    return {
      type: 'error' as const,
      title: 'FAILED',
      detail: props.errorMessage ?? '当前流程失败，请重新发起规划。',
    }
  }

  if (props.errorMessage) {
    return {
      type: 'error' as const,
      title: 'error',
      detail: props.errorMessage,
    }
  }

  return null
})
</script>

<template>
  <NCard class="state-summary-panel" :bordered="false">
    <template #header>
      <div class="state-summary-header">
        <div>
          <span id="state-summary-title" class="section-title">状态总览</span>
          <p>{{ planId ?? '等待 plan 创建' }}</p>
        </div>
        <NTag :bordered="false" :type="stateTagType">
          {{ agentState }}
        </NTag>
      </div>
    </template>

    <section
      class="state-summary-body"
      aria-labelledby="state-summary-title"
      aria-live="polite"
    >
      <div class="state-summary-current">
        <span>当前链路</span>
        <strong>{{ agentState }}</strong>
        <NTag :bordered="false" size="small" type="info">
          {{ connectionState }}
        </NTag>
      </div>

      <div class="terminal-state-strip" aria-label="终态轨道">
        <span
          v-for="item in terminalStates"
          :key="item.state"
          class="terminal-state"
          :class="{ 'is-active': agentState === item.state }"
        >
          <strong>{{ item.state }}</strong>
          <small>{{ item.label }}</small>
        </span>
      </div>

      <NAlert
        v-if="planBReason"
        type="warning"
        :show-icon="false"
        class="state-summary-alert"
      >
        <strong>Plan B</strong>
        <span>{{ planBReason }}</span>
      </NAlert>

      <NAlert
        v-if="terminalNotice"
        :type="terminalNotice.type"
        :show-icon="false"
        class="state-summary-alert"
        role="status"
      >
        <strong>{{ terminalNotice.title }}</strong>
        <span>{{ terminalNotice.detail }}</span>
      </NAlert>
    </section>
  </NCard>
</template>
