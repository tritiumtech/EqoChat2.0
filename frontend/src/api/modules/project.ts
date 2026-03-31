import request from '@/utils/request'

export type ProjectStatus = 'active' | 'paused' | 'completed'
export type ProjectOwnerType = 'human' | 'agent'

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
  ownerId: number
  ownerType: ProjectOwnerType | string
  pendingBidUpdate?: boolean
}

export interface ProjectMember {
  id: number
  name: string
  avatarUrl?: string
  type: ProjectOwnerType | string
  isOnline: boolean
  masterId?: number
  creditScore?: number
}

export interface ProjectDetail extends ProjectSummary {
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
  assignee: string
  isAgent: boolean
  deadline: string
  status: string
  priority: string
}

export interface ProjectPayment {
  id: number
  amount: number
  recipient: string
  isAgent: boolean
  masterWallet?: string
  status: string
  date: string
}

export interface ProjectFile {
  id: number
  name: string
  type: string
  uploadedBy: string
  isAgent: boolean
  size: string
  date: string
  downloadUrl?: string
}

export interface CreateProjectPayload {
  name: string
  bid: number
}

export interface UpdateProjectBidPayload {
  newBid: number
}

export interface TransferProjectOwnershipPayload {
  toMemberId: number
  toMemberType: 'HUMAN' | 'AGENT'
}

export const projectApi = {
  listMyProjects() {
    return request.get<ProjectSummary[]>('/api/v1/projects')
  },
  createProject(data: CreateProjectPayload) {
    return request.post<ProjectDetail>('/api/v1/projects', data)
  },
  getProjectDetail(projectId: number | string) {
    return request.get<ProjectDetail>(`/api/v1/projects/${projectId}`)
  },
  requestBidUpdate(projectId: number | string, data: UpdateProjectBidPayload) {
    return request.post<void>(`/api/v1/projects/${projectId}/bid-update`, data)
  },
  transferOwnership(projectId: number | string, data: TransferProjectOwnershipPayload) {
    return request.post<void>(`/api/v1/projects/${projectId}/transfer`, data)
  },
  shareLink(projectId: number | string) {
    return request.get<{ url: string }>(`/api/v1/projects/${projectId}/share-link`)
  },
  listSidebarTasks(projectId: number | string) {
    return request.get<ProjectTask[]>(`/api/v1/projects/${projectId}/sidebar/tasks`)
  },
  listSidebarPayments(projectId: number | string) {
    return request.get<ProjectPayment[]>(`/api/v1/projects/${projectId}/sidebar/payments`)
  },
  listSidebarFiles(projectId: number | string) {
    return request.get<ProjectFile[]>(`/api/v1/projects/${projectId}/sidebar/files`)
  },
}

