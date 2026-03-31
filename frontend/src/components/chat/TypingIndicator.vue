<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n({ useScope: 'global' })

const props = defineProps<{
  label?: string
  avatarColor?: string
  avatarText?: string
}>()

const labelText = computed(() => props.label || t('page.chat.typing_label_default'))
const avatar = computed(() => props.avatarText || '…')
const typingText = computed(() => t('page.chat.typing_text', { label: labelText.value }))

const safeAvatarColor = computed(() => {
  const v = String(props.avatarColor || '#7c3aed').trim()
  // 允许传入类似 #7c3aed 或任意可用于 CSS 的颜色值
  return v
})
</script>

<template>
  <view class="typing-wrap">
    <view class="typing-avatar" :style="{ background: `linear-gradient(135deg, ${safeAvatarColor}f0, ${safeAvatarColor}e0)` }">
      <text class="typing-avatar-text">{{ avatar }}</text>
    </view>
    <view class="typing-bubble">
      <view class="dots">
        <view class="dot d1" />
        <view class="dot d2" />
        <view class="dot d3" />
      </view>
      <text class="typing-text">{{ typingText }}</text>
    </view>
  </view>
</template>

<style scoped>
@import "@/styles/tokens.css";

.typing-wrap {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 0;
}

.typing-avatar {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8rpx 20rpx rgba(3, 2, 19, 0.12);
}

.typing-avatar-text {
  color: #fff;
  font-weight: 800;
  font-size: 20rpx;
  line-height: 1;
}

.typing-bubble {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: rgba(91, 103, 241, 0.08);
  border: 1rpx solid rgba(91, 103, 241, 0.2);
  border-radius: 16rpx;
  padding: 10rpx 16rpx;
  box-sizing: border-box;
}

.dots {
  display: flex;
  gap: 8rpx;
}

.dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: rgba(3, 2, 19, 0.28);
  animation: typingDot 1s infinite ease-in-out;
}

.d2 {
  animation-delay: 0.15s;
}

.d3 {
  animation-delay: 0.3s;
}

@keyframes typingDot {
  0%,
  100% {
    transform: translateY(0) scale(1);
    opacity: 0.45;
  }
  50% {
    transform: translateY(-4rpx) scale(1.2);
    opacity: 1;
  }
}

.typing-text {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 600;
  white-space: nowrap;
}
</style>

