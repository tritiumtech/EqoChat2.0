<template>
  <view class="page-header" :style="customStyle">
    <view class="safe-area-top" />

    <!-- 返回行（详情页模式） -->
    <view v-if="hasBackRow" class="back-row">
      <slot name="back">
        <view v-if="backText" class="back-link" @click="handleBackClick">
          <text class="back-glyph">←</text>
          <text class="back-text">{{ backText }}</text>
        </view>
      </slot>

      <view class="back-row-actions">
        <slot name="back-actions" />
      </view>
    </view>

    <!-- 标题行 -->
    <view class="header-row">
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
import { computed } from 'vue'
import IconBtn from './IconBtn.vue'

type ActionVariant = 'default' | 'bordered' | 'primary' | 'ghost'
type ActionSize = 'sm' | 'md' | 'lg'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  actionIcon?: string
  actionVariant?: ActionVariant
  actionSize?: ActionSize
  hasTabs?: boolean
  customStyle?: Record<string, string | number>
  hasBackRow?: boolean
  backText?: string
}>(), {
  subtitle: '',
  actionIcon: '',
  actionVariant: 'bordered',
  actionSize: 'md',
  hasTabs: false,
  hasBackRow: false,
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

.back-row {
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

.back-row-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
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
  margin-top: 4rpx;
}
</style>
