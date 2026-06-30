import request from '@/utils/request'
import type { PageResponse } from '@/types/pagination'

export type SubjectType = 'HUMAN' | 'AGENT' | 'SYSTEM'

export interface ConversationSummary {
  id: number
  title: string
  avatarUrl?: string
  conversationType: string
  lastMessage?: string
  lastMessageAt?: string
  unreadCount?: number
  online?: boolean
  targetSubjectId?: number
  targetSubjectType?: SubjectType
}

export interface CreateConversationRequest {
  targetSubjectId: number
  targetSubjectType: SubjectType
  title?: string
  avatarUrl?: string
}

export interface MessageItem {
  id: number | string
  conversationId: number
  senderSubjectId: number
  senderSubjectType: SubjectType
  liableHumanId?: number
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

/** @deprecated 使用 PageResponse<MessageItem> 替代 */
export type MessagePageResponse = PageResponse<MessageItem>

export interface SendMessageRequest {
  content: string
  messageType?: string
  metadata?: Record<string, unknown>
  replyToMessageId?: string
  intentData?: string
  actorSubjectId: number
  actorSubjectType: SubjectType
}

export interface MarkConversationReadRequest {
  messageId: number
  readerSubjectId: number
  readerSubjectType: SubjectType
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

  markRead(conversationId: number, data: MarkConversationReadRequest) {
    return request.post<void>(`/api/v1/conversations/${conversationId}/read`, data)
  }
}
