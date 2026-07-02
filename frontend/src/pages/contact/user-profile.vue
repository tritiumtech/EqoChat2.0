<template>
  <view class="page">
    <view v-if="loading" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="!userInfo" class="state">{{ t('toast.load_failed') }}</view>
    <view v-else>
      <!-- 页面头部 -->
      <PageHeader
        :title="t('page.contact.user_profile')"
        show-back-icon
        @back-click="goBack"
      />

      <view class="page-inner">
        <!-- 用户资料卡片 -->
        <view class="profile-card">
          <view class="profile-header">
            <view class="avatar-wrap">
              <image
                v-if="userInfo.avatarUrl"
                class="avatar-img"
                :src="userInfo.avatarUrl"
                mode="aspectFill"
              />
              <view v-else class="avatar" :style="avatarStyle">
                <text class="avatar-letter">{{ avatarLetter }}</text>
              </view>
              <view v-if="isOnline" class="online-dot" />
            </view>
            <view class="profile-main">
              <view class="name-row">
                <text class="name">{{ userInfo.nickname }}</text>
                <text v-if="isAgent" class="agent-badge">{{ t('page.world.ai_agent') }}</text>
              </view>
              <text v-if="userInfo.bio" class="bio">{{ userInfo.bio }}</text>
              <text class="meta-line">{{ t('page.contact.user_id') }}: {{ targetSubjectId }}</text>
              <text class="meta-line">{{ tf('page.contact.world_posts_line', { n: userInfo.worldPostCount }) }}</text>
            </view>
          </view>
        </view>

        <!-- 操作按钮区 -->
        <view class="action-section">
          <!-- 已是好友 -->
          <view v-if="isFriendForActiveSubject" class="friend-status">
            <view class="status-icon">✓</view>
            <text class="status-text">{{ t('page.contact.already_friend') }}</text>
            <button class="action-btn chat-btn" @click="startChat">
              <text class="btn-text">{{ t('page.contact.chat_short') }}</text>
            </button>
          </view>

          <!-- 已发送申请 -->
          <view v-else-if="hasSentRequest" class="friend-status">
            <view class="status-icon">W</view>
            <text class="status-text">{{ t('page.contact.request_sent_waiting') }}</text>
          </view>

          <!-- 收到对方申请 -->
          <view v-else-if="hasReceivedRequest" class="friend-status received">
            <view class="status-icon">R</view>
            <text class="status-text">{{ t('page.contact.request_received_from') }}</text>
            <view class="request-actions">
              <button class="action-btn reject-btn" @click="handleReject">
                <text class="btn-text">{{ t('action.reject') }}</text>
              </button>
              <button class="action-btn accept-btn" @click="handleAccept">
                <text class="btn-text">{{ t('action.accept') }}</text>
              </button>
            </view>
          </view>

          <!-- 可以添加 -->
          <view v-else class="add-section">
            <button class="add-friend-btn" @click="showRequestModal = true">
              <text class="add-icon">+</text>
              <text class="add-text">{{ t('page.contact.add_to_contacts') }}</text>
            </button>
          </view>
        </view>

        <!-- 更多信息 -->
        <view v-if="capabilities.length > 0" class="info-card">
          <text class="card-title">{{ t('page.contact.capabilities_title') }}</text>
          <view class="cap-wrap">
            <text v-for="(c, i) in capabilities" :key="i" class="cap-pill">{{ c }}</text>
          </view>
        </view>

        <view class="bottom-spacer" />
      </view>
    </view>

    <!-- 发送申请弹窗 -->
    <view v-if="showRequestModal" class="modal-mask" @click="showRequestModal = false">
      <view class="modal-sheet" @click.stop>
        <view class="modal-header">
          <text class="modal-title">{{ t('page.contact.send_request_title') }}</text>
          <text class="modal-close" @click="showRequestModal = false">x</text>
        </view>
        <view class="modal-body">
          <view class="target-user">
            <view class="target-avatar-wrap">
              <image
                v-if="userInfo?.avatarUrl"
                class="target-avatar"
                :src="userInfo.avatarUrl"
                mode="aspectFill"
              />
              <view v-else class="target-avatar" :style="avatarStyle">
                <text class="target-avatar-text">{{ avatarLetter }}</text>
              </view>
            </view>
            <text class="target-name">{{ userInfo?.nickname }}</text>
          </view>
          <view class="form-group">
            <text class="form-label">{{ t('page.contact.add_modal_label_message') }}</text>
            <view class="textarea-wrap">
              <textarea
                v-model="requestMessage"
                class="textarea"
                :placeholder="t('placeholder.friend_request_message')"
                maxlength="200"
                :show-confirm-bar="false"
              />
              <text class="char-count">{{ requestMessage.length }}/200</text>
            </view>
          </view>
          <view v-if="requestError" class="form-error">{{ requestError }}</view>
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @click="showRequestModal = false">
            <text class="btn-text">{{ t('toast.cancel') }}</text>
          </button>
          <button class="modal-btn confirm" :disabled="sending" @click="sendRequest">
            <text class="btn-text">{{ sending ? t('common.sending') : t('action.send_request') }}</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { subjectApi, type SubjectPublicProfile } from '@/api/modules/subject'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import { contactApi, type ContactItem, type ContactSubjectType } from '@/api/modules/contact'
