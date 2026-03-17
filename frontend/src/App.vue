<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { watch } from 'vue'
import { i18n } from '@/i18n'
import { useChatStore } from '@/store/modules/chat'
import { useUserStore } from '@/store/modules/user'

const TAB_BAR_PAGES = new Set([
  'pages/chat/chat-list',
  'pages/contact/contact-list',
  'pages/discover/discover',
  'pages/profile/profile'
])

const CHAT_TAB_INDEX = 0

onLaunch(() => {
  console.log('EqoChat App Launch')
  updateTabBar()
})

onShow(() => {
  console.log('EqoChat App Show')
  updateTabBar()
  updateChatBadge()
})

onHide(() => {
  console.log('EqoChat App Hide')
})

const updateChatBadge = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const route = current?.route || current?.$page?.route || current?.$page?.fullPath
  if (!route || !TAB_BAR_PAGES.has(route)) {
    return
  }
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    try { uni.removeTabBarBadge({ index: CHAT_TAB_INDEX }) } catch { /* ignore */ }
    return
  }
  const chatStore = useChatStore()
  const total = chatStore.totalUnread
  try {
    if (total > 0) {
      uni.setTabBarBadge({ index: CHAT_TAB_INDEX, text: total > 99 ? '99+' : String(total) })
    } else {
      uni.removeTabBarBadge({ index: CHAT_TAB_INDEX })
    }
  } catch {
    /* ignore */
  }
}

const updateTabBar = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const route = current?.route || current?.$page?.route || current?.$page?.fullPath
  if (!route || !TAB_BAR_PAGES.has(route)) {
    return
  }
  const t = i18n.global.t
  const trySet = (options: UniApp.SetTabBarItemOptions) => {
    try {
      const res = uni.setTabBarItem(options) as unknown as Promise<void> | void
      if (res && typeof (res as Promise<void>).catch === 'function') {
        ;(res as Promise<void>).catch(() => {})
      }
    } catch (e) {
      // no-op to avoid crashing when not on tabBar page
    }
  }
  trySet({ index: 0, text: t('page.chat.title') })
  trySet({ index: 1, text: t('page.contact.title') })
  trySet({ index: 2, text: t('page.discover.title') })
  trySet({ index: 3, text: t('page.profile.title') })
}

watch(
  () => i18n.global.locale.value,
  () => updateTabBar()
)

const chatStore = useChatStore()
watch(() => chatStore.totalUnread, updateChatBadge)
</script>

<style>
:root {
  --c-bg: #f6f2ee;
  --c-bg-2: #edf2ff;
  --c-ink: #1a1720;
  --c-muted: #6b6673;
  --c-primary: #ff7a59;
  --c-primary-deep: #e85b3a;
  --c-secondary: #4f6bff;
  --c-surface: #ffffff;
  --c-border: rgba(26, 23, 32, 0.08);
  --c-shadow: 0 24rpx 60rpx rgba(27, 21, 44, 0.16);
  --c-shadow-soft: 0 12rpx 32rpx rgba(27, 21, 44, 0.1);
  --radius-xl: 32rpx;
  --radius-lg: 24rpx;
  --radius-md: 18rpx;
  --radius-pill: 999rpx;
  --font-display: "HarmonyOS Sans", "DIN Alternate", "MiSans", "PingFang SC", "Noto Sans CJK SC", sans-serif;
}

page {
  background: var(--c-bg);
  color: var(--c-ink);
  font-family: var(--font-display);
}
</style>
