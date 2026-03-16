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
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
}

.sheet {
  width: 100%;
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  background: #ffffff;
  padding: 16rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -16rpx 36rpx rgba(27, 21, 44, 0.18);
  box-sizing: border-box;
}

.sheet-header {
  padding: 12rpx 4rpx;
}

.sheet-body {
  padding: 8rpx 4rpx;
}

.sheet-footer {
  padding-top: 12rpx;
}
</style>

