<template>
  <view
    class="message-row"
    :class="{ self: isSelf, other: !isSelf }"
  >
    <view
      class="avatar small"
      :class="avatarClass"
    >
      <text class="avatar-glyph">{{ avatarGlyph }}</text>
    </view>
    <view
      class="bubble"
      :class="{ self: isSelf, other: !isSelf, agent: isAgent && !isSelf, failed: failed }"
      @click="$emit('click')"
    >
      <view class="bubble-header">
        <view class="bubble-meta-left">
          <text class="author">{{ authorText }}</text>
          <view
            v-if="isAgent && !isSelf"
            class="agent-badge"
          >
            AI Agent
          </view>
        </view>
        <text class="time">{{ time }}</text>
      </view>

      <text v-if="!isAttachmentOnly" class="content">{{ content }}</text>
      <FileAttachment
        v-if="isAttachment"
        :file-name="attachment?.fileName || ''"
        :file-size="attachment?.fileSize || ''"
        :file-type="attachment?.fileType || ''"
        :download-url="attachment?.downloadUrl"
      />

      <view class="bubble-checks">
        <text v-if="failed" class="failed-check">{{ retryText }}</text>
        <text v-else-if="local && !failed" class="sending-check">{{ sendingText }}</text>
        <text v-else class="checks">{{ isSelf ? '✓✓' : '✓' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FileAttachment from '@/components/chat/FileAttachment.vue'

const props = defineProps<{
  isSelf: boolean
  isAgent?: boolean
  content: string
  messageType?: string
  attachment?: {
    fileName?: string
    fileSize?: string
    fileType?: string
    downloadUrl?: string
  }
  author?: string
  time: string
  local?: boolean
  failed?: boolean
  avatarText?: string
  sendingText?: string
  retryText?: string
}>()

const isAgent = computed(() => !!props.isAgent)

const authorText = computed(() => {
  const v = (props.author || '').toString()
  return v.length > 0 ? v : (props.avatarText || '')
})

const avatarGlyph = computed(() => {
  if (isAgent.value && !props.isSelf) return 'AI'
  const source = String(props.avatarText || props.author || '').trim()
  return (source.slice(0, 1) || (props.isSelf ? 'M' : 'H')).toUpperCase()
})

const avatarClass = computed(() => {
  return {
    inbound: !props.isSelf,
    outbound: props.isSelf,
    agent: isAgent.value && !props.isSelf,
  }
})

const isAttachment = computed(() => {
  const mt = (props.messageType || '').toUpperCase()
  return mt === 'FILE' || mt === 'IMAGE' || mt === 'CARD'
})

const isAttachmentOnly = computed(() => {
  const content = (props.content || '').trim()
  return isAttachment.value && content.length === 0
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.message-row {
  margin-bottom: 4rpx;
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 16rpx 24rpx;
  transition: background-color 160ms ease;
}

.message-row:active {
  background: rgba(3, 2, 19, 0.03);
}

.message-row.self {
  justify-content: flex-start;
}

.message-row.other {
  justify-content: flex-start;
}

.avatar.small {
  width: 86rpx;
  height: 86rpx;
  border-radius: var(--radius-md);
  font-size: 24rpx;
  margin-top: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.bubble {
  max-width: calc(100% - 96rpx);
  display: flex;
  flex-direction: column;
  font-size: 25rpx;
  line-height: 1.55;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  color: var(--c-ink);
}

.bubble.self {
  color: var(--c-ink);
}

.bubble.other {
  background: transparent;
  border: none;
  color: var(--c-ink);
}

.bubble.other.agent {
  background: transparent;
  border: none;
}

.content {
  display: block;
  margin-top: 8rpx;
  word-break: break-word;
}

.bubble-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8rpx;
  margin-top: 0;
}

.bubble-meta-left {
  flex: 1;
  min-width: 0;
}

.author {
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1.1;
  color: rgba(26, 23, 32, 0.86);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260rpx;
}

.time {
  font-size: 21rpx;
  color: rgba(26, 23, 32, 0.55);
  flex-shrink: 0;
  line-height: 1.1;
}

.bubble-header .bubble-meta-left {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.bubble-checks {
  margin-top: 4rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.bubble.self .bubble-checks {
  justify-content: flex-end;
}

.checks {
  font-size: 18rpx;
  color: rgba(26, 23, 32, 0.55);
}

.sending-check {
  font-size: 18rpx;
  color: rgba(26, 23, 32, 0.55);
}

.failed-check {
  font-size: 18rpx;
  color: #ef4444;
}

.avatar.small.inbound {
  background: linear-gradient(135deg, #60a5fa 0%, #0ea5e9 100%);
  color: #fff;
}

.avatar.small.inbound.agent {
  background: linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%);
  color: #fff;
}

.avatar.small.outbound {
  background: linear-gradient(135deg, #ffb39c 0%, #ff8b6d 100%);
  color: #fff;
}

.avatar-glyph {
  position: relative;
  z-index: 1;
}

.avatar.small.agent::after {
  content: '';
  position: absolute;
  top: -2rpx;
  right: -2rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: #10b981;
  border: 2rpx solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10rpx 18rpx rgba(0, 0, 0, 0.12);
}

.agent-badge {
  padding: 4rpx 12rpx;
  border-radius: var(--radius-md);
  background: rgba(124, 58, 237, 0.08);
  border: 1rpx solid rgba(124, 58, 237, 0.18);
  color: #7c3aed;
  font-size: 16rpx;
  font-weight: 600;
  white-space: nowrap;
}
</style>
