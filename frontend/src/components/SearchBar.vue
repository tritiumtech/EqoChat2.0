<template>
  <u-search
    :model-value="modelValue"
    shape="square"
    :placeholder="placeholder"
    :show-action="false"
    :clearabled="true"
    search-icon="search"
    search-icon-color="#6b7280"
    color="#111827"
    placeholder-color="#6b7280"
    bg-color="#ffffff"
    border-color="rgba(17, 24, 39, 0.12)"
    height="72rpx"
    :custom-style="{ width: '100%' }"
    @change="onChange"
    @clear="$emit('update:modelValue', '')"
  />
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

defineProps<{
  modelValue: string
  placeholder: string
}>()

function onChange(value: unknown) {
  emit('update:modelValue', typeof value === 'string' ? value : String(value ?? ''))
}
</script>

<style scoped>
@import "@/styles/tokens.css";

:deep(.u-search__content) {
  box-sizing: border-box;
  padding: 0 16rpx !important;
  border-radius: var(--radius-control) !important;
  border-width: 1rpx !important;
  box-shadow: none;
}

:deep(.u-search__content__input) {
  margin: 0 8rpx !important;
  font-size: 26rpx !important;
}

:deep(.u-search__content__icon),
:deep(.u-search__content__close) {
  transform: scale(0.92);
}
</style>
