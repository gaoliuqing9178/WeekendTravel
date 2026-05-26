<script setup lang="ts">
import {
  NButton,
  NCard,
  NConfigProvider,
  NGrid,
  NGridItem,
  NAlert,
  NMessageProvider,
  NSpace,
  NStatistic,
  NTag,
  type GlobalThemeOverrides,
} from 'naive-ui'

import InputPanel from '@/components/InputPanel.vue'
import LogPanel from '@/components/LogPanel.vue'
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
            <NTag type="info" round>F1-004</NTag>
            <h1 id="app-title">WeekendTravel</h1>
            <p>InputPanel 已接入创建 plan 链路，提交后会进入规划状态并连接实时日志流。</p>
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
            <InputPanel />
          </NGridItem>

          <NGridItem span="12 m:5">
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
              <NAlert
                v-if="planner.currentPlan || planner.planBReason"
                class="fixture-summary"
                :type="planner.errorMessage ? 'warning' : 'success'"
                :show-icon="false"
              >
                <strong v-if="planner.currentPlan">
                  {{ planner.currentPlan.summary }}
                </strong>
                <span v-if="planner.planBReason">
                  Plan B：{{ planner.planBReason }}
                </span>
                <span v-if="planner.pendingClarification">
                  反问：{{ planner.pendingClarification.question }}
                </span>
                <span v-if="planner.errorMessage">
                  {{ planner.errorMessage }}
                </span>
              </NAlert>
              <NButton class="reset-button" quaternary @click="planner.resetSkeletonFlow">
                重置
              </NButton>
            </NCard>
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
