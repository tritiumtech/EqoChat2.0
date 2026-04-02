import request, { forceLogoutAndGoLogin, shouldForceReloginPayload } from '@/utils/request'

const API_BASE =
  import.meta.env.VITE_API_BASE_URL === undefined || import.meta.env.VITE_API_BASE_URL === ''
    ? (import.meta.env.DEV ? 'http://localhost:8080' : 'http://localhost:8080')
    : import.meta.env.VITE_API_BASE_URL

export type WorldSort = 'friends' | 'upvotes' | 'topics'

export type WorldMediaType = 'TEXT' | 'IMAGE' | 'VIDEO'

export interface WorldAuthor {
  name: string
  avatar: string
  ai: boolean
}

export interface WorldPost {
  id: string
  author: WorldAuthor
  content: string
  mediaType?: WorldMediaType | string
  imageUrl?: string
  videoUrl?: string
  /**
   * 相对时间显示（如 "now", "15m", "2h", "3d"）
   */
  timestamp: string
  /**
   * 完整的 ISO-8601 时间戳（如 "2026-04-02T10:30:00"），用于时间线分组
   */
  createdAt?: string
  upvotes: number
  replies: number
  topics: string[]
  upvoted: boolean
  friend: boolean
}

export interface WorldPostReply {
  id: string
  author: WorldAuthor
  content: string
  timestamp: string
  upvotes: number
  upvoted: boolean
  parentId?: string | null
  replies?: WorldPostReply[]
}

export interface WorldTopic {
  id: string
  name: string
  posts: number
  followers: number
  favorite: boolean
}

export interface CreateWorldPostPayload {
  content: string
  mediaType: WorldMediaType
  mentionedUserIds?: number[]
  imageUrl?: string
  videoUrl?: string
}

export interface CreateWorldPostReplyPayload {
  content: string
}

function uploadWorldFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    if (!token) {
      reject({ message: '未登录' })
      return
    }
    uni.uploadFile({
      url: `${API_BASE}/api/v1/world/uploads`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${token}`,
      },
      success: (res) => {
        if (res.statusCode === 401) {
          forceLogoutAndGoLogin()
          reject({ message: '未登录', errorCode: 'auth.unauthorized' })
          return
        }
        let payload: any = res.data
        if (typeof payload === 'string') {
          try {
            payload = JSON.parse(payload)
          } catch {
            reject({ message: '上传响应解析失败' })
            return
          }
        }
        if (payload && payload.code === 200 && payload.data?.url) {
          resolve(String(payload.data.url))
          return
        }
        if (payload && shouldForceReloginPayload(payload)) {
          forceLogoutAndGoLogin()
        }
        reject({
          message: payload?.message || '上传失败',
          errorCode: payload?.errorCode,
          code: payload?.code,
        })
      },
      fail: (err: any) => {
        reject({ message: err?.errMsg || err?.message || '网络错误' })
      },
    })
  })
}

export const worldApi = {
  listPosts(params?: { sort?: WorldSort; cursorId?: number | string; limit?: number }) {
    return request.get<WorldPost[]>('/api/v1/world/posts', params)
  },

  /** 指定好友最近发布的动态（需互为好友） */
  listPostsByAuthor(authorId: number, params?: { cursorId?: number | string; limit?: number }) {
    return request.get<WorldPost[]>(`/api/v1/world/users/${authorId}/posts`, params)
  },

  createPost(data: CreateWorldPostPayload) {
    return request.post<WorldPost>('/api/v1/world/posts', data)
  },

  createReply(postId: string | number, data: CreateWorldPostReplyPayload) {
    return request.post<{ replyCount: number }>(`/api/v1/world/posts/${postId}/replies`, data)
  },

  listReplies(postId: string | number, params?: { cursorId?: number | string; limit?: number }) {
    return request.get<WorldPostReply[]>(`/api/v1/world/posts/${postId}/replies`, params)
  },

  toggleReplyUpvote(replyId: string | number) {
    return request.post<{ upvoted: boolean }>(`/api/v1/world/replies/${replyId}/upvote`)
  },

  getShareLink(postId: string | number) {
    return request.get<{ url: string }>(`/api/v1/world/posts/${postId}/share-link`)
  },

  uploadMedia(filePath: string) {
    return uploadWorldFile(filePath)
  },

  listTopics(params?: { limit?: number }) {
    return request.get<WorldTopic[]>('/api/v1/world/topics', params)
  },

  listTopicPosts(topicName: string, params?: { cursorId?: number | string; limit?: number }) {
    return request.get<WorldPost[]>(`/api/v1/world/topics/${encodeURIComponent(topicName)}/posts`, params)
  },

  listMentionedMe(params?: { cursorId?: number | string; limit?: number }) {
    return request.get<WorldPost[]>('/api/v1/world/mentions', params)
  },

  listMyPosts(params?: { cursorId?: number | string; limit?: number }) {
    return request.get<WorldPost[]>('/api/v1/world/my-posts', params)
  },

  toggleUpvote(postId: string | number) {
    return request.post<{ upvoted: boolean }>(`/api/v1/world/posts/${postId}/upvote`)
  },

  toggleFollow(topicName: string) {
    return request.post<{ following: boolean }>(`/api/v1/world/topics/${encodeURIComponent(topicName)}/follow`)
  },
}
