/**
 * EqoChat WebSocket TypeScript 类型定义
 */

// 消息类型枚举
export enum MessageType {
  CHAT_MESSAGE = 'CHAT_MESSAGE',
  CHAT_TYPING = 'CHAT_TYPING',
  CHAT_READ = 'CHAT_READ',
  NOTIFICATION = 'NOTIFICATION',
  AGENT_INTENT = 'AGENT_INTENT',
  AGENT_RESPONSE = 'AGENT_RESPONSE',
  PRESENCE_ONLINE = 'PRESENCE_ONLINE',
  PRESENCE_OFFLINE = 'PRESENCE_OFFLINE',
  PRESENCE_TYPING = 'PRESENCE_TYPING',
  CONNECT_ACK = 'CONNECT_ACK',
  PING = 'PING',
  PONG = 'PONG',
  ERROR = 'ERROR',
  SESSION_KICKED = 'SESSION_KICKED'
}

// 发送者类型
export enum SenderType {
  USER = 'USER',
  AGENT = 'AGENT',
  SYSTEM = 'SYSTEM'
}

// 消息内容类型
export enum ContentType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  VOICE = 'VOICE',
  VIDEO = 'VIDEO',
  CARD = 'CARD',
  INTENT = 'INTENT'
}

// 连接状态
export enum ConnectionStatus {
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  DISCONNECTED = 'disconnected',
  RECONNECTING = 'reconnecting',
  ERROR = 'error'
}

// WebSocket消息基础接口
export interface BaseMessage {
  id: string
  type: MessageType
  senderId: string
  senderType: SenderType
  recipientId?: string
  timestamp: string
  payload: unknown
}

// 聊天消息内容
export interface ChatMessagePayload {
  conversationId: string
  messageType: ContentType
  content: string
  metadata?: Record<string, unknown>
  replyToMessageId?: string
  intentData?: string
}

// 已读回执内容
export interface ReadReceiptPayload {
  conversationId: string
  messageId: string
  readerId: string
}

// 输入状态内容
export interface TypingPayload {
  conversationId: string
  userId: string
  isTyping: boolean
}

// 在线状态内容
export interface PresencePayload {
  userId: string
  status: 'ONLINE' | 'OFFLINE' | 'BUSY'
  lastSeenAt?: number
}

// 连接确认内容
export interface ConnectAckPayload {
  userId: string
  connectionId: string
  serverTime: number
}

// 错误内容
export interface ErrorPayload {
  code: number
  message: string
  originalMessageId?: string
}

// 被挤下线通知内容
export interface SessionKickedPayload {
  reason: string
  kickedAt: number
  newDeviceId?: string
}

// 回调函数类型
export type MessageCallback<T = unknown> = (payload: T, message: BaseMessage) => void

export interface WebSocketCallbacks {
  onOpen?: () => void
  onClose?: () => void
  onError?: (error: Error) => void
  onMessage?: (message: BaseMessage) => void
  onChatMessage?: MessageCallback<ChatMessagePayload>
  onTyping?: MessageCallback<TypingPayload>
  onReadReceipt?: MessageCallback<ReadReceiptPayload>
  onPresence?: MessageCallback<PresencePayload>
  onNotification?: MessageCallback<unknown>
  onAgentResponse?: MessageCallback<unknown>
  onConnectAck?: MessageCallback<ConnectAckPayload>
  onSessionKicked?: MessageCallback<SessionKickedPayload>
}

// WebSocket配置
export interface WebSocketConfig {
  baseUrl: string
  reconnectInterval: number
  maxReconnectTimes: number
  heartbeatInterval: number
  connectTimeout: number
}

// 连接状态信息
export interface ConnectionInfo {
  isConnected: boolean
  isConnecting: boolean
  connectionId: string | null
  reconnectCount: number
}
