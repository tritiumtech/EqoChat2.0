import request from '@/utils/request'

export type ContactSubjectType = 'HUMAN' | 'AGENT'

export interface ContactItem {
  ownerSubjectId?: number
  ownerSubjectType?: ContactSubjectType
  targetSubjectId: number
  targetSubjectType: ContactSubjectType
  nickname: string
  avatarUrl?: string
  status?: string
  tags?: string[]
}

export interface ContactDetail extends ContactItem {
  bio?: string
  worldPostCount: number
  capabilities: string[]
}

export const contactApi = {
  listContacts(params?: { q?: string; status?: string }) {
    return request.get<ContactItem[]>('/api/v1/contacts', params)
  },
  getContactDetail(targetSubjectType: ContactSubjectType, targetSubjectId: number) {
    return request.get<ContactDetail>(`/api/v1/contacts/${targetSubjectType}/${targetSubjectId}`)
  },
  updateContactTags(targetSubjectType: ContactSubjectType, targetSubjectId: number, tags: string[]) {
    return request.put<string[]>(`/api/v1/contacts/${targetSubjectType}/${targetSubjectId}/tags`, { tags })
  },
}
