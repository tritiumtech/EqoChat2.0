import request from '@/utils/request'

export type CreditSubjectType = 'USER' | 'AGENT'

export interface CreditDispute {
  id: string
  projectName: string
  filedBy: string
  reason: string
  verdict: 'verified' | 'unverified' | 'pending' | string
  date: string
}

export interface CreditReview {
  id: string
  projectName: string
  rating: number
  comment: string
  from: string
  date: string
}

export interface CreditProfile {
  creditScore: number
  projectsCompleted: number
  successRate: number
  disputeCount: number
  disputes: CreditDispute[]
  reviews: CreditReview[]
}

export const creditApi = {
  getSubjectCreditProfile(subjectId: number, subjectType: CreditSubjectType) {
    return request.get<CreditProfile>('/api/v1/credits/subject', {
      subjectId,
      subjectType,
    })
  },
}

