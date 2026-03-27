<template>
  <view class="page">
    <view class="hero">
      <view class="hero-glow" />
      <text class="brand">EqoChat</text>
      <text class="title">{{ t('page.register.title') }}</text>
      <text class="subtitle">{{ t('page.register.subtitle') }}</text>
    </view>

    <view class="panel">
      <view class="field">
        <text class="label">{{ t('field.phone') }}</text>
        <input v-model="phone" class="input" type="number" :placeholder="t('placeholder.phone')" />
      </view>
      <view class="field code-field">
        <view class="code-input">
          <text class="label">{{ t('field.verify_code') }}</text>
          <input v-model="verifyCode" class="input" type="number" :placeholder="t('placeholder.verify_code')" />
        </view>
        <button class="btn-code" :disabled="countdown > 0" @click="handleSendCode">
          {{ countdown > 0 ? `${countdown}s` : t('action.send_code') }}
        </button>
      </view>
      <view class="field">
        <text class="label">{{ t('field.nickname') }}</text>
        <input v-model="nickname" class="input" :placeholder="t('placeholder.nickname')" />
      </view>
      <view class="field">
        <text class="label">{{ t('field.password') }}</text>
        <input v-model="password" class="input" password :placeholder="t('placeholder.password')" />
      </view>
      <button class="btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? t('action.register_loading') : t('action.register') }}
      </button>
      <view class="footer">
        <text class="link" @click="goToLogin">{{ t('page.register.switch') }}</text>
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
const { t } = useI18n()

const phone = ref('')
const verifyCode = ref('')
const nickname = ref('')
const password = ref('')
const loading = ref(false)
const countdown = ref(0)

let timer: number | null = null

const handleSendCode = async () => {
  if (!phone.value) {
    uni.showToast({ title: t('placeholder.phone'), icon: 'none' })
    return
  }
  try {
    await authApi.sendVerifyCode(phone.value)
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
  if (!phone.value || !verifyCode.value || !nickname.value || !password.value) {
    uni.showToast({ title: t('toast.fill_required'), icon: 'none' })
    return
  }

  loading.value = true
  try {
    const data = await authApi.register({
      phone: phone.value,
      verifyCode: verifyCode.value,
      password: password.value,
      nickname: nickname.value
    })
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    uni.redirectTo({ url: '/pages/chat/chat-list' })
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
.page {
  min-height: 100vh;
  padding: 40rpx 24rpx 60rpx;
  background: linear-gradient(180deg, #f5f1ee 0%, #eef3ff 100%);
}

.hero {
  position: relative;
  padding: 36rpx 32rpx 24rpx;
  border-radius: var(--radius-xl);
  background: #141222;
  color: #fff;
  overflow: hidden;
  box-shadow: var(--c-shadow);
}

.hero-glow {
  position: absolute;
  width: 300rpx;
  height: 300rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 107, 255, 0.6) 0%, rgba(79, 107, 255, 0) 70%);
  left: -60rpx;
  bottom: -90rpx;
}

.brand {
  font-size: 24rpx;
  letter-spacing: 6rpx;
  color: rgba(255, 255, 255, 0.6);
  text-transform: uppercase;
  display: block;
}

.title {
  font-size: 42rpx;
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

.code-field {
  display: flex;
  gap: 16rpx;
  align-items: flex-end;
}

.code-input {
  flex: 1;
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

.btn-code {
  height: 76rpx;
  line-height: 76rpx;
  border-radius: var(--radius-pill);
  background: #f1ecff;
  color: #5848c2;
  font-size: 24rpx;
  padding: 0 22rpx;
}

.btn-primary {
  margin-top: 4rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--c-secondary) 0%, #89a1ff 100%);
  color: #0f1220;
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
