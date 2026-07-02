<template>
  <view class="page">
    <PageHeader 
      :title="t('page.chat.title')"
      action-icon="+"
      action-variant="bordered"
      action-size="md"
      @action-click="onCompose"
    >
      <template #search>
        <SearchBar v-model="searchQuery" :placeholder="t('page.chat.search_placeholder')" />
      </template>
    </PageHeader>

    <!-- 连接状态栏 -->
    <view v-if="showConnectionStatus" class="connection-bar" :class="connectionStatusClass" @click="handleConnectionBarClick">
      <view class="connection-indicator" />
      <text class="connection-text">{{ connectionStatusText }}</text>
      <text v-if="canReconnect" class="connection-action">{{ t('page.chat.click_to_reconnect') }}</text>
    </view>

    <view class="list-scroll" :class="{ 'with-connection-bar': showConnectionStatus }">
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="filteredSorted.length === 0" class="state">
        <EmptyState :title="t('common.empty_conversation')" icon="C" />
      </view>
      <view
        v-else
        v-for="item in filteredSorted"
        :key="item.id"
        class="conv-wrap"
        :class="{ pinned: isPinned(item.id) }"
        @click="openConversation(item)"
      >
        <view class="conv-inner">
          <view class="avatar-wrap">
            <view class="avatar" :style="avatarStyle(item)">
              <text class="avatar-letter">{{ avatarText(item) }}</text>
            </view>
            <view v-if="isOnline(item)" class="online-dot" />
          </view>
          <view class="body">
            <view class="row-top">
              <view class="name-block">
                <text class="name">{{ item.title || t('common.conversation') }}</text>
                <text v-if="isAgentConversation(item)" class="badge-ai">{{ t('page.chat.agent_badge') }}</text>
              </view>
              <text class="time">{{ formatTimeShort(item) }}</text>
            </view>
            <view class="row-bottom">
              <text class="preview">{{ item.lastMessage || t('page.chat.no_message') }}</text>
              <view v-if="item.unreadCount && item.unreadCount > 0" class="unread">
                <text>{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</text>
              </view>
            </view>
          </view>
          <view
            class="pin-btn"
            :class="{ on: isPinned(item.id) }"
            @click.stop="togglePin(item.id)"
          >
            <text class="pin-glyph">{{ isPinned(item.id) ? t('page.chat.pinned') : t('page.chat.pin') }}</text>
          </view>
        </view>
      </view>
    </view>

    <FgTabbar />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { conversationApi, type ConversationSummary } from '../../api/modules/conversation'
import { useUserStore } from '../../store/modules/user'
import { useChatStore } from '../../store/modules/chat'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import EmptyState from '../../components/EmptyState.vue'
import SearchBar from '../../components/SearchBar.vue'
import PageHeader from '../../components/PageHeader.vue'
import FgTabbar from '@/tabbar/index.vue'

const PIN_STORAGE_KEY = 'eqo_chat_pins'

const conversations = ref<ConversationSummary[]>([])
const loading = ref(false)
const searchQuery = ref('')
const pinIds = ref<Set<number>>(new Set())
const userStore = useUserStore()
const chatStore = useChatStore()
const activeSubjectStore = useActiveSubjectStore()
const { t } = useI18n({ useScope: 'global' })

const loadPins = () => {
  try {
    const raw = uni.getStorageSync(PIN_STORAGE_KEY) as string | null
    if (!raw) {
      pinIds.value = new Set()
      return
    }
    const arr = JSON.parse(raw) as number[]
    pinIds.value = new Set(arr.filter((n) => typeof n === 'number'))
  } catch {
    pinIds.value = new Set()
  }
}

const savePins = () => {
  uni.setStorageSync(PIN_STORAGE_KEY, JSON.stringify([...pinIds.value]))
}

const isPinned = (id: number) => pinIds.value.has(id)

const togglePin = (id: number) => {
  const next = new Set(pinIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  pinIds.value = next
  savePins()
}

const avatarHue = (s: string) => {
  const hues = ['#7C3AED', '#2563EB', '#DC2626', '#059669', '#D97706', '#0EA5E9', '#EC4899', '#14B8A6']
  let h = 0
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return hues[Math.abs(h) % hues.length]!
}

const avatarStyle = (item: ConversationSummary) => {
  const key = item.title || String(item.id)
  const c = avatarHue(key)
  return { background: `linear-gradient(135deg, ${c}f0, ${c}c0)` }
}

const avatarText = (item: ConversationSummary) => {
  if (isAgentConversation(item)) return 'AI'
  const title = String(item.title || '').trim()
  return (title.slice(0, 1) || 'H').toUpperCase()
}

const isAgentConversation = (item: ConversationSummary) => {
  return item.targetSubjectType === 'AGENT'
}

const isOnline = (item: ConversationSummary) => {
  return item.online === true
}

const formatTimeShort = (item: ConversationSummary) => {
  const time = item.lastMessageAt
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return ''
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) {
    const h = `${d.getHours()}`.padStart(2, '0')
    const m = `${d.getMinutes()}`.padStart(2, '0')
    return `${h}:${m}`
  }
  const yest = new Date(now)
  yest.setDate(yest.getDate() - 1)
  if (d.toDateString() === yest.toDateString()) return t('page.chat.yesterday')
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const filteredSorted = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  let list = [...conversations.value]
  if (q) {
    list = list.filter(
      (c) =>
        (c.title || '').toLowerCase().includes(q) ||
        (c.lastMessage || '').toLowerCase().includes(q)
    )
  }
  list.sort((a, b) => {
    const ap = isPinned(a.id)
    const bp = isPinned(b.id)
    if (ap && !bp) return -1
    if (!ap && bp) return 1
    const ta = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0
    const tb = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0
    return tb - ta
  })
  return list
})

