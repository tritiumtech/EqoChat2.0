<template>
  <scroll-view class="page" scroll-y>
    <view class="profile-head">
      <view class="profile-row">
        <view class="avatar" :style="avatarStyle">
          <text class="avatar-text">{{ initials }}</text>
        </view>
        <view class="profile-text">
          <text class="profile-name">{{ userInfo?.nickname || t('common.not_logged_in') }}</text>
          <text class="profile-email">{{ subLine }}</text>
          <view class="level-pill">
            <text>{{ tf('page.profile.level_short', { n: levelInfo.level, name: t(levelInfo.nameKey) }) }}</text>
          </view>
        </view>
      </view>
      <view class="progress-card">
        <view class="progress-top">
          <text class="progress-label">★ {{ tf('page.profile.points', { n: userPoints }) }}</text>
          <text class="progress-hint">{{ levelInfo.isMax ? t('page.profile.level_max') : tf('page.profile.points_to_level', { n: levelInfo.need, name: t(levelInfo.nextNameKey) }) }}</text>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: levelInfo.pct + '%' }" />
        </view>
        <view class="progress-feet">
          <text>{{ tf('page.profile.level_abbr', { n: levelInfo.level }) }}</text>
          <text>{{ tf('page.profile.level_abbr', { n: levelInfo.nextLevel }) }}</text>
        </view>
      </view>
    </view>

    <view class="menu-block">
      <button class="menu-row" @click="openSettings">
        <view class="menu-icon">⚙️</view>
        <view class="menu-body">
          <text class="menu-title">{{ t('page.profile.menu_settings') }}</text>
          <text class="menu-desc">{{ t('page.profile.menu_settings_desc') }}</text>
        </view>
        <text class="chev">›</text>
      </button>
      <button class="menu-row" @click="openNotifications">
        <view class="menu-icon">🔔</view>
        <view class="menu-body">
          <text class="menu-title">{{ t('page.profile.menu_notifications') }}</text>
          <text class="menu-desc">{{ unreadCount > 0 ? tf('page.profile.unread_count', { n: unreadCount }) : t('page.profile.menu_notifications_desc') }}</text>
        </view>
        <text class="chev">›</text>
      </button>
      <button class="menu-row" @click="openMyAgents">
        <view class="menu-icon">🤖</view>
        <view class="menu-body">
          <text class="menu-title">{{ t('page.profile.menu_my_agents') }}</text>
          <text class="menu-desc">{{ t('page.profile.menu_my_agents_desc') }}</text>
        </view>
        <text class="chev">›</text>
      </button>
      <button class="menu-row" @click="noop">
        <view class="menu-icon">🛡</view>
        <view class="menu-body">
          <text class="menu-title">{{ t('page.profile.menu_privacy') }}</text>
          <text class="menu-desc">{{ t('page.profile.menu_privacy_desc') }}</text>
        </view>
        <text class="chev">›</text>
      </button>
      <button class="menu-row" @click="noop">
        <view class="menu-icon">?</view>
        <view class="menu-body">
          <text class="menu-title">{{ t('page.profile.menu_help') }}</text>
          <text class="menu-desc">{{ t('page.profile.menu_help_desc') }}</text>
        </view>
        <text class="chev">›</text>
      </button>
    </view>

    <button class="btn-logout" @click="logout">{{ t('common.logout') }}</button>
    <view class="foot-pad" />

  </scroll-view>

  <view v-if="showNotifications" class="sheet-mask" @click="showNotifications = false">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.profile.menu_notifications') }}</text>
        <text class="sheet-close" @click="showNotifications = false">✕</text>
      </view>
      <view class="notice-filter-row">
        <view class="notice-filter-btn" :class="{ active: notificationFilter === 'all' }" @click="notificationFilter = 'all'">{{ t('page.contact.filter_all') }}</view>
        <view class="notice-filter-btn" :class="{ active: notificationFilter === 'mention' }" @click="notificationFilter = 'mention'">{{ t('page.profile.notif_filter_mention') }}</view>
      </view>
      <scroll-view class="sheet-body" scroll-y>
        <view v-if="filteredNotifications.length === 0" class="sheet-empty">{{ t('common.empty_conversation') }}</view>
        <view v-for="n in filteredNotifications" :key="n.id" class="notice" :class="{ unread: !n.read }" @click="markNotificationRead(n.id)">
          <text class="notice-title">{{ n.title }}</text>
          <text class="notice-content">{{ n.content || t('common.dash') }}</text>
        </view>
      </scroll-view>
    </view>
  </view>

  <view v-if="showSettings" class="sheet-mask" @click="showSettings = false">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.profile.menu_settings') }}</text>
        <text class="sheet-close" @click="showSettings = false">✕</text>
      </view>
      <view class="sheet-settings">
        <text class="setting-label">{{ t('page.profile.language') }}</text>
        <view class="setting-lang-row">
          <button
            class="setting-lang-btn"
            :class="{ active: locale === 'zh-Hans' }"
            @click="changeLocale('zh-Hans')"
          >{{ t('page.profile.zh') }}</button>
          <button
            class="setting-lang-btn"
            :class="{ active: locale === 'en' }"
            @click="changeLocale('en')"
          >{{ t('page.profile.en') }}</button>
        </view>
      </view>
    </view>
  </view>

  <view v-if="showMyAgents" class="sheet-mask" @click="closeMyAgents">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.profile.my_agents.title') }}</text>
        <text class="sheet-close" @click="closeMyAgents">✕</text>
      </view>

      <scroll-view class="sheet-body" scroll-y>
        <view v-if="myAgentsLoading" class="sheet-empty">{{ t('page.profile.my_agents.loading') }}</view>
        <view v-else-if="myAgents.length === 0" class="sheet-empty">{{ t('page.profile.my_agents.empty') }}</view>

        <view v-for="a in myAgents" :key="a.id" class="agent-row">
          <view class="agent-avatar">
            <text class="agent-avatar-text">{{ (a.name || '').slice(0, 1) || '?' }}</text>
          </view>
          <view class="agent-main">
            <text class="agent-name">{{ a.name }}</text>
            <text class="agent-desc">{{ a.description || '—' }}</text>

            <view class="agent-cap-row">
              <text v-for="cap in (a.capabilities || []).slice(0, 4)" :key="cap" class="cap-chip">
                {{ cap }}
              </text>
            </view>

            <view class="agent-bottom">
              <text class="agent-credit">{{ t('page.profile.my_agents.credit_score_label') }}{{ a.creditScore ?? 0 }}</text>
              <text v-if="a.walletEnabled" class="wallet-ok">{{ t('page.profile.my_agents.wallet_enabled') }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>

  <FgTabbar />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { userApi, type UserInfo } from '@/api/modules/user'
import { useUserStore } from '@/store/modules/user'
import { setLocale } from '../../locale/i18n'
import { notificationApi, type NotificationItem } from '@/api/modules/notification'
import { getApiErrorMessage } from '@/utils/request'
import { agentApi, type MyAgentItem } from '@/api/modules/agent'
import FgTabbar from '@/tabbar/index.vue'

const userStore = useUserStore()
const userInfo = ref<UserInfo | null>(userStore.userInfo || null)
const { t, tf, locale } = useI18nWithFormat()
const notifications = ref<NotificationItem[]>([])
const notificationFilter = ref<'all' | 'mention'>('all')
const showNotifications = ref(false)
const showSettings = ref(false)
const showMyAgents = ref(false)
const myAgents = ref<MyAgentItem[]>([])
const myAgentsLoading = ref(false)
const unreadCount = computed(() => notifications.value.filter((x) => !x.read).length)
const filteredNotifications = computed(() => {
  if (notificationFilter.value === 'all') return notifications.value
  return notifications.value.filter((x) => x.type === 'MESSAGE_MENTION')
})

const LEVELS = [
  { level: 1, nameKey: 'page.profile.level_1', min: 0 },
  { level: 2, nameKey: 'page.profile.level_2', min: 50 },
  { level: 3, nameKey: 'page.profile.level_3', min: 150 },
  { level: 4, nameKey: 'page.profile.level_4', min: 250 },
  { level: 5, nameKey: 'page.profile.level_5', min: 500 },
]

const userPoints = computed(() => {
  const s = userInfo.value?.creditScore
  if (s == null || Number.isNaN(Number(s))) return 0
  return Math.max(0, Number(s))
})

const levelInfo = computed(() => {
  const pts = userPoints.value
  let idx = 0
  for (let i = LEVELS.length - 1; i >= 0; i--) {
    if (pts >= LEVELS[i]!.min) {
      idx = i
      break
    }
  }
  const cur = LEVELS[idx]!
  const isMax = idx >= LEVELS.length - 1
  const next = !isMax ? LEVELS[idx + 1]! : cur
  const span = !isMax ? (next.min - cur.min) || 1 : 1
  const pct = isMax ? 100 : Math.min(100, Math.round(((pts - cur.min) / span) * 100))
  const need = isMax ? 0 : Math.max(0, next.min - pts)
  
  return {
    level: cur.level,
    nameKey: cur.nameKey,
    nextLevel: isMax ? cur.level : next.level,
    pct,
    need,
    isMax,
    nextNameKey: next.nameKey,
  }
})

const initials = computed(() => {
  const n = userInfo.value?.nickname?.trim()
  if (!n) return '?'
  return n.slice(0, 1).toUpperCase()
})

const subLine = computed(() => {
  if (!userInfo.value) return ''
  return userInfo.value.email || userInfo.value.phone || ''
})

const avatarStyle = computed(() => ({
  background: 'linear-gradient(135deg, #0EA5E9f0, #0EA5E9d8)',
}))

const fetchUserInfo = async () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  try {
    const data = await userApi.me()
    userInfo.value = data
    userStore.setUserInfo(data)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

const loadNotifications = async () => {
  try {
    notifications.value = await notificationApi.list(30)
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  }
}

const openNotifications = async () => {
  showNotifications.value = true
  notificationFilter.value = 'all'
  if (notifications.value.length === 0) {
    await loadNotifications()
  }
}

const openSettings = () => {
  showSettings.value = true
}

const loadMyAgents = async () => {
  try {
    myAgentsLoading.value = true
    myAgents.value = await agentApi.getMyAgents()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    myAgents.value = []
  } finally {
    myAgentsLoading.value = false
  }
}

const openMyAgents = async () => {
  showMyAgents.value = true
  if (myAgents.value.length === 0) {
    await loadMyAgents()
  }
}

const closeMyAgents = () => {
  showMyAgents.value = false
}

const markNotificationRead = async (id: number) => {
  const target = notifications.value.find((x) => x.id === id)
  if (!target || target.read) return
  target.read = true
  try {
    await notificationApi.markRead(id)
  } catch {
    // ignore rollback for UX smoothness
  }
}

const logout = () => {
  userStore.logout()
  uni.reLaunch({ url: '/pages/auth/login' })
}

const changeLocale = (value: 'zh-Hans' | 'en') => {
  if (locale.value === value) return
  setLocale(value)
  if (userInfo.value) {
    userInfo.value = { ...userInfo.value, locale: value }
    userStore.setUserInfo(userInfo.value)
  }
}

const noop = () => {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

onShow(() => {
  fetchUserInfo()
  loadNotifications()
  uni.setNavigationBarTitle({ title: t('page.profile.title') })
})

</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  height: 100vh;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  box-sizing: border-box;
  padding-bottom: var(--page-pad-bottom-tabbar);
}

.profile-head {
  margin: 0;
  padding: calc(var(--status-bar-height) + 32rpx) 24rpx 28rpx;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(14rpx);
  border-radius: 0;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04);
}

.profile-row {
  display: flex;
  gap: 28rpx;
  align-items: center;
  margin-bottom: 28rpx;
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--c-shadow-soft);
  flex-shrink: 0;
}

.avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.profile-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  height: 112rpx;
  gap: 8rpx;
  box-sizing: border-box;
}

.profile-name {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  line-height: 36rpx;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-email {
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
  line-height: 36rpx;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.level-pill {
  display: inline-flex;
  padding: 6rpx 16rpx;
  border-radius: var(--radius-md);
  background: rgba(255, 122, 89, 0.1);
  border: 1rpx solid rgba(255, 122, 89, 0.22);
}

.level-pill text {
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-primary);
  line-height: 22rpx;
}

.progress-card {
  padding: 24rpx;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.6);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
}

.progress-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.progress-label {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.progress-hint {
  font-size: 22rpx;
  color: var(--c-muted);
}

.progress-track {
  height: 12rpx;
  border-radius: var(--radius-pill);
  background: rgba(26, 23, 32, 0.06);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--c-primary);
}

.progress-feet {
  display: flex;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.menu-block {
  margin: 24rpx 24rpx 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  align-items: stretch;
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 24rpx;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12rpx);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  text-align: left;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
  width: 100%;
  box-sizing: border-box;
}

.menu-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-md);
  background: rgba(246, 242, 238, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  flex-shrink: 0;
}

.menu-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  height: 64rpx;
  gap: 6rpx;
  box-sizing: border-box;
}

