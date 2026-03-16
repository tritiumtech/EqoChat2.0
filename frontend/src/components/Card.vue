<template>
  <view class="card" :class="variant">
    <slot />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  variant?: 'default' | 'primary' | 'secondary'
}>()
</script>

<style scoped>
@import "@/styles/tokens.css";

.card {
  border-radius: var(--radius-lg);
  padding: 24rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  box-shadow: var(--c-shadow-soft);
}

.primary {
  background: linear-gradient(135deg, #ffe6d8 0%, #fff7f2 100%);
  border-color: rgba(255, 122, 89, 0.2);
}

.secondary {
  background: linear-gradient(135deg, #e8edff 0%, #f6f8ff 100%);
  border-color: rgba(79, 107, 255, 0.2);
}
</style>

