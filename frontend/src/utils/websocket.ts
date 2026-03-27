/**
 * EqoChat WebSocket客户端工具类 (TypeScript版本)
 * 兼容后端WebSocket协议
 */

import {
  MessageType,
  SenderType,
  ContentType,
  ConnectionStatus,
  type BaseMessage,
  type ChatMessagePayload,
  type ReadReceiptPayload,
  type TypingPayload,
  type PresencePayload,
  type ConnectAckPayload,
  type ErrorPayload,
  type WebSocketCallbacks,
  type WebSocketConfig,
  type ConnectionInfo
} from '@/types/websocket'

// 默认配置
const DEFAULT_CONFIG: WebSocketConfig = {
  baseUrl: import.meta.env.VITE_WS_URL || 'ws://localhost:8080',
  reconnectInterval: 3000,
  maxReconnectTimes: 5,
  heartbeatInterval: 30000,
  connectTimeout: 10000
}

class WebSocketClient {
  private ws: UniApp.SocketTask | null = null
  private token: string = ''
  private userId: string = ''
  private connectionId: string = ''
  
  // 状态
  private isConnected: boolean = false
  private isConnecting: boolean = false
  private reconnectCount: number = 0
  
  // 定时器
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private connectTimeoutTimer: number | null = null
  
  // 回调函数
  private callbacks: WebSocketCallbacks = {}
  
  // 配置
  private config: WebSocketConfig = DEFAULT_CONFIG

  /**
   * 初始化WebSocket
   * @param token JWT token
   * @param callbacks 回调函数
   * @param config 自定义配置
   */
  init(token: string, callbacks: WebSocketCallbacks = {}, config?: Partial<WebSocketConfig>): void {
    this.token = token
    this.callbacks = callbacks
    this.config = { ...this.config, ...config }
    
    // 从token解析userId
    try {
      const base64 = token.split('.')[1]
      const json = JSON.parse(atob(base64))
      this.userId = json.userId || json.sub || ''
    } catch (e) {
      console.warn('解析token失败:', e)
    }
  }

  /**
   * 连接WebSocket
   */
  connect(): void {
    if (this.isConnecting || this.isConnected) {
      console.log('WebSocket已在连接中或已连接')
      return
    }

    if (!this.token) {
      console.error('WebSocket连接失败: 未设置token')
      return
    }

    this.isConnecting = true
    const url = `${this.config.baseUrl}/ws/chat?token=${this.token}`
    
    console.log('WebSocket连接中...', url)

    this.ws = uni.connectSocket({
      url,
      success: () => {
        console.log('WebSocket连接请求已发送')
      },
      fail: (err) => {
        console.error('WebSocket连接失败:', err)
        this.handleError(new Error(String(err)))
      }
    })

    // 设置超时
    this.connectTimeoutTimer = setTimeout(() => {
      if (!this.isConnected) {
        console.error('WebSocket连接超时')
        this.close()
        this.scheduleReconnect()
      }
    }, this.config.connectTimeout) as unknown as number

    // 监听打开
    this.ws.onOpen(() => {
      console.log('WebSocket连接已打开')
      this.isConnected = true
      this.isConnecting = false
      this.reconnectCount = 0
      
      if (this.connectTimeoutTimer) {
        clearTimeout(this.connectTimeoutTimer)
        this.connectTimeoutTimer = null
      }
      
      this.startHeartbeat()
      
      if (this.callbacks.onOpen) {
        this.callbacks.onOpen()
      }
    })

    // 监听消息
    this.ws.onMessage((res) => {
      this.handleMessage(res.data as string)
    })

    // 监听关闭
    this.ws.onClose(() => {
      console.log('WebSocket连接已关闭')
      this.handleClose()
    })

    // 监听错误
    this.ws.onError((err) => {
      console.error('WebSocket错误:', err)
      this.handleError(new Error(String(err)))
    })
  }

  /**
   * 发送消息
   * @param type 消息类型
   * @param payload 消息内容
   * @param recipientId 接收者ID
   */
  send(type: MessageType, payload: unknown, recipientId?: string): boolean {
    if (!this.isConnected) {
      console.error('WebSocket未连接，无法发送消息')
      return false
    }

    const message: BaseMessage = {
      id: this.generateMessageId(),
      type,
      senderId: this.userId,
      senderType: SenderType.USER,
      recipientId,
      timestamp: new Date().toISOString(),
      payload
    }

    try {
      const data = JSON.stringify(message)
      this.ws?.send({
        data,
        success: () => {
          console.log('消息发送成功:', type)
        },
        fail: (err) => {
          console.error('消息发送失败:', err)
        }
      })
      return true
    } catch (e) {
      console.error('消息发送异常:', e)
      return false
    }
  }

  /**
   * 发送聊天消息
   * @param conversationId 会话ID
   * @param content 消息内容
   * @param messageType 消息类型
   * @param options 可选参数
   */
  sendChatMessage(
    conversationId: string,
    content: string,
    messageType: ContentType = ContentType.TEXT,
    options?: Partial<ChatMessagePayload>
  ): boolean {
    const payload: ChatMessagePayload = {
      conversationId,
      messageType,
      content,
      metadata: options?.metadata,
      replyToMessageId: options?.replyToMessageId,
      intentData: options?.intentData
    }
    return this.send(MessageType.CHAT_MESSAGE, payload, conversationId)
  }

