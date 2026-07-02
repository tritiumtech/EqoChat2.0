import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { agentApi, type MyAgentItem } from '@/api/modules/agent'
import { useUserStore } from '@/store/modules/user'
import type { ContactOwnerSubjectParams, ContactSubjectType } from '@/api/modules/contact'
import type { ConversationViewerParams, CreateConversationRequest } from '@/api/modules/conversation'
import type { FriendRequestSubjectParams } from '@/api/modules/friendRequest'
import type { NotificationRecipientSubjectParams } from '@/api/modules/notification'
import type { CreateProjectPayload, ProjectActorParams, ProjectViewerParams } from '@/api/modules/project'
import type { SubjectViewerParams } from '@/api/modules/subject'
import type { WorldSubjectParams } from '@/api/modules/world'

const STORAGE_KEY = 'activeSubject'

export interface ActiveSubject {
  subjectId: number
  subjectType: ContactSubjectType
  displayName: string
  avatarUrl?: string
  source: 'HUMAN' | 'AGENT'
}

type StoredSubject = Pick<ActiveSubject, 'subjectId' | 'subjectType'>

function readStoredSubject(): StoredSubject | null {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return null
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    const subjectId = Number(parsed?.subjectId)
    const subjectType = String(parsed?.subjectType || '').toUpperCase()
    if (!Number.isFinite(subjectId) || subjectId <= 0) return null
    if (subjectType !== 'HUMAN' && subjectType !== 'AGENT') return null
    return { subjectId, subjectType: subjectType as ContactSubjectType }
  } catch {
    return null
  }
}

function writeStoredSubject(subject: StoredSubject | null) {
  if (!subject) {
    uni.removeStorageSync(STORAGE_KEY)
    return
  }
  uni.setStorageSync(STORAGE_KEY, subject)
}

export const useActiveSubjectStore = defineStore('activeSubject', () => {
  const userStore = useUserStore()
  const selected = ref<StoredSubject | null>(readStoredSubject())
  const agents = ref<MyAgentItem[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  let refreshPromise: Promise<void> | null = null

  const humanSubject = computed<ActiveSubject | null>(() => {
    const id = Number(userStore.userInfo?.id)
    if (!Number.isFinite(id) || id <= 0) return null
    return {
      subjectId: id,
      subjectType: 'HUMAN',
      displayName: userStore.userInfo?.nickname || `HUMAN ${id}`,
      avatarUrl: userStore.userInfo?.avatarUrl,
      source: 'HUMAN',
    }
  })

  const agentSubjects = computed<ActiveSubject[]>(() => {
    return agents.value
      .map((agent) => {
        const subjectId = Number(agent.agentSubjectId ?? agent.id)
        if (!Number.isFinite(subjectId) || subjectId <= 0) return null
        if (agent.agentSubjectType && agent.agentSubjectType !== 'AGENT') return null
        return {
          subjectId,
          subjectType: 'AGENT' as const,
          displayName: agent.name || `AGENT ${subjectId}`,
          avatarUrl: agent.avatarUrl,
          source: 'AGENT' as const,
        }
      })
      .filter((item): item is ActiveSubject => !!item)
  })

  const subjects = computed<ActiveSubject[]>(() => {
    const out: ActiveSubject[] = []
    if (humanSubject.value) out.push(humanSubject.value)
    out.push(...agentSubjects.value)
    return out
  })

  const currentSubject = computed<ActiveSubject | null>(() => {
    const wanted = selected.value
    if (!wanted) return null
    return subjects.value.find(
      (item) => item.subjectId === wanted.subjectId && item.subjectType === wanted.subjectType,
    ) ?? null
  })

  const currentLabel = computed(() => currentSubject.value?.displayName || '')
  const isAgentActive = computed(() => currentSubject.value?.subjectType === 'AGENT')

  const setActiveSubject = (subject: StoredSubject) => {
    const match = subjects.value.find(
      (item) => item.subjectId === subject.subjectId && item.subjectType === subject.subjectType,
    )
    if (!match) return false
    selected.value = { subjectId: match.subjectId, subjectType: match.subjectType }
    writeStoredSubject(selected.value)
    return true
  }

  const setHuman = () => {
    if (!humanSubject.value) return false
    selected.value = {
      subjectId: humanSubject.value.subjectId,
      subjectType: humanSubject.value.subjectType,
    }
    writeStoredSubject(selected.value)
    return true
  }

  const refresh = async () => {
    if (refreshPromise) return refreshPromise
    loading.value = true
    refreshPromise = (async () => {
      try {
        userStore.syncFromStorage()
        if (!userStore.isLoggedIn) {
          agents.value = []
          selected.value = null
          writeStoredSubject(null)
          loaded.value = true
          return
        }
        agents.value = await agentApi.getMyAgents()
        const wanted = selected.value
        if (!wanted) {
          writeStoredSubject(null)
        } else if (!subjects.value.some((item) => item.subjectId === wanted.subjectId && item.subjectType === wanted.subjectType)) {
          selected.value = null
          writeStoredSubject(null)
        }
        loaded.value = true
      } catch {
        agents.value = []
        loaded.value = true
      } finally {
        loading.value = false
        refreshPromise = null
      }
    })()
    return refreshPromise
  }

  const ensureLoaded = async (force = false) => {
    if (force || !loaded.value) {
      await refresh()
    }
  }

  const contactOwnerParams = (): ContactOwnerSubjectParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { ownerSubjectId: subject.subjectId, ownerSubjectType: subject.subjectType }
  }

  const friendRequestSubjectParams = (): FriendRequestSubjectParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { subjectId: subject.subjectId, subjectType: subject.subjectType }
  }

  const notificationRecipientParams = (): NotificationRecipientSubjectParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { recipientSubjectId: subject.subjectId, recipientSubjectType: subject.subjectType }
  }

  const worldSubjectParams = (): WorldSubjectParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { subjectId: subject.subjectId, subjectType: subject.subjectType }
  }

  const conversationViewerParams = (): ConversationViewerParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { viewerSubjectId: subject.subjectId, viewerSubjectType: subject.subjectType }
  }

  const conversationCreatorParams = (): Pick<CreateConversationRequest, 'creatorSubjectId' | 'creatorSubjectType'> => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { creatorSubjectId: subject.subjectId, creatorSubjectType: subject.subjectType }
  }

  const projectViewerParams = (): ProjectViewerParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { viewerSubjectId: subject.subjectId, viewerSubjectType: subject.subjectType }
  }

  const projectOwnerParams = (): Pick<CreateProjectPayload, 'ownerSubjectId' | 'ownerSubjectType'> => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { ownerSubjectId: subject.subjectId, ownerSubjectType: subject.subjectType }
  }

  const projectActorParams = (): ProjectActorParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { actorSubjectId: subject.subjectId, actorSubjectType: subject.subjectType }
  }

  const subjectViewerParams = (): SubjectViewerParams => {
    const subject = currentSubject.value
    if (!subject) {
      throw new Error('active subject unavailable')
    }
    return { viewerSubjectId: subject.subjectId, viewerSubjectType: subject.subjectType }
  }

  return {
    selected,
    agents,
    loading,
    loaded,
    humanSubject,
    agentSubjects,
    subjects,
    currentSubject,
    currentLabel,
    isAgentActive,
    setActiveSubject,
    setHuman,
    refresh,
    ensureLoaded,
    contactOwnerParams,
    friendRequestSubjectParams,
    notificationRecipientParams,
    worldSubjectParams,
    conversationViewerParams,
    conversationCreatorParams,
    projectViewerParams,
    projectOwnerParams,
    projectActorParams,
    subjectViewerParams,
  }
})
