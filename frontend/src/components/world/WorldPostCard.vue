<script setup lang="ts">
import { computed } from 'vue'
import type { WorldPost } from '@/api/modules/world'

const props = defineProps<{
  post: WorldPost
}>()

const emit = defineEmits<{
  (e: 'upvote'): void
  (e: 'reply'): void
  (e: 'share'): void
}>()

function isHttpUrlAvatar(avatar: string) {
  return /^https?:\/\//i.test(avatar || '') || avatar.startsWith('/api/')
}

/** 设计稿中与项目 2 一致：非 URL 时 avatar 为色值，用于渐变底 */
function avatarHex(avatar: string) {
  const a = (avatar || '').trim()
  if (/^#[0-9A-Fa-f]{6}$/.test(a)) return a
  if (/^#[0-9A-Fa-f]{3}$/.test(a)) return a
  return '#7c3aed'
}

function avatarLetter() {
  const n = props.post.author.name || '?'
  return n.charAt(0).toUpperCase()
}

function gradientStyle(hex: string) {
  const h = avatarHex(hex)
  return {
    background: `linear-gradient(135deg, ${h}f0, ${h}c0)`,
  }
}

const mediaType = () => String(props.post.mediaType || 'TEXT').toUpperCase()

type ContentSeg = { type: 'text' | 'mention' | 'topic'; value: string }

const contentParts = computed<ContentSeg[]>(() => {
  const content = props.post.content || ''
  // 支持中文、英文、数字、下划线与短横线的话题/提及高亮
  const re = /(@[A-Za-z0-9_\u4E00-\u9FFF-]+|#[A-Za-z0-9_\u4E00-\u9FFF-]+)/g
  const parts: ContentSeg[] = []
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(content)) !== null) {
    if (m.index > last) {
      parts.push({ type: 'text', value: content.slice(last, m.index) })
    }
    const token = m[0]
    parts.push(token.startsWith('@') ? { type: 'mention', value: token } : { type: 'topic', value: token })
    last = m.index + token.length
  }
  if (last < content.length) {
    parts.push({ type: 'text', value: content.slice(last) })
  }
  if (parts.length === 0 && content) {
    parts.push({ type: 'text', value: content })
  }
  return parts
})
</script>

<template>
  <view class="post-card">
    <view class="post-header">
      <image
        v-if="isHttpUrlAvatar(post.author.avatar)"
        class="post-avatar-img"
        :src="post.author.avatar"
        mode="aspectFill"
      />
      <view v-else class="post-avatar-fallback" :style="gradientStyle(post.author.avatar)">
        <text class="post-avatar-letter">{{ avatarLetter() }}</text>
      </view>
      <view class="post-author">
        <view class="post-name-row">
          <text class="post-name">{{ post.author.name }}</text>
          <text v-if="post.author.ai" class="ai-chip">{{ $t('page.world.ai_agent') }}</text>
          <text v-if="post.friend" class="friend-chip">{{ $t('page.world.friend') }}</text>
        </view>
        <text class="post-time">{{ post.timestamp }}</text>
      </view>
    </view>

    <view v-if="mediaType() === 'IMAGE' && post.imageUrl" class="post-media">
      <image class="post-media-image" :src="post.imageUrl" mode="widthFix" />
    </view>
    <view v-else-if="mediaType() === 'VIDEO' && post.videoUrl" class="post-media">
      <video class="post-media-video" :src="post.videoUrl" controls object-fit="contain" />
    </view>

    <view class="post-content">
      <block v-for="(seg, i) in contentParts" :key="i">
        <text v-if="seg.type === 'text'" class="seg-plain">{{ seg.value }}</text>
        <text v-else-if="seg.type === 'mention'" class="seg-mention">{{ seg.value }}</text>
        <text v-else class="seg-topic">{{ seg.value }}</text>
      </block>
    </view>
    <view v-if="post.topics?.length" class="post-topics">
      <text v-for="topic in post.topics" :key="topic" class="topic-chip">#{{ topic }}</text>
    </view>

    <view class="post-actions">
      <view class="action-item" :class="{ 'is-upvoted': post.upvoted }" @click="emit('upvote')">
        <text class="action-icon">↑</text>
        <text class="action-num">{{ post.upvotes }}</text>
      </view>
      <view class="action-item" @click="emit('reply')">
        <text class="action-icon">💬</text>
        <text class="action-num">{{ post.replies }}</text>
      </view>
      <view class="action-item action-share" @click="emit('share')">
        <text class="action-icon">↗</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
@import "@/styles/tokens.css";

/* 对齐项目 2：半透明卡片 + 细边框 + 轻阴影，尺寸用 rpx 与整页一致 */
.post-card {
  background: rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  padding: 24rpx;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.06);
}

.post-header {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  margin-bottom: 16rpx;
}

.post-avatar-fallback {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.12);
}

.post-avatar-letter {
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
}

.post-avatar-img {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}

.post-author {
  flex: 1;
  min-width: 0;
}

.post-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
}

.post-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
}

/* 与项目 2 AI Agent 胶囊一致：紫系描边 + 浅底 */
.ai-chip {
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 20rpx;
  font-weight: 500;
  color: var(--c-violet);
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.12), rgba(168, 85, 247, 0.1));
  border: 1rpx solid rgba(139, 92, 246, 0.22);
}

.friend-chip {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  background: rgba(16, 185, 129, 0.14);
  color: #059669;
}

.post-time {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  display: block;
}

.post-media {
  margin-bottom: 20rpx;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1rpx solid rgba(0, 0, 0, 0.06);
}

.post-media-image {
  width: 100%;
  display: block;
}

.post-media-video {
  width: 100%;
  height: 360rpx;
  background: #000;
}

.post-content {
  margin-bottom: 20rpx;
  line-height: 1.65;
  word-break: break-word;
}

.seg-plain {
  font-size: 26rpx;
  color: var(--c-ink);
}

.seg-mention {
  font-size: 26rpx;
  color: var(--c-primary);
  font-weight: 600;
}

.seg-topic {
  font-size: 26rpx;
  color: var(--c-violet);
  font-weight: 600;
}

.post-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.topic-chip {
  padding: 6rpx 16rpx;
  background: rgba(124, 58, 237, 0.08);
  border-radius: 999rpx;
  font-size: 22rpx;
  color: var(--c-violet);
  font-weight: 500;
}

/* 与项目 2：横向图标 + 文案，非三枚大按钮 */
.post-actions {
  display: flex;
  align-items: center;
  gap: 32rpx;
  padding-top: 8rpx;
}

.action-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8rpx;
}

.action-icon {
  font-size: 28rpx;
  color: var(--c-muted);
  line-height: 1;
}

.action-num {
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-muted);
}

.action-share .action-icon {
  font-size: 30rpx;
}

.action-item.is-upvoted .action-icon,
.action-item.is-upvoted .action-num {
  color: var(--c-secondary);
}
</style>
