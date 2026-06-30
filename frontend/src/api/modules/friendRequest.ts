import request from '@/utils/request'
import type { ContactSubjectType } from './contact'

export interface FriendRequestItem {
  id: number
  requesterSubjectId: number
  requesterSubjectType: ContactSubjectType
  recipientSubjectId: number
  recipientSubjectType: ContactSubjectType
  requestMessage?: string
  status: string
  createTime: string
  requesterNickname?: string
  requesterAvatarUrl?: string
  recipientNickname?: string
  recipientAvatarUrl?: string
}

export interface SendFriendRequestDto {
  actorSubjectId: number
  actorSubjectType: ContactSubjectType
  recipientSubjectId: number
  recipientSubjectType: ContactSubjectType
  requestMessage?: string
}

export const friendRequestApi = {
  sendRequest(data: SendFriendRequestDto) {
    return request.post<FriendRequestItem>('/api/v1/friend-requests', data)
  },

  accept(id: number) {
    return request.post<void>(`/api/v1/friend-requests/${id}/accept`)
  },

  reject(id: number) {
    return request.post<void>(`/api/v1/friend-requests/${id}/reject`)
  },

  listReceived() {
    return request.get<FriendRequestItem[]>('/api/v1/friend-requests/received')
  },

  listSent() {
    return request.get<FriendRequestItem[]>('/api/v1/friend-requests/sent')
  }
}
