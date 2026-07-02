<template>
  <Button
    :variant="buttonVariant"
    :size="buttonSize"
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
import { computed } from 'vue'
import Button from './Button.vue'

type IconBtnVariant = 'default' | 'bordered' | 'primary' | 'ghost'
type IconBtnSize = 'sm' | 'md' | 'lg'

const props = withDefaults(defineProps<{
  icon?: string
  variant?: IconBtnVariant
  size?: IconBtnSize
  disabled?: boolean
  customStyle?: Record<string, string | number>
}>(), {
  icon: '',
  variant: 'default',
  size: 'md',
  disabled: false
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const buttonVariant = computed(() => {
  switch (props.variant) {
    case 'bordered':
      return 'secondary'
    case 'primary':
      return 'primary'
    case 'ghost':
      return 'ghost'
    default:
      return 'secondary'
  }
})

const buttonSize = computed(() => {
  switch (props.size) {
    case 'sm':
      return 'small'
    case 'lg':
      return 'large'
    default:
      return 'medium'
  }
})

const sizeStyle = computed(() => {
  switch (props.size) {
    case 'sm':
      return { width: '56rpx', height: '56rpx', padding: '0' }
    case 'lg':
      return { width: '88rpx', height: '88rpx', padding: '0' }
    default:
      return { width: '72rpx', height: '72rpx', padding: '0' }
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

const mergedStyle = computed(() => {
  const base = {
    ...sizeStyle.value,
    borderRadius: 'var(--radius-action-icon)',
    boxShadow: props.variant === 'primary' ? 'var(--shadow-action)' : 'none',
  }
  return {
    ...base,
    ...props.customStyle,
  }
})

const handleClick = () => {
  if (!props.disabled) {
    emit('click')
  }
}
</script>

<style scoped>
.icon-glyph {
  color: inherit;
  font-weight: 800;
  line-height: 1;
}

.icon-glyph--sm {
  font-size: 24rpx;
}

.icon-glyph--md {
  font-size: 30rpx;
}

.icon-glyph--lg {
  font-size: 36rpx;
}
</style>
