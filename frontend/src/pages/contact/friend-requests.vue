<template>
  <view class="page">
    <!-- 页面头部 -->
    <PageHeader
      :title="t('page.contact.new_friends')"
      show-back-icon
      has-tabs
      @back-click="goBack"
    >
      <template #tabs>
        <view class="tab-bar">
          <view
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <text class="tab-text">{{ tab.label }}</text>
            <text v-if="tab.badge > 0" class="tab-badge">{{ tab.badge > 99 ? '99+' : tab.badge }}</text>
          </view>
        </view>
      </template>
    </PageHeader>

    <!-- 收到的申请 -->
    <scroll-view v-if="activeTab === 'received'" class="request-list" scroll-y>
      <view v-if="receivedLoading" class="state-loading">
        <u-loading-icon mode="circle" size="28" color="#030213" />
        <text class="state-text">{{ t('common.loading') }}</text>
      </view>

      <view v-else-if="receivedRequests.length === 0" class="state-empty">
        <view class="empty-icon-wrap">
          <u-icon name="search" :size="48" color="#717182" />
        </view>
        <text class="empty-title">{{ t('page.contact.no_requests') }}</text>
        <text class="empty-sub">{{ t('page.contact.no_requests_hint') }}</text>
      </view>

      <view v-else class="request-items">
        <view
          v-for="req in receivedRequests"
          :key="req.id"
          class="request-card"
        >
          <view class="requester-info" @click="goToSubjectProfile(req.requesterSubjectId, req.requesterSubjectType)">
            <view class="requester-avatar-wrap">
              <image
                v-if="req.requesterAvatarUrl"
                class="requester-avatar"
                :src="req.requesterAvatarUrl"
                mode="aspectFill"
              />
              <view v-else class="requester-avatar" :style="getAvatarStyle(req.requesterNickname, req.requesterSubjectId)">
                <text class="avatar-text">{{ (req.requesterNickname || '?').slice(0, 1) }}</text>
              </view>
            </view>
            <view class="requester-meta">
              <view class="name-row">
                <text class="requester-name">{{ req.requesterNickname || `${req.requesterSubjectType} ${req.requesterSubjectId}` }}</text>
                <text v-if="req.requesterSubjectType === 'AGENT'" class="status-tag pending">{{ t('page.world.ai_agent') }}</text>
              </view>
              <text class="request-time">{{ t('page.contact.request_received_at') }} {{ formatTime(req.createTime) }}</text>
            </view>
          </view>

          <view v-if="req.requestMessage" class="request-message">
            <text class="message-label">{{ t('page.contact.request_message_label') }}:</text>
            <text class="message-content">{{ req.requestMessage }}</text>
          </view>

          <view v-if="req.status === 'PENDING'" class="request-actions">
            <button class="action-btn reject" @click="handleReject(req.id)">
              <text class="btn-text">{{ t('action.reject') }}</text>
            </button>
            <button class="action-btn accept" @click="handleAccept(req.id)">
              <text class="btn-text">{{ t('action.accept') }}</text>
            </button>
          </view>

          <view v-else class="request-status">
            <text :class="['status-text', req.status.toLowerCase()]">
              {{ req.status === 'ACCEPTED' ? t('page.contact.request_accepted') : t('page.contact.request_rejected') }}
            </text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 已发送的申请 -->
    <scroll-view v-else class="request-list" scroll-y>
      <view v-if="sentLoading" class="state-loading">
        <u-loading-icon mode="circle" size="28" color="#030213" />
        <text class="state-text">{{ t('common.loading') }}</text>
      </view>

      <view v-else-if="sentRequests.length === 0" class="state-empty">
        <view class="empty-icon-wrap">
          <u-icon name="search" :size="48" color="#717182" />
        </view>
        <text class="empty-title">{{ t('page.contact.no_sent_requests') }}</text>
        <text class="empty-sub">{{ t('page.contact.no_sent_requests_hint') }}</text>
      </view>

      <view v-else class="request-items">
        <view
          v-for="req in sentRequests"
          :key="req.id"
          class="request-card"
        >
          <view class="requester-info" @click="goToSubjectProfile(req.recipientSubjectId, req.recipientSubjectType)">
            <view class="requester-avatar-wrap">
              <image
                v-if="req.recipientAvatarUrl"
                class="requester-avatar"
                :src="req.recipientAvatarUrl"
                mode="aspectFill"
              />
              <view v-else class="requester-avatar" :style="getAvatarStyle(req.recipientNickname, req.recipientSubjectId)">
                <text class="avatar-text">{{ (req.recipientNickname || '?').slice(0, 1) }}</text>
              </view>
            </view>
            <view class="requester-meta">
              <view class="name-row">
                <text class="requester-name">{{ req.recipientNickname || `${req.recipientSubjectType} ${req.recipientSubjectId}` }}</text>
                <text v-if="req.recipientSubjectType === 'AGENT'" class="status-tag pending">{{ t('page.world.ai_agent') }}</text>
                <text :class="['status-tag', req.status.toLowerCase()]">
                  {{ getStatusText(req.status) }}
                </text>
              </view>
              <text class="request-time">{{ formatTime(req.createTime) }}</text>
            </view>
          </view>

          <view v-if="req.requestMessage" class="request-message">
            <text class="message-label">{{ t('page.contact.request_message_label') }}:</text>
            <text class="message-content">{{ req.requestMessage }}</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import type { ContactSubjectType } from '@/api/modules/contact'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import { wsClient } from '@/utils/websocket'
