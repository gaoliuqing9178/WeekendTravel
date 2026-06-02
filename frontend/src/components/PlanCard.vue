<script setup lang="ts">
import { computed, ref } from 'vue'
import { NAlert, NButton, NCard, NTag } from 'naive-ui'

import type { Plan, TimeSlot } from '@/api/types'

const props = defineProps<{
  plan: Plan | null
}>()

const copied = ref(false)

const sortedTimeline = computed(() =>
  [...(props.plan?.timeline ?? [])].sort((left, right) => left.order - right.order),
)

const placeholderSlots = [
  { time: '14:00', title: '活动待定', meta: '寻找附近可玩地点' },
  { time: '17:30', title: '晚餐待定', meta: '校验排队和订位' },
  { time: '19:00', title: '收尾待定', meta: '安排轻松回家路线' },
]

const shareButtonLabel = computed(() =>
  copied.value ? '已复制给家人' : '复制分享文案',
)

function scenarioLabel(plan: Plan) {
  return plan.scenario === 'family' ? '家庭' : '朋友'
}

function formatDistance(slot: TimeSlot) {
  return `${slot.poi.distanceMinutes} 分钟路程`
}

function formatWait(slot: TimeSlot) {
  return slot.poi.waitMinutes === null ? '等待待确认' : `等待 ${slot.poi.waitMinutes} 分钟`
}

function availabilityLabel(slot: TimeSlot) {
  if (slot.poi.availabilityStatus === 'available') {
    return '可安排'
  }

  if (slot.poi.availabilityStatus === 'limited') {
    return '余量紧张'
  }

  return slot.poi.availabilityStatus
}

function availabilityType(slot: TimeSlot) {
  if (slot.poi.availabilityStatus === 'available') {
    return 'success'
  }

  if (slot.poi.availabilityStatus === 'limited') {
    return 'warning'
  }

  return 'default'
}

function slotKey(slot: TimeSlot) {
  return `${slot.order}-${slot.title}-${slot.startTime}`
}

function visualClass(slot: TimeSlot) {
  return `is-${slot.poi.category}`
}

async function copyShareMessage() {
  if (!props.plan?.shareMessage) {
    return
  }

  try {
    await navigator.clipboard.writeText(props.plan.shareMessage)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 1600)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <NCard
    class="plan-card itinerary-panel"
    :class="{ 'is-plan-b': plan?.isPlanB }"
    :bordered="false"
  >
    <template #header>
      <div class="plan-card-header">
        <div>
          <span id="plan-card-title" class="section-title">今日下午行程</span>
          <p v-if="plan">{{ plan.summary }}</p>
          <p v-else>先搭时间骨架，找到合适地点后会自动补全。</p>
        </div>
        <div v-if="plan" class="plan-card-tags">
          <NTag v-if="plan.isPlanB" :bordered="false" type="warning">
            Plan B
          </NTag>
          <NTag :bordered="false" type="info">
            {{ scenarioLabel(plan) }}
          </NTag>
        </div>
      </div>
    </template>

    <article class="plan-card-body" aria-labelledby="plan-card-title">
      <div v-if="plan?.isPlanB && plan.planBReason" class="plan-switcher">
        <span>Plan A: 亲子优先</span>
        <strong>Plan B: 已智能替换</strong>
      </div>

      <NAlert
        v-if="plan?.isPlanB && plan.planBReason"
        type="warning"
        :show-icon="false"
        class="plan-b-alert"
      >
        <strong>遇到排队问题，已换成更稳的方案</strong>
        <span>{{ plan.planBReason }}</span>
      </NAlert>

      <ol v-if="!plan" class="plan-timeline skeleton-rail" aria-label="行程骨架">
        <li
          v-for="slot in placeholderSlots"
          :key="slot.time"
          class="timeline-slot is-placeholder"
        >
          <div class="slot-time">
            <strong>{{ slot.time }}</strong>
            <span>待确认</span>
          </div>
          <div class="slot-card-main">
            <div class="slot-visual is-placeholder-visual" aria-hidden="true"></div>
            <div class="slot-body">
              <h3>{{ slot.title }}</h3>
              <p>{{ slot.meta }}</p>
            </div>
          </div>
        </li>
      </ol>

      <template v-else>
        <dl class="plan-metrics" aria-label="方案摘要">
          <div>
            <dt>总时长</dt>
            <dd>{{ plan.totalDurationHours }} 小时</dd>
          </div>
          <div>
            <dt>重排</dt>
            <dd>{{ plan.replanCount }} 次</dd>
          </div>
          <div>
            <dt>执行动作</dt>
            <dd>{{ plan.actions.length }} 个</dd>
          </div>
        </dl>

        <ol class="plan-timeline itinerary-rail" aria-label="时间轴行程">
          <li
            v-for="(slot, index) in sortedTimeline"
            :key="slotKey(slot)"
            class="timeline-slot"
          >
            <div class="slot-time">
              <strong>{{ slot.startTime }}</strong>
              <span>{{ slot.endTime }}</span>
            </div>
            <div class="slot-card-main">
              <div class="slot-visual" :class="visualClass(slot)" aria-hidden="true">
                <span>{{ index + 1 }}</span>
              </div>
              <div class="slot-body">
                <div class="slot-title-row">
                  <h3>{{ slot.poi.name }}</h3>
                  <NTag
                    :bordered="false"
                    size="small"
                    :type="availabilityType(slot)"
                  >
                    {{ availabilityLabel(slot) }}
                  </NTag>
                </div>
                <p class="slot-poi">{{ slot.title }} · {{ slot.poi.address }}</p>
                <div class="slot-meta">
                  <span>评分 {{ slot.poi.rating }}</span>
                  <span>{{ formatDistance(slot) }}</span>
                  <span>{{ formatWait(slot) }}</span>
                </div>
                <div class="slot-tags" aria-label="地点标签">
                  <NTag
                    v-for="tag in slot.poi.tags.slice(0, 3)"
                    :key="tag"
                    :bordered="false"
                    size="small"
                  >
                    {{ tag }}
                  </NTag>
                </div>
              </div>
            </div>
          </li>
        </ol>

        <section class="plan-actions-strip" aria-label="执行包">
          <div class="section-heading">
            <h2>执行包</h2>
            <NTag :bordered="false" size="small">
              {{ plan.status }}
            </NTag>
          </div>
          <ul class="plan-actions">
            <li v-for="action in plan.actions" :key="action.actionId">
              <span>{{ action.description }}</span>
              <NTag :bordered="false" size="small">
                {{ action.status }}
              </NTag>
            </li>
          </ul>
        </section>

        <section class="share-message" aria-label="分享消息">
          <div>
            <h2>发给家人的一句话</h2>
            <p>{{ plan.shareMessage }}</p>
          </div>
          <NButton secondary type="primary" @click="copyShareMessage">
            {{ shareButtonLabel }}
          </NButton>
        </section>
      </template>
    </article>
  </NCard>
</template>
