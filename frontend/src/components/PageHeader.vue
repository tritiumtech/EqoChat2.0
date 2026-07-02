<template>
  <view class="page-header" :style="customStyle">
    <view class="page-header__inner">
      <view v-if="title" class="header-row">
        <view v-if="showBackIcon" class="back-icon-btn" @click="handleBackClick">
          <text class="back-icon">&lt;</text>
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

      <view v-else-if="showBackIcon" class="back-only-row">
        <view class="back-link" @click="handleBackClick">
          <text class="back-glyph">&lt;</text>
          <text v-if="backText" class="back-text">{{ backText }}</text>
        </view>
      </view>

      <view v-if="subtitle" class="subtitle-row">
        <text class="screen-subtitle">{{ subtitle }}</text>
      </view>

      <slot name="subtitle-custom" />
      <slot name="search" />

      <view v-if="hasTabs" class="tabs">
        <slot name="tabs" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import IconBtn from './IconBtn.vue'

type ActionVariant = 'default' | 'bordered' | 'primary' | 'ghost'
type ActionSize = 'sm' | 'md' | 'lg'

withDefaults(defineProps<{
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
  box-sizing: border-box;
  padding: calc(var(--header-safe-padding-top) + 16rpx) 28rpx 18rpx;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: var(--c-shadow-soft);
}

.page-header__inner {
  width: 100%;
  max-width: var(--page-content-max);
  margin: 0 auto;
}

.header-row,
.back-only-row {
  display: flex;
  align-items: center;
}

.header-row {
  gap: 12rpx;
  min-height: 72rpx;
}

.back-only-row {
  justify-content: flex-start;
  min-height: 64rpx;
}

.back-icon-btn,
.back-link {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-action-icon);
  color: var(--c-ink);
}

.back-icon-btn {
  width: var(--action-icon-size-sm);
  height: var(--action-icon-size-sm);
  border: 1rpx solid var(--c-border);
  background: var(--c-surface);
}

.back-link {
  min-height: 56rpx;
  padding: 0 16rpx;
  gap: 8rpx;
  border: 1rpx solid var(--c-border);
  background: var(--c-surface);
}

.back-icon-btn:active,
.back-link:active {
  background: var(--c-surface-muted);
}

.back-icon,
.back-glyph {
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1;
}

.back-text {
  font-size: 26rpx;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 38rpx;
  font-weight: 700;
  color: var(--c-ink);
  line-height: 1.2;
  letter-spacing: 0;
}

.subtitle-row {
  margin-top: 6rpx;
}

.screen-subtitle {
  display: block;
  font-size: 24rpx;
  color: var(--c-muted);
  line-height: 1.35;
}

.actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.tabs {
  margin-top: 14rpx;
}
</style>
