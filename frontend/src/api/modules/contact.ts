import request from '@/utils/request'

export interface ContactItem {
  id: number
  nickname: string
  avatarUrl?: string
  status?: string
}

export interface AddContactRequest {
  friendId: number
}

export const contactApi = {
  listContacts() {
    return request.get<ContactItem[]>('/api/v1/contacts')
  },

  addContact(data: AddContactRequest) {
    return request.post<ContactItem>('/api/v1/contacts', data)
  }
}
