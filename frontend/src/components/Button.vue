<template>
  <view
    :class="[
      'fg-btn',
      variantClass,
      sizeClass,
      {
        'fg-btn--block': block,
        'fg-btn--loading': loading,
        'fg-btn--disabled': disabled,
        'fg-btn--circle': shape === 'circle',
        'fg-btn--round': shape === 'round'
      }
    ]"
    :style="customStyle"
    @click="handleClick"
  >
    <template v-if="loading">
      <text class="fg-btn__loading">⟳</text>
      <text v-if="showText" class="fg-btn__text fg-btn__text--loading">{{ text }}</text>
    </template>
    <template v-else>
      <slot>
        <text v-if="showText" class="fg-btn__text">{{ text }}</text>
      </slot>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ButtonVariant = 'default' | 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
type ButtonSize = 'mini' | 'small' | 'medium' | 'large'
type ButtonShape = 'square' | 'round' | 'circle'

const props = withDefaults(defineProps<{
  text?: string
  variant?: ButtonVariant
  size?: ButtonSize
  shape?: ButtonShape
  block?: boolean
  loading?: boolean
  disabled?: boolean
  customStyle?: Record<string, string | number>
  showText?: boolean
}>(), {
  variant: 'default',
  size: 'medium',
  shape: 'square',
  block: false,
  loading: false,
  disabled: false,
  showText: true
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const variantClass = computed(() => {
  switch (props.variant) {
    case 'primary':
      return 'fg-btn--primary'
    case 'secondary':
      return 'fg-btn--secondary'
    case 'outline':
      return 'fg-btn--outline'
    case 'ghost':
      return 'fg-btn--ghost'
    case 'danger':
      return 'fg-btn--danger'
    default:
      return ''
  }
})

const sizeClass = computed(() => {
  switch (props.size) {
    case 'mini':
      return 'fg-btn--mini'
    case 'small':
      return 'fg-btn--small'
    case 'large':
      return 'fg-btn--large'
    default:
      return 'fg-btn--medium'
  }
})

const handleClick = () => {
  if (!props.disabled && !props.loading) {
    emit('click')
  }
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.fg-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  box-sizing: border-box;
  border-radius: var(--radius-lg);
  font-weight: 700;
  transition: all 0.15s ease;
  cursor: pointer;
  user-select: none;
  touch-action: manipulation;
}

.fg-btn:active {
  opacity: 0.7;
  transform: scale(0.98);
}

/* 禁用状态 */
.fg-btn--disabled {
  opacity: 0.4 !important;
  cursor: not-allowed;
}

.fg-btn--disabled:active {
  opacity: 0.4;
  transform: none;
}

/* 加载中 */
.fg-btn--loading {
  opacity: 0.7;
  cursor: wait;
}

.fg-btn--loading:active {
  opacity: 0.7;
  transform: none;
}

.fg-btn__loading {
  font-size: 1.2em;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 形状 */
.fg-btn--circle {
  border-radius: 999rpx;
}

.fg-btn--round {
  border-radius: calc(var(--radius-lg) * 1.5);
}

/* 块级按钮 */
.fg-btn--block {
  display: flex;
  width: 100%;
}

/* 尺寸 - Mini */
.fg-btn--mini {
  height: 48rpx;
  padding: 0 16rpx;
  font-size: 20rpx;
}

/* 尺寸 - Small */
.fg-btn--small {
  height: 56rpx;
  padding: 0 20rpx;
  font-size: 22rpx;
}

/* 尺寸 - Medium */
.fg-btn--medium {
  height: 72rpx;
  padding: 0 28rpx;
  font-size: 26rpx;
}

/* 尺寸 - Large */
.fg-btn--large {
  height: 88rpx;
  padding: 0 36rpx;
  font-size: 30rpx;
}

/* 样式变体 - Default */
.fg-btn--default {
  background: var(--c-input-bg);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  color: var(--c-ink);
}

/* 样式变体 - Primary */
.fg-btn--primary {
  background: var(--c-primary);
  border: 1rpx solid transparent;
  color: #ffffff;
}

/* 样式变体 - Secondary */
.fg-btn--secondary {
  background: rgba(255, 255, 255, 0.9);
  border: 1rpx solid var(--c-border);
  color: var(--c-ink);
}

/* 样式变体 - Outline */
.fg-btn--outline {
  background: transparent;
  border: 1rpx solid var(--c-border);
  color: var(--c-ink);
}

/* 样式变体 - Ghost */
.fg-btn--ghost {
  background: transparent;
  border: 1rpx solid transparent;
  color: var(--c-muted);
}

.fg-btn--ghost:active {
  background: rgba(0, 0, 0, 0.04);
}

/* 样式变体 - Danger */
.fg-btn--danger {
  background: var(--c-destructive);
  border: 1rpx solid transparent;
  color: #ffffff;
}

/* 按钮文字 */
.fg-btn__text {
  line-height: 1;
}

.fg-btn__text--loading {
  margin-left: 6rpx;
}
</style>
