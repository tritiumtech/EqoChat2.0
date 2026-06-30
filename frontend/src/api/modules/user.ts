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
  points?: number
  lastLoginAt?: string
  createTime?: string
}

export interface UserSearchParams {
  keyword: string
}

export interface UserSearchResult {
  id: number
  nickname: string
  avatarUrl?: string
  bio?: string
  phone?: string
  email?: string
  worldPostCount: number
  isFriend: boolean
  status?: string
}

export const userApi = {
  me() {
    return request.get<UserInfo>('/api/v1/auth/me')
  },

  /**
   * 通过账号搜索用户（精准匹配）
   * @param params 搜索参数
   */
  searchUserByAccount(params: UserSearchParams) {
    return request.get<UserSearchResult>('/api/v1/users/search', params)
  },

  /**
   * 获取用户公开资料（非好友视角）
   * @param userId 用户ID
   */
  getUserPublicProfile(userId: number) {
    return request.get<UserSearchResult>(`/api/v1/users/${userId}/public`)
  }
}