let searchTimer: number | null = null
const fetchConversations = async () => {
  loading.value = true
  try {
    const q = searchQuery.value.trim()
    const params = {
      ...activeSubjectStore.conversationViewerParams(),
      ...(q ? { q } : {}),
    }
    const list = await conversationApi.listConversations(params)
    conversations.value = list
    chatStore.setConversations(list)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const openConversation = (item: ConversationSummary) => {
  const conversationId = Number((item as any)?.id ?? (item as any)?.conversationId)
  if (!conversationId || Number.isNaN(conversationId)) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  const title = encodeURIComponent(item.title || t('common.conversation'))
  chatStore.markConversationRead(conversationId)
  uni.navigateTo({
    url: `/pages/chat/chat-room?conversationId=${conversationId}&title=${title}`
  })
}

const onCompose = () => {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

// 连接状态计算属性
const showConnectionStatus = computed(() => {
  // 未登录不显示
  if (!userStore.isLoggedIn) return false
  // 连接中或已连接不显示
  if (chatStore.isRealtimeConnected) return false
  return true
})

const connectionStatusClass = computed(() => {
  if (chatStore.isSessionKicked) return 'kicked'
  return 'disconnected'
})

const connectionStatusText = computed(() => {
  if (chatStore.isSessionKicked) return t('page.chat.session_kicked')
  return t('page.chat.connection_disconnected')
})

const canReconnect = computed(() => {
  return !chatStore.isSessionKicked && userStore.isLoggedIn
})

// 处理连接状态栏点击
const handleConnectionBarClick = () => {
  if (chatStore.isSessionKicked) {
    // 被踢下线，需要重新登录
    uni.showModal({
      title: t('page.chat.login_prompt'),
      content: t('page.chat.session_kicked_message'),
      showCancel: false,
      confirmText: t('page.chat.relogin'),
      success: () => {
        userStore.logout()
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    })
    return
  }

  if (!canReconnect.value) return

  // 手动重连
  uni.showLoading({ title: t('page.chat.connecting'), mask: true })
  const success = chatStore.reconnect()
  if (success) {
    setTimeout(() => {
      uni.hideLoading()
      if (chatStore.isRealtimeConnected) {
        uni.showToast({ title: t('page.chat.connection_success'), icon: 'success' })
      } else {
        uni.showToast({ title: t('page.chat.connecting'), icon: 'none' })
      }
    }, 800)
  } else {
    uni.hideLoading()
    uni.showToast({ title: t('page.chat.connection_failed'), icon: 'none' })
  }
}

onShow(async () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  await activeSubjectStore.ensureLoaded()
  if (!activeSubjectStore.currentSubject) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  chatStore.setCurrentActiveSubject(activeSubjectStore.currentSubject)
  loadPins()
  uni.setNavigationBarTitle({ title: t('page.chat.title') })
  void fetchConversations()
})

watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchConversations()
  }, 220) as unknown as number
})

