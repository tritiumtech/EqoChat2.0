import request from '@/utils/request'

export interface ContactItem {
  id: number
  nickname: string
  avatarUrl?: string
  status?: string
  tags?: string[]
}

export const contactApi = {
  listContacts(params?: { q?: string; status?: string }) {
    return request.get<ContactItem[]>('/api/v1/contacts', params)
  },
  updateContactTags(contactId: number, tags: string[]) {
    return request.put<string[]>(`/api/v1/contacts/${contactId}/tags`, { tags })
  }
}
