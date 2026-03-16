<template>
  <view class="page">
    <view class="hero">
      <view class="orb orb-1" />
      <view class="orb orb-2" />
      <view class="hero-content">
        <text class="eyebrow">EqoChat</text>
        <text class="title">{{ t('app.name') }}</text>
        <text class="subtitle">{{ t('app.slogan') }}</text>
        <button class="btn-primary" @click="goToLogin">{{ t('common.start') }}</button>
      </view>
    </view>

    <view class="feature-grid">
      <view class="feature-card accent">
        <text class="feature-title">{{ t('page.index.feature_agent') }}</text>
        <text class="feature-desc">{{ t('page.index.feature_agent_desc') }}</text>
      </view>
      <view class="feature-card">
        <text class="feature-title">{{ t('page.index.feature_chat') }}</text>
        <text class="feature-desc">{{ t('page.index.feature_chat_desc') }}</text>
      </view>
      <view class="feature-card soft">
        <text class="feature-title">{{ t('page.index.feature_open') }}</text>
        <text class="feature-desc">{{ t('page.index.feature_open_desc') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const { t } = useI18n()

const goToLogin = () => {
  if (userStore.isLoggedIn) {
    uni.switchTab({ url: '/pages/chat/chat-list' })
    return
  }
  uni.navigateTo({ url: '/pages/auth/login' })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('app.name') })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 32rpx 24rpx 48rpx;
  background: linear-gradient(180deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  padding: 48rpx 32rpx;
  background: #1a1720;
  color: #fff;
  box-shadow: var(--c-shadow);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(0);
  opacity: 0.85;
}

.orb-1 {
  width: 280rpx;
  height: 280rpx;
  background: radial-gradient(circle, #ffb08a 0%, rgba(255, 122, 89, 0) 70%);
  top: -80rpx;
  left: -60rpx;
}

.orb-2 {
  width: 240rpx;
  height: 240rpx;
  background: radial-gradient(circle, #7da0ff 0%, rgba(79, 107, 255, 0) 70%);
  bottom: -80rpx;
  right: -60rpx;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.eyebrow {
  font-size: 22rpx;
  text-transform: uppercase;
  letter-spacing: 6rpx;
  color: rgba(255, 255, 255, 0.65);
  display: block;
  margin-bottom: 18rpx;
}

.title {
  font-size: 56rpx;
  font-weight: 700;
  display: block;
}

.subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 18rpx;
  line-height: 1.6;
  display: block;
}

.btn-primary {
  margin-top: 28rpx;
  height: 84rpx;
  line-height: 84rpx;
  padding: 0 36rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #ff7a59 0%, #ff9f6b 100%);
  color: #1a1720;
  font-weight: 700;
  font-size: 28rpx;
  border: none;
}

.feature-grid {
  margin-top: 32rpx;
  display: grid;
  grid-template-columns: 1fr;
  gap: 20rpx;
}

.feature-card {
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 28rpx;
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.feature-card.accent {
  background: linear-gradient(135deg, #ffe5da 0%, #fff6f2 100%);
  border-color: rgba(255, 122, 89, 0.2);
}

.feature-card.soft {
  background: linear-gradient(135deg, #eef2ff 0%, #f6f7ff 100%);
  border-color: rgba(79, 107, 255, 0.2);
}

.feature-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.feature-desc {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 12rpx;
  display: block;
}
</style>
