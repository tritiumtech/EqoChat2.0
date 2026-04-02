<template>
  <view class="timeline-end">
    <view class="timeline-end-badge">
      <view class="timeline-end-dot" />
      <text>{{ label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'

const { t } = useI18nWithFormat()

interface Props {
  label?: string
  labelKey?: string
}

const props = withDefaults(defineProps<Props>(), {
  labelKey: 'page.world.timeline_end'
})

const label = computed(() => {
  if (props.label) return props.label
  return t(props.labelKey)
})
</script>

<style scoped>
.timeline-end {
  text-align: center;
  margin-top: 48rpx;
}

.timeline-end-badge {
  display: inline-flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 32rpx;
}

.timeline-end-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: var(--c-primary);
  animation: pulse 2s infinite;
}

.timeline-end-badge text {
  font-size: 22rpx;
  color: var(--c-muted);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
