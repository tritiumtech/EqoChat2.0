<template>
  <view class="item" @click="$emit('click')">
    <view class="avatar">
      <text>{{ displayInitial }}</text>
    </view>
    <view class="content">
      <text class="name">{{ displayTitle }}</text>
      <text class="message">{{ displaySubtitle }}</text>
    </view>
    <view class="meta">
      <text class="time">{{ time }}</text>
      <view v-if="unreadCount" class="badge">{{ unreadCount }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  title?: string
  subtitle?: string
  time?: string
  unreadCount?: number
  fallbackTitle: string
  fallbackSubtitle: string
}>()

const displayTitle = computed(() => props.title || props.fallbackTitle)
const displaySubtitle = computed(() => props.subtitle || props.fallbackSubtitle)
const displayInitial = computed(() => displayTitle.value.slice(0, 1))
</script>

<style scoped>
@import "@/styles/tokens.css";

.item {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 22rpx;
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ffddb3 0%, #ffd3c6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7a3b2a;
  font-size: 30rpx;
  font-weight: 700;
  margin-right: 18rpx;
}

.content {
  flex: 1;
}

.name {
  font-size: 30rpx;
  color: var(--c-ink);
  font-weight: 700;
  display: block;
}

.message {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
  display: block;
}

.meta {
  text-align: right;
  min-width: 120rpx;
}

.time {
  font-size: 22rpx;
  color: #9b98a5;
  display: block;
  margin-bottom: 8rpx;
}

.badge {
  display: inline-block;
  min-width: 36rpx;
  padding: 4rpx 12rpx;
  background: #ff7a59;
  color: #fff;
  font-size: 20rpx;
  border-radius: var(--radius-pill);
}
</style>

