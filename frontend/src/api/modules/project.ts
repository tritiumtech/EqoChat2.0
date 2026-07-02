import request from '@/utils/request'

export type ProjectStatus = 'active' | 'paused' | 'completed'
export type ProjectSubjectType = 'HUMAN' | 'AGENT' | 'SYSTEM'
export type ProjectBusinessSubjectType = 'HUMAN' | 'AGENT'

export interface ProjectSummary {
  id: number
  name: string
  status: ProjectStatus | string
  color: string
  humans: number
  agents: number
  revenue: string
  bid: number
  depositPaid: boolean
  deadline: string
  progress: number
  ownerSubjectId: number
  ownerSubjectType: ProjectSubjectType
  ownerDisplayName?: string
  associatedHumanId?: number
  associatedHumanName?: string
  liableHumanId?: number
  agentFullyAuthorized?: boolean
  walletRouting?: string
  responsibilityChain?: string
  pendingBidUpdate?: boolean
}

export interface ProjectMember {
  memberSubjectId: number
  memberSubjectType: ProjectSubjectType
  name: string
  avatarUrl?: string
  isOnline: boolean
  associatedHumanId?: number
  associatedHumanName?: string
  liableHumanId?: number
  creditScore?: number
}

export interface ProjectDetail extends Omit<ProjectSummary, 'pendingBidUpdate'> {
  members: ProjectMember[]
  pendingBidUpdate?: {
    newBid: number
    approvals: string[]
    rejections: string[]
    pending: number
  } | null
  stats: {
    earned: string
    pending: string
    tasksCompleted: number
    tasksTotal: number
    efficiency: string
  }
}

export interface ProjectTask {
  id: number
  title: string
  assigneeSubjectId: number
  assigneeSubjectType: ProjectSubjectType
  assigneeDisplayName: string
  deadline: string
  status: string
  priority: string
}

export interface ProjectPayment {
  id: number
  amount: number
  recipientSubjectId: number
  recipientSubjectType: ProjectBusinessSubjectType
  recipientDisplayName: string
  masterWallet?: string
  walletRouting: string
  directRecipientSubjectId: number
  directRecipientSubjectType: ProjectBusinessSubjectType
  settlementSubjectId: number
  settlementSubjectType: ProjectBusinessSubjectType
  settlementHumanId?: number | null
  financialAutonomy: boolean
  walletPolicyState?: string
  walletPolicyReason?: string | null
  liableHumanId: number
  liabilityRoute: string
  liabilityReason?: string | null
  status: string
  date: string
}

export interface ProjectFile {
  id: number
  name: string
  type: string
  uploaderSubjectId: number
  uploaderSubjectType: ProjectSubjectType
  uploaderDisplayName: string
  size: string
  date: string
  downloadUrl?: string
}

export interface CreateProjectPayload {
  name: string
  bid: number
  ownerSubjectId: number
  ownerSubjectType: ProjectBusinessSubjectType
}

export interface ProjectViewerParams {
  viewerSubjectId?: number
  viewerSubjectType?: ProjectBusinessSubjectType
}

export interface ProjectActorParams {
  actorSubjectId: number
  actorSubjectType: ProjectBusinessSubjectType
}

export interface UpdateProjectBidPayload {
  newBid: number
  actorSubjectId: number
  actorSubjectType: ProjectBusinessSubjectType
}

export interface TransferProjectOwnershipPayload {
  newOwnerSubjectId: number
  newOwnerSubjectType: ProjectBusinessSubjectType
  actorSubjectId: number
  actorSubjectType: ProjectBusinessSubjectType
}

export interface CreateProjectTaskPayload {
  title: string
  value: number
  deadline: string
  priority: 'low' | 'medium' | 'high'
  assigneeSubjectId: number
  assigneeSubjectType: ProjectBusinessSubjectType
  actorSubjectId: number
  actorSubjectType: ProjectBusinessSubjectType
}

export interface CreateProjectPaymentPayload {
  amount: number
  recipientSubjectId: number
  recipientSubjectType: ProjectBusinessSubjectType
  status?: string
  date?: string
  actorSubjectId: number
  actorSubjectType: ProjectBusinessSubjectType
}

export const projectApi = {
  listMyProjects(params?: ProjectViewerParams) {
    return request.get<ProjectSummary[]>('/api/v1/projects', params)
  },
  createProject(data: CreateProjectPayload) {
    return request.post<ProjectDetail>('/api/v1/projects', data)
  },
  getProjectDetail(projectId: number | string, params?: ProjectViewerParams) {
    return request.get<ProjectDetail>(`/api/v1/projects/${projectId}`, params)
  },
  requestBidUpdate(projectId: number | string, data: UpdateProjectBidPayload) {
    return request.post<void>(`/api/v1/projects/${projectId}/bid-update`, data)
  },
  transferOwnership(projectId: number | string, data: TransferProjectOwnershipPayload) {
    return request.post<void>(`/api/v1/projects/${projectId}/transfer`, data)
  },
  shareLink(projectId: number | string, params?: ProjectViewerParams) {
    return request.get<{ url: string }>(`/api/v1/projects/${projectId}/share-link`, params)
  },
  listSidebarTasks(projectId: number | string, params?: ProjectViewerParams) {
    return request.get<ProjectTask[]>(`/api/v1/projects/${projectId}/sidebar/tasks`, params)
  },
  listSidebarPayments(projectId: number | string, params?: ProjectViewerParams) {
    return request.get<ProjectPayment[]>(`/api/v1/projects/${projectId}/sidebar/payments`, params)
  },
  listSidebarFiles(projectId: number | string, params?: ProjectViewerParams) {
    return request.get<ProjectFile[]>(`/api/v1/projects/${projectId}/sidebar/files`, params)
  },
  createTask(projectId: number | string, data: CreateProjectTaskPayload) {
    return request.post<ProjectTask>(`/api/v1/projects/${projectId}/tasks`, data)
  },
  createPayment(projectId: number | string, data: CreateProjectPaymentPayload) {
    return request.post<ProjectPayment>(`/api/v1/projects/${projectId}/payments`, data)
  },
}
