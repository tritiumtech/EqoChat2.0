import request from '@/utils/request'

export interface MyAgentItem {
  id: number
  name: string
  avatarUrl?: string
  description?: string
  agentType?: string
  permissionLevel?: string
  creditScore?: number
  capabilities: string[]
  walletEnabled: boolean
  earnings: number
}

export const agentApi = {
  getMyAgents() {
    return request.get<MyAgentItem[]>('/api/v1/agents/me')
  },
}

