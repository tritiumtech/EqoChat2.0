<template>
  <view class="timeline-index-panel">
    <view class="timeline-index-list">
      <template v-for="(yearData, year) in structure" :key="year">
        <view class="timeline-index-year">
          <view class="timeline-index-item" @click="handleToggleYear(year)">
            <u-icon 
              name="arrow-right" 
              :size="20" 
              :color="expandedYears.has(year) ? 'var(--c-primary)' : 'var(--c-muted)'"
              :class="['timeline-index-chevron', { expanded: expandedYears.has(year) }]"
            />
            <text class="timeline-index-label">{{ tf('page.world.timeline_year', { year }) }}</text>
            <text class="timeline-index-meta">{{ tf('page.world.timeline_months', { n: Object.keys(yearData).length }) }}</text>
          </view>
          <view v-if="expandedYears.has(year)" class="timeline-index-months">
            <template v-for="(days, month) in yearData" :key="month">
              <view class="timeline-index-month">
                <view class="timeline-index-item timeline-index-month-item" @click="handleToggleMonth(`${year}-${month}`)">
                  <u-icon 
                    name="arrow-right" 
                    :size="20" 
                    :color="expandedMonths.has(`${year}-${month}`) ? 'var(--c-primary)' : 'var(--c-muted)'"
                    :class="['timeline-index-chevron', { expanded: expandedMonths.has(`${year}-${month}`) }]"
                  />
                  <text class="timeline-index-label-month">{{ month }}</text>
                  <text class="timeline-index-meta">{{ tf('page.world.timeline_days', { n: days.length }) }}</text>
                </view>
                <view v-if="expandedMonths.has(`${year}-${month}`)" class="timeline-index-days">
                  <view
                    v-for="day in days"
                    :key="day"
                    class="timeline-index-day"
                    @click="handleDayClick"
                  >
                    <text>{{ day }}</text>
                  </view>
                </view>
              </view>
            </template>
          </view>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'

const { tf } = useI18nWithFormat()

interface Props {
  structure: { [year: string]: { [month: string]: string[] } }
  expandedYears: Set<string>
  expandedMonths: Set<string>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'toggle-year': [year: string]
  'toggle-month': [monthKey: string]
  'close': []
}>()

function handleToggleYear(year: string) {
  emit('toggle-year', year)
}

function handleToggleMonth(monthKey: string) {
  emit('toggle-month', monthKey)
}

function handleDayClick() {
  emit('close')
}
</script>

<style scoped>
.timeline-index-panel {
  margin: 0 24rpx 24rpx;
  background: var(--c-surface);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.timeline-index-list {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.timeline-index-year {
  display: flex;
  flex-direction: column;
}

.timeline-index-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  border-radius: 16rpx;
  transition: background 0.2s;
}

.timeline-index-item:active {
  background: rgba(0, 0, 0, 0.05);
}

.timeline-index-chevron {
  margin-right: 16rpx;
  transition: transform 0.2s;
}

.timeline-index-chevron.expanded {
  transform: rotate(90deg);
}

.timeline-index-label {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.timeline-index-label-month {
  flex: 1;
  font-size: 24rpx;
  font-weight: 500;
  color: var(--c-ink);
}

.timeline-index-meta {
  font-size: 22rpx;
  color: var(--c-muted);
}

.timeline-index-months {
  margin-left: 40rpx;
  margin-top: 8rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.timeline-index-month-item {
  padding: 12rpx 20rpx;
}

.timeline-index-days {
  margin-left: 40rpx;
  margin-top: 8rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.timeline-index-day {
  padding: 8rpx 16rpx;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12rpx;
}

.timeline-index-day text {
  font-size: 22rpx;
  font-weight: 500;
  color: var(--c-ink);
}
</style>
