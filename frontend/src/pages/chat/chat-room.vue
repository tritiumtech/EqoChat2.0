<template>
  <view class="page">
    <z-paging
      ref="paging"
      v-model="messages"
      :auto="false"
      :fixed="false"
      :use-chat-record-mode="true"
      :auto-hide-keyboard-when-chat="true"
      :auto-adjust-position-when-chat="true"
      :auto-to-bottom-when-chat="true"
      :show-default-loading-page="false"
      :show-default-empty-view="false"
      :show-default-loading-more="false"
      :loading-more-enabled="false"
      :refresher-enabled="false"
      :chat-loading-more-default-as-loading="false"
      @cellStyleChange="onCellStyleChange"
      @scrolltoupper="loadMoreHistory"
      class="paging-root"
    >
      <template #top>
        <view class="header">
          <view class="header-row">
            <view class="back-wrap" @click="goBack">
              <text class="back-icon">‹</text>
            </view>
            <text class="header-title">{{ title }}</text>
            <view class="right-placeholder" />
          </view>
        </view>
      </template>

      <view v-if="loading" class="state" :style="chatCellStyle">{{ t('common.loading') }}</view>
      <view v-else-if="messages.length === 0" class="state" :style="chatCellStyle">{{ t('common.empty_conversation') }}</view>
      <view v-for="item in messages" :key="item.id" :id="`msg-${item.id}`" :style="chatCellStyle">
        <MessageBubble
          :is-self="item.isSelf"
          :is-agent="item.isAgent"
          :content="item.content"
          :message-type="item.messageType"
          :attachment="item.attachment"
          :author="item.isSelf ? t('common.you') : title"
          :time="formatTime(item.createTime)"
          :local="item.local"
          :failed="item.failed"
          :avatar-text="title.slice(0, 1)"
          :sending-text="t('common.sending')"
          :retry-text="t('common.retry')"
          @click="retrySend(item)"
        />
      </view>

      <template #bottom>
        <view class="input-bar">
          <view v-if="attachments.length > 0" class="attach-chips">
            <view
              v-for="a in attachments"
              :key="a.id"
              class="chip"
            >
              <text class="chip-name">{{ a.attachment.fileName }}</text>
              <view class="chip-x" @click="removeAttachment(a.id)">
                <text>×</text>
              </view>
            </view>
          </view>

          <view class="input-row">
            <view class="circle-btn" @click="toggleVoice">
              <text class="btn-glyph">🎤</text>
            </view>

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

            <view class="circle-btn" @click="toggleEmoji">
              <text class="btn-glyph">😊</text>
            </view>

            <button
              v-if="canSend"
              class="btn-send"
              :disabled="!canSend"
              @click="send"
            >
              <text class="btn-send-arrow">➤</text>
            </button>

            <view v-else class="circle-btn" @click="toggleAttachments">
              <text class="btn-glyph">＋</text>
            </view>
          </view>

          <view v-if="showEmoji" class="popover emoji-pop">
            <view class="emoji-grid">
              <view
                v-for="e in EMOJIS"
                :key="e"
                class="emoji-btn"
                @click="appendEmoji(e)"
              >
                <text>{{ e }}</text>
              </view>
            </view>
          </view>

          <view v-if="showAttachMenu" class="popover attach-pop">
            <view class="attach-item" @click="pickPhoto">
              <text class="attach-ico">🖼️</text>
              <text class="attach-txt">{{ t('page.chat.attach_photo') }}</text>
            </view>
            <view class="attach-item" @click="pickCamera">
              <text class="attach-ico">📷</text>
              <text class="attach-txt">{{ t('page.chat.attach_camera') }}</text>
            </view>
            <view class="attach-item" @click="pickFile">
              <text class="attach-ico">📄</text>
              <text class="attach-txt">{{ t('page.chat.attach_file') }}</text>
            </view>
            <view class="attach-item" @click="pickLocation">
              <text class="attach-ico">📍</text>
              <text class="attach-txt">{{ t('page.chat.attach_location') }}</text>
            </view>
          </view>
        </view>
      </template>
    </z-paging>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { useWebSocket } from '@/composables/useWebSocket'
