<template>
  <view class="month-group">
    <!-- Year Header (only show when year changes) -->
    <view v-if="showYearHeader" class="year-header">
      <text class="year-header-text">{{ yearLabel }}</text>
      <view class="year-header-line" />
    </view>

    <!-- Month Header -->
    <view class="month-header">
      <view class="month-badge">
        <text class="month-badge-name">{{ monthShortName }}</text>
        <text class="month-badge-day">{{ monthGroup.day }}</text>
      </view>
      <view class="month-header-line" />
    </view>

    <!-- Posts in this month -->
    <view class="month-posts">
      <MyPostCard
        v-for="post in monthGroup.posts"
        :key="post.id"
        :post="post"
        @click="$emit('post-click', post)"
        @upvote="$emit('post-upvote', post)"
        @reply="$emit('post-reply', post)"
        @share="$emit('post-share', post)"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import type { WorldPost } from '@/api/modules/world'
import MyPostCard from './MyPostCard.vue'

const { tf } = useI18nWithFormat()

interface MonthGroupData {
  year: string
  month: string
  day: number  // 添加日期字段
  posts: WorldPost[]
}

interface Props {
  monthGroup: MonthGroupData
  groupIndex: number
  allGroups: MonthGroupData[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'post-click': [post: WorldPost]
  'post-upvote': [post: WorldPost]
  'post-reply': [post: WorldPost]
  'post-share': [post: WorldPost]
}>()

const showYearHeader = computed(() => {
  return props.groupIndex === 0 || 
         props.allGroups[props.groupIndex - 1]?.year !== props.monthGroup.year
})

const yearLabel = computed(() => {
  return props.monthGroup.year
})

const monthShortName = computed(() => {
  return props.monthGroup.month.substring(0, 3).toUpperCase()
})
</script>

<style scoped>
.month-group {
  margin-bottom: 48rpx;
}

.year-header {
  margin-bottom: 32rpx;
}

.year-header-text {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.year-header-line {
  height: 2rpx;
  background: rgba(0, 0, 0, 0.06);
  margin-top: 16rpx;
}

.month-header {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 32rpx;
}

.month-badge {
  width: 128rpx;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.month-badge-name {
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-primary);
  text-transform: uppercase;
  letter-spacing: 2rpx;
}

.month-badge-day {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-ink);
  line-height: 1;
}

.month-header-line {
  flex: 1;
  height: 2rpx;
  background: rgba(0, 0, 0, 0.08);
}

.month-posts {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
</style>
