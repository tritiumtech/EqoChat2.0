import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { friendRequestApi, type FriendRequestItem, type SendFriendRequestDto } from '@/api/modules/friendRequest'

export const useFriendRequestStore = defineStore('friendRequest', () => {
  // 状态
  const receivedRequests = ref<FriendRequestItem[]>([])
  const sentRequests = ref<FriendRequestItem[]>([])
  const isLoading = ref(false)
  const lastFetchTime = ref(0)

  // 计算属性
  const pendingCount = computed(() => 
    receivedRequests.value.filter(r => r.status === 'PENDING').length
  )

  const hasNewRequests = computed(() => pendingCount.value > 0)

  // 是否需要刷新（5分钟内不重复请求）
  const shouldRefresh = computed(() => {
    const now = Date.now()
    return now - lastFetchTime.value > 5 * 60 * 1000
  })

  // 加载收到的申请
  const loadReceivedRequests = async (force = false) => {
    if (isLoading.value) return
    if (!force && !shouldRefresh.value) return

    isLoading.value = true
    try {
      const list = await friendRequestApi.listReceived()
      receivedRequests.value = list
      lastFetchTime.value = Date.now()
    } catch {
      // 保持现有数据
    } finally {
      isLoading.value = false
    }
  }

  // 加载已发送的申请
  const loadSentRequests = async () => {
    if (isLoading.value) return

    isLoading.value = true
    try {
      const list = await friendRequestApi.listSent()
      sentRequests.value = list
    } catch {
      // 保持现有数据
    } finally {
      isLoading.value = false
    }
  }

  // 刷新所有申请
  const refreshAll = async () => {
    await Promise.all([
      loadReceivedRequests(true),
      loadSentRequests()
    ])
  }

  // 同意申请
  const acceptRequest = async (id: number) => {
    try {
      await friendRequestApi.accept(id)
      // 更新本地状态
      const request = receivedRequests.value.find(r => r.id === id)
      if (request) {
        request.status = 'ACCEPTED'
      }
      return true
    } catch {
      return false
    }
  }

  // 拒绝申请
  const rejectRequest = async (id: number) => {
    try {
      await friendRequestApi.reject(id)
      // 更新本地状态
      const request = receivedRequests.value.find(r => r.id === id)
      if (request) {
        request.status = 'REJECTED'
      }
      return true
    } catch {
      return false
    }
  }

  // 发送申请
  const sendRequest = async (data: SendFriendRequestDto) => {
    try {
      const result = await friendRequestApi.sendRequest(data)
      // 添加到已发送列表
      sentRequests.value.unshift(result)
      return true
    } catch {
      return false
    }
  }

  // 清除状态（退出登录时）
  const clear = () => {
    receivedRequests.value = []
    sentRequests.value = []
    lastFetchTime.value = 0
  }

  return {
    // 状态
    receivedRequests,
    sentRequests,
    isLoading,
    // 计算属性
    pendingCount,
    hasNewRequests,
    shouldRefresh,
    // 方法
    loadReceivedRequests,
    loadSentRequests,
    refreshAll,
    acceptRequest,
    rejectRequest,
    sendRequest,
    clear
  }
})
