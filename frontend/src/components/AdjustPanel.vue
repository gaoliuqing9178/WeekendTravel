<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { NAlert, NButton, NCard, NInput, NTag } from 'naive-ui'

import type { AgentState } from '@/api/types'

const props = defineProps<{
  agentState: AgentState
  planId: string | null
  adjustCount: number
  adjustLimit: number
  canAdjust: boolean
  loading: boolean
  message: string | null
  errorMessage: string | null
}>()

const emit = defineEmits<{
  adjust: [instruction: string]
}>()

const instruction = ref('')
const lastSubmitted = ref<string | null>(null)

const remainingCount = computed(() =>
  Math.max(props.adjustLimit - props.adjustCount, 0),
)

const normalizedInstruction = computed(() => instruction.value.trim())

const disabledReason = computed(() => {
  if (!props.planId) {
    return '等待方案生成'
  }

  if (props.agentState !== 'CONFIRM') {
    return `当前状态：${props.agentState}`
  }

  if (props.adjustCount >= props.adjustLimit) {
    return '已达最大微调次数'
  }

  return null
})

const canSubmit = computed(
  () =>
    props.canAdjust &&
    !props.loading &&
    normalizedInstruction.value.length > 0 &&
    props.adjustCount < props.adjustLimit,
)

const progressWidth = computed(() => {
  if (props.adjustLimit <= 0) {
    return '100%'
  }

  return `${Math.min((props.adjustCount / props.adjustLimit) * 100, 100)}%`
})

const quickInstructions = [
  '换一家餐厅，要能订位',
  '把总时长压缩 30 分钟',
  '减少步行，优先室内',
]

function useQuickInstruction(value: string) {
  instruction.value = value
}

function submitAdjust() {
  if (!canSubmit.value) {
    return
  }

  lastSubmitted.value = normalizedInstruction.value
  emit('adjust', normalizedInstruction.value)
}

watch(
  () => [props.loading, props.message, props.errorMessage] as const,
  ([loading, message, errorMessage]) => {
    if (loading || errorMessage || !message || !lastSubmitted.value) {
      return
    }

    if (instruction.value.trim() === lastSubmitted.value) {
      instruction.value = ''
      lastSubmitted.value = null
    }
  },
)
</script>

<template>
  <NCard
    class="adjust-panel"
    :bordered="false"
    role="region"
    aria-labelledby="adjust-title"
  >
    <template #header>
      <div class="adjust-panel-header">
        <div>
          <span id="adjust-title" class="section-title">微调方案</span>
          <p>{{ remainingCount }} 次可用</p>
        </div>
        <NTag
          :bordered="false"
          :type="adjustCount >= adjustLimit ? 'error' : 'info'"
        >
          {{ adjustCount }} / {{ adjustLimit }}
        </NTag>
      </div>
    </template>

    <div class="adjust-progress" aria-hidden="true">
      <span :style="{ width: progressWidth }"></span>
    </div>

    <div class="adjust-presets" aria-label="微调快捷项">
      <NButton
        v-for="item in quickInstructions"
        :key="item"
        quaternary
        size="small"
        :disabled="loading || adjustCount >= adjustLimit"
        @click="useQuickInstruction(item)"
      >
        {{ item }}
      </NButton>
    </div>

    <label class="adjust-label" for="adjust-instruction">微调要求</label>
    <NInput
      id="adjust-instruction"
      v-model:value="instruction"
      type="textarea"
      maxlength="120"
      show-count
      :autosize="{ minRows: 3, maxRows: 5 }"
      :disabled="loading || adjustCount >= adjustLimit"
      placeholder="例如：换一家餐厅，要能订位"
      @keyup.ctrl.enter="submitAdjust"
    />

    <NButton
      type="primary"
      block
      size="large"
      class="adjust-submit"
      :disabled="!canSubmit"
      :loading="loading"
      :aria-disabled="!canSubmit"
      @click="submitAdjust"
    >
      提交微调
    </NButton>

    <p v-if="disabledReason && !loading" class="adjust-disabled-reason">
      {{ disabledReason }}
    </p>

    <NAlert
      v-if="message || errorMessage"
      class="adjust-message"
      :type="errorMessage ? 'error' : 'info'"
      :show-icon="false"
      role="status"
    >
      {{ errorMessage ?? message }}
    </NAlert>
  </NCard>
</template>
