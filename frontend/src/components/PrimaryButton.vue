<template>
  <button
    class="btn btn-primary"
    :disabled="disabled"
    @click="handleClick"
  >
    <view v-if="loading" class="spinner" />
    <text class="label">
      <slot />
    </text>
  </button>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: 'click'): void
}>()

const props = defineProps<{
  disabled?: boolean
  loading?: boolean
}>()

const handleClick = () => {
  if (props.disabled || props.loading) return
  emit('click')
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.btn {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 32rpx;
  border-radius: var(--radius-pill);
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 26rpx;
  font-weight: 700;
}

.btn-primary {
  background: linear-gradient(135deg, var(--c-primary) 0%, #ff9f6b 100%);
  color: #1a1720;
}

.btn[disabled] {
  opacity: 0.6;
}

.spinner {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  border-top-color: #ffffff;
  animation: spin 0.9s linear infinite;
}

.label {
  display: block;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>