import type { BaseMessage } from '@/types/websocket'
import PageHeader from '@/components/PageHeader.vue'

const { t, tf } = useI18nWithFormat()
const userStore = useUserStore()

// WebSocket 监听 ID
let wsListenerId: string | null = null

// Tab 状态
const activeTab = ref<'received' | 'sent'>('received')

// 数据
const receivedRequests = ref<FriendRequestItem[]>([])
const sentRequests = ref<FriendRequestItem[]>([])
const receivedLoading = ref(false)
const sentLoading = ref(false)

// Tab 配置
const tabs = computed(() => [
  { 
    key: 'received' as const, 
    label: t('page.contact.received_requests'),
    badge: pendingCount.value
  },
  { 
    key: 'sent' as const, 
    label: t('page.contact.sent_requests'),
    badge: 0
  }
])

// 待处理数量
const pendingCount = computed(() => 
  receivedRequests.value.filter(r => r.status === 'PENDING').length
)

// 头像样式
const getAvatarStyle = (nickname?: string, id?: number) => {
  const hues = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  const s = nickname || String(id || 0)
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  const c = hues[Math.abs(h) % hues.length]!
  return { background: `linear-gradient(135deg, ${c}f0, ${c}c0)` }
}

// 格式化时间
const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  // 小于 1 小时
  if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000)
    return minutes < 1 ? t('common.just_now') : tf('common.minutes_ago', { n: minutes })
  }
  // 小于 24 小时
  if (diff < 86400000) {
    return tf('common.hours_ago', { n: Math.floor(diff / 3600000) })
  }
  // 小于 7 天
  if (diff < 604800000) {
    return tf('common.days_ago', { n: Math.floor(diff / 86400000) })
  }
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 获取状态文本
const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING':
      return t('page.contact.request_status_pending')
    case 'ACCEPTED':
      return t('page.contact.request_status_accepted')
    case 'REJECTED':
      return t('page.contact.request_status_rejected')
    default:
      return status
  }
}

// 返回上一页
const goBack = () => {
  uni.navigateBack()
}

// 跳转到主体资料
const goToSubjectProfile = (subjectId: number, subjectType: ContactSubjectType) => {
  if (subjectType === 'AGENT') {
    uni.navigateTo({
      url: `/pages/contact/contact-detail?targetSubjectType=AGENT&targetSubjectId=${subjectId}`
    })
    return
  }
  uni.navigateTo({
    url: `/pages/contact/user-profile?targetSubjectType=HUMAN&targetSubjectId=${subjectId}`
  })
}

// 加载收到的申请
const loadReceivedRequests = async () => {
  receivedLoading.value = true
  try {
    receivedRequests.value = await friendRequestApi.listReceived()
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
    receivedRequests.value = []
  } finally {
    receivedLoading.value = false
  }
}

// 加载已发送的申请
const loadSentRequests = async () => {
  sentLoading.value = true
  try {
    sentRequests.value = await friendRequestApi.listSent()
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
    sentRequests.value = []
  } finally {
    sentLoading.value = false
  }
}

