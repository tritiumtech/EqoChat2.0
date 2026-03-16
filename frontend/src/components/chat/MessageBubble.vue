<template>
  <view
    class="message-row"
    :class="sideClass"
  >
    <view v-if="!isSelf" class="avatar small">
      <text>{{ avatarText }}</text>
    </view>
    <view
      class="bubble"
      :class="[isSelf ? 'self' : 'other', failed ? 'failed' : '']"
      @click="$emit('click')"
    >
      <text class="content">{{ content }}</text>
      <view class="meta-row">
        <text class="time">{{ time }}</text>
        <text v-if="local && !failed" class="sending">{{ sendingText }}</text>
        <text v-if="failed" class="failed-text">{{ retryText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  isSelf: boolean
  content: string
  time: string
  local?: boolean
  failed?: boolean
  avatarText?: string
  sendingText?: string
  retryText?: string
}>()

const sideClass = computed(() => (props.isSelf ? 'self' : 'other'))
</script>

<style scoped>
@import "@/styles/tokens.css";

.message-row {
  margin-bottom: 22rpx;
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
}

.message-row.self {
  justify-content: flex-end;
}

.message-row.other {
  justify-content: flex-start;
}

.avatar.small {
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  font-size: 24rpx;
  margin-top: 6rpx;
  background: linear-gradient(135deg, #ffe2d3 0%, #ffd1c4 100%);
  color: #6a3a2b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble {
  max-width: 74%;
  padding: 20rpx 24rpx 16rpx;
  border-radius: 22rpx;
  font-size: 28rpx;
  line-height: 1.5;
  box-shadow: var(--c-shadow-soft);
  position: relative;
}

.bubble.self {
  margin-left: auto;
  background: var(--c-message-outbound);
  color: #2a1c1a;
}

.bubble.other {
  margin-right: auto;
  background: var(--c-message-inbound);
  border: 1rpx solid var(--c-border);
  color: var(--c-ink);
}

.content {
  display: block;
}

.meta-row {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  justify-content: flex-end;
}

.time {
  font-size: 22rpx;
  color: rgba(26, 23, 32, 0.55);
}

.sending {
  font-size: 22rpx;
  color: rgba(26, 23, 32, 0.55);
}

.failed-text {
  font-size: 22rpx;
  color: #ef4444;
}
</style>

