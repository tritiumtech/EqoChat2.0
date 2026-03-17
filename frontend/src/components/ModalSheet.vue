<template>
  <view v-if="visible" class="sheet-backdrop" @click="handleBackdrop">
    <view class="sheet" @click.stop>
      <view v-if="$slots.header" class="sheet-header">
        <slot name="header" />
      </view>
      <view class="sheet-body">
        <slot />
      </view>
      <view v-if="$slots.footer" class="sheet-footer">
        <slot name="footer" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: 'close'): void
}>()

const props = defineProps<{
  visible: boolean
  closeOnBackdrop?: boolean
}>()

const handleBackdrop = () => {
  if (props.closeOnBackdrop === false) return
  emit('close')
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.sheet-backdrop {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(21, 19, 28, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sheet {
  width: 100%;
  max-height: 85vh;
  border-top-left-radius: 40rpx;
  border-top-right-radius: 40rpx;
  background: #ffffff;
  padding: 24rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -24rpx 48rpx rgba(27, 21, 44, 0.22);
  box-sizing: border-box;
  animation: slideUp 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-header {
  padding: 8rpx 0;
}

.sheet-body {
  padding: 12rpx 0;
}

.sheet-footer {
  padding-top: 16rpx;
}
</style>

