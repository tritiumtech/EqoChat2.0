<template>
  <view class="page-header" :style="customStyle">
    <view class="safe-area-top" />

    <!-- 单行模式：返回图标 + 标题 -->
    <view v-if="title" class="header-row">
      <view v-if="showBackIcon" class="back-icon-btn" @click="handleBackClick">
        <text class="back-icon">←</text>
      </view>
      <view class="titles">
        <slot name="title-prefix" />
        <text class="screen-title">{{ title }}</text>
      </view>

      <view class="actions">
        <slot name="actions">
          <IconBtn
            v-if="actionIcon"
            :icon="actionIcon"
            :variant="actionVariant"
            :size="actionSize"
            @click="handleActionClick"
          />
        </slot>
      </view>
    </view>

    <!-- 纯返回行模式（无标题） -->
    <view v-else-if="showBackIcon" class="back-only-row">
      <view class="back-link" @click="handleBackClick">
        <text class="back-glyph">←</text>
        <text v-if="backText" class="back-text">{{ backText }}</text>
      </view>
    </view>

    <!-- 副标题行 -->
    <view v-if="subtitle" class="subtitle-row">
      <text class="screen-subtitle">{{ subtitle }}</text>
    </view>

    <!-- 自定义副标题 slot -->
    <slot name="subtitle-custom" />

    <slot name="search" />

    <view v-if="hasTabs" class="tabs">
      <slot name="tabs" />
    </view>
  </view>
</template>

<script setup lang="ts">
import IconBtn from './IconBtn.vue'

type ActionVariant = 'default' | 'bordered' | 'primary' | 'ghost'
type ActionSize = 'sm' | 'md' | 'lg'

const props = withDefaults(defineProps<{
  title?: string
  subtitle?: string
  actionIcon?: string
  actionVariant?: ActionVariant
  actionSize?: ActionSize
  hasTabs?: boolean
  customStyle?: Record<string, string | number>
  showBackIcon?: boolean
  backText?: string
}>(), {
  title: '',
  subtitle: '',
  actionIcon: '',
  actionVariant: 'bordered',
  actionSize: 'md',
  hasTabs: false,
  showBackIcon: false,
  backText: ''
})

const emit = defineEmits<{
  (e: 'action-click'): void
  (e: 'back-click'): void
}>()

const handleActionClick = () => {
  emit('action-click')
}

const handleBackClick = () => {
  emit('back-click')
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.page-header {
  flex-shrink: 0;
  padding: calc(var(--status-bar-height) + 16rpx) 32rpx 20rpx;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.safe-area-top {
  display: none;
}

.back-only-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.back-link {
  display: flex;
  align-items: center;
  padding: 10rpx 12rpx;
  border-radius: 16rpx;
}

.back-link:active {
  background: rgba(0, 0, 0, 0.04);
}

.back-glyph {
  font-size: 28rpx;
  color: var(--c-primary);
  margin-right: 4rpx;
}

.back-text {
  font-size: 26rpx;
  color: var(--c-primary);
}

.header-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
   margin-bottom: 16rpx;
}

.back-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  margin-right: 4rpx;
}

.back-icon-btn:active {
  background: rgba(0, 0, 0, 0.04);
}

.back-icon {
  font-size: 32rpx;
  color: var(--c-ink);
}

.titles {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.screen-title {
  font-size: 40rpx;
  font-weight: 600;
  color: var(--c-ink);
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.subtitle-row {
  margin-top: 4rpx;
}

.screen-subtitle {
  display: block;
  font-size: 22rpx;
  color: var(--c-muted);
  line-height: 1.3;
}

.actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.tabs {
  margin-top: 12rpx;
}
</style>