import { wsClient } from '@/utils/websocket'
import { conversationApi, type MessageItem, type MessagePageResponse } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import type { ChatMessagePayload } from '@/types/websocket'

interface ChatMessage {
  id: string
  senderId: number
  senderType: string
  isAgent: boolean
  content: string
  createTime: string
  isSelf: boolean
  local?: boolean
  failed?: boolean
  messageType?: string
  attachment?: {
    fileName?: string
    fileSize?: string
    fileType?: string
    downloadUrl?: string
  }
}

const userStore = useUserStore()
const { t } = useI18n()

const parseUserIdFromToken = (token: string | null | undefined): number | null => {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length < 2) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4))
    const payloadStr = atob(base64 + pad)
    const payload = JSON.parse(payloadStr)
    const raw = payload.userId ?? payload.sub ?? payload.id ?? payload.uid
    if (raw == null) return null
    const n = Number(raw)
    if (Number.isNaN(n)) return null
    return n
  } catch {
    return null
  }
}

const currentUserId = computed(() => {
  const infoId = userStore.userInfo?.id
  if (infoId != null) {
    const n = Number(infoId)
    if (!Number.isNaN(n)) return n
  }
  const wsUserId = Number((wsClient as any).userId)
  if (!Number.isNaN(wsUserId) && wsUserId > 0) return wsUserId
  const token = userStore.token
  const fromToken = parseUserIdFromToken(token)
  return fromToken ?? 0
})

const conversationId = ref<number>(0)
const title = ref(t('common.conversation'))
const inputText = ref('')
const paging = ref<any>(null)
const chatCellStyle = ref<Record<string, string>>({})
const messages = ref<ChatMessage[]>([])
const historyPageSize = 50
const historyCursor = ref<number | null>(null)
const historyHasMore = ref(true)
const historyLoadingMore = ref(false)
const historyInitialized = ref(false)
const attachments = ref<Array<{
  id: string
  messageType: string
  attachment: {
    fileName: string
    fileSize: string
    fileType: string
    downloadUrl?: string
  }
}>>([])
const messageIdSet = new Set<string>()
const hasConversation = computed(() => conversationId.value > 0)
const canSend = computed(() => {
  if (!hasConversation.value) return false
  return inputText.value.trim().length > 0 || attachments.value.length > 0
})
const loading = ref(false)
const isPageVisible = ref(true)
const pendingTimers = new Map<string, number>()

const showEmoji = ref(false)
const showAttachMenu = ref(false)

const EMOJIS = ['😀', '😂', '❤️', '👍', '🎉', '🔥', '💯', '✨', '🙏', '💪', '🎯', '🚀']

const formatBytes = (bytes: number) => {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0B'
  const units = [
    t('page.chat.file_size_unit_b'),
    t('page.chat.file_size_unit_kb'),
    t('page.chat.file_size_unit_mb'),
    t('page.chat.file_size_unit_gb'),
  ]
  let v = bytes
  let idx = 0
  while (v >= 1024 && idx < units.length - 1) {
    v = v / 1024
    idx++
  }
  const fixed = idx === 0 ? 0 : 1
  return `${v.toFixed(fixed)}${units[idx]}`
}

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
  if (showEmoji.value) showAttachMenu.value = false
}

const toggleAttachments = () => {
  showAttachMenu.value = !showAttachMenu.value
  if (showAttachMenu.value) showEmoji.value = false
}

const toggleVoice = () => {
  // UI占位：当前语音发送未在 Phase 1 实现
  showComingSoon()
}

const appendEmoji = (emoji: string) => {
  inputText.value += emoji
  showEmoji.value = false
}

const removeAttachment = (id: string) => {
  attachments.value = attachments.value.filter((a) => a.id !== id)
}

const closePopovers = () => {
  showEmoji.value = false
  showAttachMenu.value = false
}

const onCellStyleChange = (style: Record<string, string>) => {
  chatCellStyle.value = style || {}
}

