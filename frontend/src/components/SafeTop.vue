<template>
  <view :class="['safe-top', { 'safe-top--fixed': fixed }]" :style="customStyle">
    <slot />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  fixed?: boolean
  customStyle?: Record<string, string | number>
}>()
</script>

<style scoped>
.safe-top {
  padding-top: var(--status-bar-height);
  background: inherit;
}

.safe-top--fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
}
</style>
