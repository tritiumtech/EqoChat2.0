<template>
  <button
    class="btn btn-ghost"
    :disabled="disabled"
    @click="handleClick"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: 'click'): void
}>()

const props = defineProps<{
  disabled?: boolean
}>()

const handleClick = () => {
  if (props.disabled) return
  emit('click')
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.btn {
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 24rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 24rpx;
}

.btn[disabled] {
  opacity: 0.5;
}
</style>

