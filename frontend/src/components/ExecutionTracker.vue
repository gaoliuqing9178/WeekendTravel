<script setup lang="ts">
import { computed } from 'vue'
import { NCard, NEmpty, NTag } from 'naive-ui'

import type { ActionItem, ActionStatus, AgentState } from '@/api/types'

const props = defineProps<{
  actions: ActionItem[]
  agentState: AgentState
}>()

const completedCount = computed(
  () => props.actions.filter((action) => action.status === 'success').length,
)

const progressPercent = computed(() => {
  if (props.actions.length === 0) {
    return 0
  }

  return Math.round((completedCount.value / props.actions.length) * 100)
})

function statusLabel(status: ActionStatus) {
  const labels: Record<ActionStatus, string> = {
    pending: '等待',
    executing: '执行中',
    success: '已完成',
    failed: '失败',
    manual: '人工处理',
  }

  return labels[status]
}

function statusType(status: ActionStatus) {
  if (status === 'success') {
    return 'success'
  }

  if (status === 'failed') {
    return 'error'
  }

  if (status === 'executing') {
    return 'info'
  }

  if (status === 'manual') {
    return 'warning'
  }

  return 'default'
}

function actionTypeLabel(action: ActionItem) {
  return action.actionType.replaceAll('_', ' ')
}
</script>

<template>
  <NCard class="execution-tracker" :bordered="false">
    <template #header>
      <div class="execution-header">
        <div>
          <span id="execution-title" class="section-title">执行追踪</span>
          <p>{{ completedCount }} / {{ actions.length }} 个动作完成</p>
        </div>
        <NTag :bordered="false" type="info">
          {{ agentState }}
        </NTag>
      </div>
    </template>

    <NEmpty v-if="actions.length === 0" description="暂无执行动作" />

    <div
      v-else
      class="execution-body"
      role="region"
      aria-labelledby="execution-title"
      aria-live="polite"
    >
      <div class="execution-progress" aria-hidden="true">
        <span :style="{ width: `${progressPercent}%` }" />
      </div>

      <ol class="execution-list">
        <li
          v-for="action in actions"
          :key="action.actionId"
          class="execution-action"
          :class="`is-${action.status}`"
        >
          <div class="action-status-dot" aria-hidden="true" />
          <div class="action-body">
            <div class="action-title-row">
              <h3>{{ action.description }}</h3>
              <NTag :bordered="false" size="small" :type="statusType(action.status)">
                {{ statusLabel(action.status) }}
              </NTag>
            </div>
            <p>{{ actionTypeLabel(action) }} · {{ action.actionId }}</p>
            <p v-if="action.confirmationNo" class="confirmation-number">
              确认号：{{ action.confirmationNo }}
            </p>
          </div>
        </li>
      </ol>
    </div>
  </NCard>
</template>
