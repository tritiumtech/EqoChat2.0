<template>
  <u-search
    :model-value="modelValue"
    shape="round"
    :placeholder="placeholder"
    :show-action="false"
    :clearabled="true"
    search-icon="search"
    :search-icon-color="'#717182'"
    :color="'#030213'"
    :placeholder-color="'#717182'"
    :bg-color="'rgba(255, 255, 255, 0.9)'"
    :border-color="'rgba(0, 0, 0, 0.1)'"
    height="72rpx"
    :custom-style="{ width: '100%' }"
    @change="onChange"
    @clear="$emit('update:modelValue', '')"
  />
</template>

<script setup lang="ts">
function onChange(value: any) {
  if (typeof value === 'string') {
    emit('update:modelValue', value)
    return
  }
  emit('update:modelValue', String(value ?? ''))
}

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

defineProps<{
  modelValue: string
  placeholder: string
}>()
</script>

<style scoped>
@import "@/styles/tokens.css";

/* u-search 默认按 px 体系，做一层紧凑化以匹配项目现有视觉 */
:deep(.u-search__content) {
  padding: 0 14rpx !important;
  border-radius: var(--radius-lg) !important;
  border-width: 1rpx !important;
}

:deep(.u-search__content__input) {
  margin: 0 8rpx !important;
  font-size: 26rpx !important;
}

:deep(.u-search__content__icon) {
  transform: scale(0.92);
}

:deep(.u-search__content__close) {
  transform: scale(0.9);
}
</style>

