<template>
  <view v-if="customTabbarEnable && !shouldHide" class="border-and-fixed" @touchmove.stop.prevent>
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
              mode="scaleToFill"
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
import { customTabbarEnable, needHideNativeTabbar, tabbarCacheEnable } from './config'
import type { CustomTabBarItem } from './types'
import { tabbarList, tabbarStore } from './store'

const { t } = useI18n({ useScope: 'global' })
const chatStore = useChatStore()

const currentRoute = ref('')

const resolveRoute = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const raw = current?.$page?.path || current?.route || ''
  currentRoute.value = raw ? (raw.startsWith('/') ? raw : `/${raw}`) : ''
}

const shouldHide = computed(() => {
  const r = (currentRoute.value || '').toString()
  if (r.includes('/pages/chat/chat-room') || r.includes('pages/chat/chat-room')) return true
  if (r.includes('/pages/contact/contact-detail') || r.includes('pages/contact/contact-detail')) return true
  if (r.includes('/pages/project/project-detail') || r.includes('pages/project/project-detail')) return true
  return false
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
  if (!item || item.pagePath !== '/pages/chat/chat-list') return 0
  return chatStore.totalUnread
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

onMounted(() => {
  resolveRoute()
  // 组件内再隐藏一次，避免仅 App 时机过早未生效（与 techjewelry tabbar/index onLoad 对齐）
  // #ifndef MP-WEIXIN || MP-ALIPAY
  needHideNativeTabbar &&
    uni.hideTabBar({
      animation: false,
      fail() {},
    })
  // #endif
  // #ifdef MP-ALIPAY
  customTabbarEnable &&
    uni.hideTabBar({
      animation: false,
      fail() {},
    })
  // #endif
})

onShow(() => {
  resolveRoute()
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1] as any
  const raw = currentPage?.$page?.path || currentPage?.route || ''
  const path = raw ? (raw.startsWith('/') ? raw : `/${raw}`) : ''
  const fIndex = tabbarList.findIndex((item) => (item.subPagePath ?? item.pagePath) === path)
  if (fIndex !== -1) {
    tabbarStore.setCurIdx(fIndex)
  }
})
</script>

<style scoped>
@import '@/styles/tokens.css';

.border-and-fixed {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 15;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid var(--c-border);
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: var(--bottom-nav-inner-height);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
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
  width: 76rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
}

.icon {
  font-size: 38rpx;
  line-height: 1;
  color: var(--c-muted);
}

.icon-img {
  width: 48rpx;
  height: 48rpx;
}

.label {
  font-size: 20rpx;
  color: var(--c-muted);
  font-weight: 600;
}

.nav-item.active .icon {
  color: var(--c-ink);
}

.nav-item.active .label {
  color: var(--c-ink);
  font-weight: 800;
}

.nav-item.active .icon-wrap {
  background: rgba(3, 2, 19, 0.06);
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