  /**
   * 发送输入状态
   * @param conversationId 会话ID
   * @param isTyping 是否正在输入
   */
  sendTyping(conversationId: string, isTyping: boolean = true): boolean {
    const payload: TypingPayload = {
      conversationId,
      userId: this.userId,
      isTyping
    }
    return this.send(MessageType.CHAT_TYPING, payload, conversationId)
  }

  /**
   * 发送已读回执
   * @param conversationId 会话ID
   * @param messageId 消息ID
   */
  sendReadReceipt(conversationId: string, messageId: string): boolean {
    const payload: ReadReceiptPayload = {
      conversationId,
      messageId,
      readerId: this.userId
    }
    return this.send(MessageType.CHAT_READ, payload, conversationId)
  }

  /**
   * 发送心跳
   */
  sendHeartbeat(): boolean {
    return this.send(MessageType.PING, {})
  }

  /**
   * 处理收到的消息
   */
  private handleMessage(data: string): void {
    try {
      const message = JSON.parse(data) as BaseMessage
      console.log('收到WebSocket消息:', message.type, message)

      // 通用消息回调
      if (this.callbacks.onMessage) {
        this.callbacks.onMessage(message)
      }

      // 根据消息类型分发
      switch (message.type) {
        case MessageType.CHAT_MESSAGE:
          if (this.callbacks.onChatMessage) {
            this.callbacks.onChatMessage(message.payload as ChatMessagePayload, message)
          }
          break

        case MessageType.CHAT_TYPING:
          if (this.callbacks.onTyping) {
            this.callbacks.onTyping(message.payload as TypingPayload, message)
          }
          break

        case MessageType.CHAT_READ:
          if (this.callbacks.onReadReceipt) {
            this.callbacks.onReadReceipt(message.payload as ReadReceiptPayload, message)
          }
          break

        case MessageType.PRESENCE_ONLINE:
        case MessageType.PRESENCE_OFFLINE:
          if (this.callbacks.onPresence) {
            this.callbacks.onPresence(message.payload as PresencePayload, message)
          }
          break

        case MessageType.NOTIFICATION:
          if (this.callbacks.onNotification) {
            this.callbacks.onNotification(message.payload, message)
          }
          break

        case MessageType.AGENT_RESPONSE:
          if (this.callbacks.onAgentResponse) {
            this.callbacks.onAgentResponse(message.payload, message)
          }
          break

        case MessageType.CONNECT_ACK:
          this.connectionId = (message.payload as ConnectAckPayload).connectionId
          if (this.callbacks.onConnectAck) {
            this.callbacks.onConnectAck(message.payload as ConnectAckPayload, message)
          }
          break

        case MessageType.PONG:
          console.log('收到心跳响应')
          break

        case MessageType.ERROR:
          console.error('收到错误消息:', message.payload)
          if (this.callbacks.onError) {
            this.callbacks.onError(new Error((message.payload as ErrorPayload).message))
          }
          break

        default:
          console.log('未知消息类型:', message.type)
      }
    } catch (e) {
      console.error('处理消息失败:', e, data)
    }
  }

  /**
   * 处理连接关闭
   */
  private handleClose(): void {
    this.isConnected = false
    this.isConnecting = false
    this.stopHeartbeat()
    
    if (this.callbacks.onClose) {
      this.callbacks.onClose()
    }

    this.scheduleReconnect()
  }

  /**
   * 处理错误
   */
  private handleError(err: Error): void {
    this.isConnecting = false
    
    if (this.callbacks.onError) {
      this.callbacks.onError(err)
    }

    this.scheduleReconnect()
  }

  /**
   * 启动心跳
   */
  private startHeartbeat(): void {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        this.sendHeartbeat()
      }
    }, this.config.heartbeatInterval) as unknown as number
  }

  /**
   * 停止心跳
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 计划重连
   */
  private scheduleReconnect(): void {
    if (this.reconnectCount >= this.config.maxReconnectTimes) {
      console.error('WebSocket重连次数已达上限')
      return
    }

    this.reconnectCount++
    console.log(`计划${this.config.reconnectInterval}ms后重连(第${this.reconnectCount}次)...`)

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.config.reconnectInterval) as unknown as number
  }

  /**
   * 关闭连接
   */
  close(): void {
    console.log('关闭WebSocket连接')
    
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.connectTimeoutTimer) {
      clearTimeout(this.connectTimeoutTimer)
      this.connectTimeoutTimer = null
    }

    if (this.ws) {
      this.ws.close({})
      this.ws = null
    }

    this.isConnected = false
    this.isConnecting = false
  }

  /**
   * 生成消息ID
   */
  private generateMessageId(): string {
    return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  }

  /**
   * 检查是否已连接
   */
  getConnectionStatus(): ConnectionInfo {
    return {
      isConnected: this.isConnected,
      isConnecting: this.isConnecting,
      connectionId: this.connectionId,
      reconnectCount: this.reconnectCount
    }
  }
}

// 导出单例
export const wsClient = new WebSocketClient()

// 导出工具函数
export default {
  wsClient,
  MessageType,
  SenderType,
  ContentType,
  ConnectionStatus,
  
  /**
   * 快捷初始化方法
   */
  init(token: string, callbacks?: WebSocketCallbacks, config?: Partial<WebSocketConfig>): WebSocketClient {
    wsClient.init(token, callbacks, config)
    return wsClient
  },

  /**
   * 快捷连接方法
   */
  connect(token: string, callbacks?: WebSocketCallbacks, config?: Partial<WebSocketConfig>): WebSocketClient {
    wsClient.init(token, callbacks, config)
    wsClient.connect()
    return wsClient
  }
}