.menu-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
  display: block;
  line-height: 28rpx;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-desc {
  font-size: 22rpx;
  color: var(--c-muted);
  display: block;
  line-height: 28rpx;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chev {
  font-size: 36rpx;
  color: var(--c-muted);
}

.btn-logout {
  margin: 0 24rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: var(--radius-lg);
  background: rgba(239, 68, 68, 0.1);
  border: 1rpx solid rgba(239, 68, 68, 0.2);
  color: #dc2626;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.foot-pad {
  /* 底部留白已由 .page padding-bottom 统一处理 */
  height: 0;
}

.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid var(--c-border);
}

.sheet-title {
  font-size: 30rpx;
  font-weight: 700;
}

.sheet-close {
  font-size: 30rpx;
  color: var(--c-muted);
}

.sheet-body {
  padding: 16rpx 24rpx 24rpx;
  height: 50vh;
  box-sizing: border-box;
}

.notice-filter-row {
  display: flex;
  gap: 12rpx;
  padding: 14rpx 24rpx 0;
}

.notice-filter-btn {
  padding: 8rpx 18rpx;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.05);
  color: var(--c-muted);
  font-size: 22rpx;
}

.notice-filter-btn.active {
  background: rgba(255, 122, 89, 0.12);
  color: var(--c-primary);
  font-weight: 600;
}

