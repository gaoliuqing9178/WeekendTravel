<script setup lang="ts">
import {
  NButton,
  NCard,
  NConfigProvider,
  NGrid,
  NGridItem,
  NAlert,
  NInput,
  NInputGroup,
  NMessageProvider,
  NRadioButton,
  NRadioGroup,
  NSpace,
  NStatistic,
  NTag,
  NTimeline,
  NTimelineItem,
  type GlobalThemeOverrides,
} from 'naive-ui'

import { usePlannerStore } from '@/stores/planner'

const planner = usePlannerStore()

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#0EA5E9',
    primaryColorHover: '#0284C7',
    primaryColorPressed: '#0369A1',
    borderRadius: '8px',
    fontFamily:
      'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  Button: {
    borderRadiusMedium: '8px',
  },
  Card: {
    borderRadius: '8px',
  },
}

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
  <NConfigProvider :theme-overrides="themeOverrides">
    <NMessageProvider>
      <main class="app-shell">
        <section class="workspace-hero" aria-labelledby="app-title">
          <div class="hero-copy">
            <NTag type="info" round>F1-002</NTag>
            <h1 id="app-title">WeekendTravel</h1>
            <p>API client 与 mock fixture mode 已接入，默认不访问后端网络。</p>
          </div>
          <NSpace class="hero-actions" align="center" :size="12">
            <NTag :bordered="false" type="success">
              {{ planner.agentState }}
            </NTag>
            <NTag :bordered="false" type="warning">
              {{ planner.connectionState }}
            </NTag>
          </NSpace>
        </section>

        <NGrid :cols="12" :x-gap="20" :y-gap="20" responsive="screen">
          <NGridItem span="12 m:7">
            <NCard title="自然语言输入" :bordered="false">
              <NSpace vertical :size="16">
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
                <NInput
                  v-model:value="planner.userInput"
                  type="textarea"
                  :autosize="{ minRows: 5, maxRows: 7 }"
                  placeholder="说一句你想怎么安排这个下午"
                  show-count
                />
                <NInputGroup>
                  <NInput
                    v-model:value="planner.origin"
                    placeholder="出发位置"
                    clearable
                  />
                  <NButton
                    type="primary"
                    :disabled="!planner.canStart"
                    @click="planner.previewSkeletonFlow"
                  >
                    开始预演
                  </NButton>
                </NInputGroup>
              </NSpace>
            </NCard>
          </NGridItem>

          <NGridItem span="12 m:5">
            <NCard title="Pinia 状态" :bordered="false">
              <div class="state-grid">
                <NStatistic label="场景" :value="planner.scenarioLabel" />
                <NStatistic
                  label="Plan ID"
                  :value="planner.planId ?? '未创建'"
                />
                <NStatistic label="Agent" :value="planner.agentState" />
                <NStatistic label="SSE" :value="planner.connectionState" />
              </div>
              <NAlert
                v-if="planner.currentPlan"
                class="fixture-summary"
                type="success"
                :show-icon="false"
              >
                <strong>{{ planner.currentPlan.summary }}</strong>
                <span v-if="planner.currentPlan.isPlanB">
                  Plan B：{{ planner.currentPlan.planBReason }}
                </span>
              </NAlert>
              <NButton class="reset-button" quaternary @click="planner.resetSkeletonFlow">
                重置
              </NButton>
            </NCard>
          </NGridItem>

          <NGridItem span="12">
            <NCard title="Fixture 日志预览" :bordered="false">
              <NTimeline>
                <NTimelineItem
                  v-for="event in planner.logEvents"
                  :key="event.id"
                  :type="event.type === 'state_change' ? 'info' : 'default'"
                  :title="event.title"
                  :content="event.detail"
                  :time="new Date(event.timestamp).toLocaleTimeString('zh-CN')"
                />
              </NTimeline>
            </NCard>
          </NGridItem>
        </NGrid>
      </main>
    </NMessageProvider>
  </NConfigProvider>
</template>
