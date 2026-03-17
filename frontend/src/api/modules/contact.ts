import request from '@/utils/request'

export interface ContactItem {
  id: number
  nickname: string
  avatarUrl?: string
  status?: string
}

export const contactApi = {
  listContacts() {
    return request.get<ContactItem[]>('/api/v1/contacts')
  }
}
