<template>
  <view class="page">
    <scroll-view class="profile-scroll" scroll-y>
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
            <text class="progress-label">{{ tf('page.profile.points', { n: userPoints }) }}</text>
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
        <view class="active-subject-card" @click="openMyAgents">
          <view class="active-subject-main">
            <text class="active-subject-label">{{ t('page.profile.active_subject_label') }}</text>
            <text class="active-subject-name">{{ activeSubjectName }}</text>
          </view>
          <text class="active-subject-type">{{ activeSubjectTypeLabel }}</text>
        </view>
      </view>

      <view class="menu-block">
        <button class="menu-row" @click="openSettings">
          <view class="menu-icon">S</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_settings') }}</text>
            <text class="menu-desc">{{ t('page.profile.menu_settings_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
        <button class="menu-row" @click="openNotifications">
          <view class="menu-icon">N</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_notifications') }}</text>
            <text class="menu-desc">{{ unreadCount > 0 ? tf('page.profile.unread_count', { n: unreadCount }) : t('page.profile.menu_notifications_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
        <button class="menu-row" @click="openMyAgents">
          <view class="menu-icon">A</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_my_agents') }}</text>
            <text class="menu-desc">{{ t('page.profile.menu_my_agents_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
        <button class="menu-row" @click="noop">
          <view class="menu-icon">L</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_payment_methods') }}</text>
            <text class="menu-desc">{{ t('page.profile.menu_payment_methods_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
        <button class="menu-row" @click="noop">
          <view class="menu-icon">P</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_privacy') }}</text>
            <text class="menu-desc">{{ t('page.profile.menu_privacy_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
        <button class="menu-row" @click="noop">
          <view class="menu-icon">?</view>
          <view class="menu-body">
            <text class="menu-title">{{ t('page.profile.menu_help') }}</text>
            <text class="menu-desc">{{ t('page.profile.menu_help_desc') }}</text>
          </view>
          <text class="chev">&gt;</text>
        </button>
      </view>

      <view class="logout-wrap">
        <Button
          v-if="!showLogoutConfirm"
          variant="danger"
          size="large"
          shape="round"
          block
          @click="showLogoutConfirm = true"
        >
          {{ t('common.logout') }}
        </Button>

        <view v-else class="logout-confirm-wrap">
          <text class="logout-confirm-text">{{ t('page.profile.logout_confirm') }}</text>
          <view class="logout-confirm-actions">
            <Button variant="secondary" size="medium" shape="round" @click="showLogoutConfirm = false">
              {{ t('common.cancel') }}
            </Button>
            <Button variant="danger" size="medium" shape="round" :loading="loggingOut" @click="handleLogout">
              {{ t('common.confirm') }}
            </Button>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>

  <view v-if="showNotifications" class="sheet-mask" @click="showNotifications = false">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.profile.menu_notifications') }}</text>
        <text class="sheet-close" @click="showNotifications = false">x</text>
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
        <text class="sheet-close" @click="showSettings = false">x</text>
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
        <text class="sheet-close" @click="closeMyAgents">x</text>
      </view>

      <scroll-view class="sheet-body" scroll-y>
        <view v-if="myAgentsLoading" class="sheet-empty">{{ t('page.profile.my_agents.loading') }}</view>
        <view v-else-if="myAgents.length === 0 && !activeSubjectStore.humanSubject" class="sheet-empty">{{ t('page.profile.my_agents.empty') }}</view>

        <view
          v-if="!myAgentsLoading && activeSubjectStore.humanSubject"
          class="agent-row selectable"
          :class="{ selected: isSelectedSubject(activeSubjectStore.humanSubject.subjectId, activeSubjectStore.humanSubject.subjectType) }"
          @click="selectHumanSubject"
        >
          <view class="agent-avatar human-avatar">
            <text class="agent-avatar-text">{{ (activeSubjectStore.humanSubject.displayName || '?').slice(0, 1) }}</text>
          </view>
          <view class="agent-main">
            <text class="agent-name">{{ activeSubjectStore.humanSubject.displayName }}</text>
            <text class="agent-desc">HUMAN</text>
          </view>
          <text v-if="isSelectedSubject(activeSubjectStore.humanSubject.subjectId, activeSubjectStore.humanSubject.subjectType)" class="selected-mark">✓</text>
        </view>

        <view
          v-for="a in myAgents"
          :key="a.id"
          class="agent-row selectable"
          :class="{ selected: isSelectedSubject(agentSubjectId(a), 'AGENT') }"
          @click="selectAgentSubject(a)"
        >
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
              <text :class="a.walletEnabled ? 'wallet-ok' : 'wallet-off'">
                {{ a.walletEnabled ? t('page.profile.my_agents.wallet_enabled') : t('page.profile.my_agents.wallet_disabled') }}
              </text>
            </view>
            <view class="agent-action-row" @click.stop>
              <Button
                size="mini"
                shape="round"
                :variant="a.walletEnabled ? 'outline' : 'primary'"
                :loading="walletUpdatingAgentId === a.id"
                :disabled="walletUpdatingAgentId !== null && walletUpdatingAgentId !== a.id"
                @click="toggleAgentWallet(a)"
              >
                {{ a.walletEnabled ? t('page.profile.my_agents.disable_wallet') : t('page.profile.my_agents.enable_wallet') }}
              </Button>
            </view>
          </view>
          <text v-if="isSelectedSubject(agentSubjectId(a), 'AGENT')" class="selected-mark">✓</text>
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
import { useNotificationStore } from '@/store/modules/notification'
import { useFriendRequestStore } from '@/store/modules/friendRequest'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import { agentApi, type AgentWalletPolicyResponse, type MyAgentItem } from '@/api/modules/agent'
import type { ContactSubjectType } from '@/api/modules/contact'
import { wsClient } from '@/utils/websocket'
import Button from '@/components/Button.vue'
import FgTabbar from '@/tabbar/index.vue'

const userStore = useUserStore()
const userInfo = ref<UserInfo | null>(userStore.userInfo || null)
const { t, tf, locale } = useI18nWithFormat()
const notificationStore = useNotificationStore()
const friendRequestStore = useFriendRequestStore()
const activeSubjectStore = useActiveSubjectStore()
const notificationFilter = ref<'all' | 'mention'>('all')
const showNotifications = ref(false)
const showSettings = ref(false)
const showMyAgents = ref(false)
const myAgents = ref<MyAgentItem[]>([])
const myAgentsLoading = ref(false)
const walletUpdatingAgentId = ref<number | null>(null)
const showLogoutConfirm = ref(false)
const loggingOut = ref(false)
const unreadCount = computed(() => notificationStore.unreadCount)
const activeSubjectName = computed(() => activeSubjectStore.currentLabel || t('page.profile.active_subject_unselected'))
const activeSubjectTypeLabel = computed(() => activeSubjectStore.currentSubject?.subjectType || '--')
const filteredNotifications = computed(() => {
  const list = notificationStore.notifications
  if (notificationFilter.value === 'all') return list
  return list.filter((x) => x.type === 'MESSAGE_MENTION')
})

const LEVELS = [
  { level: 1, nameKey: 'page.profile.level_1', min: 0 },
  { level: 2, nameKey: 'page.profile.level_2', min: 50 },
  { level: 3, nameKey: 'page.profile.level_3', min: 150 },
  { level: 4, nameKey: 'page.profile.level_4', min: 250 },
  { level: 5, nameKey: 'page.profile.level_5', min: 500 },
]

const userPoints = computed(() => {
  const s = userInfo.value?.points ?? userInfo.value?.creditScore
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
    return false
  }
  try {
    const data = await userApi.me()
    userInfo.value = data
    userStore.setUserInfo(data)
    return true
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    if (!userInfo.value) {
      userStore.logout()
      uni.reLaunch({ url: '/pages/auth/login' })
    }
    return false
  }
}

const loadNotifications = async () => {
  await activeSubjectStore.ensureLoaded()
  await notificationStore.loadNotifications(30)
}

const openNotifications = async () => {
  showNotifications.value = true
  notificationFilter.value = 'all'
  if (notificationStore.notifications.length === 0) {
    await loadNotifications()
  }
}

const openSettings = () => {
  showSettings.value = true
}

const loadMyAgents = async () => {
  try {
    myAgentsLoading.value = true
    await activeSubjectStore.ensureLoaded(true)
    myAgents.value = [...activeSubjectStore.agents]
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    myAgents.value = []
  } finally {
    myAgentsLoading.value = false
  }
}

const agentSubjectId = (agent: MyAgentItem) => Number(agent.agentSubjectId ?? agent.id)

const applyWalletPolicy = (agentId: number, wallet: AgentWalletPolicyResponse) => {
  const update = (agent: MyAgentItem) => agent.id === agentId
    ? {
        ...agent,
        walletEnabled: wallet.walletEnabled,
        walletPolicyState: wallet.walletPolicyState,
        walletRouting: wallet.walletRouting,
        walletPolicyReason: wallet.walletPolicyReason,
        directRecipientSubjectId: wallet.directRecipientSubjectId,
        directRecipientSubjectType: wallet.directRecipientSubjectType,
        settlementSubjectId: wallet.settlementSubjectId,
        settlementSubjectType: wallet.settlementSubjectType,
        settlementHumanId: wallet.settlementHumanId,
        financialAutonomy: wallet.financialAutonomy,
      }
    : agent
  myAgents.value = myAgents.value.map(update)
  activeSubjectStore.agents = activeSubjectStore.agents.map(update)
}

const isSelectedSubject = (subjectId: number, subjectType: ContactSubjectType) => {
  const current = activeSubjectStore.currentSubject
  return !!current && current.subjectId === Number(subjectId) && current.subjectType === subjectType
}

const refreshSubjectScopedData = async () => {
  const subject = activeSubjectStore.currentSubject
  if (subject) {
    wsClient.setActiveSubject({ subjectId: subject.subjectId, subjectType: subject.subjectType })
  }
  notificationStore.setNotifications([])
  friendRequestStore.clear()
  await Promise.all([
    loadNotifications(),
    friendRequestStore.loadReceivedRequests(true),
  ])
}

const selectHumanSubject = async () => {
  if (!activeSubjectStore.setHuman()) return
  await refreshSubjectScopedData()
  closeMyAgents()
}

const selectAgentSubject = async (agent: MyAgentItem) => {
  const subjectId = agentSubjectId(agent)
  if (!Number.isFinite(subjectId) || subjectId <= 0) return
  if (!activeSubjectStore.setActiveSubject({ subjectId, subjectType: 'AGENT' })) return
  await refreshSubjectScopedData()
  closeMyAgents()
}

const toggleAgentWallet = async (agent: MyAgentItem) => {
  if (walletUpdatingAgentId.value !== null) return
  const agentId = Number(agent.id)
  if (!Number.isFinite(agentId) || agentId <= 0) return
  walletUpdatingAgentId.value = agentId
  try {
    const wallet = agent.walletEnabled
      ? await agentApi.disableWallet(agentId, 'owner disabled from profile')
      : await agentApi.enableWallet(agentId)
    applyWalletPolicy(agentId, wallet)
    await activeSubjectStore.ensureLoaded(true)
    myAgents.value = [...activeSubjectStore.agents]
    uni.showToast({ title: t('page.profile.my_agents.wallet_updated'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    walletUpdatingAgentId.value = null
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
  await notificationStore.markRead(id)
}

const handleLogout = async () => {
  loggingOut.value = true
  try {
    await userStore.logout()
    uni.reLaunch({ url: '/pages/auth/login' })
  } finally {
    loggingOut.value = false
    showLogoutConfirm.value = false
  }
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

onShow(async () => {
  const hasUser = await fetchUserInfo()
  if (!hasUser) return
  await activeSubjectStore.ensureLoaded(true)
  if (!activeSubjectStore.currentSubject) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  myAgents.value = [...activeSubjectStore.agents]
  loadNotifications()
  uni.setNavigationBarTitle({ title: t('page.profile.title') })
})

</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--c-page);
  box-sizing: border-box;
  overflow: hidden;
}

.profile-scroll {
  flex: 1;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  padding-top: 0;
  padding-bottom: var(--page-pad-bottom-tabbar);
}

.profile-head {
  width: 100%;
  max-width: var(--page-content-max);
  margin: 0 auto;
  padding: calc(var(--status-bar-height) + 28rpx) 24rpx 24rpx;
  background: #fff;
  box-sizing: border-box;
  border-radius: 0;
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: none;
}

.profile-row {
  display: flex;
  gap: 28rpx;
  align-items: flex-start;
  margin-bottom: 28rpx;
}

.avatar {
  width: var(--avatar-size);
  height: var(--avatar-size);
  border-radius: var(--radius-avatar);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
  flex-shrink: 0;
}

.avatar-text {
  font-size: 34rpx;
  font-weight: 800;
  color: #fff;
}

.profile-text {
  flex: 1;
  min-width: 0;
  min-height: var(--avatar-size);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 10rpx;
  box-sizing: border-box;
}

.profile-name {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  line-height: 1.2;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-email {
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
  line-height: 1.25;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.level-pill {
  display: inline-flex;
  padding: 6rpx 16rpx;
  border-radius: var(--radius-md);
  background: rgba(0, 0, 0, 0.06);
  border: 1rpx solid rgba(0, 0, 0, 0.12);
}

.level-pill text {
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-primary);
  line-height: 1.2;
}

.progress-card {
  padding: 24rpx;
  border-radius: var(--radius-card);
  background: var(--c-surface-muted);
  border: 1rpx solid var(--c-border);
}

.active-subject-card {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: var(--radius-card);
  background: #fff;
  border: 1rpx solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.active-subject-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.active-subject-label {
  font-size: 22rpx;
  color: var(--c-muted);
}

.active-subject-name {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.active-subject-type {
  flex-shrink: 0;
  padding: 6rpx 12rpx;
  border-radius: var(--radius-md);
  background: rgba(3, 2, 19, 0.08);
  color: var(--c-primary);
  font-size: 20rpx;
  font-weight: 800;
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
  width: 100%;
  max-width: var(--page-content-max);
  margin: 20rpx auto;
  padding: 0 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  align-items: stretch;
  box-sizing: border-box;
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 24rpx;
  background: #fff;
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-card);
  box-shadow: none;
  text-align: left;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
  width: 100%;
  box-sizing: border-box;
}

.menu-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-md);
  background: var(--c-surface-muted);
  border: 1rpx solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 800;
  color: var(--c-ink);
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


.logout-wrap {
  width: 100%;
  max-width: var(--page-content-max);
  margin: 20rpx auto;
  padding: 0 24rpx;
  display: flex;
  justify-content: center;
  box-sizing: border-box;
}

.logout-confirm-wrap {
  width: 100%;
  background: rgba(239, 68, 68, 0.08);
  border: 1rpx solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-lg);
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.logout-confirm-text {
  font-size: 24rpx;
  color: #dc2626;
  font-weight: 600;
  text-align: center;
}

.logout-confirm-actions {
  display: flex;
  gap: 12rpx;
}

.logout-confirm-actions .fg-btn {
  flex: 1;
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
  background: var(--c-primary);
  color: #fff;
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
  background: rgba(255, 255, 255, 0.9);
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

.agent-row.selectable {
  align-items: center;
}

.agent-row.selected {
  border-color: rgba(3, 2, 19, 0.24);
  background: rgba(3, 2, 19, 0.04);
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

.human-avatar {
  background: linear-gradient(135deg, #0EA5E9f0, #0EA5E9d8);
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

.selected-mark {
  flex-shrink: 0;
  width: 44rpx;
  height: 44rpx;
  border-radius: 999rpx;
  background: var(--c-primary);
  color: #fff;
  font-size: 26rpx;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
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
  line-clamp: 2;
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

.wallet-ok,
.wallet-off {
  font-size: 20rpx;
  font-weight: 800;
  border-radius: var(--radius-pill);
  padding: 6rpx 12rpx;
  white-space: nowrap;
}

.wallet-ok {
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.22);
}

.wallet-off {
  color: #92400e;
  background: rgba(245, 158, 11, 0.12);
  border: 1rpx solid rgba(245, 158, 11, 0.22);
}

.agent-action-row {
  margin-top: 12rpx;
  display: flex;
  justify-content: flex-end;
}

.notice {
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 20rpx;
  margin-bottom: 12rpx;
  background: #fff;
}

.notice.unread {
  background: rgba(0, 0, 0, 0.04);
  border-color: rgba(0, 0, 0, 0.12);
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