watch(
  () => chatStore.conversations,
  (list) => {
    conversations.value = [...list]
  },
  { deep: true }
)
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

/* 连接状态栏 */
.connection-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: rgba(239, 68, 68, 0.1);
  border-bottom: 1rpx solid rgba(239, 68, 68, 0.2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.connection-bar:active {
  opacity: 0.8;
}

.connection-bar.kicked {
  background: rgba(245, 158, 11, 0.1);
  border-bottom-color: rgba(245, 158, 11, 0.2);
}

.connection-indicator {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #ef4444;
  animation: pulse 2s infinite;
}

.connection-bar.kicked .connection-indicator {
  background: #f59e0b;
  animation: none;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.connection-text {
  flex: 1;
  font-size: 28rpx;
  color: #ef4444;
  font-weight: 500;
}

.connection-bar.kicked .connection-text {
  color: #f59e0b;
}

.connection-action {
  font-size: 26rpx;
  color: #3b82f6;
  font-weight: 500;
  padding: 8rpx 16rpx;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 8rpx;
}

.list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  max-width: var(--page-content-max);
  margin: 0 auto;
  padding: 16rpx 20rpx var(--page-pad-bottom-tabbar-loose);
  box-sizing: border-box;
}

.list-scroll.with-connection-bar {
  padding-top: 0;
}

.native-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.conv-wrap {
  margin: 0 0 12rpx;
  border-radius: var(--radius-list-row);
  background: var(--c-list-row);
  border: 1rpx solid var(--c-border);
  box-shadow: none;
}

.conv-wrap.pinned {
  background: var(--c-surface-muted);
  border-color: var(--c-border-strong);
}

.conv-inner {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: var(--list-row-height);
  padding: 16rpx;
  box-sizing: border-box;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: var(--avatar-size);
  height: var(--avatar-size);
  border-radius: var(--radius-avatar);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
}

.avatar-letter {
  font-size: 28rpx;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0;
}

.online-dot {
  position: absolute;
  right: -2rpx;
  bottom: -2rpx;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: var(--c-success);
  border: 3rpx solid #fff;
  box-shadow: none;
}

.body {
  flex: 1;
  min-width: 0;
}

.row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.name-block {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge-ai {
  flex-shrink: 0;
  font-size: 18rpx;
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
  color: #4f46e5;
  border: 1rpx solid rgba(79, 70, 229, 0.22);
  background: rgba(79, 70, 229, 0.08);
}

.time {
  flex-shrink: 0;
  font-size: 22rpx;
  color: var(--c-muted);
}

.row-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.preview {
  flex: 1;
  font-size: 24rpx;
  color: var(--c-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread {
  flex-shrink: 0;
  min-width: 36rpx;
  padding: 4rpx 12rpx;
  border-radius: var(--radius-pill);
  background: var(--c-primary);
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  text-align: center;
}

.pin-btn {
  min-width: 68rpx;
  height: 52rpx;
  padding: 0 10rpx;
  margin: 0;
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-sm);
  background: #fff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pin-btn:active {
  opacity: 0.7;
}

.pin-btn.on {
  background: var(--c-accent);
  border-color: var(--c-border-strong);
}

.pin-glyph {
  font-size: 20rpx;
  opacity: 0.9;
  font-weight: 700;
  color: var(--c-muted);
}

.pin-btn.on .pin-glyph {
  color: var(--c-primary);
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 48rpx;
  font-size: 26rpx;
}
</style>
