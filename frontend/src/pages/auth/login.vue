<template>
  <view class="page">
    <!-- Header - 对齐 React CollabAI，左侧 Logo 右侧文字一行 -->
    <view class="header">
      <view class="header-inner">
        <view class="header-logo">
          <text class="header-logo-icon">🤖</text>
        </view>
        <view class="header-text">
          <text class="header-title">{{ t('app.name') }}</text>
          <text class="header-subtitle">{{ t('app.slogan') }}</text>
        </view>
      </view>
    </view>

    <!-- Main Card -->
    <view class="panel">
      <!-- Welcome Text -->
      <view class="welcome">
        <text class="welcome-title">{{ t('page.login.title') }}</text>
        <text class="welcome-subtitle">{{ t('page.login.subtitle') }}</text>
      </view>
      <view class="mode-switch">
        <text
          class="mode-pill"
          :class="mode === 'phone' ? 'mode-pill--active' : ''"
          @click="mode = 'phone'"
        >
          {{ t('page.login.use_phone') }}
        </text>
        <text
          class="mode-pill"
          :class="mode === 'email' ? 'mode-pill--active' : ''"
          @click="mode = 'email'"
        >
          {{ t('page.login.use_email') }}
        </text>
      </view>
      <view class="field" v-if="mode === 'phone'">
        <text class="label">{{ t('field.phone') }}</text>
        <view class="auth-input-wrap">
          <u-input
            v-model="phone"
            type="number"
            border="none"
            :customStyle="authInputStyle"
            :font-size="'32rpx'"
            color="#030213"
            :placeholder="t('placeholder.phone')"
            :placeholder-style="authPlaceholderStyle"
            prefix-icon="phone-fill"
            :prefix-icon-style="authIconStyle"
            clearable
            cursor-color="#030213"
          />
        </view>
      </view>
      <view class="field" v-else>
        <text class="label">{{ t('field.email') }}</text>
        <view class="auth-input-wrap">
          <u-input
            v-model="email"
            type="text"
            border="none"
            :customStyle="authInputStyle"
            :font-size="'32rpx'"
            color="#030213"
            :placeholder="t('placeholder.email')"
            :placeholder-style="authPlaceholderStyle"
            prefix-icon="email-fill"
            :prefix-icon-style="authIconStyle"
            clearable
            cursor-color="#030213"
          />
        </view>
      </view>
      <view class="field">
        <text class="label">{{ t('field.password') }}</text>
        <view class="auth-input-wrap">
          <u-input
            v-model="password"
            type="password"
            password
            border="none"
            :customStyle="authInputStyle"
            :font-size="'32rpx'"
            color="#030213"
            :placeholder="t('placeholder.password')"
            :placeholder-style="authPlaceholderStyle"
            prefix-icon="lock-fill"
            :prefix-icon-style="authIconStyle"
            cursor-color="#030213"
          />
        </view>
      </view>
      <view class="field footer-links">
        <text class="link">{{ t('page.login.forgot_password') }}</text>
      </view>
      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        {{ loading ? t('action.login_loading') : t('action.login') }}
      </button>
      <!-- Divider -->
      <view class="divider">
        <view class="divider-line" />
        <text class="divider-text">{{ t('common.or') }}</text>
        <view class="divider-line" />
      </view>
      <!-- Social Login (占位，视觉对齐 React) -->
      <view class="social">
        <button class="social-btn">
          <text class="social-icon">G</text>
          <text class="social-text">{{ t('page.login.social_google') }}</text>
        </button>
        <button class="social-btn">
          <text class="social-icon">GH</text>
          <text class="social-text">{{ t('page.login.social_github') }}</text>
        </button>
      </view>
      <!-- Sign up link -->
      <view class="footer">
        <text class="footer-text">
          {{ t('page.login.switch') }}
        </text>
        <text class="link" @click="goToRegister">{{ t('action.register') }}</text>
      </view>
    </view>

    <!-- Global footer -->
    <view class="page-footer">
      <view class="page-footer-text">
        <text>{{ t('page.auth.agree_intro') }}</text>
        <text class="page-footer-link">{{ t('page.register.terms_of_service') }}</text>
        <text>{{ t('page.auth.agree_between') }}</text>
        <text class="page-footer-link">{{ t('page.register.privacy_policy') }}</text>
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

const mode = ref<'phone' | 'email'>('phone')
const phone = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)

/** 与 theme --input-background / border 对齐，加高便于点击 */
const authInputStyle = {
  width: '100%',
  minHeight: '100rpx',
  height: '100rpx',
  backgroundColor: '#f3f3f5',
  border: '2rpx solid rgba(0, 0, 0, 0.12)',
  borderRadius: '24rpx',
  paddingLeft: '12rpx',
  paddingRight: '12rpx',
  boxSizing: 'border-box',
}
const authPlaceholderStyle = 'color: #717182; font-size: 30rpx;'
const authIconStyle = { color: '#717182' }

