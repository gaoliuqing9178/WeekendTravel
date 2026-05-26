<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { NCard, NEmpty, NTag } from 'naive-ui'

import type { SseConnectionState } from '@/composables/useSSE'
import type { LogEvent } from '@/stores/planner'

const props = defineProps<{
  events: LogEvent[]
  connectionState: SseConnectionState
}>()

const logViewport = ref<HTMLElement | null>(null)

const eventCountLabel = computed(() => `${props.events.length} 条事件`)

watch(
  () => props.events.length,
  () => {
    scrollToLatest()
  },
  { flush: 'post' },
)

onMounted(() => {
  scrollToLatest()
})

function scrollToLatest() {
  void nextTick(() => {
    const viewport = logViewport.value
    if (!viewport) {
      return
    }

    viewport.scrollTo({
      top: viewport.scrollHeight,
      behavior: prefersReducedMotion() ? 'auto' : 'smooth',
    })
  })
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function tagType(event: LogEvent) {
  if (event.type === 'error') {
    return 'error'
  }

  if (event.type === 'replan') {
    return 'warning'
  }

  if (
    event.type === 'plan_ready' ||
    event.type === 'done' ||
    event.type === 'execute_result'
  ) {
    return 'success'
  }

  if (event.type === 'state_change' || event.type === 'tool_result') {
    return 'info'
  }

  return 'default'
}

function eventClass(event: LogEvent) {
  return {
    'is-replan': event.type === 'replan',
    'is-error': event.type === 'error',
  }
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour12: false,
  })
}
</script>

<template>
  <NCard class="log-panel" :bordered="false">
    <template #header>
      <div class="log-panel-header">
        <div>
          <span id="log-panel-title" class="log-panel-title">实时日志面板</span>
          <p>按 SSE 事件顺序追加，最新事件会自动滚动到可见区域。</p>
        </div>
        <div class="log-panel-meta" aria-label="日志状态">
          <NTag :bordered="false" size="small" type="info">
            {{ connectionState }}
          </NTag>
          <NTag :bordered="false" size="small">
            {{ eventCountLabel }}
          </NTag>
        </div>
      </div>
    </template>

    <div
      ref="logViewport"
      class="log-panel-viewport"
      role="log"
      aria-labelledby="log-panel-title"
      aria-live="polite"
      aria-relevant="additions text"
    >
      <NEmpty
        v-if="events.length === 0"
        description="等待 SSE 事件"
      />
      <ol v-else class="log-event-list">
        <li
          v-for="event in events"
          :key="event.id"
          class="log-event"
          :class="eventClass(event)"
        >
          <div class="log-event-marker" aria-hidden="true" />
          <div class="log-event-body">
            <div class="log-event-row">
              <NTag
                class="log-event-type"
                :bordered="false"
                size="small"
                :type="tagType(event)"
              >
                {{ event.type }}
              </NTag>
              <time :datetime="new Date(event.timestamp).toISOString()">
                {{ formatTime(event.timestamp) }}
              </time>
            </div>
            <h3>{{ event.title }}</h3>
            <p>{{ event.detail }}</p>
          </div>
        </li>
      </ol>
    </div>
  </NCard>
</template>
