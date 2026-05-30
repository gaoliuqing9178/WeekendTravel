<script setup lang="ts">
import { computed } from 'vue'
import { NAlert, NCard, NEmpty, NTag } from 'naive-ui'

import type { Plan, TimeSlot } from '@/api/types'

const props = defineProps<{
  plan: Plan | null
}>()

const sortedTimeline = computed(() =>
  [...(props.plan?.timeline ?? [])].sort((left, right) => left.order - right.order),
)

function scenarioLabel(plan: Plan) {
  return plan.scenario === 'family' ? '家庭' : '朋友'
}

function formatDistance(slot: TimeSlot) {
  return `${slot.poi.distanceMinutes} 分钟`
}

function formatWait(slot: TimeSlot) {
  return slot.poi.waitMinutes === null ? '等待未知' : `等待 ${slot.poi.waitMinutes} 分钟`
}

function slotKey(slot: TimeSlot) {
  return `${slot.order}-${slot.title}-${slot.startTime}`
}
</script>

<template>
  <NCard
    class="plan-card"
    :class="{ 'is-plan-b': plan?.isPlanB }"
    :bordered="false"
  >
    <template #header>
      <div class="plan-card-header">
        <div>
          <span id="plan-card-title" class="section-title">方案卡</span>
          <p v-if="plan">{{ plan.summary }}</p>
          <p v-else>等待 plan_ready 事件生成可确认方案。</p>
        </div>
        <div v-if="plan" class="plan-card-tags">
          <NTag v-if="plan.isPlanB" :bordered="false" type="warning">
            Plan B
          </NTag>
          <NTag :bordered="false" type="info">
            {{ plan.status }}
          </NTag>
        </div>
      </div>
    </template>

    <NEmpty v-if="!plan" description="暂无方案" />

    <article
      v-else
      class="plan-card-body"
      aria-labelledby="plan-card-title"
    >
      <NAlert
        v-if="plan.isPlanB && plan.planBReason"
        type="warning"
        :show-icon="false"
        class="plan-b-alert"
      >
        <strong>Plan B 已启用</strong>
        <span>{{ plan.planBReason }}</span>
      </NAlert>

      <dl class="plan-metrics" aria-label="方案摘要">
        <div>
          <dt>场景</dt>
          <dd>{{ scenarioLabel(plan) }}</dd>
        </div>
        <div>
          <dt>总时长</dt>
          <dd>{{ plan.totalDurationHours }} 小时</dd>
        </div>
        <div>
          <dt>重排</dt>
          <dd>{{ plan.replanCount }} 次</dd>
        </div>
        <div>
          <dt>Plan ID</dt>
          <dd>{{ plan.planId }}</dd>
        </div>
      </dl>

      <section class="plan-section" aria-label="时间线">
        <div class="section-heading">
          <h2>时间线</h2>
          <NTag :bordered="false" size="small">
            {{ sortedTimeline.length }} 段
          </NTag>
        </div>
        <ol class="plan-timeline">
          <li
            v-for="slot in sortedTimeline"
            :key="slotKey(slot)"
            class="timeline-slot"
          >
            <div class="slot-time">
              <strong>{{ slot.startTime }}</strong>
              <span>{{ slot.endTime }}</span>
            </div>
            <div class="slot-body">
              <div class="slot-title-row">
                <h3>{{ slot.title }}</h3>
                <NTag :bordered="false" size="small" type="info">
                  {{ slot.type }}
                </NTag>
              </div>
              <p class="slot-poi">
                {{ slot.poi.name }} · {{ slot.poi.address }}
              </p>
              <div class="slot-meta">
                <span>评分 {{ slot.poi.rating }}</span>
                <span>{{ formatDistance(slot) }}</span>
                <span>{{ formatWait(slot) }}</span>
              </div>
              <div class="slot-tags" aria-label="地点标签">
                <NTag
                  v-for="tag in slot.poi.tags"
                  :key="tag"
                  :bordered="false"
                  size="small"
                >
                  {{ tag }}
                </NTag>
              </div>
              <ul class="slot-notes">
                <li v-for="note in slot.notes" :key="note">{{ note }}</li>
              </ul>
            </div>
          </li>
        </ol>
      </section>

      <section class="plan-section" aria-label="执行动作">
        <div class="section-heading">
          <h2>执行包</h2>
          <NTag :bordered="false" size="small">
            {{ plan.actions.length }} 个动作
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
        <h2>分享消息</h2>
        <p>{{ plan.shareMessage }}</p>
      </section>
    </article>
  </NCard>
</template>
