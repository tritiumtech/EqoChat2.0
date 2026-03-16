<template>
  <view class="page">
    <view class="hero">
      <view class="hero-glow" />
      <text class="title">{{ t('page.discover.title') }}</text>
      <text class="subtitle">{{ t('page.discover.subtitle') }}</text>
    </view>

    <view class="grid">
      <view class="card primary">
        <text class="card-title">{{ t('page.discover.feature_search') }}</text>
        <text class="card-desc">{{ t('page.discover.card_search_desc') }}</text>
      </view>
      <view class="card secondary">
        <text class="card-title">{{ t('page.discover.feature_recommend') }}</text>
        <text class="card-desc">{{ t('page.discover.card_recommend_desc') }}</text>
      </view>
      <view class="card">
        <text class="card-title">{{ t('page.discover.feature_graph') }}</text>
        <text class="card-desc">{{ t('page.discover.card_graph_desc') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

onShow(() => {
  uni.setNavigationBarTitle({ title: t('page.discover.title') })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx 24rpx 48rpx;
  background: linear-gradient(180deg, #f6f2ee 0%, #f7f9ff 100%);
}

.hero {
  position: relative;
  border-radius: var(--radius-xl);
  background: #15131c;
  color: #fff;
  padding: 36rpx 28rpx;
  box-shadow: var(--c-shadow);
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 186, 130, 0.65) 0%, rgba(255, 186, 130, 0) 70%);
  right: -90rpx;
  top: -100rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  display: block;
}

.subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 12rpx;
  display: block;
}

.grid {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: 1fr;
  gap: 18rpx;
}

.card {
  border-radius: var(--radius-lg);
  padding: 28rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  box-shadow: var(--c-shadow-soft);
}

.card.primary {
  background: linear-gradient(135deg, #ffe6d8 0%, #fff7f2 100%);
  border-color: rgba(255, 122, 89, 0.2);
}

.card.secondary {
  background: linear-gradient(135deg, #e8edff 0%, #f6f8ff 100%);
  border-color: rgba(79, 107, 255, 0.2);
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.card-desc {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 10rpx;
  display: block;
}
</style>
