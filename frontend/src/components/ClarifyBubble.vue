<script setup lang="ts">
import { computed } from 'vue'
import { NAlert, NButton, NCard, NTag } from 'naive-ui'

import type { ClarificationRequestEvent } from '@/api/types'

const props = defineProps<{
  clarification: ClarificationRequestEvent | null
  loading: boolean
  message: string | null
  errorMessage: string | null
}>()

const emit = defineEmits<{
  reply: [reply: string]
}>()

const optionCount = computed(() => props.clarification?.options.length ?? 0)
const hasExpectedOptionCount = computed(
  () => optionCount.value >= 2 && optionCount.value <= 3,
)
</script>

<template>
  <NCard
    v-if="clarification"
    class="clarify-bubble"
    :bordered="false"
    role="region"
    aria-labelledby="clarify-title"
  >
    <template #header>
      <div class="clarify-header">
        <div>
          <span id="clarify-title" class="section-title">需要确认</span>
          <p>{{ clarification.question }}</p>
        </div>
        <NTag :bordered="false" type="warning">
          {{ clarification.field }}
        </NTag>
      </div>
    </template>

    <NAlert
      v-if="!hasExpectedOptionCount"
      class="clarify-message"
      type="error"
      :show-icon="false"
      role="status"
    >
      当前反问选项数量为 {{ optionCount }}，请检查 clarification_request。
    </NAlert>

    <div class="clarify-options" aria-label="反问快捷选项">
      <NButton
        v-for="option in clarification.options"
        :key="option"
        secondary
        type="primary"
        size="large"
        :disabled="loading"
        :loading="loading"
        :aria-label="`回答：${option}`"
        @click="emit('reply', option)"
      >
        {{ option }}
      </NButton>
    </div>

    <NAlert
      v-if="message || errorMessage"
      class="clarify-message"
      :type="errorMessage ? 'error' : 'info'"
      :show-icon="false"
      role="status"
    >
      {{ errorMessage ?? message }}
    </NAlert>
  </NCard>
</template>
