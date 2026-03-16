<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="title">{{ t('page.contact.title') }}</text>
          <text class="subtitle">{{ t('page.contact.subtitle') }}</text>
        </view>
        <view class="chip">{{ contacts.length }}</view>
      </view>
      <view class="add-bar">
        <input v-model="newFriendId" class="input" type="number" :placeholder="t('placeholder.new_contact')" />
        <button class="btn" @click="addContact">{{ t('action.add') }}</button>
      </view>
    </view>

    <view class="list">
      <view v-if="loading" class="state">{{ t('common.loading') }}</view>
      <view v-else-if="contacts.length === 0" class="state">{{ t('common.empty_contact') }}</view>
      <view v-for="item in contacts" :key="item.id" class="item">
        <view class="avatar">
          <text>{{ item.nickname?.slice(0, 1) || '?' }}</text>
        </view>
        <view class="content">
          <text class="name">{{ item.nickname }}</text>
          <text class="meta">ID: {{ item.id }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { useUserStore } from '@/store/modules/user'

const contacts = ref<ContactItem[]>([])
const loading = ref(false)
const newFriendId = ref('')
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

const addContact = async () => {
  if (!newFriendId.value) {
    uni.showToast({ title: t('placeholder.new_contact'), icon: 'none' })
    return
  }
  try {
    await contactApi.addContact({ friendId: Number(newFriendId.value) })
    newFriendId.value = ''
    fetchContacts()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.add_failed'), icon: 'none' })
  }
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.title') })
  fetchContacts()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
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

.add-bar {
  display: flex;
  gap: 16rpx;
  background: rgba(255, 255, 255, 0.1);
  padding: 16rpx;
  border-radius: var(--radius-lg);
}

.input {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16rpx;
  padding: 18rpx 20rpx;
  font-size: 26rpx;
  color: var(--c-ink);
}

.btn {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: var(--radius-pill);
  background: linear-gradient(135deg, #8aa2ff 0%, #b0c0ff 100%);
  color: #1a1720;
  font-weight: 700;
  font-size: 24rpx;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.item {
  display: flex;
  align-items: center;
  background: var(--c-surface);
  padding: 22rpx;
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  border: 1rpx solid var(--c-border);
}

.avatar {
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

.content {
  flex: 1;
}

.name {
  font-size: 30rpx;
  color: var(--c-ink);
  font-weight: 700;
  display: block;
}

.meta {
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
</style>
