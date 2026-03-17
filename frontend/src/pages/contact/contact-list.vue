<template>
  <view class="page">
    <scroll-view class="scroll-body" scroll-y>
      <view class="hero">
        <view class="hero-top">
          <view>
            <text class="title">{{ t('page.contact.title') }}</text>
            <text class="subtitle">{{ t('page.contact.subtitle') }}</text>
          </view>
          <view class="chip">{{ contacts.length }}</view>
        </view>
        <button class="btn-add" @click="showAddModal = true; addFormError = ''">{{ t('action.add_friend') }}</button>
      </view>

      <view v-if="receivedRequests.length > 0" class="section">
        <text class="section-title">{{ t('page.contact.friend_requests') }}</text>
        <view v-for="req in receivedRequests" :key="req.id" class="request-item">
          <view class="avatar">
            <text>{{ req.requesterNickname?.slice(0, 1) || '?' }}</text>
          </view>
          <view class="content">
            <text class="name">{{ req.requesterNickname || '用户' + req.requesterId }}</text>
            <text class="meta" v-if="req.requestMessage">{{ req.requestMessage }}</text>
            <text class="meta" v-else>ID: {{ req.requesterId }}</text>
          </view>
          <view class="actions">
            <button class="btn-sm accept" @click="handleAccept(req.id)">{{ t('action.accept') }}</button>
            <button class="btn-sm reject" @click="handleReject(req.id)">{{ t('action.reject') }}</button>
          </view>
        </view>
      </view>

      <view class="list">
        <text v-if="contacts.length > 0" class="section-title">{{ t('page.contact.friends') }} ({{ contacts.length }})</text>
        <view v-if="loading" class="state">{{ t('common.loading') }}</view>
        <view v-else-if="contacts.length === 0 && receivedRequests.length === 0" class="state">{{ t('common.empty_contact') }}</view>
        <navigator
          v-else
          v-for="item in contacts"
          :key="item.id"
          class="contact-item"
          :url="'/pages/contact/contact-detail?id=' + item.id"
          open-type="navigate"
          hover-class="contact-item-hover"
        >
          <view class="item-inner">
            <view class="avatar contact-avatar">
              <text>{{ (item.nickname || '?').slice(0, 1) }}</text>
            </view>
            <view class="content">
              <text class="name">{{ item.nickname }}</text>
              <text class="meta">ID: {{ item.id }}</text>
            </view>
          </view>
        </navigator>
      </view>
    </scroll-view>

    <ModalSheet :visible="showAddModal" @close="showAddModal = false; addFormError = ''">
      <template #header>
        <view class="modal-header">
          <view class="modal-icon-wrap">
            <text class="modal-icon">👋</text>
          </view>
          <text class="modal-title">{{ t('action.add_friend') }}</text>
          <text class="modal-subtitle">{{ t('page.contact.add_modal_subtitle') }}</text>
        </view>
      </template>
      <view class="modal-form">
        <view class="form-group">
          <text class="form-label">{{ t('page.contact.add_modal_label_id') }}</text>
          <view class="input-wrap">
            <text class="input-prefix">#</text>
            <input
              v-model="addForm.friendId"
              class="input"
              type="number"
              :placeholder="t('placeholder.new_contact')"
            />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">{{ t('page.contact.add_modal_label_message') }} <text class="optional">{{ t('page.contact.add_modal_optional') }}</text></text>
          <view class="textarea-wrap">
            <textarea
              v-model="addForm.requestMessage"
              class="textarea"
              :placeholder="t('placeholder.friend_request_message')"
              maxlength="200"
              :show-confirm-bar="false"
            />
            <text class="char-count">{{ addForm.requestMessage.length }}/200</text>
          </view>
        </view>
        <view v-if="addFormError" class="form-error">{{ addFormError }}</view>
      </view>
      <template #footer>
        <view class="modal-footer">
          <button class="btn-cancel" @click="showAddModal = false">{{ t('toast.cancel') }}</button>
          <button class="btn-send" @click="sendFriendRequest" :disabled="!addForm.friendId">
            <text class="btn-send-text">{{ t('action.send_request') }}</text>
          </button>
        </view>
      </template>
    </ModalSheet>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem as Contact } from '@/api/modules/contact'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import ModalSheet from '@/components/ModalSheet.vue'

const contacts = ref<Contact[]>([])
const receivedRequests = ref<FriendRequestItem[]>([])
const loading = ref(false)
const showAddModal = ref(false)
const addFormError = ref('')
const addForm = reactive({ friendId: '', requestMessage: '' })
const userStore = useUserStore()
const { t } = useI18n()

