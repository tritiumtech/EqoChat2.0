import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, type NotificationItem } from '@/api/modules/notification'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationItem[]>([])
  const isLoading = ref(false)

  const unreadCount = computed(() => {
    return notifications.value.filter((n) => !n.read).length
  })

  const setNotifications = (list: NotificationItem[]) => {
    notifications.value = Array.isArray(list) ? [...list] : []
  }

  const loadNotifications = async (limit = 30) => {
    if (isLoading.value) return
    isLoading.value = true
    try {
      const list = await notificationApi.list(limit)
      setNotifications(list)
    } finally {
      isLoading.value = false
    }
  }

  const markRead = async (id: number) => {
    const target = notifications.value.find((n) => n.id === id)
    if (!target || target.read) return
    target.read = true
    try {
      await notificationApi.markRead(id)
    } catch {
      // 忽略回滚，保持 UX 流畅
    }
  }

  return {
    notifications,
    isLoading,
    unreadCount,
    setNotifications,
    loadNotifications,
    markRead,
  }
})