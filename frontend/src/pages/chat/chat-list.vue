<template>
  <view class="page">
    <view class="head">
      <view class="head-row">
        <text class="screen-title">{{ t('page.chat.title') }}</text>
        <button class="icon-btn" @click="onCompose">
          <text class="icon-edit">✎</text>
        </button>
      </view>
      <SearchBar v-model="searchQuery" :placeholder="t('page.chat.search_placeholder')" />
    </view>

    <!-- #ifdef H5 -->
    <view class="list-scroll native-scroll">
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="filteredSorted.length === 0" class="state">
        <EmptyState :title="t('common.empty_conversation')" icon="💬" />
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
              <text class="avatar-letter">{{ isAgentConversation(item) ? '🤖' : '👤' }}</text>
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
          <button class="pin-btn" :class="{ on: isPinned(item.id) }" @click.stop="togglePin(item.id)">
            <text class="pin-glyph">{{ isPinned(item.id) ? t('page.chat.pinned') : t('page.chat.pin') }}</text>
          </button>
        </view>
      </view>
    </view>
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <scroll-view class="list-scroll" scroll-y>
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="filteredSorted.length === 0" class="state">
        <EmptyState :title="t('common.empty_conversation')" icon="💬" />
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
              <text class="avatar-letter">{{ isAgentConversation(item) ? '🤖' : '👤' }}</text>
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
          <button class="pin-btn" :class="{ on: isPinned(item.id) }" @click.stop="togglePin(item.id)">
            <text class="pin-glyph">{{ isPinned(item.id) ? t('page.chat.pinned') : t('page.chat.pin') }}</text>
          </button>
        </view>
      </view>
    </scroll-view>
    <!-- #endif -->

    <BottomNav />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { conversationApi, type ConversationSummary } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'
import { useChatStore } from '@/store/modules/chat'
import EmptyState from '@/components/EmptyState.vue'
import SearchBar from '@/components/SearchBar.vue'
import BottomNav from '@/components/BottomNav.vue'

const PIN_STORAGE_KEY = 'eqo_chat_pins'

const conversations = ref<ConversationSummary[]>([])
const loading = ref(false)
const searchQuery = ref('')
const pinIds = ref<Set<number>>(new Set())
const userStore = useUserStore()
const chatStore = useChatStore()
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

const isAgentConversation = (item: ConversationSummary) => {
  const ty = (item.conversationType || '').toLowerCase()
  return ty.includes('agent') || ty.includes('ai')
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
    const list = await conversationApi.listConversations(q ? { q } : undefined)
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

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  box-sizing: border-box;
}

.head {
  flex-shrink: 0;
  padding: 20rpx 24rpx 16rpx;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.04);
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.screen-title {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.icon-btn {
  width: 72rpx;
  height: 72rpx;
  padding: 0;
  margin: 0;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-edit {
  font-size: 32rpx;
  color: var(--c-ink);
}

.list-scroll {
  flex: 1;
  height: 0;
  padding: 12rpx 16rpx var(--page-pad-bottom-tabbar-loose);
  box-sizing: border-box;
}

.native-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.conv-wrap {
  margin: 8rpx 4rpx;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.05);
}

.conv-wrap.pinned {
  background: rgba(3, 2, 19, 0.04);
  border: 1rpx solid rgba(3, 2, 19, 0.18);
}

.conv-inner {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 16rpx;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--c-shadow-soft);
}

.avatar-letter {
  font-size: 44rpx;
  font-weight: 600;
  color: #fff;
}

.online-dot {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #10b981;
  border: 3rpx solid #fff;
  box-shadow: 0 6rpx 10rpx rgba(0, 0, 0, 0.12);
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
  min-width: 76rpx;
  height: 56rpx;
  padding: 0 10rpx;
  margin: 0;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.75);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pin-btn.on {
  background: rgba(3, 2, 19, 0.1);
  border-color: rgba(3, 2, 19, 0.18);
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
