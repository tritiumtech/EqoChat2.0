import request from '@/utils/request'

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
  list(limit = 20) {
    return request.get<NotificationItem[]>('/api/v1/notifications', { limit })
  },
  markRead(notificationId: number) {
    return request.post<void>('/api/v1/notifications/read', { notificationId })
  },
}
