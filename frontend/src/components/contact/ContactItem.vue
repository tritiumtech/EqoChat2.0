<template>
  <view class="item" @click="$emit('click')">
    <view class="avatar">
      <text>{{ displayInitial }}</text>
    </view>
    <view class="content">
      <text class="name">{{ name }}</text>
      <text class="meta">ID: {{ id }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  id: number
  name?: string
}>()

defineEmits<{ (e: 'click'): void }>()

const displayInitial = computed(() => (props.name || '?').slice(0, 1))
</script>

<style scoped>
@import "@/styles/tokens.css";

.item {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 22rpx;
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #065f46;
  font-size: 30rpx;
  font-weight: 700;
  margin-right: 18rpx;
}

.content {
  flex: 1;
}

.name {
  font-size: 30rpx;
  color: var(--c-ink);
  font-weight: 700;
  display: block;
}

.meta {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
  display: block;
}
</style>
