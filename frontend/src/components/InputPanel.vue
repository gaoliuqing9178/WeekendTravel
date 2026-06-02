<script setup lang="ts">
import {
  NAlert,
  NButton,
  NCard,
  NForm,
  NFormItem,
  NInput,
  NRadioButton,
  NRadioGroup,
  NSpace,
  NTag,
} from 'naive-ui'

import { usePlannerStore } from '@/stores/planner'

const planner = usePlannerStore()

const scenarioOptions = [
  { label: '家庭', value: 'family' },
  { label: '朋友', value: 'friends' },
] as const

function handleScenarioUpdate(value: string | number) {
  if (value === 'family' || value === 'friends') {
    planner.setScenario(value)
  }
}
</script>

<template>
  <NCard
    class="input-panel"
    title="告诉我怎么安排"
    :bordered="false"
  >
    <template #header-extra>
      <NTag :bordered="false" size="small" type="info">
        {{ planner.apiMode }}
      </NTag>
    </template>

    <NForm
      class="input-panel-form"
      :show-require-mark="false"
      @submit.prevent="planner.submitPlan"
    >
      <NFormItem label="场景" path="scenario">
        <NRadioGroup
          :value="planner.scenario"
          name="scenario"
          @update:value="handleScenarioUpdate"
        >
          <NRadioButton
            v-for="option in scenarioOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </NRadioButton>
        </NRadioGroup>
      </NFormItem>

      <NFormItem label="自然语言输入" path="text" required>
        <NInput
          v-model:value="planner.userInput"
          type="textarea"
          :autosize="{ minRows: 5, maxRows: 7 }"
          placeholder="比如：今天下午想和家人出去玩几个小时，别太远，顺便吃点轻松的"
          show-count
          :maxlength="240"
          clearable
        />
      </NFormItem>

      <NFormItem label="出发位置" path="origin">
        <NInput
          v-model:value="planner.origin"
          placeholder="当前位置"
          clearable
        />
      </NFormItem>

      <NSpace vertical :size="12">
        <NButton
          type="primary"
          attr-type="submit"
          :disabled="!planner.canSubmit"
          :loading="planner.isSubmitting"
          block
        >
          生成路线方案
        </NButton>

        <NAlert
          v-if="planner.submitMessage || planner.errorMessage"
          class="input-panel-status"
          :type="planner.errorMessage ? 'error' : 'info'"
          :show-icon="false"
          role="status"
        >
          {{ planner.errorMessage ?? planner.submitMessage }}
        </NAlert>
      </NSpace>
    </NForm>
  </NCard>
</template>