import { conversationApi } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'

const userInfo = ref<SubjectPublicProfile | null>(null)
const loading = ref(false)
const targetSubjectId = ref(0)
const targetSubjectType = ref<ContactSubjectType>('HUMAN')
const userStore = useUserStore()
const activeSubjectStore = useActiveSubjectStore()
const { t, tf } = useI18nWithFormat()

// 好友申请相关
const showRequestModal = ref(false)
const requestMessage = ref('')
const requestError = ref('')
const sending = ref(false)
const receivedRequests = ref<FriendRequestItem[]>([])
const sentRequests = ref<FriendRequestItem[]>([])
const contacts = ref<ContactItem[]>([])

// 是否在线
const isOnline = computed(() => (userInfo.value?.status || '').toUpperCase() === 'ACTIVE')

// 是否是智能体
const isAgent = computed(() => targetSubjectType.value === 'AGENT')

const isFriendForActiveSubject = computed(() =>
  contacts.value.some(
    (item) =>
      item.targetSubjectId === targetSubjectId.value &&
      item.targetSubjectType === targetSubjectType.value,
  ),
)

// 能力标签
const capabilities = computed(() => (userInfo.value as any)?.capabilities ?? [])

// 头像字母
const avatarLetter = computed(() => userInfo.value?.nickname?.slice(0, 1) || '?')

// 头像样式
const avatarStyle = computed(() => {
  const hues = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  const s = userInfo.value?.nickname || String(targetSubjectId.value || 0)
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  const c = hues[Math.abs(h) % hues.length]!
  return { background: `linear-gradient(135deg, ${c}f0, ${c}e0)` }
})

// 是否已发送申请
const hasSentRequest = computed(() => {
  return sentRequests.value.some(r =>
    r.recipientSubjectId === targetSubjectId.value
    && r.recipientSubjectType === targetSubjectType.value
    && r.status === 'PENDING'
  )
})

// 是否收到对方申请
const hasReceivedRequest = computed(() => {
  return receivedRequests.value.some(r =>
    r.requesterSubjectId === targetSubjectId.value
    && r.requesterSubjectType === targetSubjectType.value
    && r.status === 'PENDING'
  )
})

// 获取收到的申请ID
const receivedRequestId = computed(() => {
  const req = receivedRequests.value.find(r =>
    r.requesterSubjectId === targetSubjectId.value
    && r.requesterSubjectType === targetSubjectType.value
    && r.status === 'PENDING'
  )
  return req?.id
})

// 返回上一页
const goBack = () => {
  uni.navigateBack()
}

