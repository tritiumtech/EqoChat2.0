<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { worldApi, type WorldPost, type WorldPostReply, type WorldSubjectParams } from '@/api/modules/world'
import { getApiErrorMessage } from '@/utils/request'

const props = defineProps<{
  post: WorldPost
  viewer?: WorldSubjectParams
}>()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'upvote'): void
  (e: 'reply'): void
  (e: 'share'): void
}>()

const replies = ref<WorldPostReply[]>([])
const loadingReplies = ref(false)
const flatReplies = computed(() => replies.value || [])

function avatarLetter(name: string) {
  const n = String(name || '?')
  return n.charAt(0).toUpperCase()
}

async function loadReplies() {
  if (!props.post?.id) return
  loadingReplies.value = true
  try {
    const list = await worldApi.listReplies(props.post.id, {
      limit: 100,
      viewerSubjectId: props.viewer?.subjectId,
      viewerSubjectType: props.viewer?.subjectType,
    })
    replies.value = Array.isArray(list) ? list : []
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, ''), icon: 'none' })
  } finally {
    loadingReplies.value = false
  }
}

onMounted(loadReplies)

watch(
  () => props.post.id,
  () => {
    loadReplies()
  },
)

defineExpose({ loadReplies })

function onToggleReplyUpvote(targetId: string) {
  const walk = (nodes: WorldPostReply[]): WorldPostReply[] =>
    nodes.map((n) => {
      if (n.id === targetId) {
        const nextUpvoted = !n.upvoted
        const delta = nextUpvoted === n.upvoted ? 0 : (nextUpvoted ? 1 : -1)
        return {
          ...n,
          upvoted: nextUpvoted,
          upvotes: Math.max(0, (n.upvotes || 0) + delta),
        }
      }
      if (n.replies && n.replies.length) {
        return {
          ...n,
          replies: walk(n.replies),
        }
      }
      return n
    })
  replies.value = walk(replies.value)
}
</script>
<template>
  <!-- 全屏详情层，覆盖 world 页 -->
  <view class="detail-overlay">
    <view class="detail-page">
      <!-- Header -->
      <view class="detail-header">
        <button class="detail-back-btn" @click="emit('back')">
          <text class="detail-back-icon">‹</text>
        </button>
        <text class="detail-title">Post</text>
      </view>

      <!-- Content -->
      <scroll-view scroll-y class="detail-scroll">
        <!-- 原帖 -->
        <view class="detail-post">
          <view class="detail-author-row">
            <view class="detail-avatar" :style="{ background: `linear-gradient(135deg, ${post.author.avatar}f0, ${post.author.avatar}e0)` }">
              <text class="detail-avatar-letter">{{ avatarLetter(post.author.name) }}</text>
            </view>
            <view class="detail-author-meta">
              <text class="detail-author-name">{{ post.author.name }}</text>
              <view class="detail-badges">
                <text v-if="post.author.ai" class="detail-ai-chip">{{ $t('page.world.ai_agent') }}</text>
                <text v-if="post.friend" class="detail-friend-chip">{{ $t('page.world.friend') }}</text>
              </view>
              <text class="detail-time">{{ post.timestamp }}</text>
            </view>
          </view>

          <view class="detail-content">
            <text class="detail-content-text">{{ post.content }}</text>
          </view>

          <view v-if="post.imageUrl" class="detail-media">
            <image class="detail-image" :src="post.imageUrl" mode="widthFix" />
          </view>
          <view v-else-if="post.videoUrl" class="detail-media">
            <video class="detail-video" :src="post.videoUrl" controls />
          </view>

          <!-- 操作条 -->
          <view class="detail-actions">
            <view class="detail-action" :class="{ 'is-upvoted': post.upvoted }" @click="emit('upvote')">
              <text class="detail-action-icon">👍</text>
              <text class="detail-action-text">{{ post.upvotes }}</text>
            </view>
            <view class="detail-action" @click="emit('reply')">
              <text class="detail-action-icon">R</text>
              <text class="detail-action-text">{{ post.replies }}</text>
            </view>
            <view class="detail-action" @click="emit('share')">
              <text class="detail-action-icon">S</text>
              <text class="detail-action-text">{{ $t('page.world.share') }}</text>
            </view>
          </view>
        </view>

        <!-- 简单评论列表，对齐 PostDetailPage 的基础结构（但不含嵌套与点赞） -->
        <view class="detail-replies">
          <view class="detail-replies-head">
            <text class="detail-replies-title">
              {{ replies.length }}
              {{ replies.length === 1 ? 'Reply' : 'Replies' }}
            </text>
          </view>

          <view v-if="loadingReplies" class="detail-replies-empty">
            <text>{{ $t('common.loading') }}</text>
          </view>
          <view v-else-if="!replies.length" class="detail-replies-empty">
            <text>{{ $t('common.empty_conversation') }}</text>
          </view>
          <view v-else>
            <view
              v-for="item in replies"
              :key="item.id"
              class="detail-reply-item"
            >
              <view class="detail-reply-avatar" :style="{ background: `linear-gradient(135deg, ${item.author.avatar}f0, ${item.author.avatar}e0)` }">
                <text class="detail-reply-avatar-letter">{{ avatarLetter(item.author.name) }}</text>
              </view>
              <view class="detail-reply-body">
                <view class="detail-reply-head">
                  <text class="detail-reply-name">{{ item.author.name }}</text>
                  <text class="detail-reply-time">{{ item.timestamp }}</text>
                </view>
                <text class="detail-reply-content">{{ item.content }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- TODO：后续可在此处扩展回复列表/时间线等 -->
      </scroll-view>
    </view>
  </view>
</template>

<style scoped>
@import "@/styles/tokens.css";

.detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
}

