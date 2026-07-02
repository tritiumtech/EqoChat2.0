<template>
  <view class="page">
    <view class="auth-shell">
      <view class="header">
        <view class="header-logo">
          <text class="header-logo-icon">EQ</text>
        </view>
        <view class="header-text">
          <text class="header-title">{{ t('app.name') }}</text>
          <text class="header-subtitle">{{ t('app.slogan') }}</text>
        </view>
      </view>

      <view class="panel">
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

        <view v-if="mode === 'phone'" class="field">
          <text class="label">{{ t('field.phone') }}</text>
          <view class="auth-input-wrap">
            <u-input
              v-model="phone"
              type="number"
              border="none"
              :customStyle="authInputStyle"
              :font-size="'32rpx'"
              color="#111827"
              :placeholder="t('placeholder.phone')"
              :placeholder-style="authPlaceholderStyle"
              prefix-icon="phone-fill"
              :prefix-icon-style="authIconStyle"
              clearable
              cursor-color="#111827"
            />
          </view>
        </view>

        <view v-else class="field">
          <text class="label">{{ t('field.email') }}</text>
          <view class="auth-input-wrap">
            <u-input
              v-model="email"
              type="text"
              border="none"
              :customStyle="authInputStyle"
              :font-size="'32rpx'"
              color="#111827"
              :placeholder="t('placeholder.email')"
              :placeholder-style="authPlaceholderStyle"
              prefix-icon="email-fill"
              :prefix-icon-style="authIconStyle"
              clearable
              cursor-color="#111827"
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
              color="#111827"
              :placeholder="t('placeholder.password')"
              :placeholder-style="authPlaceholderStyle"
              prefix-icon="lock-fill"
              :prefix-icon-style="authIconStyle"
              cursor-color="#111827"
            />
          </view>
        </view>

        <view class="field footer-links">
          <text class="link">{{ t('page.login.forgot_password') }}</text>
        </view>

        <button class="btn-primary" :disabled="loading" @click="handleLogin">
          {{ loading ? t('action.login_loading') : t('action.login') }}
        </button>

        <view class="divider">
          <view class="divider-line" />
          <text class="divider-text">{{ t('common.or') }}</text>
          <view class="divider-line" />
        </view>

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

        <view class="footer">
          <text class="footer-text">{{ t('page.login.switch') }}</text>
          <text class="link" @click="goToRegister">{{ t('action.register') }}</text>
        </view>
      </view>

      <view class="page-footer">
        <view class="page-footer-text">
          <text>{{ t('page.auth.agree_intro') }}</text>
          <text class="page-footer-link">{{ t('page.register.terms_of_service') }}</text>
          <text>{{ t('page.auth.agree_between') }}</text>
          <text class="page-footer-link">{{ t('page.register.privacy_policy') }}</text>
        </view>
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
import { useActiveSubjectStore } from '@/store/modules/activeSubject'

const userStore = useUserStore()
const activeSubjectStore = useActiveSubjectStore()
const { t } = useI18n({ useScope: 'global' })

const mode = ref<'phone' | 'email'>('phone')
const phone = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)

const authInputStyle = {
  width: '100%',
  minHeight: '96rpx',
  height: '96rpx',
  backgroundColor: '#ffffff',
  border: '1rpx solid rgba(17, 24, 39, 0.12)',
  borderRadius: '20rpx',
  paddingLeft: '12rpx',
  paddingRight: '12rpx',
  boxSizing: 'border-box',
}
const authPlaceholderStyle = 'color: #6b7280; font-size: 30rpx;'
const authIconStyle = { color: '#6b7280' }

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
    if (!activeSubjectStore.setHuman()) {
      throw new Error(t('toast.login_failed'))
    }
    await activeSubjectStore.ensureLoaded(true)
    uni.switchTab({ url: '/pages/chat/chat-list' })
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
@import '@/styles/tokens.css';

.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 48rpx 28rpx 60rpx;
  background: var(--c-page);
}

.auth-shell {
  width: 100%;
  max-width: var(--app-shell-max);
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 8rpx 4rpx 28rpx;
}

.header-logo {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-avatar);
  background: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-action);
}

.header-logo-icon {
  font-size: 24rpx;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0;
}

.header-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--c-ink);
  line-height: 1.2;
}

.header-subtitle {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  line-height: 1.35;
}

.panel {
  padding: 32rpx 28rpx 30rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.welcome {
  margin-bottom: 30rpx;
  text-align: left;
}

.welcome-title {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  color: var(--c-ink);
  line-height: 1.2;
}

.welcome-subtitle {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: var(--c-muted);
  line-height: 1.45;
}

.mode-switch {
  display: flex;
  gap: 10rpx;
  padding: 6rpx;
  margin-bottom: 28rpx;
  border-radius: var(--radius-control);
  background: var(--c-surface-muted);
  border: 1rpx solid var(--c-border);
}

.mode-pill {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: var(--radius-sm);
  font-size: 24rpx;
  color: var(--c-muted);
}

.mode-pill--active {
  color: var(--c-ink);
  background: #ffffff;
  font-weight: 800;
  box-shadow: var(--shadow-action);
}

.field {
  margin-bottom: 26rpx;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.auth-input-wrap {
  width: 100%;
}

.auth-input-wrap :deep(.u-input__content) {
  min-height: 78rpx;
  align-items: center;
}

.auth-input-wrap :deep(.u-input__content__field-wrapper__field) {
  height: auto !important;
  min-height: 44rpx !important;
  line-height: 44rpx !important;
  font-size: 30rpx !important;
}

.footer-links {
  margin-top: -4rpx;
  margin-bottom: 18rpx;
  display: flex;
  justify-content: flex-end;
}

.link {
  color: var(--c-primary);
  font-size: 26rpx;
  font-weight: 700;
}

.btn-primary {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  margin: 0;
  border: none;
  border-radius: var(--radius-control);
  background: var(--c-primary);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
  box-shadow: var(--shadow-action);
}

.btn-primary[disabled] {
  background: var(--c-accent);
  color: var(--c-muted);
  opacity: 1;
  box-shadow: none;
}

.divider {
  margin: 34rpx 0 22rpx;
  display: flex;
  align-items: center;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background-color: var(--c-border);
}

.divider-text {
  margin: 0 20rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.social {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.social-btn {
  width: 100%;
  height: 88rpx;
  margin: 0;
  border-radius: var(--radius-control);
  border: 1rpx solid var(--c-border);
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.social-btn:active {
  background: var(--c-surface-muted);
}

.social-icon {
  margin-right: 14rpx;
  font-size: 24rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.social-text {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.footer {
  margin-top: 28rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.footer-text {
  font-size: 26rpx;
  color: var(--c-muted);
  margin-right: 8rpx;
}

.page-footer {
  margin-top: 28rpx;
  padding: 0 8rpx;
}

.page-footer-text {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: baseline;
  row-gap: 6rpx;
  column-gap: 4rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  text-align: center;
  line-height: 1.6;
}

.page-footer-link {
  color: var(--c-primary);
  font-weight: 700;
}
</style>
