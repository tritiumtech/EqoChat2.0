/**
 * WebSocket Composable - Vue3组合式函数 (TypeScript版本)
 * 在组件中使用WebSocket
 */

import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import { wsClient, MessageType, type WebSocketCallbacks, type ConnectionInfo } from '@/utils/websocket'
import { useUserStore } from '@/store/modules/user'
import type { ChatMessagePayload, TypingPayload, ReadReceiptPayload, PresencePayload } from '@/types/websocket'

export interface UseWebSocketReturn {
  isConnected: Ref<boolean>
  isConnecting: Ref<boolean>
  connectionId: Ref<string | null>
  reconnectCount: Ref<number>
  error: Ref<Error | null>
  connect: () => void
  disconnect: () => void
  reconnect: () => void
  sendMessage: (conversationId: string, content: string, messageType?: string, options?: Partial<ChatMessagePayload>) => boolean
  sendTyping: (conversationId: string, isTyping?: boolean) => boolean
  sendReadReceipt: (conversationId: string, messageId: string) => boolean
}

export interface UseWebSocketOptions extends WebSocketCallbacks {
  autoConnect?: boolean
  disconnectOnUnmount?: boolean
}

export function useWebSocket(options: UseWebSocketOptions = {}): UseWebSocketReturn {
  const userStore = useUserStore()
  let listenerId: string | null = null
  
  // 状态
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const connectionId = ref<string | null>(null)
  const reconnectCount = ref(0)
  const error = ref<Error | null>(null)

  /**
   * 连接WebSocket
   */
  const connect = (): void => {
    if (!userStore.token) {
      console.error('未登录，无法连接WebSocket')
      return
    }

    isConnecting.value = true
    error.value = null

    wsClient.init(userStore.token)
    const listeners: WebSocketCallbacks = {
      onOpen: () => {
        console.log('WebSocket连接成功')
        isConnected.value = true
        isConnecting.value = false
        reconnectCount.value = 0
        
        if (options.onOpen) {
          options.onOpen()
        }
      },

      onClose: () => {
        console.log('WebSocket连接关闭')
        isConnected.value = false
        isConnecting.value = false
        connectionId.value = null
        
        if (options.onClose) {
          options.onClose()
        }
      },

      onError: (err: Error) => {
        console.error('WebSocket错误:', err)
        error.value = err
        isConnecting.value = false
        
        if (options.onError) {
          options.onError(err)
        }
      },

      onChatMessage: (payload: ChatMessagePayload, message) => {
        if (options.onChatMessage) {
          options.onChatMessage(payload, message)
        }
      },

      onTyping: (payload: TypingPayload, message) => {
        if (options.onTyping) {
          options.onTyping(payload, message)
        }
      },

      onReadReceipt: (payload: ReadReceiptPayload, message) => {
        if (options.onReadReceipt) {
          options.onReadReceipt(payload, message)
        }
      },

      onPresence: (payload: PresencePayload, message) => {
        if (options.onPresence) {
          options.onPresence(payload, message)
        }
      },

      onNotification: (payload, message) => {
        if (options.onNotification) {
          options.onNotification(payload, message)
        }
      },

      onAgentResponse: (payload, message) => {
        if (options.onAgentResponse) {
          options.onAgentResponse(payload, message)
        }
      },

      onConnectAck: (payload) => {
        console.log('WebSocket连接确认:', payload)
        connectionId.value = payload.connectionId
        
        if (options.onConnectAck) {
          options.onConnectAck(payload)
        }
      }
    }

    if (listenerId) {
      wsClient.removeListener(listenerId)
      listenerId = null
    }
    listenerId = wsClient.addListener(listeners)
    const status = wsClient.getConnectionStatus()
    isConnected.value = status.isConnected
    isConnecting.value = status.isConnecting
    reconnectCount.value = status.reconnectCount
    connectionId.value = status.connectionId || null
    if (!status.isConnected && !status.isConnecting) {
      wsClient.connect()
    }
  }

  /**
   * 断开连接
   */
  const disconnect = (): void => {
    wsClient.close()
    isConnected.value = false
    isConnecting.value = false
    connectionId.value = null
  }

  /**
   * 发送聊天消息
   */
  const sendMessage = (
    conversationId: string,
    content: string,
    messageType: string = 'TEXT',
    options?: Partial<ChatMessagePayload>
  ): boolean => {
    return wsClient.sendChatMessage(conversationId, content, messageType as any, options)
  }

  /**
   * 发送输入状态
   */
  const sendTyping = (conversationId: string, isTyping: boolean = true): boolean => {
    return wsClient.sendTyping(conversationId, isTyping)
  }

  /**
   * 发送已读回执
   */
  const sendReadReceipt = (conversationId: string, messageId: string): boolean => {
    return wsClient.sendReadReceipt(conversationId, messageId)
  }

  /**
   * 重连
   */
  const reconnect = (): void => {
    disconnect()
    setTimeout(() => {
      connect()
    }, 1000)
  }

  // 组件挂载时自动连接
  onMounted(() => {
    if (options.autoConnect !== false) {
      connect()
    }
  })

  // 组件卸载时断开连接
  onUnmounted(() => {
    if (listenerId) {
      wsClient.removeListener(listenerId)
      listenerId = null
    }
    if (options.disconnectOnUnmount) {
      disconnect()
    }
  })

  return {
    isConnected,
    isConnecting,
    connectionId,
    reconnectCount,
    error,
    connect,
    disconnect,
    reconnect,
    sendMessage,
    sendTyping,
    sendReadReceipt
  }
}

export default useWebSocket
