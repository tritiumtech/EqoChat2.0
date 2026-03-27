import request from '@/utils/request'

export interface ConversationSummary {
  id: number
  title: string
  avatarUrl?: string
  conversationType: string
  lastMessage?: string
  lastMessageAt?: string
  unreadCount?: number
  online?: boolean
}

export interface CreateConversationRequest {
  targetUserId: number
  title?: string
  avatarUrl?: string
}

export interface MessageItem {
  id: number | string
  conversationId: number
  senderId: number
  senderType: string
  messageType: string
  content: string
  createTime: string
  attachment?: {
    fileName?: string
    fileSize?: string
    fileType?: string
    downloadUrl?: string
  }
}

export interface MessagePageResponse {
  items: MessageItem[]
  total: number
  hasMore: boolean
  nextLastMessageId?: number
}

export interface SendMessageRequest {
  content: string
  messageType?: string
  metadata?: Record<string, unknown>
  replyToMessageId?: string
  intentData?: string
}

export const conversationApi = {
  listConversations(params?: { q?: string }) {
    return request.get<ConversationSummary[]>('/api/v1/conversations', params)
  },

  getConversation(conversationId: number) {
    return request.get<ConversationSummary>(`/api/v1/conversations/${conversationId}`)
  },

  createConversation(data: CreateConversationRequest) {
    return request.post<ConversationSummary>('/api/v1/conversations', data)
  },

  getMessages(conversationId: number, params?: { lastMessageId?: number; limit?: number }) {
    return request.get<MessagePageResponse>(`/api/v1/conversations/${conversationId}/messages`, params)
  },

  sendMessage(conversationId: number, data: SendMessageRequest) {
    return request.post<MessageItem>(`/api/v1/conversations/${conversationId}/messages`, data)
  },

  markRead(conversationId: number, messageId: number | string) {
    return request.post<void>(`/api/v1/conversations/${conversationId}/read`, {
      messageId: Number(messageId),
    })
  }
}
