<template>
  <view class="page">
    <view v-if="loading" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="!contact" class="state">{{ t('toast.load_failed') }}</view>
    <template v-else>
      <view class="profile">
        <view class="avatar">{{ contact.nickname?.slice(0, 1) || '?' }}</view>
        <text class="name">{{ contact.nickname }}</text>
        <text class="meta">{{ t('page.contact.user_id') }}: {{ contact.id }}</text>
      </view>
      <view class="actions">
        <button class="btn-chat" @click="startChat">{{ t('action.start_chat') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { conversationApi } from '@/api/modules/conversation'
import { useUserStore } from '@/store/modules/user'

const contact = ref<ContactItem | null>(null)
const loading = ref(false)
const friendId = ref(0)
const userStore = useUserStore()
const { t } = useI18n()

const loadContact = async () => {
  const id = friendId.value
  if (!id) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  loading.value = true
  try {
    const list = await contactApi.listContacts()
    contact.value = list.find((c) => c.id === id) || null
    if (!contact.value) {
      contact.value = { id, nickname: `用户${id}`, avatarUrl: undefined, status: undefined }
    }
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const startChat = async () => {
  const id = contact.value?.id ?? friendId.value
  if (!id) return
  try {
    const data = await conversationApi.createConversation({ targetUserId: id })
    const title = encodeURIComponent(contact.value?.nickname || data.title || '会话')
    uni.redirectTo({
      url: `/pages/chat/chat-room?conversationId=${data.id}&title=${title}`
    })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.create_failed'), icon: 'none' })
  }
}

onLoad((query) => {
  friendId.value = Number((query as any)?.id ?? (query as any)?.friendId ?? 0)
  loadContact()
})

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.detail') })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 48rpx 24rpx;
  background: linear-gradient(180deg, #f6f2ee 0%, #f0f7ff 100%);
}

.profile {
  background: var(--c-surface);
  border-radius: var(--radius-xl);
  padding: 48rpx 32rpx;
  text-align: center;
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
  margin-bottom: 32rpx;
}

.avatar {
  width: 160rpx;
  height: 160rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  font-weight: 700;
  color: #065f46;
}

.name {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.meta {
  font-size: 26rpx;
  color: var(--c-muted);
  margin-top: 12rpx;
  display: block;
}

.actions {
  padding: 0 8rpx;
}

.btn-chat {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #ff7a59 0%, #ff9f6b 100%);
  color: #1a1720;
  font-size: 32rpx;
  font-weight: 700;
  border: none;
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 80rpx;
  font-size: 28rpx;
}
</style>
