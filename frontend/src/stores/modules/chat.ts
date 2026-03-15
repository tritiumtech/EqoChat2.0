import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // State
  const conversations = ref<any[]>([])
  const unreadCount = ref(0)
  
  // Getters
  const totalUnread = computed(() => {
    return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  })
  
  // Actions
  const setConversations = (list: any[]) => {
    conversations.value = list
  }
  
  const updateUnreadCount = (count: number) => {
    unreadCount.value = count
  }
  
  return {
    conversations,
    unreadCount,
    totalUnread,
    setConversations,
    updateUnreadCount
  }
})