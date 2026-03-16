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
  width: 120rpx;
  height: 120rpx;
  margin: 0 auto 16rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffe6d8 0%, #fff7f2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff7a59;
  font-size: 48rpx;
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

