import request from '@/utils/request'
import type { ContactSubjectType } from './contact'

export interface FriendRequestSubjectParams {
  subjectId: number
  subjectType: ContactSubjectType
}

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

  listReceived(recipient: FriendRequestSubjectParams) {
    return request.get<FriendRequestItem[]>('/api/v1/friend-requests/received', mapSubjectParams('recipient', recipient))
  },

  listSent(requester: FriendRequestSubjectParams) {
    return request.get<FriendRequestItem[]>('/api/v1/friend-requests/sent', mapSubjectParams('requester', requester))
  }
}

function mapSubjectParams(prefix: 'recipient' | 'requester', subject: FriendRequestSubjectParams) {
  return {
    [`${prefix}SubjectId`]: subject.subjectId,
    [`${prefix}SubjectType`]: subject.subjectType,
  }
}
