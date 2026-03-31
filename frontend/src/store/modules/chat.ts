import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ConversationSummary } from '@/api/modules/conversation'
import type { BaseMessage, ChatMessagePayload } from '@/types/websocket'
import { wsClient } from '@/utils/websocket'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<ConversationSummary[]>([])
  const unreadCount = ref(0)
  const activeConversationId = ref<number | null>(null)
  const currentUserId = ref<number | null>(null)
  const isRealtimeConnected = ref(false)
  const wsListenerId = ref<string | null>(null)

  const totalUnread = computed(() => {
    return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  })

  const setConversations = (list: ConversationSummary[]) => {
    conversations.value = Array.isArray(list) ? [...list] : []
  }

  const updateUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  const setCurrentUserId = (id: number | null) => {
    currentUserId.value = id
  }

  const setActiveConversation = (conversationId: number | null) => {
    activeConversationId.value = conversationId
    if (conversationId != null) {
      markConversationRead(conversationId)
    }
  }

  const markConversationRead = (conversationId: number) => {
    const idx = conversations.value.findIndex((x) => Number(x.id) === Number(conversationId))
    if (idx < 0) return
    const target = conversations.value[idx]
    if (!target) return
    if ((target.unreadCount || 0) === 0) return
    conversations.value[idx] = {
      ...target,
      unreadCount: 0,
    }
  }

  const handleIncomingMessage = (payload: ChatMessagePayload, message: BaseMessage) => {
    const conversationId = Number(payload.conversationId)
    if (!conversationId || Number.isNaN(conversationId)) return
    const senderId = Number(message.senderId)
    const fromSelf = currentUserId.value != null && senderId === currentUserId.value
    const isActive = activeConversationId.value != null && Number(activeConversationId.value) === conversationId

    const idx = conversations.value.findIndex((x) => Number(x.id) === conversationId)
    const base: ConversationSummary = idx >= 0
      ? conversations.value[idx]!
      : {
          id: conversationId,
          title: `会话 ${conversationId}`,
          conversationType: 'DIRECT',
          unreadCount: 0,
          lastMessage: '',
          lastMessageAt: '',
          online: false,
        }

    const nextUnread = fromSelf || isActive ? 0 : (base.unreadCount || 0) + 1
    const next: ConversationSummary = {
      ...base,
      lastMessage: payload.content || base.lastMessage || '',
      lastMessageAt: message.timestamp || new Date().toISOString(),
      unreadCount: nextUnread,
    }

    if (idx >= 0) {
      conversations.value[idx] = next
    } else {
      conversations.value.unshift(next)
    }
  }

  const startRealtime = (token: string) => {
    if (!token) return
    wsClient.init(token)
    if (!wsListenerId.value) {
      wsListenerId.value = wsClient.addListener({
        onOpen: () => {
          isRealtimeConnected.value = true
        },
        onClose: () => {
          isRealtimeConnected.value = false
        },
        onError: () => {
          isRealtimeConnected.value = false
        },
        onChatMessage: (payload, message) => {
          handleIncomingMessage(payload, message)
        },
      })
    }
    const status = wsClient.getConnectionStatus()
    if (!status.isConnected && !status.isConnecting) {
      wsClient.connect()
    }
  }

  const stopRealtime = () => {
    if (wsListenerId.value) {
      wsClient.removeListener(wsListenerId.value)
      wsListenerId.value = null
    }
    isRealtimeConnected.value = false
    wsClient.close()
  }

  return {
    conversations,
    unreadCount,
    totalUnread,
    activeConversationId,
    isRealtimeConnected,
    setConversations,
    updateUnreadCount,
    setCurrentUserId,
    setActiveConversation,
    markConversationRead,
    startRealtime,
    stopRealtime,
  }
})

