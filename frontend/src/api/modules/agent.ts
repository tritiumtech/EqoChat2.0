import request from '@/utils/request'

export type AgentSubjectType = 'HUMAN' | 'AGENT' | 'SYSTEM'

export interface CapabilityPolicyItem {
  code: string
  state?: string
  reason?: string | null
}

export interface MyAgentItem {
  id: number
  name: string
  avatarUrl?: string
  description?: string
  agentType?: string
  permissionLevel?: string
  creditScore?: number
  agentSubjectId?: number
  agentSubjectType?: AgentSubjectType
  ownerSubjectId?: number
  ownerSubjectType?: AgentSubjectType
  ownerId?: number
  ownerName?: string
  ownerType?: 'human' | 'agent' | string
  capabilities: string[]
  profileCapabilities?: string[]
  capabilityPolicy?: CapabilityPolicyItem[]
  liabilityAccepted?: boolean
  bindingLiabilityAccepted?: boolean
  liableHumanId?: number
  liabilityRoute?: string
  liabilityReason?: string | null
  walletEnabled: boolean
  walletPolicyState?: string
  walletRouting?: string
  walletPolicyReason?: string | null
  directRecipientSubjectId?: number
  directRecipientSubjectType?: AgentSubjectType
  settlementSubjectId?: number
  settlementSubjectType?: AgentSubjectType
  settlementHumanId?: number | null
  financialAutonomy?: boolean
  responsibilityChain?: string
  earnings: number
}

export type AgentWalletPolicyResponse = Pick<
  MyAgentItem,
  | 'walletEnabled'
  | 'walletPolicyState'
  | 'walletRouting'
  | 'walletPolicyReason'
  | 'directRecipientSubjectId'
  | 'directRecipientSubjectType'
  | 'settlementSubjectId'
  | 'settlementSubjectType'
  | 'settlementHumanId'
  | 'financialAutonomy'
>

export const agentApi = {
  getMyAgents() {
    return request.get<MyAgentItem[]>('/api/v1/agents/me')
  },

  enableWallet(agentId: number) {
    return request.post<AgentWalletPolicyResponse>(`/api/v1/agents/${agentId}/wallet/enable`)
  },

  disableWallet(agentId: number, reason?: string) {
    return request.post<AgentWalletPolicyResponse>(`/api/v1/agents/${agentId}/wallet/disable`, { reason })
  },
}
