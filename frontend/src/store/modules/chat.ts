import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { t } from '@/utils/i18n'
import type { ConversationSummary } from '@/api/modules/conversation'
import type { BaseMessage, ChatMessagePayload, SessionKickedPayload } from '@/types/websocket'
import { wsClient } from '@/utils/websocket'
import { useUserStore } from './user'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<ConversationSummary[]>([])
  const unreadCount = ref(0)
  const activeConversationId = ref<number | null>(null)
  const currentPrincipalHumanId = ref<number | null>(null)
  const isRealtimeConnected = ref(false)
  const wsListenerId = ref<string | null>(null)
  const isSessionKicked = ref(false)

  const totalUnread = computed(() => {
    return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  })

  const setConversations = (list: ConversationSummary[]) => {
    conversations.value = Array.isArray(list) ? [...list] : []
  }

  const updateUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  const setCurrentPrincipalHumanId = (id: number | null) => {
    currentPrincipalHumanId.value = id
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
    const senderSubjectId = Number(message.senderSubjectId)
    const fromSelf = currentPrincipalHumanId.value != null
      && senderSubjectId === currentPrincipalHumanId.value
      && message.senderSubjectType === 'HUMAN'
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

  /**
   * 处理被踢下线
   */
  const handleSessionKicked = (payload: SessionKickedPayload) => {
    isSessionKicked.value = true
    stopRealtime()

    // 清除用户登录状态
    const userStore = useUserStore()
    userStore.logout()

    // 显示提示并跳转到登录页
    uni.showModal({
      title: t('common.notice', '登录提示'),
      content: payload.reason || t('auth.session_kicked', '您的账号已在其他设备登录'),
      showCancel: false,
      confirmText: t('auth.relogin', '重新登录'),
      success: () => {
        isSessionKicked.value = false
        uni.reLaunch({ url: '/pages/auth/login' })
      },
    })
  }

  const startRealtime = (token: string) => {
    if (!token) return
    // 如果被踢下线，不再自动重连
    if (isSessionKicked.value) return

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
        onSessionKicked: (payload) => {
          handleSessionKicked(payload)
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

  /**
   * 手动重连 WebSocket
   */
  const reconnect = () => {
    if (isSessionKicked.value) {
      return false
    }
    const token = useUserStore().token
    if (!token) {
      return false
    }
    stopRealtime()
    setTimeout(() => {
      startRealtime(token)
    }, 300)
    return true
  }

  return {
    conversations,
    unreadCount,
    totalUnread,
    activeConversationId,
    isRealtimeConnected,
    isSessionKicked,
    setConversations,
    updateUnreadCount,
    setCurrentPrincipalHumanId,
    setActiveConversation,
    markConversationRead,
    startRealtime,
    stopRealtime,
    reconnect,
    handleSessionKicked,
  }
})
