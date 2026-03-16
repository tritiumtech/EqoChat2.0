<template>
  <view class="page">
    <view class="header">
      <view class="header-left">
        <view class="back-wrap" @click="goBack">
          <text class="back-icon">‹</text>
        </view>
        <view class="avatar">{{ title.slice(0, 1) }}</view>
        <view class="title-block">
          <text class="title">{{ title }}</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="message-list"
      scroll-y
      :scroll-into-view="scrollIntoView"
    >
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="messages.length === 0" class="state">{{ t('common.empty_conversation') }}</view>
      <view
        v-for="item in messages"
        :key="item.id"
        :id="`msg-${item.id}`"
      >
        <MessageBubble
          :is-self="item.isSelf"
          :content="item.content"
          :time="formatTime(item.createTime)"
          :local="item.local"
          :failed="item.failed"
          :avatar-text="title.slice(0, 1)"
          :sending-text="t('common.sending')"
          :retry-text="t('common.retry')"
          @click="retrySend(item)"
        />
      </view>
      <view id="bottom" />
    </scroll-view>

    <view class="input-bar">
      <view class="input-wrap">
        <textarea
          v-model="inputText"
          class="input"
          auto-height
          :maxlength="5000"
          :placeholder="t('placeholder.message')"
          confirm-type="send"
          cursor-spacing="24"
          @confirm="send"
        />
      </view>
      <button class="btn-send" :disabled="!canSend" @click="send">{{ t('common.send') }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { useWebSocket } from '@/composables/useWebSocket'
import { conversationApi, type MessageItem } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'
import MessageBubble from '@/components/chat/MessageBubble.vue'

interface ChatMessage {
  id: string
  senderId: number
  content: string
  createTime: string
  isSelf: boolean
  local?: boolean
  failed?: boolean
}

const userStore = useUserStore()
const { t } = useI18n()
const conversationId = ref<number>(0)
const title = ref('会话')
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const scrollIntoView = ref('bottom')
const messageIdSet = new Set<string>()
const hasConversation = computed(() => conversationId.value > 0)
const canSend = computed(() => hasConversation.value && inputText.value.trim().length > 0)
const loading = ref(false)
const pendingTimers = new Map<string, number>()

const { isConnected, sendMessage, sendReadReceipt } = useWebSocket({
  autoConnect: true,
  onChatMessage: (payload, message) => {
    if (payload.conversationId !== String(conversationId.value)) return
    const id = String(message.id)
    if (messageIdSet.has(id)) return
    const senderId = Number(message.senderId)
    const createTime = message.timestamp
    const replaced = replaceLocalMessage(senderId, payload.content, createTime, id)
    if (!replaced) {
      messageIdSet.add(id)
      messages.value.push({
        id,
        senderId,
        content: payload.content,
        createTime,
        isSelf: senderId === userStore.userInfo?.id
      })
    }
    scrollToBottom()
    if (senderId !== userStore.userInfo?.id) {
      sendReadReceipt(payload.conversationId, id)
    }
  }
})

const loadConversationMeta = async () => {
  if (!conversationId.value) return
  try {
    const meta = await conversationApi.getConversation(conversationId.value)
    if (meta?.title) {
      title.value = meta.title
      uni.setNavigationBarTitle({ title: meta.title })
    }
  } catch (err) {
    // ignore meta load errors, history load will handle
  }
}

const loadHistory = async () => {
  if (!conversationId.value) return
  loading.value = true
  try {
    const list = await conversationApi.getMessages(conversationId.value, { limit: 50 })
    messages.value = list
      .slice()
      .reverse()
      .map((item: MessageItem) => {
        const id = String(item.id)
        messageIdSet.add(id)
        return {
          id,
          senderId: item.senderId,
          content: item.content,
          createTime: item.createTime,
          isSelf: item.senderId === userStore.userInfo?.id
        }
      })
    const latest = messages.value[messages.value.length - 1]
    if (latest && latest.senderId !== userStore.userInfo?.id) {
      sendReadReceipt(String(conversationId.value), latest.id)
    }
    scrollToBottom()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const scrollToBottom = () => {
  scrollIntoView.value = 'bottom'
  nextTick(() => {
    scrollIntoView.value = 'bottom'
  })
}

const send = async () => {
  if (!inputText.value.trim()) return
  if (!conversationId.value) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  const content = inputText.value
  const localId = `local-${Date.now()}`
  const localMessage: ChatMessage = {
    id: localId,
    senderId: userStore.userInfo?.id || 0,
    content,
    createTime: new Date().toISOString(),
    isSelf: true,
    local: true
  }
  messages.value.push(localMessage)
  messageIdSet.add(localId)
  scrollToBottom()

  const ok = isConnected.value && sendMessage(String(conversationId.value), content)
  if (ok) {
    const timer = setTimeout(() => {
      const target = messages.value.find(item => item.id === localId && item.local)
      if (target) {
        sendHttpMessage(localId, content)
      }
    }, 4000) as unknown as number
    pendingTimers.set(localId, timer)
  } else {
    await sendHttpMessage(localId, content)
  }
  inputText.value = ''
}

const sendHttpMessage = async (localId: string, content: string) => {
  try {
    const saved = await conversationApi.sendMessage(conversationId.value, { content })
    updateLocalMessage(localId, saved)
  } catch (err: any) {
    markLocalFailed(localId)
    uni.showToast({ title: err?.message || t('toast.message_failed'), icon: 'none' })
  }
}

const clearPendingTimer = (localId: string) => {
  const timer = pendingTimers.get(localId)
  if (timer) {
    clearTimeout(timer)
    pendingTimers.delete(localId)
  }
}

const replaceLocalMessage = (senderId: number, content: string, createTime: string, realId: string) => {
  if (senderId !== userStore.userInfo?.id) return false
  const targetIndex = messages.value.findIndex((item) => {
    if (!item.local || !item.isSelf) return false
    if (item.content !== content) return false
    const diff = Math.abs(new Date(item.createTime).getTime() - new Date(createTime).getTime())
    return diff < 10000
  })
  if (targetIndex < 0) return false
  clearPendingTimer(messages.value[targetIndex].id)
  messageIdSet.delete(messages.value[targetIndex].id)
  messages.value[targetIndex] = {
    ...messages.value[targetIndex],
    id: realId,
    createTime,
    local: false,
    failed: false
  }
  messageIdSet.add(realId)
  return true
}

const updateLocalMessage = (localId: string, saved: MessageItem) => {
  const targetIndex = messages.value.findIndex(item => item.id === localId)
  if (targetIndex < 0) return
  clearPendingTimer(localId)
  messageIdSet.delete(localId)
  messages.value[targetIndex] = {
    ...messages.value[targetIndex],
    id: String(saved.id),
    createTime: saved.createTime,
    local: false,
    failed: false
  }
  messageIdSet.add(String(saved.id))
}

const markLocalFailed = (localId: string) => {
  const targetIndex = messages.value.findIndex(item => item.id === localId)
  if (targetIndex < 0) return
  clearPendingTimer(localId)
  messages.value[targetIndex] = {
    ...messages.value[targetIndex],
    failed: true
  }
}

const retrySend = (item: ChatMessage) => {
  if (!item.failed || !item.content) return
  const localId = `retry-${Date.now()}`
  messages.value.push({
    id: localId,
    senderId: userStore.userInfo?.id || 0,
    content: item.content,
    createTime: new Date().toISOString(),
    isSelf: true,
    local: true
  })
  messageIdSet.add(localId)
  scrollToBottom()
  sendHttpMessage(localId, item.content)
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${hours}:${minutes}`
}

const resolveConversationId = (query?: Record<string, any>) => {
  const raw = query?.conversationId ?? query?.id
  const id = Number(raw)
  if (!Number.isNaN(id) && id > 0) return id
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  const options = current?.options || {}
  const fallbackRaw = options.conversationId ?? options.id
  const fallbackId = Number(fallbackRaw)
  if (!Number.isNaN(fallbackId) && fallbackId > 0) return fallbackId
  return 0
}

const ensureConversationId = (query?: Record<string, any>) => {
  if (conversationId.value > 0) return true
  const id = resolveConversationId(query)
  if (id) {
    conversationId.value = id
    return true
  }
  return false
}

onLoad((query) => {
  ensureConversationId(query as Record<string, any>)
  if (query?.title) {
    title.value = decodeURIComponent(String(query.title))
  }
})

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  if (!ensureConversationId()) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    setTimeout(() => {
      goBack()
    }, 300)
    return
  }
  if (title.value) {
    uni.setNavigationBarTitle({ title: title.value })
  } else {
    title.value = t('common.conversation')
  }
  loadConversationMeta()
  loadHistory()
})

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.switchTab({ url: '/pages/chat/chat-list' })
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #f5f1ee 0%, #eef3ff 100%);
  overflow: hidden;
}

.header {
  padding: calc(24rpx + env(safe-area-inset-top)) 24rpx 18rpx;
  background: rgba(21, 19, 28, 0.92);
  color: #fff;
  backdrop-filter: blur(14rpx);
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.back-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
}

.back-icon {
  color: #fff;
  font-size: 32rpx;
  line-height: 1;
}

.avatar {
  width: 70rpx;
  height: 70rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #ffb39c 0%, #ff8b6d 100%);
  color: #2a1c1a;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar.small {
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  font-size: 24rpx;
  margin-top: 6rpx;
  background: linear-gradient(135deg, #ffe2d3 0%, #ffd1c4 100%);
  color: #6a3a2b;
}

.title-block {
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
  display: block;
}

/* 连接状态已移除，保留名称即可 */

.message-list {
  flex: 1;
  padding: 24rpx 24rpx 200rpx;
  min-height: 0;
  box-sizing: border-box;
}

.state {
  text-align: center;
  color: #8f8a98;
  margin-top: 32rpx;
  font-size: 24rpx;
}

.message-row {
  margin-bottom: 22rpx;
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
}

.message-row.self {
  justify-content: flex-end;
}

.message-row.other {
  justify-content: flex-start;
}

.bubble {
  max-width: 74%;
  padding: 20rpx 24rpx 16rpx;
  border-radius: 22rpx;
  font-size: 28rpx;
  line-height: 1.5;
  box-shadow: var(--c-shadow-soft);
  position: relative;
}

.bubble.self {
  margin-left: auto;
  background: linear-gradient(135deg, #ffb39c 0%, #ff8b6d 100%);
  color: #2a1c1a;
}

.bubble.other {
  margin-right: auto;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  color: var(--c-ink);
}

.content {
  display: block;
}

.time {
  font-size: 22rpx;
  color: rgba(26, 23, 32, 0.55);
}

.meta-row {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  justify-content: flex-end;
}

.sending {
  font-size: 22rpx;
  color: rgba(26, 23, 32, 0.55);
}

.failed-text {
  font-size: 22rpx;
  color: #ef4444;
}

.input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(26, 23, 32, 0.08);
  box-shadow: 0 -16rpx 36rpx rgba(27, 21, 44, 0.08);
}

.input-wrap {
  flex: 1;
  background: #f3f1f7;
  border-radius: var(--radius-lg);
  padding: 12rpx 16rpx;
}

.input {
  width: 100%;
  min-height: 60rpx;
  max-height: 200rpx;
  background: transparent;
  padding: 8rpx;
  font-size: 28rpx;
  color: var(--c-ink);
}

.btn-send {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, var(--c-primary) 0%, #ff9f6b 100%);
  color: #1a1720;
  font-size: 26rpx;
  font-weight: 700;
}

.btn-send[disabled] {
  opacity: 0.6;
}
</style>
