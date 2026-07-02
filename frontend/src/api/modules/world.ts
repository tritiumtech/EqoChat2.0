import request, { forceLogoutAndGoLogin, shouldForceReloginPayload } from '@/utils/request'
import type { PageResponse } from '@/types/pagination'
import { buildApiUrl } from '@/utils/runtime-config'

export type WorldSort = 'friends' | 'upvotes' | 'topics'

export type WorldMediaType = 'TEXT' | 'IMAGE' | 'VIDEO'
export type WorldSubjectType = 'HUMAN' | 'AGENT' | 'SYSTEM'

export interface WorldAuthor {
  id?: number | string
  name: string
  type?: 'human' | 'agent' | string
  avatar: string
  ai: boolean
  associatedHumanId?: number | string
  associatedHumanName?: string
  walletRouting?: string
}

export interface WorldSharedProject {
  id: string
  name: string
  ownerName?: string
  ownerAi?: boolean
  associatedHumanName?: string
  budget?: string
  teamMix?: string
  deadline?: string
  status?: string
}

export interface WorldPost {
  id: string
  author: WorldAuthor
  content: string
  mediaType?: WorldMediaType | string
  imageUrl?: string
  videoUrl?: string
  sharedProject?: WorldSharedProject | null
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
  followed?: boolean
}

export interface CreateWorldPostPayload {
  actorSubjectId: number
  actorSubjectType: WorldSubjectType
  content: string
  mediaType: WorldMediaType
  mentionedSubjects?: WorldMentionedSubject[]
  imageUrl?: string
  videoUrl?: string
}

export interface WorldMentionedSubject {
  subjectId: number
  subjectType: WorldSubjectType
}

export interface CreateWorldPostReplyPayload {
  actorSubjectId: number
  actorSubjectType: WorldSubjectType
  content: string
}

export interface WorldSubjectParams {
  subjectId: number
  subjectType: WorldSubjectType
}

export type WorldViewerParams = {
  viewerSubjectId: number
  viewerSubjectType: WorldSubjectType
}

export type WorldActorParams = {
  actorSubjectId: number
  actorSubjectType: WorldSubjectType
}

function requireSubjectParams(
  subjectId: number | string | undefined,
  subjectType: WorldSubjectType | undefined,
  label: string,
): { id: number; type: WorldSubjectType } {
  const id = Number(subjectId)
  const type = String(subjectType || '').toUpperCase() as WorldSubjectType
  if (!Number.isFinite(id) || id <= 0 || (type !== 'HUMAN' && type !== 'AGENT')) {
    throw new Error(`${label} subject is required`)
  }
  return { id, type }
}

function viewerParams(viewer?: Partial<WorldViewerParams>): WorldViewerParams {
  const subject = requireSubjectParams(viewer?.viewerSubjectId, viewer?.viewerSubjectType, 'viewer')
  return { viewerSubjectId: subject.id, viewerSubjectType: subject.type }
}

function actorParams(actor?: WorldSubjectParams): WorldActorParams {
  const subject = requireSubjectParams(actor?.subjectId, actor?.subjectType, 'actor')
  return { actorSubjectId: subject.id, actorSubjectType: subject.type }
}

function subjectQuery(params: Partial<WorldViewerParams | WorldActorParams>): string {
  const parts = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  return parts.length ? `?${parts.join('&')}` : ''
}

function uploadWorldFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    if (!token) {
      reject({ message: '未登录' })
      return
    }
    uni.uploadFile({
      url: buildApiUrl('/api/v1/world/uploads'),
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
  listPosts(params?: { sort?: WorldSort; cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>) {
    return request.get<PageResponse<WorldPost>>('/api/v1/world/posts', { ...params, ...viewerParams(params) })
  },

  /** 指定主体最近发布的动态（需互为好友） */
  listPostsByAuthor(
    authorId: number,
    authorType: WorldSubjectType,
    params?: { cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>,
  ) {
    return request.get<PageResponse<WorldPost>>(
      `/api/v1/world/subjects/${authorType}/${authorId}/posts`,
      { ...params, ...viewerParams(params) },
    )
  },

  createPost(data: CreateWorldPostPayload) {
    return request.post<WorldPost>('/api/v1/world/posts', data)
  },

  createReply(postId: string | number, data: CreateWorldPostReplyPayload) {
    return request.post<{ replyCount: number }>(`/api/v1/world/posts/${postId}/replies`, data)
  },

  listReplies(postId: string | number, params?: { cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>) {
    return request.get<WorldPostReply[]>(`/api/v1/world/posts/${postId}/replies`, { ...params, ...viewerParams(params) })
  },

  toggleReplyUpvote(replyId: string | number, actor?: WorldSubjectParams) {
    return request.post<{ upvoted: boolean }>(
      `/api/v1/world/replies/${replyId}/upvote${subjectQuery(actorParams(actor))}`,
    )
  },

  getShareLink(postId: string | number) {
    return request.get<{ url: string }>(`/api/v1/world/posts/${postId}/share-link`)
  },

  uploadMedia(filePath: string) {
    return uploadWorldFile(filePath)
  },

  listTopics(params?: { limit?: number; cursorId?: number | string } & Partial<WorldViewerParams>) {
    return request.get<PageResponse<WorldTopic>>('/api/v1/world/topics', { ...params, ...viewerParams(params) })
  },

  listTopicPosts(topicName: string, params?: { cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>) {
    return request.get<PageResponse<WorldPost>>(`/api/v1/world/topics/${encodeURIComponent(topicName)}/posts`, { ...params, ...viewerParams(params) })
  },

  listMentionedMe(params?: { cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>) {
    return request.get<PageResponse<WorldPost>>('/api/v1/world/mentions', { ...params, ...viewerParams(params) })
  },

  listMyPosts(params?: { cursorId?: number | string; limit?: number } & Partial<WorldViewerParams>) {
    return request.get<PageResponse<WorldPost>>('/api/v1/world/my-posts', { ...params, ...viewerParams(params) })
  },

  toggleUpvote(postId: string | number, actor?: WorldSubjectParams) {
    return request.post<{ upvoted: boolean }>(
      `/api/v1/world/posts/${postId}/upvote${subjectQuery(actorParams(actor))}`,
    )
  },

  upvotePost(postId: string | number, actor?: WorldSubjectParams) {
    return worldApi.toggleUpvote(postId, actor)
  },

  toggleFollow(topicName: string, actor?: WorldSubjectParams) {
    return request.post<{ following: boolean }>(
      `/api/v1/world/topics/${encodeURIComponent(topicName)}/follow${subjectQuery(actorParams(actor))}`,
    )
  },

  followTopic(topicName: string, _following?: boolean, actor?: WorldSubjectParams) {
    return worldApi.toggleFollow(topicName, actor)
  },

  replyToPost(postId: string | number, content: string, actorSubjectId: number, actorSubjectType: WorldSubjectType) {
    return worldApi.createReply(postId, { content, actorSubjectId, actorSubjectType })
  },
}
