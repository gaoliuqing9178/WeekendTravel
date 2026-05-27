<script setup lang="ts">
import { computed } from 'vue'
import { NAlert, NButton, NCard, NTag } from 'naive-ui'

import type { AgentState } from '@/api/types'

const props = defineProps<{
  agentState: AgentState
  canConfirm: boolean
  loading: boolean
  planId: string | null
  message: string | null
  errorMessage: string | null
}>()

const emit = defineEmits<{
  confirm: []
}>()

const disabledReason = computed(() => {
  if (!props.planId) {
    return '等待方案生成'
  }

  if (props.agentState !== 'CONFIRM') {
    return `当前状态 ${props.agentState}`
  }

  return null
})
</script>

<template>
  <NCard class="confirm-panel" :bordered="false">
    <template #header>
      <div class="confirm-panel-header">
        <span class="section-title">确认执行</span>
        <NTag
          :bordered="false"
          :type="agentState === 'CONFIRM' ? 'success' : 'default'"
        >
          {{ agentState }}
        </NTag>
      </div>
    </template>

    <NButton
      type="primary"
      block
      size="large"
      :disabled="!canConfirm || loading"
      :loading="loading"
      :aria-disabled="!canConfirm || loading"
      @click="emit('confirm')"
    >
      确认执行
    </NButton>

    <p v-if="disabledReason && !loading" class="confirm-disabled-reason">
      {{ disabledReason }}
    </p>

    <NAlert
      v-if="message || errorMessage"
      class="confirm-message"
      :type="errorMessage ? 'error' : 'info'"
      :show-icon="false"
      role="status"
    >
      {{ errorMessage ?? message }}
    </NAlert>
  </NCard>
</template>
