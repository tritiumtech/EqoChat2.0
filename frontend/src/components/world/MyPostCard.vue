<template>
  <view class="my-post-card" @click="handleCardClick">
    <!-- Thumbnail -->
    <image
      v-if="post.mediaType === 'IMAGE' && post.imageUrl"
      :src="post.imageUrl"
      class="my-post-thumbnail"
      mode="aspectFill"
    />
    <view v-else-if="post.mediaType === 'VIDEO'" class="my-post-thumbnail my-post-thumbnail-video">
      <u-icon name="play-circle-fill" :size="32" color="var(--c-ink)" />
    </view>

    <!-- Post Info -->
    <view class="my-post-info">
      <!-- Post Content Preview -->
      <text class="my-post-content">{{ post.content }}</text>

      <!-- Metadata -->
      <view class="my-post-meta">
        <text class="my-post-time">{{ post.timestamp }}</text>
      </view>

      <!-- Post Actions -->
      <view class="my-post-actions">
        <view class="my-post-action" :class="{ active: post.upvoted }" @click.stop="handleUpvote">
          <u-icon 
            :name="post.upvoted ? 'thumb-up-fill' : 'thumb-up'" 
            :size="28" 
            :color="post.upvoted ? 'var(--c-primary)' : 'var(--c-muted)'"
          />
          <text class="action-count">{{ post.upvotes + (post.upvoted ? 1 : 0) }}</text>
        </view>
        <view class="my-post-action" @click.stop="handleReply">
          <u-icon name="chat" :size="28" color="var(--c-muted)" />
          <text class="action-count">{{ post.replies }}</text>
        </view>
        <view class="my-post-action" @click.stop="handleShare">
          <u-icon name="share" :size="28" color="var(--c-muted)" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import type { WorldPost } from '@/api/modules/world'

interface Props {
  post: WorldPost
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'click': []
  'upvote': []
  'reply': []
  'share': []
}>()

function handleCardClick() {
  emit('click')
}

function handleUpvote() {
  emit('upvote')
}

function handleReply() {
  emit('reply')
}

function handleShare() {
  emit('share')
}
</script>

<style scoped>
.my-post-card {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.my-post-card:active {
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}

.my-post-thumbnail {
  width: 120rpx;
  height: 120rpx;
  border-radius: 16rpx;
  background: rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  object-fit: cover;
}

.my-post-thumbnail-video {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.08);
}

.my-post-info {
  flex: 1;
  min-width: 0;
}

.my-post-content {
  display: block;
  font-size: 26rpx;
  color: var(--c-ink);
  line-height: 1.5;
  margin-bottom: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
}

.my-post-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.my-post-time {
  font-size: 22rpx;
  color: var(--c-muted);
}

.my-post-actions {
  display: flex;
  align-items: center;
  gap: 32rpx;
}

.my-post-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  transition: color 0.2s;
}

.my-post-action.active {
  color: var(--c-primary);
}

.my-post-action:not(.active):active {
  color: var(--c-primary);
}

.action-count {
  font-size: 22rpx;
  font-weight: 500;
}
</style>
