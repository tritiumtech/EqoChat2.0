import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, type NotificationItem } from '@/api/modules/notification'
import { wsClient } from '@/utils/websocket'
import type { BaseMessage } from '@/types/websocket'

export const useNotificationStore = defineStore('notification', () => {
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

  /**
   * 处理 WebSocket 推送的通知
   */
  const handleNotification = (payload: unknown, message: BaseMessage) => {
    const notification = payload as NotificationItem
    if (!notification || !notification.id) return

    // 避免重复添加
    const exists = notifications.value.some((n) => n.id === notification.id)
    if (exists) return

    // 添加到列表顶部
    notifications.value.unshift({
      ...notification,
      read: false,
    })

    // 显示提示
    uni.showToast({
      title: notification.title || '新通知',
      icon: 'none',
      duration: 2000,
    })
  }

  /**
   * 启动 WebSocket 监听通知
   */
  const startRealtime = () => {
    if (wsListenerId.value) return
    wsListenerId.value = wsClient.addListener({
      onNotification: handleNotification,
    })
  }

  /**
   * 停止 WebSocket 监听
   */
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