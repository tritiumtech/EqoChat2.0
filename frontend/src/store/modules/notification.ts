import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, type NotificationItem } from '@/api/modules/notification'
import { wsClient } from '@/utils/websocket'
import type { BaseMessage } from '@/types/websocket'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'

export const useNotificationStore = defineStore('notification', () => {
  const activeSubjectStore = useActiveSubjectStore()
  const notifications = ref<NotificationItem[]>([])
  const isLoading = ref(false)
  const wsListenerId = ref<string | null>(null)

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
      await activeSubjectStore.ensureLoaded()
      const list = await notificationApi.list(limit, activeSubjectStore.notificationRecipientParams())
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
      await activeSubjectStore.ensureLoaded()
      await notificationApi.markRead(id, activeSubjectStore.notificationRecipientParams())
    } catch {
      // Keep the optimistic UX smooth; the next reload restores canonical state.
    }
  }

  const isForCurrentSubject = (notification: NotificationItem, message: BaseMessage) => {
    const current = activeSubjectStore.currentSubject
    if (!current) return true
    const rawId = notification.recipientSubjectId ?? message.recipientSubjectId
    const rawType = notification.recipientSubjectType ?? message.recipientSubjectType
    if (rawId == null || rawType == null) return true
    const recipientId = Number(rawId)
    const recipientType = String(rawType).toUpperCase()
    return recipientId === current.subjectId && recipientType === current.subjectType
  }

  const handleNotification = (payload: unknown, message: BaseMessage) => {
    const notification = payload as NotificationItem
    if (!notification || !notification.id) return
    if (!isForCurrentSubject(notification, message)) return

    const exists = notifications.value.some((n) => n.id === notification.id)
    if (exists) return

    notifications.value.unshift({
      ...notification,
      read: false,
    })

    uni.showToast({
      title: notification.title || 'New notification',
      icon: 'none',
      duration: 2000,
    })
  }

  const startRealtime = () => {
    if (wsListenerId.value) return
    wsListenerId.value = wsClient.addListener({
      onNotification: handleNotification,
    })
  }

  const stopRealtime = () => {
    if (wsListenerId.value) {
      wsClient.removeListener(wsListenerId.value)
      wsListenerId.value = null
    }
  }

  return {
    notifications,
    isLoading,
    unreadCount,
    setNotifications,
    loadNotifications,
    markRead,
    startRealtime,
    stopRealtime,
  }
})
