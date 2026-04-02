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
        <text class="welcome-title">{{ t('page.register.title') }}</text>
        <text class="welcome-subtitle">{{ t('page.register.subtitle') }}</text>
      </view>
      <view class="mode-switch">
        <text
          class="mode-pill"
          :class="mode === 'phone' ? 'mode-pill--active' : ''"
          @click="mode = 'phone'"
        >
          {{ t('page.register.use_phone') }}
        </text>
        <text
          class="mode-pill"
          :class="mode === 'email' ? 'mode-pill--active' : ''"
          @click="mode = 'email'"
        >
          {{ t('page.register.use_email') }}
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

      <view class="field code-block">
        <text class="label">{{ t('field.verify_code') }}</text>
        <view class="code-field">
          <view class="code-input">
            <view class="auth-input-wrap">
              <u-input
                v-model="verifyCode"
                type="number"
                border="none"
                :customStyle="authInputStyle"
                :font-size="'32rpx'"
                color="#030213"
                :placeholder="t('placeholder.verify_code')"
                :placeholder-style="authPlaceholderStyle"
                cursor-color="#030213"
              />
            </view>
          </view>
          <button class="btn-code" :disabled="countdown > 0" @click="handleSendCode">
            {{ countdown > 0 ? t('common.seconds', { n: countdown }) : t('action.send_code') }}
          </button>
        </view>
      </view>
      <view class="field">
        <text class="label">{{ t('field.nickname') }}</text>
        <view class="auth-input-wrap">
          <u-input
            v-model="nickname"
            type="text"
            border="none"
            :customStyle="authInputStyle"
            :font-size="'32rpx'"
            color="#030213"
            :placeholder="t('placeholder.nickname')"
            :placeholder-style="authPlaceholderStyle"
            prefix-icon="account-fill"
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
      <view class="field">
        <text class="label">{{ t('page.register.confirm_password') }}</text>
        <view class="auth-input-wrap">
          <u-input
            v-model="confirmPassword"
            type="password"
            password
            border="none"
            :customStyle="authInputStyle"
            :font-size="'32rpx'"
            color="#030213"
            :placeholder="t('page.register.confirm_password_placeholder')"
            :placeholder-style="authPlaceholderStyle"
            prefix-icon="lock-fill"
            :prefix-icon-style="authIconStyle"
            cursor-color="#030213"
          />
        </view>
      </view>
      <view class="field terms-field">
        <checkbox :checked="acceptTerms" @click="acceptTerms = !acceptTerms" />
        <text class="terms-text">
          {{ t('page.register.terms_prefix') }}
          <text class="terms-link">{{ t('page.register.terms_of_service') }}</text>
          {{ t('page.register.and') }}
          <text class="terms-link">{{ t('page.register.privacy_policy') }}</text>
        </text>
      </view>
      <button class="btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? t('action.register_loading') : t('action.register') }}
      </button>

      <!-- Divider -->
      <view class="divider">
        <view class="divider-line" />
        <text class="divider-text">{{ t('common.or') }}</text>
        <view class="divider-line" />
      </view>

      <!-- Social Register (占位，视觉对齐 React Register) -->
      <view class="social">
        <button class="social-btn">
          <text class="social-icon">G</text>
          <text class="social-text">{{ t('page.register.social_google') }}</text>
        </button>
        <button class="social-btn">
          <text class="social-icon">GH</text>
          <text class="social-text">{{ t('page.register.social_github') }}</text>
        </button>
      </view>

      <!-- Login link -->
      <view class="footer">
        <text class="footer-text">
          {{ t('page.register.switch') }}
        </text>
        <text class="link" @click="goToLogin">{{ t('action.login') }}</text>
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
import { ref, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { authApi } from '@/api/modules/auth'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const { t } = useI18n({ useScope: 'global' })

const authInputStyle = {
  width: '100%',
  minHeight: '100rpx',
  height: '100rpx',
  backgroundColor: 'rgba(255, 255, 255, 0.9)',
  border: '2rpx solid rgba(0, 0, 0, 0.12)',
  borderRadius: '24rpx',
  paddingLeft: '12rpx',
  paddingRight: '12rpx',
  boxSizing: 'border-box',
}
const authPlaceholderStyle = 'color: #717182; font-size: 30rpx;'
const authIconStyle = { color: '#717182' }

const mode = ref<'phone' | 'email'>('phone')
const phone = ref('')
const email = ref('')
const verifyCode = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const countdown = ref(0)
const acceptTerms = ref(false)

let timer: number | null = null

const handleSendCode = async () => {
  if (mode.value === 'phone') {
    if (!phone.value) {
      uni.showToast({ title: t('placeholder.phone'), icon: 'none' })
      return
    }
  } else {
    if (!email.value) {
      uni.showToast({ title: t('placeholder.email'), icon: 'none' })
      return
    }
    if (!email.value.includes('@')) {
      uni.showToast({ title: t('toast.email_invalid'), icon: 'none' })
      return
    }
  }
  try {
    if (mode.value === 'phone') {
      await authApi.sendVerifyCode(phone.value)
    } else {
      await authApi.sendEmailVerifyCode(email.value)
    }
    uni.showToast({ title: t('toast.send_code_success'), icon: 'success' })
    startCountdown()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.send_failed'), icon: 'none' })
  }
}

const startCountdown = () => {
  countdown.value = 60
  if (timer) {
    clearInterval(timer)
  }
  timer = setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      if (timer) {
        clearInterval(timer)
        timer = null
      }
      return
    }
    countdown.value -= 1
  }, 1000) as unknown as number
}

