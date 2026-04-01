import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wsClient } from '@/utils/websocket'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(uni.getStorageSync('token') || null)
  const sessionId = ref<string | null>(uni.getStorageSync('sessionId') || null)
  const userInfo = ref<any>(uni.getStorageSync('userInfo') || null)
  const isLoggedIn = computed(() => !!token.value)

  const setToken = (newToken: string) => {
    token.value = newToken
    uni.setStorageSync('token', newToken)
    ;(globalThis as any).__EQOCHAT_TOKEN__ = newToken
  }

  const setSessionId = (newSessionId: string) => {
    sessionId.value = newSessionId
    uni.setStorageSync('sessionId', newSessionId)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  const logout = () => {
    token.value = null
    sessionId.value = null
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('sessionId')
    uni.removeStorageSync('userInfo')
    ;(globalThis as any).__EQOCHAT_TOKEN__ = null
    wsClient.close()
  }

  /** 与本地 storage 对齐（请求续期 token、多端恢复等场景） */
  const syncFromStorage = () => {
    const raw = uni.getStorageSync('token')
    token.value = raw ? String(raw) : null
    const info = uni.getStorageSync('userInfo')
    userInfo.value = info ?? null
    ;(globalThis as any).__EQOCHAT_TOKEN__ = token.value
  }

  return {
    token,
    sessionId,
    userInfo,
    isLoggedIn,
    setToken,
    setSessionId,
    setUserInfo,
    logout,
    syncFromStorage,
  }
})
