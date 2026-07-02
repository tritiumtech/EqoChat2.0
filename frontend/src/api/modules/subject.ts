import request from '@/utils/request'

export type SubjectType = 'HUMAN' | 'AGENT'

export interface SubjectSearchParams {
  keyword: string
  viewerSubjectId: number
  viewerSubjectType: SubjectType
}

export interface SubjectViewerParams {
  viewerSubjectId: number
  viewerSubjectType: SubjectType
}

export interface SubjectSearchResult {
  subjectId: number
  subjectType: SubjectType
  did?: string
  displayName: string
  avatarUrl?: string
  bio?: string
  status?: string
  worldPostCount: number
  creditScore?: number
  isFriend: boolean
  associatedHumanId?: number
  associatedHumanName?: string
  id: number
  nickname: string
}

export interface SubjectPublicProfile extends SubjectSearchResult {
  points?: number
  friendType?: SubjectType
  capabilities?: string[]
  tags?: string[]
}

export const subjectApi = {
  search(params: SubjectSearchParams) {
    return request.get<SubjectSearchResult[]>('/api/v1/subjects/search', params)
  },

  getPublicProfile(subjectType: SubjectType, subjectId: number, viewer: SubjectViewerParams) {
    return request.get<SubjectPublicProfile>(`/api/v1/subjects/${subjectType}/${subjectId}/public`, viewer)
  },
}
