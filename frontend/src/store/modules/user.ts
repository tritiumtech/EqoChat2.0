import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wsClient } from '@/utils/websocket'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(uni.getStorageSync('token') || null)
  const userInfo = ref<any>(uni.getStorageSync('userInfo') || null)
  const isLoggedIn = computed(() => !!token.value)

  const setToken = (newToken: string) => {
    token.value = newToken
    uni.setStorageSync('token', newToken)
    ;(globalThis as any).__EQOCHAT_TOKEN__ = newToken
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
    uni.setStorageSync('userInfo', info)
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    ;(globalThis as any).__EQOCHAT_TOKEN__ = null
    wsClient.close()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout,
  }
})