.sheet-settings {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.setting-label {
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
}

.setting-lang-row {
  display: flex;
  gap: 16rpx;
}

.setting-lang-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: var(--radius-pill);
  background: rgba(246, 242, 238, 0.9);
  color: var(--c-ink);
  border: 1rpx solid transparent;
  margin: 0;
}

.setting-lang-btn.active {
  background: var(--c-primary);
  color: #fff;
  border-color: rgba(3, 2, 19, 0.2);
}

.sheet-empty {
  text-align: center;
  color: var(--c-muted);
  margin-top: 60rpx;
}

.agent-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 18rpx;
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.9);
  margin-bottom: 12rpx;
}

.agent-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7c3aedf0, #6366f1e0);
  box-shadow: 0 10rpx 18rpx rgba(3, 2, 19, 0.08);
  flex-shrink: 0;
}

.agent-avatar-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
}

.agent-main {
  flex: 1;
  min-width: 0;
}

.agent-name {
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  color: var(--c-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-cap-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 10rpx;
}

.cap-chip {
  padding: 6rpx 12rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid rgba(91, 103, 241, 0.2);
  background: rgba(91, 103, 241, 0.08);
  color: var(--c-primary);
  font-size: 20rpx;
  font-weight: 700;
}

.agent-bottom {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.agent-credit {
  font-size: 22rpx;
  color: var(--c-muted);
}

.wallet-ok {
  font-size: 20rpx;
  font-weight: 800;
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.22);
  border-radius: var(--radius-pill);
  padding: 6rpx 12rpx;
  white-space: nowrap;
}

.notice {
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 20rpx;
  margin-bottom: 12rpx;
  background: #fff;
}

.notice.unread {
  background: rgba(255, 122, 89, 0.06);
  border-color: rgba(255, 122, 89, 0.25);
}

.notice-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.notice-content {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}
</style>
