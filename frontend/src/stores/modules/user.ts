import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(uni.getStorageSync('token') || null)
  const userInfo = ref<any>(null)
  const isLoggedIn = computed(() => !!token.value)
  
  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }
  
  const setUserInfo = (info: any) => {
    userInfo.value = info
  }
  
  const logout = () => {
    token.value = null
    userInfo.value = null
    uni.removeStorageSync('token')
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout
  }
})