const normalizeMessagePage = (
  raw: MessagePageResponse | MessageItem[]
): MessagePageResponse => {
  if (Array.isArray(raw)) {
    const items = raw
    const oldest = items.length > 0 ? Number(items[items.length - 1].id) : undefined
    return {
      items,
      total: items.length,
      hasMore: items.length >= historyPageSize,
      nextLastMessageId: oldest,
    }
  }
  const items = Array.isArray(raw?.items) ? raw.items : []
  const oldest = items.length > 0 ? Number(items[items.length - 1].id) : undefined
  return {
    items,
    total: Number(raw?.total || items.length),
    hasMore: Boolean(raw?.hasMore),
    nextLastMessageId: raw?.nextLastMessageId ?? oldest,
  }
}

const toChatMessage = (item: MessageItem): ChatMessage => ({
  id: String(item.id),
  senderId: item.senderId,
  senderType: item.senderType || 'USER',
  isAgent: String(item.senderType || '').toUpperCase() === 'AGENT',
  content: item.content || '',
  createTime: item.createTime,
  isSelf: item.senderId === currentUserId.value,
  messageType: item.messageType || 'TEXT',
  attachment: item.attachment,
})

const updateHistoryState = (page: MessagePageResponse, mapped: ChatMessage[]) => {
  historyHasMore.value = Boolean(page.hasMore)
  if (page.nextLastMessageId != null) {
    const next = Number(page.nextLastMessageId)
    historyCursor.value = Number.isNaN(next) ? null : next
  } else {
    const oldest = mapped[mapped.length - 1]
    historyCursor.value = oldest ? Number(oldest.id) : null
  }
  // 同步z-paging顶部加载状态，避免仍显示“点击加载更多”
  paging.value?.completeByNoMore?.(messages.value as unknown as MessageItem[], !historyHasMore.value)
}

const appendChatMessage = (message: ChatMessage, scrollToBottom = true) => {
  messageIdSet.add(message.id)
  const pagingRef = paging.value
  if (pagingRef?.addChatRecordData) {
    pagingRef.addChatRecordData(message, scrollToBottom, true)
    return
  }
  messages.value.push(message)
  if (scrollToBottom) {
    nextTick(() => {
      pagingRef?.scrollToBottom?.(true)
    })
  }
}

const lastSegment = (p: string) => {
  const parts = (p || '').split('/')
  return parts[parts.length - 1] || ''
}

const pickPhoto = () => {
  closePopovers()
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed', 'original'],
    sourceType: ['album'],
    success: (res) => {
      const tf = res.tempFiles?.[0] as any
      const filePath = String(tf?.path || '')
      const fileSize = typeof tf?.size === 'number' ? formatBytes(tf.size) : '-'
      const fileName = tf?.name
        ? String(tf.name)
        : lastSegment(filePath) || `${t('page.chat.default_photo_name')}_${Date.now()}`
      attachments.value = [
        {
          id: `att-${Date.now()}`,
          messageType: 'IMAGE',
          attachment: {
            fileName,
            fileSize,
            fileType: 'image/*',
          },
        },
      ]
    },
  })
}

const pickCamera = () => {
  closePopovers()
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed', 'original'],
    sourceType: ['camera'],
    success: (res) => {
      const tf = res.tempFiles?.[0] as any
      const filePath = String(tf?.path || '')
      const fileSize = typeof tf?.size === 'number' ? formatBytes(tf.size) : '-'
      const fileName = tf?.name
        ? String(tf.name)
        : lastSegment(filePath) || `${t('page.chat.default_camera_name')}_${Date.now()}`
      attachments.value = [
        {
          id: `att-${Date.now()}`,
          messageType: 'IMAGE',
          attachment: {
            fileName,
            fileSize,
            fileType: 'image/*',
          },
        },
      ]
    },
  })
}

