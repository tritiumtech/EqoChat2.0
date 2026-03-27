<template>
  <view class="topic-card" @click="$emit('open', topic.name)">
    <view class="topic-head">
      <text class="topic-hash">#{{ topic.name }}</text>
      <view class="follow-btn" :class="{ active: topic.favorite }" @click.stop="$emit('toggle-follow', topic)">
        <text>{{ topic.favorite ? followedText : followText }}</text>
      </view>
    </view>
    <text class="topic-stats">{{ topic.posts }} {{ postsLabel }} · {{ topic.followers }} {{ followersLabel }}</text>
  </view>
</template>

<script setup lang="ts">
import type { WorldTopic } from '@/api/modules/world'

defineEmits<{
  (e: 'open', name: string): void
  (e: 'toggle-follow', topic: WorldTopic): void
}>()

defineProps<{
  topic: WorldTopic
  postsLabel: string
  followersLabel: string
  followText: string
  followedText: string
}>()
</script>

<style scoped>
@import "@/styles/tokens.css";

.topic-card {
  width: 100%;
  position: relative;
  box-sizing: border-box;
  text-align: left;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-lg);
  padding: 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.05);
}

.topic-head {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12rpx;
}

.topic-hash {
  display: block;
  padding-right: 180rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #4f46e5;
}

.follow-btn {
  position: absolute;
  right: 24rpx;
  top: 24rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-width: 120rpx;
  min-height: 52rpx;
  box-sizing: border-box;
  border: 1rpx solid rgba(0, 0, 0, 0.12);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.85);
  padding: 8rpx 14rpx;
  margin: 0;
  line-height: 1;
  z-index: 2;
}

.follow-btn text {
  font-size: 20rpx;
  font-weight: 700;
  color: var(--c-muted);
  white-space: nowrap;
}

.follow-btn.active {
  background: rgba(3, 2, 19, 0.08);
  border-color: rgba(3, 2, 19, 0.24);
}

.follow-btn.active text {
  color: var(--c-primary);
}

.topic-stats {
  font-size: 22rpx;
  color: var(--c-muted);
}
</style>

