import request from '@/utils/request'

export interface UserInfo {
  id: number
  did: string
  phone: string
  email?: string
  locale?: string
  nickname: string
  avatarUrl?: string
  bio?: string
  status?: string
  creditScore?: number
  lastLoginAt?: string
  createTime?: string
}

export const userApi = {
  me() {
    return request.get<UserInfo>('/api/v1/auth/me')
  }
}
