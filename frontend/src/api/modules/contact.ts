import request from '@/utils/request'

export interface ContactItem {
  id: number
  nickname: string
  avatarUrl?: string
  status?: string
  tags?: string[]
}

export type ContactFriendType = 'HUMAN' | 'AGENT'

export interface ContactDetail extends ContactItem {
  bio?: string
  worldPostCount: number
  friendType: ContactFriendType
  capabilities: string[]
}

export const contactApi = {
  listContacts(params?: { q?: string; status?: string }) {
    return request.get<ContactItem[]>('/api/v1/contacts', params)
  },
  getContactDetail(contactId: number) {
    return request.get<ContactDetail>(`/api/v1/contacts/${contactId}`)
  },
  updateContactTags(contactId: number, tags: string[]) {
    return request.put<string[]>(`/api/v1/contacts/${contactId}/tags`, { tags })
  },
}