const fetchContacts = async () => {
  loading.value = true
  try {
    contacts.value = await contactApi.listContacts()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const fetchReceivedRequests = async () => {
  try {
    receivedRequests.value = await friendRequestApi.listReceived()
  } catch {
    receivedRequests.value = []
  }
}

const sendFriendRequest = async () => {
  addFormError.value = ''
  const friendId = Number(addForm.friendId)
  if (!friendId) {
    addFormError.value = t('placeholder.new_contact')
    return
  }
  try {
    await friendRequestApi.sendRequest({
      friendId,
      requestMessage: addForm.requestMessage?.trim() || undefined
    })
    showAddModal.value = false
    addForm.friendId = ''
    addForm.requestMessage = ''
    addFormError.value = ''
    uni.showToast({ title: t('toast.request_sent') })
    fetchReceivedRequests()
  } catch (err: any) {
    addFormError.value = getApiErrorMessage(err, t('toast.add_failed'))
  }
}

const handleAccept = async (id: number) => {
  try {
    await friendRequestApi.accept(id)
    fetchContacts()
    fetchReceivedRequests()
    uni.showToast({ title: t('toast.request_accepted') })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

const handleReject = async (id: number) => {
  try {
    await friendRequestApi.reject(id)
    fetchReceivedRequests()
    uni.showToast({ title: t('toast.request_rejected') })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.title') })
  fetchContacts()
  fetchReceivedRequests()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f2ee 0%, #f0f7ff 100%);
}

.hero {
  background: #15131c;
  color: #fff;
  border-radius: var(--radius-xl);
  padding: 28rpx 24rpx;
  box-shadow: var(--c-shadow);
  margin-bottom: 24rpx;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 700;
  display: block;
}

.subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8rpx;
  display: block;
}

.chip {
  min-width: 64rpx;
  height: 64rpx;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.btn-add {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #8aa2ff 0%, #b0c0ff 100%);
  color: #1a1720;
  font-weight: 700;
  font-size: 28rpx;
  border: none;
}

.section {
  margin-bottom: 24rpx;
}

.section-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-muted);
  margin-bottom: 16rpx;
}

.request-item {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 20rpx;
  border-radius: var(--radius-lg);
  margin-bottom: 12rpx;
  border: 1rpx solid var(--c-border);
}

.request-item .avatar {
  background: linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%);
  color: #5c4a00;
}

.actions {
  display: flex;
  gap: 12rpx;
}

.btn-sm {
  padding: 12rpx 24rpx;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: var(--radius-pill);
  border: none;
}

.btn-sm.accept {
  background: linear-gradient(135deg, #55efc4 0%, #00b894 100%);
  color: #004d40;
}

.btn-sm.reject {
  background: rgba(26, 23, 32, 0.08);
  color: var(--c-muted);
}

.scroll-body {
  height: 100vh;
  box-sizing: border-box;
  padding: 24rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.contact-item {
  display: block;
  text-decoration: none;
  color: inherit;
}

.contact-item:active {
  opacity: 0.95;
}

.contact-item-hover {
  opacity: 0.9;
}

.item-inner {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 22rpx;
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.contact-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #065f46;
  font-size: 30rpx;
  font-weight: 700;
  margin-right: 18rpx;
}

.list .content {
  flex: 1;
}

.list .name {
  font-size: 30rpx;
  color: var(--c-ink);
  font-weight: 700;
  display: block;
}

.list .meta {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 8rpx;
  display: block;
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 40rpx;
  font-size: 24rpx;
}

.modal-header {
  text-align: center;
  padding: 12rpx 0 24rpx;
}

.modal-icon-wrap {
  width: 96rpx;
  height: 96rpx;
  margin: 0 auto 20rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #e8edff 0%, #f0f4ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(138, 162, 255, 0.2);
}

.modal-icon {
  font-size: 48rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.modal-subtitle {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 12rpx;
  display: block;
  line-height: 1.5;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  padding: 8rpx 0;
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

.form-label .optional {
  font-weight: 400;
  color: var(--c-muted);
  font-size: 24rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, #fafbfd 0%, #f4f6fb 100%);
  border: 2rpx solid rgba(138, 162, 255, 0.2);
  border-radius: var(--radius-lg);
  padding: 0 24rpx;
  transition: all 0.2s ease;
}

.input-wrap:focus-within {
  border-color: rgba(138, 162, 255, 0.6);
  box-shadow: 0 0 0 4rpx rgba(138, 162, 255, 0.12);
}

.input-prefix {
  font-size: 28rpx;
  font-weight: 600;
  color: #8aa2ff;
  margin-right: 8rpx;
}

.modal-form .input {
  flex: 1;
  height: 88rpx;
  background: transparent;
  font-size: 30rpx;
  color: var(--c-ink);
}

.textarea-wrap {
  position: relative;
  background: linear-gradient(180deg, #fafbfd 0%, #f4f6fb 100%);
  border: 2rpx solid rgba(138, 162, 255, 0.2);
  border-radius: var(--radius-lg);
  padding: 24rpx;
  min-height: 160rpx;
  transition: all 0.2s ease;
}

.textarea-wrap:focus-within {
  border-color: rgba(138, 162, 255, 0.6);
  box-shadow: 0 0 0 4rpx rgba(138, 162, 255, 0.12);
}

.form-error {
  margin-top: 8rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: #e53e3e;
  background: rgba(229, 62, 62, 0.08);
  border-radius: var(--radius-md);
  border: 1rpx solid rgba(229, 62, 62, 0.2);
}

.modal-form .textarea {
  width: 100%;
  min-height: 120rpx;
  background: transparent;
  font-size: 28rpx;
  color: var(--c-ink);
  line-height: 1.5;
}

.char-count {
  position: absolute;
  right: 24rpx;
  bottom: 16rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.modal-footer {
  display: flex;
  gap: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid var(--c-border);
}

.btn-cancel {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 28rpx;
  color: var(--c-muted);
  background: rgba(26, 23, 32, 0.04);
  border-radius: var(--radius-lg);
  border: none;
}

.btn-send {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #6b8cff 0%, #8aa2ff 100%);
  border-radius: var(--radius-lg);
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(107, 140, 255, 0.35);
}

.btn-send-text {
  color: #fff;
}

.btn-send[disabled] {
  opacity: 0.5;
  box-shadow: none;
}
</style>
