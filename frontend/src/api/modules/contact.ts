import request from '@/utils/request'

export type ContactSubjectType = 'HUMAN' | 'AGENT'

export interface ContactOwnerSubjectParams {
  ownerSubjectId?: number
  ownerSubjectType?: ContactSubjectType
}

export interface ContactItem {
  ownerSubjectId?: number
  ownerSubjectType?: ContactSubjectType
  targetSubjectId: number
  targetSubjectType: ContactSubjectType
  nickname: string
  avatarUrl?: string
  status?: string
  tags?: string[]
}

export interface ContactDetail extends ContactItem {
  bio?: string
  worldPostCount: number
  capabilities: string[]
}

export const contactApi = {
  listContacts(params?: { q?: string; status?: string } & ContactOwnerSubjectParams) {
    return request.get<ContactItem[]>('/api/v1/contacts', params)
  },
  getContactDetail(
    targetSubjectType: ContactSubjectType,
    targetSubjectId: number,
    owner?: ContactOwnerSubjectParams
  ) {
    return request.get<ContactDetail>(`/api/v1/contacts/${targetSubjectType}/${targetSubjectId}`, owner)
  },
  updateContactTags(
    targetSubjectType: ContactSubjectType,
    targetSubjectId: number,
    tags: string[],
    owner?: ContactOwnerSubjectParams
  ) {
    return request.put<string[]>(
      withOwnerParams(`/api/v1/contacts/${targetSubjectType}/${targetSubjectId}/tags`, owner),
      { tags }
    )
  },
}

function withOwnerParams(url: string, owner?: ContactOwnerSubjectParams): string {
  if (!owner) return url
  const params = new URLSearchParams()
  if (owner.ownerSubjectId !== undefined && owner.ownerSubjectId !== null) {
    params.set('ownerSubjectId', String(owner.ownerSubjectId))
  }
  if (owner.ownerSubjectType) {
    params.set('ownerSubjectType', owner.ownerSubjectType)
  }
  if (params.size === 0) return url
  return `${url}?${params.toString()}`
}
