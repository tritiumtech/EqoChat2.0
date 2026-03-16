<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="title">{{ t('page.chat.title') }}</text>
          <text class="subtitle">{{ t('page.chat.subtitle') }}</text>
        </view>
        <view class="chip">{{ conversations.length }}</view>
      </view>
      <view class="new-chat">
        <input v-model="newUserId" class="input" type="number" :placeholder="t('placeholder.new_chat')" />
        <button class="btn" @click="createConversation">{{ t('action.create') }}</button>
      </view>
    </view>

    <view class="list">
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="conversations.length === 0" class="state">
        <EmptyState :title="t('common.empty_conversation')" icon="💬" />
      </view>
      <ConversationItem
        v-else
        v-for="item in conversations"
        :key="item.id"
        :title="item.title"
        :subtitle="item.lastMessage"
        :time="formatTime(item.lastMessageAt)"
        :unread-count="item.unreadCount"
        :fallback-title="t('common.conversation')"
        :fallback-subtitle="t('page.chat.no_message')"
        @click="openConversation(item)"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { conversationApi, type ConversationSummary } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'
import EmptyState from '@/components/EmptyState.vue'
import ConversationItem from '@/components/chat/ConversationItem.vue'

const conversations = ref<ConversationSummary[]>([])
const loading = ref(false)
const newUserId = ref('')
const userStore = useUserStore()
const { t } = useI18n()

const fetchConversations = async () => {
  loading.value = true
  try {
    conversations.value = await conversationApi.listConversations()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const createConversation = async () => {
  if (!newUserId.value) {
    uni.showToast({ title: t('placeholder.new_chat'), icon: 'none' })
    return
  }
  try {
    const data = await conversationApi.createConversation({ targetUserId: Number(newUserId.value) })
    newUserId.value = ''
    openConversation(data)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.create_failed'), icon: 'none' })
  }
}

const openConversation = (item: ConversationSummary) => {
  const conversationId = Number((item as any)?.id ?? (item as any)?.conversationId)
  if (!conversationId || Number.isNaN(conversationId)) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  const title = encodeURIComponent(item.title || '会话')
  uni.navigateTo({
    url: `/pages/chat/chat-room?conversationId=${conversationId}&title=${title}`
  })
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${hours}:${minutes}`
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.chat.title') })
  fetchConversations()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, var(--c-bg) 0%, #fff7f0 100%);
}

.hero {
  background: #15131c;
  color: #fff;
  border-radius: var(--radius-xl);
  padding: 28rpx 24rpx;
  box-shadow: var(--c-shadow);
  margin-bottom: 24rpx;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  display: block;
}

.subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
  display: block;
}

.chip {
  min-width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.new-chat {
  display: flex;
  gap: 16rpx;
  background: rgba(255, 255, 255, 0.1);
  padding: 16rpx;
  border-radius: var(--radius-lg);
}

.input {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16rpx;
  padding: 18rpx 20rpx;
  font-size: 26rpx;
  color: var(--c-ink);
}

.btn {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #ff7a59 0%, #ff9f6b 100%);
  color: #1a1720;
  font-weight: 700;
  font-size: 24rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.item {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 22rpx;
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ffddb3 0%, #ffd3c6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7a3b2a;
  font-size: 30rpx;
  font-weight: 700;
  margin-right: 18rpx;
}

.content {
  flex: 1;
}

.name {
  font-size: 30rpx;
  color: var(--c-ink);
  font-weight: 700;
  display: block;
}

.message {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
  display: block;
}

.meta {
  text-align: right;
  min-width: 120rpx;
}

.time {
  font-size: 22rpx;
  color: #9b98a5;
  display: block;
  margin-bottom: 8rpx;
}

.badge {
  display: inline-block;
  min-width: 36rpx;
  padding: 4rpx 12rpx;
  background: #ff7a59;
  color: #fff;
  font-size: 20rpx;
  border-radius: var(--radius-pill);
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 40rpx;
  font-size: 24rpx;
}
</style>
