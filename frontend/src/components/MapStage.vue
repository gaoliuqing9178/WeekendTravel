<script setup lang="ts">
import { computed } from 'vue'
import { NTag } from 'naive-ui'

import type { AgentState, Plan, TimeSlot } from '@/api/types'
import type { SseConnectionState } from '@/composables/useSSE'
import type { LogEvent } from '@/stores/planner'

const props = defineProps<{
  agentState: AgentState
  connectionState: SseConnectionState
  plan: Plan | null
  planId: string | null
  planBReason: string | null
  events: LogEvent[]
}>()

interface MapPoint {
  key: string
  label: string
  title: string
  meta: string
  x: number
  y: number
  status: 'home' | 'pending' | 'active' | 'plan-b' | 'done'
}

const fallbackPoints: MapPoint[] = [
  {
    key: 'home',
    label: '家',
    title: '当前位置',
    meta: '出发点',
    x: 24,
    y: 60,
    status: 'home',
  },
  {
    key: 'activity',
    label: '玩',
    title: '活动待定',
    meta: '规划中',
    x: 44,
    y: 38,
    status: 'pending',
  },
  {
    key: 'meal',
    label: '吃',
    title: '晚餐待定',
    meta: '校验中',
    x: 66,
    y: 54,
    status: 'pending',
  },
  {
    key: 'walk',
    label: '逛',
    title: '收尾待定',
    meta: '待生成',
    x: 78,
    y: 30,
    status: 'pending',
  },
]

const sortedTimeline = computed(() =>
  [...(props.plan?.timeline ?? [])].sort((left, right) => left.order - right.order),
)

const mapPoints = computed<MapPoint[]>(() => {
  if (!props.plan || sortedTimeline.value.length === 0) {
    return fallbackPoints
  }

  const routeSlots = sortedTimeline.value.slice(0, 3)
  const coordinates = [
    { x: 42, y: 40 },
    { x: 64, y: 55 },
    { x: 78, y: 32 },
  ]

  return [
    fallbackPoints[0],
    ...routeSlots.map((slot, index) => toMapPoint(slot, coordinates[index], index)),
  ]
})

const routePoints = computed(() =>
  mapPoints.value.map((point) => `${point.x},${point.y}`).join(' '),
)

const latestEvent = computed(() => [...props.events].reverse()[0] ?? null)

const activePhase = computed(() => {
  const state = props.agentState

  if (state === 'START') {
    return '等待你的想法'
  }

  if (state === 'INTENT' || state === 'CLARIFY') {
    return '理解需求'
  }

  if (state === 'SKELETON') {
    return '搭时间线'
  }

  if (state === 'RECALL') {
    return '寻找地点'
  }

  if (state === 'VALIDATE' || state === 'REPLAN' || state === 'ADJUST') {
    return '校验可行性'
  }

  if (state === 'PACK' || state === 'CONFIRM') {
    return '方案出炉'
  }

  if (state === 'EXECUTE') {
    return '执行预订'
  }

  if (state === 'DONE') {
    return '行程就绪'
  }

  if (state === 'DEGRADE') {
    return '需要降级'
  }

  return '需要重试'
})

const stageTagType = computed(() => {
  if (props.agentState === 'DONE') {
    return 'success'
  }

  if (props.agentState === 'REPLAN' || props.agentState === 'DEGRADE') {
    return 'warning'
  }

  if (props.agentState === 'FAILED') {
    return 'error'
  }

  return 'info'
})

const mapSummary = computed(() => {
  if (props.plan) {
    return props.plan.summary
  }

  if (props.agentState === 'START') {
    return '说一句想法，我先把活动、晚餐和回家路线搭起来。'
  }

  return latestEvent.value?.detail ?? '正在把你的需求翻译成可执行行程。'
})

function toMapPoint(
  slot: TimeSlot,
  coordinate: { x: number; y: number } | undefined,
  index: number,
): MapPoint {
  return {
    key: `${slot.order}-${slot.poi.id}`,
    label: pointLabel(index),
    title: slot.poi.name,
    meta: `${slot.startTime} · ${slot.poi.distanceMinutes}min`,
    x: coordinate?.x ?? 50 + index * 12,
    y: coordinate?.y ?? 42 + index * 8,
    status:
      props.agentState === 'DONE'
        ? 'done'
        : props.plan?.isPlanB && index === 1
          ? 'plan-b'
          : 'active',
  }
}

function pointLabel(index: number) {
  return ['玩', '吃', '逛'][index] ?? `${index + 1}`
}
</script>

<template>
  <section class="map-stage" aria-labelledby="map-stage-title">
    <div class="map-topbar">
      <div>
        <p class="map-kicker">WeekendTravel</p>
        <h1 id="map-stage-title">今天下午怎么走</h1>
      </div>
      <div class="map-topbar-tags" aria-label="当前规划状态">
        <NTag :bordered="false" :type="stageTagType">
          {{ activePhase }}
        </NTag>
        <NTag :bordered="false" type="default">
          {{ connectionState }}
        </NTag>
      </div>
    </div>

    <div class="map-canvas" aria-label="行程地图示意">
      <div class="map-grid" aria-hidden="true"></div>
      <span class="street street-a" aria-hidden="true"></span>
      <span class="street street-b" aria-hidden="true"></span>
      <span class="street street-c" aria-hidden="true"></span>
      <span class="home-radius" aria-hidden="true"></span>

      <svg
        class="route-line"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <polyline class="route-shadow" :points="routePoints" />
        <polyline
          class="route-active"
          :class="{ 'is-pending': !plan, 'is-plan-b': Boolean(plan?.isPlanB) }"
          :points="routePoints"
        />
      </svg>

      <div
        v-for="point in mapPoints"
        :key="point.key"
        class="map-pin"
        :class="`is-${point.status}`"
        :style="{ left: `${point.x}%`, top: `${point.y}%` }"
      >
        <span class="pin-dot">{{ point.label }}</span>
        <span class="pin-card">
          <strong>{{ point.title }}</strong>
          <small>{{ point.meta }}</small>
        </span>
      </div>

      <div class="map-status-card" aria-live="polite">
        <span>Agent</span>
        <strong>{{ activePhase }}</strong>
        <p>{{ mapSummary }}</p>
      </div>

      <div v-if="planBReason" class="map-replan-card" role="status">
        <span>智能替换</span>
        <p>{{ planBReason }}</p>
      </div>
    </div>

    <div class="map-bottom-strip">
      <span>{{ planId ?? 'plan 待创建' }}</span>
      <strong>{{ agentState }}</strong>
      <span>{{ events.length }} 条动态</span>
    </div>
  </section>
</template>
