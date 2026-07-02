import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { friendRequestApi, type FriendRequestItem, type SendFriendRequestDto } from '@/api/modules/friendRequest'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'

export const useFriendRequestStore = defineStore('friendRequest', () => {
  const activeSubjectStore = useActiveSubjectStore()
  const receivedRequests = ref<FriendRequestItem[]>([])
  const sentRequests = ref<FriendRequestItem[]>([])
  const isLoading = ref(false)
  const lastFetchTime = ref(0)
  const lastSubjectKey = ref('')

  const pendingCount = computed(() =>
    receivedRequests.value.filter((r) => r.status === 'PENDING').length,
  )

  const hasNewRequests = computed(() => pendingCount.value > 0)

  const shouldRefresh = computed(() => {
    const now = Date.now()
    return now - lastFetchTime.value > 5 * 60 * 1000
  })

  const currentSubjectKey = () => {
    const subject = activeSubjectStore.currentSubject
    return subject ? `${subject.subjectType}:${subject.subjectId}` : ''
  }

  const loadReceivedRequests = async (force = false) => {
    if (isLoading.value) return
    await activeSubjectStore.ensureLoaded()
    const subjectKey = currentSubjectKey()
    if (!force && subjectKey === lastSubjectKey.value && !shouldRefresh.value) return

    isLoading.value = true
    try {
      const list = await friendRequestApi.listReceived(activeSubjectStore.friendRequestSubjectParams())
      receivedRequests.value = list
      lastFetchTime.value = Date.now()
      lastSubjectKey.value = subjectKey
    } catch {
      // Keep existing data for a smoother tabbar badge experience.
    } finally {
      isLoading.value = false
    }
  }

  const loadSentRequests = async () => {
    if (isLoading.value) return

    isLoading.value = true
    try {
      await activeSubjectStore.ensureLoaded()
      const list = await friendRequestApi.listSent(activeSubjectStore.friendRequestSubjectParams())
      sentRequests.value = list
    } catch {
      // Keep existing data for a smoother tabbar badge experience.
    } finally {
      isLoading.value = false
    }
  }

  const refreshAll = async () => {
    await loadReceivedRequests(true)
    await loadSentRequests()
  }

  const acceptRequest = async (id: number) => {
    try {
      await friendRequestApi.accept(id)
      const request = receivedRequests.value.find((r) => r.id === id)
      if (request) {
        request.status = 'ACCEPTED'
      }
      return true
    } catch {
      return false
    }
  }

  const rejectRequest = async (id: number) => {
    try {
      await friendRequestApi.reject(id)
      const request = receivedRequests.value.find((r) => r.id === id)
      if (request) {
        request.status = 'REJECTED'
      }
      return true
    } catch {
      return false
    }
  }

  const sendRequest = async (data: SendFriendRequestDto) => {
    try {
      const result = await friendRequestApi.sendRequest(data)
      sentRequests.value.unshift(result)
      return true
    } catch {
      return false
    }
  }

  const clear = () => {
    receivedRequests.value = []
    sentRequests.value = []
    lastFetchTime.value = 0
    lastSubjectKey.value = ''
  }

  return {
    receivedRequests,
    sentRequests,
    isLoading,
    pendingCount,
    hasNewRequests,
    shouldRefresh,
    loadReceivedRequests,
    loadSentRequests,
    refreshAll,
    acceptRequest,
    rejectRequest,
    sendRequest,
    clear,
  }
})
