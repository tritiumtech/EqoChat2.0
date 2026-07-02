<template>
  <view class="empty">
    <view v-if="icon" class="empty-icon">
      <text>{{ icon }}</text>
    </view>
    <text class="empty-title">
      <slot name="title">{{ title }}</slot>
    </text>
    <text v-if="description" class="empty-desc">
      <slot name="description">{{ description }}</slot>
    </text>
    <view v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
  icon?: string
}>()
</script>

<style scoped>
@import "@/styles/tokens.css";

.empty {
  padding: 40rpx 24rpx;
  text-align: center;
  color: var(--c-muted);
}

.empty-icon {
  width: 104rpx;
  height: 104rpx;
  margin: 0 auto 16rpx;
  border-radius: var(--radius-avatar);
  background: var(--c-surface-muted);
  border: 1rpx solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-muted);
  font-size: 34rpx;
  font-weight: 800;
}

.empty-title {
  font-size: 28rpx;
  font-weight: 600;
  display: block;
}

.empty-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  display: block;
}

.empty-action {
  margin-top: 20rpx;
}
</style>
