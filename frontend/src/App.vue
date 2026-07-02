<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { t } from '@/utils/i18n'
import { needHideNativeTabbar } from '@/tabbar/config'
import { useUserStore } from '@/store/modules/user'
import { useChatStore } from '@/store/modules/chat'
import { useFriendRequestStore } from '@/store/modules/friendRequest'
import { useNotificationStore } from '@/store/modules/notification'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import { wsClient } from '@/utils/websocket'
import { conversationApi } from '@/api/modules/conversation'

const userStore = useUserStore()
const chatStore = useChatStore()
const friendRequestStore = useFriendRequestStore()
const notificationStore = useNotificationStore()
const activeSubjectStore = useActiveSubjectStore()

const parsePrincipalHumanIdFromToken = (token?: string | null): number | null => {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4))
    const payloadStr = atob(base64 + pad)
    const payload = JSON.parse(payloadStr)
    const raw = payload.principalHumanId ?? payload.sub
    if (raw == null) return null
    const n = Number(raw)
    return Number.isNaN(n) ? null : n
  } catch {
    return null
  }
}

const ensureRealtime = async () => {
  if (chatStore.isSessionKicked) {
    return
  }
  if (!userStore.token) {
    chatStore.stopRealtime()
    notificationStore.stopRealtime()
    return
  }

  const principalHumanId = parsePrincipalHumanIdFromToken(userStore.token)
  chatStore.setCurrentPrincipalHumanId(principalHumanId)
  await activeSubjectStore.ensureLoaded()

  const subject = activeSubjectStore.currentSubject
  if (!subject) {
    chatStore.setCurrentActiveSubject(null)
    wsClient.setActiveSubject(null)
    chatStore.stopRealtime()
    notificationStore.stopRealtime()
    return
  }

  chatStore.setCurrentActiveSubject(subject)
  wsClient.setActiveSubject({ subjectId: subject.subjectId, subjectType: subject.subjectType })
  chatStore.startRealtime(userStore.token)
  notificationStore.startRealtime()
}

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

const setupUnauthorizedInterceptor = () => {
  const originalRequest = uni.request
  // @ts-ignore
  uni.request = (options: UniApp.RequestOptions) => {
    const originalSuccess = options.success
    const originalFail = options.fail

    options.success = (res: any) => {
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
  void ensureRealtime()
  // #ifndef MP-WEIXIN || MP-ALIPAY
  needHideNativeTabbar &&
    uni.hideTabBar({
      animation: false,
      fail() {},
    })
  // #endif

  if (userStore.token) {
    void preloadBadgeData()
  }
})

const preloadBadgeData = async () => {
  try {
    await activeSubjectStore.ensureLoaded()
  } catch {
    return
  }
  if (!activeSubjectStore.currentSubject) {
    return
  }

  await Promise.all([
    conversationApi
      .listConversations(activeSubjectStore.conversationViewerParams())
      .then((list) => {
        chatStore.setConversations(list)
      })
      .catch(() => {}),
    friendRequestStore.loadReceivedRequests(true).catch(() => {}),
    notificationStore.loadNotifications(30).catch(() => {}),
  ])
}

onShow(() => {
  userStore.syncFromStorage()
  void ensureRealtime()
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
