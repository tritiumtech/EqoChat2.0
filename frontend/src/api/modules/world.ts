import request from '@/utils/request'

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
  timestamp: string
  upvotes: number
  replies: number
  topics: string[]
  upvoted: boolean
  friend: boolean
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
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/auth/login' })
          reject({ message: '未登录' })
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
        reject({ message: payload?.message || '上传失败' })
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

  createPost(data: CreateWorldPostPayload) {
    return request.post<WorldPost>('/api/v1/world/posts', data)
  },

  createReply(postId: string | number, data: CreateWorldPostReplyPayload) {
    return request.post<{ replyCount: number }>(`/api/v1/world/posts/${postId}/replies`, data)
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

  toggleUpvote(postId: string | number) {
    return request.post<{ upvoted: boolean }>(`/api/v1/world/posts/${postId}/upvote`)
  },

  toggleFollow(topicName: string) {
    return request.post<{ following: boolean }>(`/api/v1/world/topics/${encodeURIComponent(topicName)}/follow`)
  },
}
