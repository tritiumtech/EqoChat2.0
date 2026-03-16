<template>
  <view class="app-shell">
    <view v-if="title || subtitle" class="app-shell-header">
      <view class="app-shell-title-block">
        <text v-if="title" class="app-shell-title">{{ title }}</text>
        <text v-if="subtitle" class="app-shell-subtitle">{{ subtitle }}</text>
      </view>
      <view class="app-shell-header-actions">
        <slot name="header-right" />
      </view>
    </view>
    <view class="app-shell-body">
      <slot />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
}>()
</script>

<style scoped>
@import "@/styles/tokens.css";

.app-shell {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, var(--c-bg) 0%, #fff7f0 100%);
  box-sizing: border-box;
}

.app-shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.app-shell-title-block {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.app-shell-title {
  font-size: var(--font-size-title);
  font-weight: 700;
}

.app-shell-subtitle {
  font-size: var(--font-size-caption);
  color: var(--c-muted);
}

.app-shell-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.app-shell-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
</style>

