<script setup lang="ts">
import {
  NButton,
  NConfigProvider,
  NMessageProvider,
  NTag,
  type GlobalThemeOverrides,
} from 'naive-ui'

import AdjustPanel from '@/components/AdjustPanel.vue'
import ClarifyBubble from '@/components/ClarifyBubble.vue'
import ConfirmButton from '@/components/ConfirmButton.vue'
import ExecutionTracker from '@/components/ExecutionTracker.vue'
import InputPanel from '@/components/InputPanel.vue'
import LogPanel from '@/components/LogPanel.vue'
import MapStage from '@/components/MapStage.vue'
import PlanCard from '@/components/PlanCard.vue'
import StateSummaryPanel from '@/components/StateSummaryPanel.vue'
import { usePlannerStore } from '@/stores/planner'

const planner = usePlannerStore()

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#F97316',
    primaryColorHover: '#EA580C',
    primaryColorPressed: '#C2410C',
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
        <MapStage
          :agent-state="planner.agentState"
          :connection-state="planner.connectionState"
          :plan="planner.currentPlan"
          :plan-id="planner.planId"
          :plan-b-reason="planner.planBReason"
          :events="planner.logEvents"
        />

        <section class="planner-drawer" aria-label="规划抽屉">
          <div class="drawer-handle" aria-hidden="true">
            <span></span>
          </div>

          <header class="drawer-header">
            <div>
              <span class="drawer-eyebrow">出行主理人</span>
              <h2>把今天下午排顺</h2>
            </div>
            <div class="drawer-status" aria-label="当前模式">
              <NTag :bordered="false" type="success">
                {{ planner.scenarioLabel }}
              </NTag>
              <NTag :bordered="false" type="warning">
                {{ planner.apiMode }}
              </NTag>
              <NButton quaternary size="small" @click="planner.resetSkeletonFlow">
                重置
              </NButton>
            </div>
          </header>

          <div class="drawer-content">
            <section class="drawer-primary" aria-label="需求和行程">
              <InputPanel />
              <ClarifyBubble
                :clarification="planner.pendingClarification"
                :loading="planner.isClarifying"
                :message="planner.clarifyMessage"
                :error-message="planner.errorMessage"
                @reply="planner.replyToClarification"
              />
              <PlanCard :plan="planner.currentPlan" />
            </section>

            <aside class="drawer-secondary" aria-label="Agent 动态和执行">
              <LogPanel
                :events="planner.logEvents"
                :connection-state="planner.connectionState"
              />

              <StateSummaryPanel
                :agent-state="planner.agentState"
                :connection-state="planner.connectionState"
                :plan-id="planner.planId"
                :plan-b-reason="planner.planBReason"
                :error-message="planner.errorMessage"
                :execute-message="planner.executeMessage"
              />

              <AdjustPanel
                v-if="planner.agentState === 'CONFIRM'"
                :agent-state="planner.agentState"
                :plan-id="planner.planId"
                :adjust-count="planner.adjustCount"
                :adjust-limit="planner.adjustLimit"
                :can-adjust="planner.canAdjustPlan"
                :loading="planner.isAdjusting"
                :message="planner.adjustMessage"
                :error-message="planner.adjustErrorMessage"
                @adjust="planner.adjustPlan"
              />

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
            </aside>
          </div>
        </section>
      </main>
    </NMessageProvider>
  </NConfigProvider>
</template>
