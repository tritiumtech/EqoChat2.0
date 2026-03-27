<template>
  <view v-if="!shouldHide" class="bottom-nav">
    <view class="nav-inner">
      <view
        v-for="item in items"
        :key="item.key"
        class="nav-item"
        :class="{ active: item.key === activeKey }"
        @click="handleTap(item.key)"
      >
        <view class="icon-wrap">
          <text class="icon">{{ item.icon }}</text>
          <view v-if="badgeCount(item.key) > 0" class="badge">
            <text class="badge-text">{{ badgeText(badgeCount(item.key)) }}</text>
          </view>
        </view>
        <text class="label">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { useChatStore } from '@/store/modules/chat'

type NavKey = 'chat' | 'world' | 'contacts' | 'me'

const { t } = useI18n()

const items = computed<Array<{ key: NavKey; icon: string; label: string }>>(() => [
  { key: 'chat', icon: '💬', label: t('nav.chat') },
  { key: 'world', icon: '🌐', label: t('nav.world') },
  { key: 'contacts', icon: '👥', label: t('nav.contacts') },
  { key: 'me', icon: '👤', label: t('nav.me') },
])

const chatStore = useChatStore()

const currentRoute = ref('')

const resolveRoute = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const route = current?.route || current?.$page?.route || current?.$page?.fullPath || ''
  currentRoute.value = route
}

const activeKey = computed<NavKey>(() => {
  const r = (currentRoute.value || '').toString()
  if (r.includes('/pages/chat/') || r.includes('pages/chat/')) return 'chat'
  if (r.includes('/pages/world/') || r.includes('pages/world/')) return 'world'
  if (r.includes('/pages/contact/') || r.includes('pages/contact/')) return 'contacts'
  if (r.includes('/pages/profile/') || r.includes('pages/profile/')) return 'me'
  return 'chat'
})

const shouldHide = computed(() => {
  const r = (currentRoute.value || '').toString()
  // 二级页面（详情/对话等）隐藏底部导航
  if (r.includes('/pages/chat/chat-room') || r.includes('pages/chat/chat-room')) return true
  if (r.includes('/pages/contact/contact-detail') || r.includes('pages/contact/contact-detail')) return true
  return false
})

const badgeCount = (key: NavKey) => {
  if (key === 'chat') return chatStore.totalUnread
  return 0
}

const badgeText = (n: number) => {
  return n > 99 ? '99+' : String(n)
}

const handleTap = (key: NavKey) => {
  const urlMap: Record<NavKey, string> = {
    chat: '/pages/chat/chat-list',
    world: '/pages/world/world',
    contacts: '/pages/contact/contact-list',
    me: '/pages/profile/profile',
  }
  uni.redirectTo({ url: urlMap[key] })
}

onMounted(() => {
  resolveRoute()
})

onShow(() => {
  resolveRoute()
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 15;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1rpx solid var(--c-border);
  backdrop-filter: blur(12rpx);
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 96rpx;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  position: relative;
}

.icon-wrap {
  position: relative;
  width: 76rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon {
  font-size: 38rpx;
  line-height: 1;
  color: var(--c-muted);
}

.label {
  font-size: 20rpx;
  color: var(--c-muted);
  font-weight: 600;
}

.nav-item.active .icon {
  color: var(--c-primary);
}

.nav-item.active .label {
  color: var(--c-primary);
}

.badge {
  position: absolute;
  top: -6rpx;
  right: 0;
  min-width: 36rpx;
  height: 28rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: rgba(239, 68, 68, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 18rpx rgba(0, 0, 0, 0.12);
}

.badge-text {
  color: #fff;
  font-size: 18rpx;
  font-weight: 800;
}
</style>

