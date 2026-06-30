import request from '@/utils/request'

export interface MyAgentItem {
  id: number
  name: string
  avatarUrl?: string
  description?: string
  agentType?: string
  permissionLevel?: string
  creditScore?: number
  ownerId?: number
  ownerName?: string
  ownerType?: 'human' | 'agent' | string
  capabilities: string[]
  liabilityAccepted?: boolean
  walletEnabled: boolean
  walletRouting?: string
  responsibilityChain?: string
  earnings: number
}

export const agentApi = {
  getMyAgents() {
    return request.get<MyAgentItem[]>('/api/v1/agents/me')
  },
}