const pickFile = () => {
  closePopovers()
  uni.chooseFile({
    count: 1,
    success: (res) => {
      const tf = res.tempFiles?.[0] as any
      const filePath = String(tf?.path || '')
      const fileSize = typeof tf?.size === 'number' ? formatBytes(tf.size) : '-'
      const fileName = tf?.name
        ? String(tf.name)
        : lastSegment(filePath) || `${t('page.chat.default_file_name')}_${Date.now()}`
      const fileType = tf?.type ? String(tf.type) : 'application/octet-stream'
      attachments.value = [
        {
          id: `att-${Date.now()}`,
          messageType: 'FILE',
          attachment: {
            fileName,
            fileSize,
            fileType,
          },
        },
      ]
    },
  })
}

const pickLocation = () => {
  closePopovers()
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

const showComingSoon = () => {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

const { isConnected, sendMessage, sendReadReceipt } = useWebSocket({
  autoConnect: true,
  onChatMessage: (payload, message) => {
    if (payload.conversationId !== String(conversationId.value)) return
    const id = String(message.id)
    if (messageIdSet.has(id)) return
    const senderId = Number(message.senderId)
    const createTime = message.timestamp
    const mt = (payload.messageType || 'TEXT').toString().toUpperCase()
    const attachment = parseAttachmentFromPayload(payload)
    const replaced = replaceLocalMessage(senderId, payload, createTime, id)
    if (!replaced) {
      appendChatMessage({
        id,
        senderId,
        senderType: String((message as any)?.senderType || 'USER').toString(),
        isAgent: String((message as any)?.senderType || '').toUpperCase() === 'AGENT',
        content: payload.content || '',
        createTime,
        isSelf: senderId === currentUserId.value,
        messageType: mt,
        attachment,
      })
    }
    scrollToBottom()
    if (isPageVisible.value && senderId !== currentUserId.value) {
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
  historyInitialized.value = false
  try {
    messageIdSet.clear()
    historyCursor.value = null
    historyHasMore.value = true
    historyLoadingMore.value = false
    const pageRaw = await conversationApi.getMessages(conversationId.value, { limit: historyPageSize })
    const page = normalizeMessagePage(pageRaw as unknown as MessagePageResponse | MessageItem[])
    const mapped = (page.items || []).map(toChatMessage)
    mapped.forEach((item) => messageIdSet.add(item.id))
    messages.value = mapped
    updateHistoryState(page, mapped)
    const latest = messages.value[0]
    if (isPageVisible.value && latest && latest.senderId !== currentUserId.value) {
      sendReadReceipt(String(conversationId.value), latest.id)
    }
    scrollToBottom()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
    historyInitialized.value = true
  }
}

const loadMoreHistory = async () => {
  if (!historyInitialized.value) return
  if (!conversationId.value || !historyHasMore.value || historyLoadingMore.value) return
  if (historyCursor.value == null) {
    historyHasMore.value = false
    return
  }
  historyLoadingMore.value = true
  try {
    const pageRaw = await conversationApi.getMessages(conversationId.value, {
      lastMessageId: historyCursor.value,
      limit: historyPageSize,
    })
    const page = normalizeMessagePage(pageRaw as unknown as MessagePageResponse | MessageItem[])
    const mapped = (page.items || [])
      .map(toChatMessage)
      .filter((item) => !messageIdSet.has(item.id))
    mapped.forEach((item) => messageIdSet.add(item.id))
    if (mapped.length > 0) {
      messages.value = [...messages.value, ...mapped]
    }
    updateHistoryState(page, mapped.length > 0 ? mapped : messages.value)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    historyLoadingMore.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    paging.value?.scrollToBottom?.(false)
  })
}

const send = async () => {
  if (!conversationId.value) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }

  const text = inputText.value.trim()
  const hasText = text.length > 0
  const hasAttachment = attachments.value.length > 0
  if (!hasText && !hasAttachment) return

  const pendingAttachment = attachments.value[0]
  const messageType = hasAttachment ? pendingAttachment.messageType : 'TEXT'
  const metadata =
    messageType !== 'TEXT' && pendingAttachment
      ? {
          fileName: pendingAttachment.attachment.fileName,
          fileSize: pendingAttachment.attachment.fileSize,
          fileType: pendingAttachment.attachment.fileType,
        }
      : undefined

  const localId = `local-${Date.now()}`
  const localMessage: ChatMessage = {
    id: localId,
    senderId: currentUserId.value,
    senderType: 'USER',
    isAgent: false,
    content: hasText ? text : '',
    createTime: new Date().toISOString(),
    isSelf: true,
    local: true,
    messageType,
    attachment: pendingAttachment?.attachment,
  }
  appendChatMessage(localMessage)

  const wsContent = hasText ? text : ''
  const ok =
    isConnected.value &&
    sendMessage(
      String(conversationId.value),
      wsContent,
      messageType,
      metadata ? { metadata } : undefined
    )

  if (ok) {
    const timer = setTimeout(() => {
      const target = messages.value.find(item => item.id === localId && item.local)
      if (target) {
        sendHttpMessage(localId, wsContent, messageType, metadata)
      }
    }, 4000) as unknown as number
    pendingTimers.set(localId, timer)
  } else {
    await sendHttpMessage(localId, wsContent, messageType, metadata)
  }

  inputText.value = ''
  attachments.value = []
  showEmoji.value = false
  showAttachMenu.value = false
}

const sendHttpMessage = async (
  localId: string,
  content: string,
  messageType: string,
  metadata?: Record<string, unknown>
) => {
  try {
    const saved = await conversationApi.sendMessage(conversationId.value, {
      content,
      messageType,
      metadata,
    })
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

const parseAttachmentFromPayload = (payload: ChatMessagePayload) => {
  const mt = (payload.messageType || 'TEXT').toString().toUpperCase()
  if (mt === 'TEXT') return undefined
  const md: any = payload.metadata
  if (!md || typeof md !== 'object') return undefined
  return {
    fileName: md.fileName != null ? String(md.fileName) : undefined,
    fileSize: md.fileSize != null ? String(md.fileSize) : undefined,
    fileType: md.fileType != null ? String(md.fileType) : undefined,
    downloadUrl: md.downloadUrl != null ? String(md.downloadUrl) : undefined,
  }
}

const replaceLocalMessage = (
  senderId: number,
  payload: ChatMessagePayload,
  createTime: string,
  realId: string
) => {
  if (senderId !== currentUserId.value) return false

  const mt = (payload.messageType || 'TEXT').toString().toUpperCase()
  const parsedAttachment = parseAttachmentFromPayload(payload)
  const payloadContent = payload.content || ''

  const payloadFileName =
    parsedAttachment?.fileName != null ? String(parsedAttachment.fileName) : ''

  const targetIndex = messages.value.findIndex((item) => {
    if (!item.local || !item.isSelf) return false
    const itemMt = (item.messageType || 'TEXT').toString().toUpperCase()
    if (itemMt !== mt) return false

    const diff = Math.abs(
      new Date(item.createTime).getTime() - new Date(createTime).getTime()
    )
    if (diff >= 10000) return false

    if (mt === 'TEXT') {
      return (item.content || '') === payloadContent
    }

    const localFileName =
      item.attachment?.fileName != null ? String(item.attachment.fileName) : ''

    if (!payloadFileName) return false
    if (!localFileName) return false
    return payloadFileName === localFileName
  })

  if (targetIndex < 0) return false
  clearPendingTimer(messages.value[targetIndex].id)
  messageIdSet.delete(messages.value[targetIndex].id)
  messages.value[targetIndex] = {
    ...messages.value[targetIndex],
    id: realId,
    createTime,
    local: false,
    failed: false,
    senderType: 'USER',
    isAgent: false,
    content: payloadContent,
    messageType: mt,
    attachment: parsedAttachment,
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
    failed: false,
    senderType: saved.senderType || 'USER',
    isAgent: String(saved.senderType || '').toUpperCase() === 'AGENT',
    messageType: (saved.messageType || 'TEXT').toString(),
    attachment: saved.attachment,
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
  if (!item.failed) return
  const localId = `retry-${Date.now()}`
  const mt = (item.messageType || 'TEXT').toString().toUpperCase()
  const metadata = mt !== 'TEXT' && item.attachment
    ? {
        fileName: item.attachment.fileName,
        fileSize: item.attachment.fileSize,
        fileType: item.attachment.fileType,
      }
    : undefined
  const content = mt === 'TEXT' ? item.content || '' : ''
  appendChatMessage({
    id: localId,
    senderId: currentUserId.value,
    senderType: 'USER',
    isAgent: false,
    content,
    createTime: new Date().toISOString(),
    isSelf: true,
    local: true,
    messageType: mt,
    attachment: item.attachment,
  })
  sendHttpMessage(localId, content, mt, metadata)
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
  isPageVisible.value = true
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

onHide(() => {
  isPageVisible.value = false
})

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.redirectTo({ url: '/pages/chat/chat-list' })
  }
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  overflow: hidden;
}

.paging-root {
  flex: 1;
}

.header {
  padding: calc(12rpx + env(safe-area-inset-top)) 16rpx 12rpx;
  background: rgba(255, 255, 255, 0.84);
  color: var(--c-ink);
  backdrop-filter: blur(18rpx);
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.04);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
}

.back-icon {
  color: var(--c-ink);
  font-size: 32rpx;
  line-height: 1;
}

.header-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--c-ink);
  line-height: 1;
  max-width: 60%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.right-placeholder {
  width: 72rpx;
  height: 72rpx;
}

.state {
  text-align: center;
  color: #8f8a98;
  margin-top: 32rpx;
  font-size: 24rpx;
}

.input-bar {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 8rpx 12rpx 8rpx;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1rpx solid rgba(26, 23, 32, 0.08);
  box-shadow: 0 -6rpx 20rpx rgba(27, 21, 44, 0.06);
  box-sizing: border-box;
}

.input-wrap {
  flex: 1;
  background: rgba(246, 242, 238, 0.65);
  border-radius: 999rpx;
  border: 1rpx solid rgba(26, 23, 32, 0.08);
  padding: 6rpx 16rpx;
}

.input {
  width: 100%;
  min-height: 40rpx;
  max-height: 110rpx;
  background: transparent;
  padding: 6rpx;
  font-size: 25rpx;
  color: var(--c-ink);
}

.btn-send {
  width: 70rpx;
  height: 70rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  color: #fff;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-send[disabled] {
  opacity: 0.6;
}

.btn-send-arrow {
  color: #fff;
  font-size: 26rpx;
  line-height: 1;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  width: 100%;
}

.attach-chips {
  display: flex;
  gap: 12rpx;
  overflow-x: auto;
  padding-bottom: 2rpx;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 14rpx;
  border-radius: var(--radius-pill);
  background: rgba(246, 242, 238, 0.75);
  border: 1rpx solid var(--c-border);
  white-space: nowrap;
}

.chip-name {
  max-width: 220rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 22rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.chip-x {
  width: 34rpx;
  height: 34rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 23, 32, 0.06);
}

.circle-btn {
  width: 70rpx;
  height: 70rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(26, 23, 32, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 24rpx rgba(27, 21, 44, 0.06);
}

.circle-btn.dim {
  opacity: 0.65;
}

.btn-glyph {
  font-size: 26rpx;
  color: var(--c-ink);
  line-height: 1;
}

.popover {
  position: absolute;
  left: 20rpx;
  right: 20rpx;
  bottom: 118rpx;
  background: rgba(255, 255, 255, 0.98);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  padding: 16rpx 16rpx;
  box-sizing: border-box;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.emoji-btn {
  width: 60rpx;
  height: 60rpx;
  border-radius: var(--radius-md);
  background: rgba(246, 242, 238, 0.75);
  border: 1rpx solid var(--c-border);
  display: flex;
  align-items: center;
  justify-content: center;
}

.attach-pop {
  left: auto;
  right: 24rpx;
  width: 280rpx;
  bottom: 118rpx;
  padding: 10rpx 10rpx;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 14rpx 12rpx;
  border-radius: var(--radius-md);
  border: 1rpx solid transparent;
}

.attach-item:active {
  background: rgba(3, 2, 19, 0.06);
  border-color: rgba(3, 2, 19, 0.12);
}

.attach-ico {
  font-size: 30rpx;
}

.attach-txt {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--c-ink);
}
</style>
