<script setup lang="ts">
import {
  NButton,
  NCard,
  NConfigProvider,
  NGrid,
  NGridItem,
  NMessageProvider,
  NSpace,
  NStatistic,
  NTag,
  type GlobalThemeOverrides,
} from 'naive-ui'

import ConfirmButton from '@/components/ConfirmButton.vue'
import ExecutionTracker from '@/components/ExecutionTracker.vue'
import InputPanel from '@/components/InputPanel.vue'
import LogPanel from '@/components/LogPanel.vue'
import PlanCard from '@/components/PlanCard.vue'
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

</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NMessageProvider>
      <main class="app-shell">
        <section class="workspace-hero" aria-labelledby="app-title">
          <div class="hero-copy">
            <NTag type="info" round>F1-005</NTag>
            <h1 id="app-title">WeekendTravel</h1>
            <p>方案卡、确认执行和执行追踪已接入，mock mode 会停在可确认状态等待用户操作。</p>
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
            <div class="main-column">
              <InputPanel />
              <PlanCard :plan="planner.currentPlan" />
            </div>
          </NGridItem>

          <NGridItem span="12 m:5">
            <div class="side-column">
              <NCard title="Pinia 状态" :bordered="false">
                <div class="state-grid">
                  <NStatistic label="场景" :value="planner.scenarioLabel" />
                  <NStatistic label="Mode" :value="planner.apiMode" />
                  <NStatistic
                    label="Plan ID"
                    :value="planner.planId ?? '未创建'"
                  />
                  <NStatistic label="Agent" :value="planner.agentState" />
                  <NStatistic label="SSE" :value="planner.connectionState" />
                </div>
                <NButton
                  class="reset-button"
                  quaternary
                  @click="planner.resetSkeletonFlow"
                >
                  重置
                </NButton>
              </NCard>

              <ConfirmButton
                :agent-state="planner.agentState"
                :can-confirm="planner.canConfirmPlan"
                :loading="planner.isExecuting"
                :plan-id="planner.planId"
                :message="planner.executeMessage"
                :error-message="planner.errorMessage"
                @confirm="planner.confirmPlanExecution"
              />

              <ExecutionTracker
                :actions="planner.currentPlan?.actions ?? []"
                :agent-state="planner.agentState"
              />
            </div>
          </NGridItem>

          <NGridItem span="12">
            <LogPanel
              :events="planner.logEvents"
              :connection-state="planner.connectionState"
            />
          </NGridItem>
        </NGrid>
      </main>
    </NMessageProvider>
  </NConfigProvider>
</template>