// 同意申请
const handleAccept = async (id: number) => {
  try {
    await friendRequestApi.accept(id)
    uni.showToast({ title: t('toast.request_accepted'), icon: 'success' })
    // 刷新列表
    await loadReceivedRequests()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

// 拒绝申请
const handleReject = async (id: number) => {
  try {
    await friendRequestApi.reject(id)
    uni.showToast({ title: t('toast.request_rejected'), icon: 'success' })
    // 刷新列表
    await loadReceivedRequests()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

// 处理 WebSocket 通知
const handleNotification = (payload: unknown, message: BaseMessage) => {
  const data = payload as { type?: string; requestId?: number }
  if (data?.type === 'FRIEND_REQUEST') {
    // 收到新的好友请求，刷新列表
    loadReceivedRequests()
    // 显示提示
    uni.showToast({
      title: '收到新的好友请求',
      icon: 'none',
      duration: 2000,
    })
  }
}

// 启动 WebSocket 监听
const startWsListener = () => {
  if (wsListenerId) return
  wsListenerId = wsClient.addListener({
    onNotification: handleNotification,
  })
}

// 停止 WebSocket 监听
const stopWsListener = () => {
  if (wsListenerId) {
    wsClient.removeListener(wsListenerId)
    wsListenerId = null
  }
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.new_friends') })
  loadReceivedRequests()
  loadSentRequests()
  startWsListener()
})

onHide(() => {
  stopWsListener()
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  background: #ffffff;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

/* Tab 栏 */
.tab-bar {
  display: flex;
  padding: 0 32rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  background: #ffffff;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 24rpx 0;
  position: relative;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: var(--c-primary);
  border-radius: 2rpx;
}

.tab-text {
  font-size: 28rpx;
  font-weight: 500;
  color: var(--c-muted);
}

.tab-item.active .tab-text {
  color: var(--c-ink);
  font-weight: 700;
}

.tab-badge {
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: rgba(239, 68, 68, 0.95);
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 请求列表 */
.request-list {
  flex: 1;
  padding: 24rpx 32rpx;
  padding-bottom: var(--page-pad-bottom-tabbar);
}

.state-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 120rpx 0;
}

.state-text {
  font-size: 26rpx;
  color: var(--c-muted);
}

.state-empty {
  text-align: center;
  padding: 120rpx 32rpx;
}

.empty-icon-wrap {
  width: 128rpx;
  height: 128rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: rgba(243, 243, 245, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
  display: block;
  margin-bottom: 12rpx;
}

.empty-sub {
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
}

/* 请求卡片 */
.request-items {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.request-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  padding: 24rpx;
  box-shadow: var(--c-shadow-soft);
}

.requester-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.requester-info:active {
  opacity: 0.7;
}

.requester-avatar-wrap {
  flex-shrink: 0;
}

.requester-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.08);
}

.avatar-text {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.requester-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.requester-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: var(--radius-pill);
  font-size: 20rpx;
  font-weight: 600;
}

.status-tag.pending {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.status-tag.accepted {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.status-tag.rejected {
  background: rgba(113, 113, 130, 0.12);
  color: var(--c-muted);
}

.request-time {
  font-size: 22rpx;
  color: var(--c-muted);
}

.request-message {
  margin-top: 16rpx;
  padding: 16rpx;
  background: rgba(243, 243, 245, 0.85);
  border-radius: var(--radius-md);
}

.message-label {
  font-size: 22rpx;
  color: var(--c-muted);
  display: block;
  margin-bottom: 8rpx;
}

.message-content {
  font-size: 26rpx;
  color: var(--c-ink);
  display: block;
  line-height: 1.5;
}

.request-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.action-btn {
  flex: 1;
  height: 80rpx;
  border-radius: var(--radius-lg);
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn.reject {
  background: rgba(3, 2, 19, 0.06);
}

.action-btn.reject .btn-text {
  color: var(--c-ink);
}

.action-btn.accept {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
}

.action-btn.accept .btn-text {
  color: #fff;
}

.btn-text {
  font-size: 28rpx;
  font-weight: 700;
}

.request-status {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx dashed rgba(0, 0, 0, 0.06);
  text-align: center;
}

.status-text {
  font-size: 24rpx;
  font-weight: 600;
}

.status-text.pending {
  color: #f59e0b;
}

.status-text.accepted {
  color: #10b981;
}

.status-text.rejected {
  color: var(--c-muted);
}
</style>
