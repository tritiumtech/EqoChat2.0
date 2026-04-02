<template>
  <view class="timeline-header">
    <view class="timeline-header-inner">
      <view>
        <text class="timeline-title">{{ title }}</text>
        <text class="timeline-count">{{ countLabel }}</text>
      </view>
      <view class="timeline-index-btn" @click="handleToggleIndex">
        <u-icon name="calendar" :size="28" color="var(--c-ink)" />
      </view>
    </view>

    <!-- Timeline Index Panel -->
    <TimelineIndex
      v-if="showIndex"
      :structure="timelineStructure"
      :expanded-years="expandedYears"
      :expanded-months="expandedMonths"
      @toggle-year="$emit('toggle-year', $event)"
      @toggle-month="$emit('toggle-month', $event)"
      @close="$emit('update:show-index', false)"
    />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import TimelineIndex from './TimelineIndex.vue'

const { t, tf } = useI18nWithFormat()

interface Props {
  title: string
  totalPosts: number
  showIndex: boolean
  timelineStructure: { [year: string]: { [month: string]: string[] } }
  expandedYears: Set<string>
  expandedMonths: Set<string>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:show-index': [value: boolean]
  'toggle-year': [year: string]
  'toggle-month': [monthKey: string]
}>()

const countLabel = computed(() => {
  return tf('page.world.timeline_total_posts', { n: props.totalPosts })
})

function handleToggleIndex() {
  emit('update:show-index', !props.showIndex)
}
</script>

<style scoped>
.timeline-header {
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.timeline-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 24rpx;
}

.timeline-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.timeline-count {
  display: block;
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
}

.timeline-index-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: var(--c-surface);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.timeline-index-btn:active {
  background: rgba(0, 0, 0, 0.05);
}
</style>