// 加载用户资料
const loadUserProfile = async () => {
  if (!targetSubjectId.value) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  
  loading.value = true
  try {
    await activeSubjectStore.ensureLoaded()
    const data = await subjectApi.getPublicProfile(
      targetSubjectType.value,
      targetSubjectId.value,
      activeSubjectStore.subjectViewerParams(),
    )
    userInfo.value = data
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 加载好友申请状态
const loadRequestStatus = async () => {
  try {
    await activeSubjectStore.ensureLoaded()
    receivedRequests.value = await friendRequestApi.listReceived(activeSubjectStore.friendRequestSubjectParams())
    sentRequests.value = await friendRequestApi.listSent(activeSubjectStore.friendRequestSubjectParams())
  } catch {
    receivedRequests.value = []
    sentRequests.value = []
  }
}

const loadContactStatus = async () => {
  try {
    await activeSubjectStore.ensureLoaded()
    contacts.value = await contactApi.listContacts(activeSubjectStore.contactOwnerParams())
  } catch {
    contacts.value = []
  }
}

// 发送好友申请
const sendRequest = async () => {
  if (!userInfo.value) return
  
  requestError.value = ''
  sending.value = true
  
  try {
    await activeSubjectStore.ensureLoaded()
    const actor = activeSubjectStore.currentSubject
    if (!actor?.subjectId) {
      throw new Error(t('toast.load_failed'))
    }
    await friendRequestApi.sendRequest({
      actorSubjectId: actor.subjectId,
      actorSubjectType: actor.subjectType,
      recipientSubjectId: targetSubjectId.value,
      recipientSubjectType: targetSubjectType.value,
      requestMessage: requestMessage.value?.trim() || undefined
    })
    
    showRequestModal.value = false
    requestMessage.value = ''
    uni.showToast({ title: t('toast.request_sent'), icon: 'success' })
    await Promise.all([loadRequestStatus(), loadContactStatus()])
  } catch (err: any) {
    requestError.value = getApiErrorMessage(err, t('toast.add_failed'))
  } finally {
    sending.value = false
  }
}

// 同意申请
const handleAccept = async () => {
  const id = receivedRequestId.value
  if (!id) return
  
  try {
    await friendRequestApi.accept(id)
    uni.showToast({ title: t('toast.request_accepted'), icon: 'success' })
    await Promise.all([loadUserProfile(), loadRequestStatus(), loadContactStatus()])
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

// 拒绝申请
const handleReject = async () => {
  const id = receivedRequestId.value
  if (!id) return
  
  try {
    await friendRequestApi.reject(id)
    uni.showToast({ title: t('toast.request_rejected'), icon: 'success' })
    await loadRequestStatus()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

// 发起聊天
const startChat = async () => {
  if (!isFriendForActiveSubject.value) {
    uni.showToast({ title: t('page.contact.need_friend_to_chat'), icon: 'none' })
    return
  }
  
  try {
    await activeSubjectStore.ensureLoaded()
    const data = await conversationApi.createConversation({
      targetSubjectId: targetSubjectId.value,
      targetSubjectType: targetSubjectType.value,
      ...activeSubjectStore.conversationCreatorParams(),
    })
    const title = encodeURIComponent(userInfo.value.nickname || data.title || t('common.conversation'))
    uni.redirectTo({
      url: `/pages/chat/chat-room?conversationId=${data.id}&title=${title}`
    })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.create_failed'), icon: 'none' })
  }
}

onLoad((query) => {
  targetSubjectId.value = Number((query as any)?.targetSubjectId ?? 0)
  targetSubjectType.value = normalizeSubjectType((query as any)?.targetSubjectType)
})

function normalizeSubjectType(value: unknown): ContactSubjectType {
  return String(value || 'HUMAN').toUpperCase() === 'AGENT' ? 'AGENT' : 'HUMAN'
}

onShow(async () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.user_profile') })
  await activeSubjectStore.ensureLoaded()
  loadUserProfile()
  loadRequestStatus()
  loadContactStatus()
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  box-sizing: border-box;
}

.page-inner {
  padding: 24rpx 24rpx 24rpx;
  box-sizing: border-box;
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 80rpx;
  font-size: 28rpx;
}

/* 资料卡片 */
.profile-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--c-shadow-soft);
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 28rpx;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--radius-xl);
  display: block;
  box-shadow: var(--c-shadow-soft);
}

.avatar {
  width: 160rpx;
  height: 160rpx;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--c-shadow-soft);
  overflow: hidden;
}

.avatar-letter {
  font-size: 56rpx;
  font-weight: 800;
  color: #fff;
}

.online-dot {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 28rpx;
  height: 28rpx;
  border-radius: 999rpx;
  background: #10b981;
  border: 4rpx solid var(--c-bg);
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
  margin-bottom: 12rpx;
}

.name {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.agent-badge {
  padding: 4rpx 12rpx;
  border-radius: var(--radius-md);
  font-size: 20rpx;
  color: #7c3aed;
  border: 1rpx solid rgba(124, 58, 237, 0.2);
  background: rgba(124, 58, 237, 0.08);
}

.bio {
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: var(--c-muted);
  display: block;
  line-height: 1.45;
}

.meta-line {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  display: block;
}

/* 操作区 */
.action-section {
  margin-bottom: 24rpx;
}

.friend-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding: 48rpx 32rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  box-shadow: var(--c-shadow-soft);
}

.friend-status.received {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.2);
}

.status-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.status-text {
  font-size: 28rpx;
  color: var(--c-muted);
}

.request-actions {
  display: flex;
  gap: 24rpx;
  width: 100%;
  margin-top: 16rpx;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  border-radius: var(--radius-lg);
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-btn {
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
}

.accept-btn {
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
}

.reject-btn {
  background: rgba(3, 2, 19, 0.06);
}

.reject-btn .btn-text {
  color: var(--c-ink);
}

.btn-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
}

.add-section {
  padding: 0;
}

.add-friend-btn {
  width: 100%;
  height: 96rpx;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 10rpx 30rpx rgba(3, 2, 19, 0.15);
}

.add-friend-btn:active {
  opacity: 0.92;
  transform: scale(0.98);
}

.add-icon {
  font-size: 32rpx;
  color: #fff;
  font-weight: 700;
}

.add-text {
  font-size: 30rpx;
  font-weight: 700;
  color: #fff;
}

/* 信息卡片 */
.info-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--c-shadow-soft);
}

