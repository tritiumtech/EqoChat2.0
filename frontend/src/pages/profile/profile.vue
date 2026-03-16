<template>
  <view class="page">
    <view class="hero">
      <view class="avatar">
        <text>{{ userInfo?.nickname?.slice(0, 1) || '?' }}</text>
      </view>
      <view class="info">
        <text class="name">{{ userInfo?.nickname || t('common.not_logged_in') }}</text>
        <text class="meta">ID: {{ userInfo?.id || '-' }}</text>
        <text class="meta">DID: {{ userInfo?.did || '-' }}</text>
      </view>
    </view>

    <view class="stats">
      <view class="stat">
        <text class="stat-value">{{ userInfo?.creditScore ?? '-' }}</text>
        <text class="stat-label">{{ t('common.credit') }}</text>
      </view>
      <view class="stat">
        <text class="stat-value">{{ userInfo?.status || '-' }}</text>
        <text class="stat-label">{{ t('common.status') }}</text>
      </view>
    </view>

    <view class="language">
      <text class="language-title">{{ t('page.profile.language') }}</text>
      <view class="language-options">
        <button
          class="lang-btn"
          :class="{ active: locale.value === 'zh-CN' }"
          @click="changeLocale('zh-CN')"
        >
          {{ t('page.profile.zh') }}
        </button>
        <button
          class="lang-btn"
          :class="{ active: locale.value === 'en-US' }"
          @click="changeLocale('en-US')"
        >
          {{ t('page.profile.en') }}
        </button>
      </view>
    </view>

    <button class="btn-logout" @click="logout">{{ t('common.logout') }}</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { userApi, type UserInfo } from '@/api/modules/user'
import { useUserStore } from '@/store/modules/user'
import { setLocale } from '@/i18n'

const userStore = useUserStore()
const userInfo = ref<UserInfo | null>(userStore.userInfo || null)
const { t, locale } = useI18n()

const fetchUserInfo = async () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  try {
    const data = await userApi.me()
    userInfo.value = data
    userStore.setUserInfo(data)
  } catch (err: any) {
    uni.showToast({ title: err?.message || '加载失败', icon: 'none' })
  }
}

const logout = () => {
  userStore.logout()
  uni.reLaunch({ url: '/pages/auth/login' })
}

const changeLocale = (value: 'zh-CN' | 'en-US') => {
  if (locale.value === value) return
  setLocale(value)
  if (userInfo.value) {
    userInfo.value = { ...userInfo.value, locale: value }
    userStore.setUserInfo(userInfo.value)
  }
}

onShow(() => {
  fetchUserInfo()
  uni.setNavigationBarTitle({ title: t('page.profile.title') })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f2ee 0%, #f2f6ff 100%);
  padding: 28rpx 24rpx 48rpx;
}

.hero {
  background: #15131c;
  border-radius: var(--radius-xl);
  padding: 28rpx;
  display: flex;
  gap: 22rpx;
  align-items: center;
  color: #fff;
  box-shadow: var(--c-shadow);
}

.avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 32rpx;
  background: linear-gradient(135deg, #ffd7c2 0%, #ffb59f 100%);
  color: #2a1c1a;
  font-size: 40rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info {
  flex: 1;
}

.name {
  font-size: 32rpx;
  font-weight: 700;
  display: block;
}

.meta {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 8rpx;
  display: block;
}

.stats {
  margin-top: 24rpx;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.stat {
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 22rpx;
  text-align: center;
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.stat-label {
  font-size: 22rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
  display: block;
}

.language {
  margin-top: 20rpx;
  background: var(--c-surface);
  border-radius: var(--radius-lg);
  padding: 22rpx;
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.language-title {
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
  margin-bottom: 16rpx;
}

.language-options {
  display: flex;
  gap: 14rpx;
}

.lang-btn {
  flex: 1;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: var(--radius-pill);
  background: #f3f0f8;
  color: var(--c-ink);
  font-size: 24rpx;
  font-weight: 600;
}

.lang-btn.active {
  background: linear-gradient(135deg, #ffb39c 0%, #ff8b6d 100%);
  color: #2a1c1a;
}

.btn-logout {
  margin-top: 28rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: var(--radius-pill);
  background: #fff1ef;
  color: #c2410c;
  font-size: 28rpx;
  font-weight: 700;
}
</style>