.detail-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  padding: 20rpx 24rpx;
  padding-top: calc(20rpx + env(safe-area-inset-top));
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 6rpx 16rpx rgba(15, 23, 42, 0.06);
}

.detail-back-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-lg);
  border: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-input-bg);
}

.detail-back-icon {
  font-size: 40rpx;
  color: var(--c-ink);
}

.detail-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.detail-scroll {
  flex: 1;
  min-height: 0;
}

.detail-post {
  padding: 24rpx;
}

.detail-author-row {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 16rpx;
}

.detail-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.16);
}

.detail-avatar-letter {
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
}

.detail-author-meta {
  flex: 1;
  min-width: 0;
}

.detail-author-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.detail-badges {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 6rpx;
}

.detail-ai-chip {
  padding: 4rpx 12rpx;
  border-radius: 10rpx;
  font-size: 20rpx;
  color: var(--c-violet);
  background: rgba(124, 58, 237, 0.1);
  border: 1rpx solid rgba(124, 58, 237, 0.22);
}

.detail-friend-chip {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  background: rgba(16, 185, 129, 0.14);
  color: #059669;
}

.detail-time {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.detail-content {
  margin-top: 10rpx;
  margin-bottom: 18rpx;
}

.detail-content-text {
  font-size: 26rpx;
  color: var(--c-ink);
  line-height: 1.7;
  white-space: pre-wrap;
}

.detail-media {
  margin-bottom: 18rpx;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
}

.detail-image {
  width: 100%;
}

.detail-video {
  width: 100%;
  height: 360rpx;
  background: #000;
}

.detail-actions {
  margin-top: 8rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid rgba(148, 163, 184, 0.5);
  display: flex;
  align-items: center;
  gap: 32rpx;
}

.detail-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.detail-action-icon {
  font-size: 24rpx;
}

.detail-action-text {
  font-size: 22rpx;
  color: var(--c-muted);
}

.detail-action.is-upvoted .detail-action-text {
  color: var(--c-secondary);
}

.detail-replies {
  padding: 0 24rpx 24rpx;
}

.detail-replies-head {
  margin-bottom: 16rpx;
}

.detail-replies-title {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.detail-replies-empty {
  padding: 32rpx 0;
  text-align: center;
  font-size: 22rpx;
  color: var(--c-muted);
}

.detail-reply-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 16rpx 0;
  border-bottom: 1rpx solid rgba(148, 163, 184, 0.3);
}

.detail-reply-item.child {
  border-bottom-width: 0;
}

.detail-reply-children {
  margin-left: 40rpx;
  padding-left: 16rpx;
  border-left: 2rpx solid rgba(148, 163, 184, 0.4);
}

.detail-reply-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-reply-avatar-letter {
  font-size: 24rpx;
  font-weight: 700;
  color: #fff;
}

.detail-reply-body {
  flex: 1;
  min-width: 0;
}

.detail-reply-head {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin-bottom: 4rpx;
}

.detail-reply-name {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.detail-reply-time {
  font-size: 20rpx;
  color: var(--c-muted);
}

.detail-reply-content {
  font-size: 24rpx;
  color: var(--c-ink);
  line-height: 1.6;
}

.detail-reply-actions {
  margin-top: 6rpx;
}

.detail-reply-like {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
}

.detail-reply-like-icon {
  font-size: 22rpx;
}

.detail-reply-like-count {
  font-size: 20rpx;
  color: var(--c-muted);
}

.detail-reply-like.is-upvoted .detail-reply-like-count {
  color: var(--c-secondary);
}
</style>
