import request from '@/utils/request'

export type NotificationSubjectType = 'HUMAN' | 'AGENT'

export interface NotificationRecipientSubjectParams {
  recipientSubjectId?: number
  recipientSubjectType?: NotificationSubjectType
}

export interface NotificationItem {
  id: number
  recipientSubjectId?: number
  recipientSubjectType?: 'HUMAN' | 'AGENT' | string
  senderSubjectId?: number
  senderSubjectType?: 'HUMAN' | 'AGENT' | 'SYSTEM' | string
  type?: string
  title: string
  content?: string
  read: boolean
  createTime?: string
}

export const notificationApi = {
  list(limit = 20, recipient?: NotificationRecipientSubjectParams) {
    return request.get<NotificationItem[]>('/api/v1/notifications', { limit, ...recipient })
  },
  markRead(notificationId: number, recipient?: NotificationRecipientSubjectParams) {
    return request.post<void>('/api/v1/notifications/read', { notificationId, ...recipient })
  },
}
