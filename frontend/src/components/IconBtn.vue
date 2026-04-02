<template>
  <Button
    :variant="variant"
    :size="size"
    :disabled="disabled"
    :custom-style="mergedStyle"
    :show-text="false"
    @click="handleClick"
  >
    <slot>
      <text v-if="icon" :class="['icon-glyph', iconClass]">{{ icon }}</text>
    </slot>
  </Button>
</template>

<script setup lang="ts">
import Button from './Button.vue'
import { computed } from 'vue'

type IconBtnVariant = 'default' | 'bordered' | 'primary' | 'ghost'
type IconBtnSize = 'sm' | 'md' | 'lg'

const props = defineProps<{
  icon?: string
  variant?: IconBtnVariant
  size?: IconBtnSize
  disabled?: boolean
  customStyle?: Record<string, string | number>
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

const variant = computed(() => {
  switch (props.variant) {
    case 'bordered':
      return 'secondary'
    case 'primary':
      return 'primary'
    case 'ghost':
      return 'ghost'
    default:
      return 'default'
  }
})

const size = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'small'
    case 'lg':
      return 'large'
    default:
      return 'medium'
  }
})

const iconClass = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'icon-glyph--sm'
    case 'lg':
      return 'icon-glyph--lg'
    default:
      return 'icon-glyph--md'
  }
})

// 为 primary 变体添加渐变背景
const mergedStyle = computed(() => {
  if (props.variant === 'primary') {
    return {
      ...props.customStyle,
      background: 'linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%)',
      boxShadow: '0 4rpx 12rpx rgba(3, 2, 19, 0.25)',
    }
  }
  return props.customStyle
})

const handleClick = () => {
  if (!props.disabled) {
    emit('click')
  }
}
</script>

<style scoped>
.icon-glyph--sm {
  font-size: 24rpx;
}

.icon-glyph--md {
  font-size: 32rpx;
}

.icon-glyph--lg {
  font-size: 40rpx;
}

.icon-glyph {
  /* 继承父级 Button 的颜色，确保 primary/danger 变体下显示白色 */
  color: inherit;
  font-weight: 700;
  line-height: 1;
}
</style>
