<template>
  <view class="page">
    <view class="hero">
      <view class="hero-glow" />
      <text class="brand">EqoChat</text>
      <text class="title">{{ t('page.login.title') }}</text>
      <text class="subtitle">{{ t('page.login.subtitle') }}</text>
    </view>

    <view class="panel">
      <view class="field">
        <text class="label">{{ t('field.phone') }}</text>
        <input v-model="phone" class="input" type="number" :placeholder="t('placeholder.phone')" />
      </view>
      <view class="field">
        <text class="label">{{ t('field.password') }}</text>
        <input v-model="password" class="input" password :placeholder="t('placeholder.password')" />
      </view>
      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        {{ loading ? t('action.login_loading') : t('action.login') }}
      </button>
      <view class="footer">
        <text class="link" @click="goToRegister">{{ t('page.login.switch') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { authApi } from '@/api/modules/auth'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const { t } = useI18n({ useScope: 'global' })

const phone = ref('')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
  if (!phone.value || !password.value) {
    uni.showToast({ title: t('toast.phone_password_required'), icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data = await authApi.login({ phone: phone.value, password: password.value })
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    uni.redirectTo({ url: '/pages/chat/chat-list' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.login_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/auth/register' })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('page.login.nav') })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 40rpx 24rpx 60rpx;
  background: linear-gradient(180deg, var(--c-bg) 0%, #fff8f3 100%);
}

.hero {
  position: relative;
  padding: 40rpx 32rpx 24rpx;
  border-radius: var(--radius-xl);
  background: #111018;
  color: #fff;
  overflow: hidden;
  box-shadow: var(--c-shadow);
}

.hero-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 122, 89, 0.7) 0%, rgba(255, 122, 89, 0) 70%);
  right: -80rpx;
  top: -60rpx;
}

.brand {
  font-size: 24rpx;
  letter-spacing: 6rpx;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  display: block;
}

.title {
  font-size: 44rpx;
  font-weight: 700;
  margin-top: 12rpx;
  display: block;
}

.subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.72);
  margin-top: 10rpx;
  display: block;
}

.panel {
  margin-top: 24rpx;
  background: var(--c-surface);
  border-radius: var(--radius-xl);
  padding: 32rpx;
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.field {
  margin-bottom: 24rpx;
}

.label {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-bottom: 12rpx;
  display: block;
}

.input {
  background: #f2f2f6;
  border-radius: 18rpx;
  padding: 22rpx 20rpx;
  font-size: 30rpx;
  color: var(--c-ink);
}

.btn-primary {
  margin-top: 8rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--c-primary) 0%, #ff9f6b 100%);
  color: #1a1720;
  font-size: 30rpx;
  font-weight: 700;
}

.btn-primary[disabled] {
  opacity: 0.6;
}

.footer {
  margin-top: 24rpx;
  text-align: center;
}

.link {
  color: var(--c-secondary);
  font-size: 26rpx;
}
</style>