const handleRegister = async () => {
  const hasBaseFields =
    !!verifyCode.value && !!nickname.value && !!password.value && !!confirmPassword.value

  if (mode.value === 'phone') {
    if (!phone.value || !hasBaseFields) {
      uni.showToast({ title: t('toast.fill_required'), icon: 'none' })
      return
    }
  } else {
    if (!email.value || !hasBaseFields) {
      uni.showToast({ title: t('toast.fill_required'), icon: 'none' })
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

  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: t('toast.password_mismatch'), icon: 'none' })
    return
  }

  if (!acceptTerms.value) {
    uni.showToast({ title: t('toast.accept_terms_required'), icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data =
      mode.value === 'phone'
        ? await authApi.register({
            phone: phone.value,
            verifyCode: verifyCode.value,
            password: password.value,
            nickname: nickname.value
          })
        : await authApi.registerByEmail({
            email: email.value,
            verifyCode: verifyCode.value,
            password: password.value,
            nickname: nickname.value
          })
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    uni.switchTab({ url: '/pages/chat/chat-list' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.register_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  uni.navigateTo({ url: '/pages/auth/login' })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('page.register.nav') })
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
@import '@/styles/tokens.css';

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
  align-items: center;
  display: flex;
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

.terms-field {
  flex-direction: row;
  align-items: flex-start;
  gap: 16rpx;
}

.terms-text {
  flex: 1;
  font-size: 24rpx;
  color: var(--c-muted);
  line-height: 1.55;
}

.terms-link {
  color: var(--c-primary);
  font-weight: 500;
}

.code-field {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.code-input {
  flex: 1;
  min-width: 0;
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

.btn-code {
  height: 100rpx;
  line-height: 100rpx;
  border-radius: var(--radius-lg);
  background: #ffffff;
  color: var(--c-primary);
  font-size: 26rpx;
  font-weight: 600;
  padding: 0 28rpx;
  border: 2rpx solid rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
  margin: 0;
}

.btn-code[disabled] {
  background: #ececf0;
  color: var(--c-muted);
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

.link {
  color: var(--c-primary);
  font-size: 26rpx;
  font-weight: 500;
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