const handleLogin = async () => {
  if (mode.value === 'phone') {
    if (!phone.value || !password.value) {
      uni.showToast({ title: t('toast.phone_password_required'), icon: 'none' })
      return
    }
  } else {
    if (!email.value || !password.value) {
      uni.showToast({ title: t('toast.email_password_required'), icon: 'none' })
      return
    }
    if (!email.value.includes('@')) {
      uni.showToast({ title: t('toast.email_invalid'), icon: 'none' })
      return
    }
  }

  if (password.value.length < 6) {
    uni.showToast({ title: t('toast.password_too_short'), icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data =
      mode.value === 'phone'
        ? await authApi.login({ phone: phone.value, password: password.value })
        : await authApi.loginByEmail({ email: email.value, password: password.value })
    userStore.setToken(data.token)
    userStore.setSessionId(data.sessionId)
    userStore.setUserInfo(data.userInfo)
    uni.switchTab({ url: '/pages/chat/chat-list' })
  } catch (err: any) {
	  console.log('login_erroree',err)
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
@import '@/styles/tokens.css';

/* 对齐 Chatinterfacedesign theme：background / primary / muted / border */
.page {
  min-height: 100vh;
  padding: 48rpx 32rpx 60rpx;
  background: #ffffff;
  box-sizing: border-box;
}

.header {
  padding: 0 0 8rpx;
}

.header-inner {
  flex-direction: row;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.header-logo {
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--c-primary) 0%, rgba(3, 2, 19, 0.82) 100%);
  align-items: center;
  justify-content: center;
  display: flex;
  box-shadow: 0 16rpx 40rpx rgba(3, 2, 19, 0.18);
}

.header-logo-icon {
  font-size: 32rpx;
  font-weight: 700;
  color: #ffffff;
}

.header-text {
  display: grid;
}

.header-title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.header-subtitle {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

/* React 无独立卡片，表单直接铺在 background 上 */
.panel {
  margin-top: 32rpx;
  background: transparent;
  border-radius: 0;
  padding: 0 0;
  box-shadow: none;
  border: none;
}

.welcome {
  margin-bottom: 40rpx;
  text-align: center;
}

.welcome-title {
  font-size: 48rpx;
  font-weight: 700;
  color: var(--c-ink);
  line-height: 1.2;
}

.welcome-subtitle {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: var(--c-muted);
  line-height: 1.45;
}

.mode-switch {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.mode-pill {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: var(--radius-lg);
  font-size: 24rpx;
  color: var(--c-muted);
  background: rgba(236, 236, 240, 0.9);
}

.mode-pill--active {
  color: #ffffff;
  background: var(--c-primary);
  font-weight: 600;
}

.field {
  margin-bottom: 32rpx;
}

.label {
  font-size: 26rpx;
  font-weight: 500;
  color: var(--c-ink);
  margin-bottom: 16rpx;
  display: block;
}

.auth-input-wrap {
  width: 100%;
}

/* 拉高 u-input 内原生 input，避免组件默认 24px 过矮 */
.auth-input-wrap :deep(.u-input__content) {
  min-height: 80rpx;
  align-items: center;
}

.auth-input-wrap :deep(.u-input__content__field-wrapper__field) {
  height: auto !important;
  min-height: 44rpx !important;
  line-height: 44rpx !important;
  font-size: 32rpx !important;
}

.btn-primary {
  margin-top: 8rpx;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--c-primary) 0%, rgba(3, 2, 19, 0.82) 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.btn-primary[disabled] {
  background: #ececf0;
  color: var(--c-muted);
  opacity: 1;
  box-shadow: none;
}

.footer {
  margin-top: 32rpx;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.footer-links {
  margin-top: 8rpx;
  margin-bottom: 4rpx;
  flex-direction: row;
  justify-content: flex-end;
}

.link {
  color: var(--c-primary);
  font-size: 26rpx;
  font-weight: 500;
}

.link.muted {
  font-size: 24rpx;
  color: var(--c-muted);
}

.footer-text {
  font-size: 26rpx;
  color: var(--c-muted);
  margin-right: 8rpx;
}

.divider {
  margin: 40rpx 0 24rpx;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background-color: rgba(0, 0, 0, 0.06);
}

.divider-text {
  margin: 0 24rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.social {
  margin-bottom: 12rpx;
  row-gap: 24rpx;
}

.social-btn {
  width: 100%;
  height: 96rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  background-color: #ffffff;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}

.social-btn:active {
  background: rgba(236, 236, 240, 0.65);
}

.social-icon {
  margin-right: 16rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.social-text {
  font-size: 28rpx;
  font-weight: 500;
  color: var(--c-ink);
}

.page-footer {
  margin-top: 40rpx;
  padding: 0 8rpx;
  align-items: center;
}

.page-footer-text {
  font-size: 22rpx;
  color: var(--c-muted);
  text-align: center;
  line-height: 1.6;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: baseline;
  row-gap: 6rpx;
  column-gap: 4rpx;
}

.page-footer-link {
  color: var(--c-primary);
}
</style>
