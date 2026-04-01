<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { t } from '@/utils/i18n'
import { needHideNativeTabbar } from '@/tabbar/config'
import { useUserStore } from '@/store/modules/user'
import { useChatStore } from '@/store/modules/chat'

const userStore = useUserStore()
const chatStore = useChatStore()

const parseUserIdFromToken = (token?: string | null): number | null => {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4))
    const payloadStr = atob(base64 + pad)
    const payload = JSON.parse(payloadStr)
    const raw = payload.userId ?? payload.sub ?? payload.id ?? payload.uid
    if (raw == null) return null
    const n = Number(raw)
    return Number.isNaN(n) ? null : n
  } catch {
    return null
  }
}

const ensureRealtime = () => {
  // 如果被踢下线，不再尝试连接
  if (chatStore.isSessionKicked) {
    return
  }
  if (!userStore.token) {
    chatStore.stopRealtime()
    return
  }
  const userId = parseUserIdFromToken(userStore.token)
  chatStore.setCurrentUserId(userId)
  chatStore.startRealtime(userStore.token)
}

/**
 * 处理被挤下线或会话过期
 */
const handleSessionExpired = (message?: string) => {
  chatStore.stopRealtime()
  userStore.logout()
  uni.showModal({
    title: t('common.notice', '登录提示'),
    content: message || t('auth.session_expired', '登录已过期，请重新登录'),
    showCancel: false,
    confirmText: t('auth.relogin', '重新登录'),
    success: () => {
      uni.reLaunch({ url: '/pages/auth/login' })
    },
  })
}

/**
 * 拦截 401 响应
 */
const setupUnauthorizedInterceptor = () => {
  // 保存原始的 uni.request
  const originalRequest = uni.request
  // @ts-ignore
  uni.request = (options: UniApp.RequestOptions) => {
    const originalSuccess = options.success
    const originalFail = options.fail

    options.success = (res: any) => {
      // 检测到 401 未授权，可能是被挤下线或 token 过期
      if (res.statusCode === 401) {
        handleSessionExpired('您的登录已失效，请重新登录')
        return
      }
      originalSuccess?.(res)
    }

    options.fail = (err: any) => {
      originalFail?.(err)
    }

    return originalRequest(options)
  }
}

onLaunch(() => {
  console.log('EqoChat App Launch')
  userStore.syncFromStorage()
  setupUnauthorizedInterceptor()
  ensureRealtime()
  // #ifndef MP-WEIXIN || MP-ALIPAY
  needHideNativeTabbar &&
    uni.hideTabBar({
      animation: false,
      fail() {},
    })
  // #endif
})

onShow(() => {
  userStore.syncFromStorage()
  ensureRealtime()
})
</script>

<style lang="scss">
@import "@/styles/tokens.css";

page {
  background: var(--c-bg);
  color: var(--c-ink);
  font-family: var(--font-display);
}
</style>