.card-title {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  margin-bottom: 16rpx;
}

.cap-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.cap-pill {
  padding: 10rpx 20rpx;
  border-radius: var(--radius-md);
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-primary);
  background: rgba(91, 103, 241, 0.1);
  border: 1rpx solid rgba(91, 103, 241, 0.25);
}

.bottom-spacer {
  height: 24rpx;
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.modal-sheet {
  width: 100%;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid var(--c-border);
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.modal-close {
  font-size: 32rpx;
  color: var(--c-muted);
  padding: 8rpx;
}

.modal-body {
  padding: 32rpx 24rpx;
}

.target-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.target-avatar-wrap {
  width: 120rpx;
  height: 120rpx;
}

.target-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.target-avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.target-name {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.form-label {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.textarea-wrap {
  position: relative;
  background: #f8f8fb;
  border: 2rpx solid rgba(3, 2, 19, 0.12);
  border-radius: var(--radius-lg);
  padding: 24rpx;
  min-height: 180rpx;
}

.textarea {
  width: 100%;
  min-height: 140rpx;
  background: transparent;
  font-size: 28rpx;
  color: var(--c-ink);
}

.char-count {
  position: absolute;
  right: 24rpx;
  bottom: 16rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.form-error {
  padding: 16rpx;
  font-size: 26rpx;
  color: #e53e3e;
  background: rgba(229, 62, 62, 0.08);
  border-radius: var(--radius-md);
  margin-top: 16rpx;
}

.modal-footer {
  display: flex;
  gap: 24rpx;
  padding: 24rpx;
  border-top: 1rpx solid var(--c-border);
}

.modal-btn {
  flex: 1;
  height: 88rpx;
  border-radius: var(--radius-lg);
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn.cancel {
  background: rgba(3, 2, 19, 0.06);
}

.modal-btn.cancel .btn-text {
  color: var(--c-ink);
}

.modal-btn.confirm {
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
}

.modal-btn[disabled] {
  opacity: 0.45;
}
</style>
