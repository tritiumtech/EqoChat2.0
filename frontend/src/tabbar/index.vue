<template>
  <view v-if="customTabbarEnable && !shouldHide" class="bottom-nav" @touchmove.stop.prevent>
    <view class="nav-inner">
      <view
        v-for="(item, index) in tabbarList"
        :key="index"
        class="nav-item"
        :class="{ active: isActive(index) }"
        @click="handleClick(index)"
      >
        <view class="nav-item-inner">
          <view class="icon-wrap">
            <text v-if="item.iconType === 'emoji'" class="icon">{{ item.icon }}</text>
            <image
              v-else-if="item.iconType === 'image'"
              :src="getImageByIndex(index, item)"
              mode="aspectFit"
              class="icon-img"
            />
            <view v-if="badgeCount(index) > 0" class="badge">
              <text class="badge-text">{{ badgeText(badgeCount(index)) }}</text>
            </view>
          </view>
          <text class="label">{{ t(item.text) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { useChatStore } from '@/store/modules/chat'
import { useFriendRequestStore } from '@/store/modules/friendRequest'
import { useNotificationStore } from '@/store/modules/notification'
import { customTabbarEnable, needHideNativeTabbar, tabbarCacheEnable } from './config'
import type { CustomTabBarItem } from './types'
import { tabbarList, tabbarStore } from './store'

const { t } = useI18n({ useScope: 'global' })
const chatStore = useChatStore()
const friendRequestStore = useFriendRequestStore()
const notificationStore = useNotificationStore()

const currentRoute = ref('')

const resolveRoute = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const raw = current?.$page?.path || current?.route || ''
  currentRoute.value = raw ? (raw.startsWith('/') ? raw : `/${raw}`) : ''
}

const shouldHide = computed(() => {
  const route = currentRoute.value
  return (
    route.includes('/pages/chat/chat-room') ||
    route.includes('/pages/contact/contact-detail') ||
    route.includes('/pages/project/project-detail')
  )
})

function isActive(index: number) {
  return tabbarStore.curIdx === index
}

function getImageByIndex(index: number, item: CustomTabBarItem) {
  if (!item.iconActive) return item.icon
  return tabbarStore.curIdx === index ? item.iconActive : item.icon
}

function badgeCount(index: number) {
  const item = tabbarList[index]
  if (!item) return 0
  if (item.pagePath === '/pages/chat/chat-list') return chatStore.totalUnread
  if (item.pagePath === '/pages/contact/contact-list') return friendRequestStore.pendingCount
  if (item.pagePath === '/pages/profile/profile') return notificationStore.unreadCount
  return 0
}

function badgeText(n: number) {
  return n > 99 ? '99+' : String(n)
}

function handleClick(index: number) {
  if (index === tabbarStore.curIdx) return
  const item = tabbarList[index]
  if (!item) return

  const url = item.subPagePath ?? item.pagePath
  tabbarStore.setCurIdx(index)
  if (tabbarCacheEnable && !item.subPagePath) {
    uni.switchTab({ url })
  } else {
    uni.navigateTo({ url })
  }
}

function syncActiveIndex() {
  resolveRoute()
  const index = tabbarList.findIndex((item) => (item.subPagePath ?? item.pagePath) === currentRoute.value)
  if (index !== -1) {
    tabbarStore.setCurIdx(index)
  }
}

onMounted(() => {
  syncActiveIndex()
  // #ifndef MP-WEIXIN || MP-ALIPAY
  if (needHideNativeTabbar) {
    uni.hideTabBar({ animation: false, fail() {} })
  }
  // #endif
  // #ifdef MP-ALIPAY
  if (customTabbarEnable) {
    uni.hideTabBar({ animation: false, fail() {} })
  }
  // #endif
})

onShow(syncActiveIndex)
</script>

<style scoped>
@import '@/styles/tokens.css';

.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 15;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  padding: 0 16rpx env(safe-area-inset-bottom);
  background: rgba(255, 255, 255, 0.98);
  border-top: 1rpx solid var(--c-border);
  box-shadow: 0 -10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  width: 100%;
  max-width: var(--page-content-max);
  height: var(--bottom-nav-inner-height);
}

.nav-item {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-item-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  width: 100%;
}

.icon-wrap {
  position: relative;
  width: 68rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-action-icon);
}

.icon {
  font-size: 34rpx;
  line-height: 1;
  color: var(--c-muted);
}

.icon-img {
  width: 44rpx;
  height: 44rpx;
}

.label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 20rpx;
  color: var(--c-muted);
  font-weight: 600;
  line-height: 1.2;
}

.nav-item.active .icon,
.nav-item.active .label {
  color: var(--c-ink);
}

.nav-item.active .label {
  font-weight: 800;
}

.nav-item.active .icon-wrap {
  background: var(--c-surface-muted);
  border: 1rpx solid var(--c-border);
}

.badge {
  position: absolute;
  top: -6rpx;
  right: -4rpx;
  min-width: 34rpx;
  height: 28rpx;
  box-sizing: border-box;
  padding: 0 8rpx;
  border-radius: var(--radius-pill);
  background: var(--c-destructive);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #ffffff;
}

.badge-text {
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 800;
  line-height: 1;
}
</style>
