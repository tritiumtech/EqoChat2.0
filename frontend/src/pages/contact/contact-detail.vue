<template>
  <view class="page">
    <view v-if="loading" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="!contact" class="state">{{ t('toast.load_failed') }}</view>
    <template v-else>
      <view class="profile-card">
        <view class="profile-top">
          <view class="avatar" :style="avatarStyle">
            <text class="avatar-letter">{{ contact.nickname?.slice(0, 1) || '?' }}</text>
          </view>
          <view class="profile-main">
            <view class="name-row">
              <text class="name">{{ contact.nickname }}</text>
              <text v-if="isAgentLike" class="agent-badge">{{ t('page.world.ai_agent') }}</text>
            </view>
            <text class="meta">{{ t('page.contact.user_id') }}: {{ contact.id }}</text>
            <text class="sub">{{ profileHint }}</text>
          </view>
        </view>
        <view class="stats-grid">
          <view class="stat-card">
            <text class="stat-value">{{ topicTags.length }}</text>
            <text class="stat-label">{{ t('page.contact.topics_title') }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ contact.id % 5 + 1 }}</text>
            <text class="stat-label">{{ t('page.world.posts') }}</text>
          </view>
          <view class="stat-card">
            <text class="stat-value">{{ creditProfile?.creditScore ?? 0 }}</text>
            <text class="stat-label">{{ t('common.credit') }}</text>
          </view>
        </view>

        <view class="credit-details-card">
          <view class="credit-details-head">
            <text class="credit-details-title">Credit Profile Details</text>
            <button class="credit-details-toggle" @click="showCreditDetails = !showCreditDetails">
              {{ showCreditDetails ? '收起' : '展开' }}
            </button>
          </view>

          <view v-if="showCreditDetails" class="credit-details-body">
            <view class="credit-row">
              <text class="credit-key">Projects</text>
              <text class="credit-val">{{ creditProfile?.projectsCompleted ?? 0 }}</text>
            </view>
            <view class="credit-row">
              <text class="credit-key">Success Rate</text>
              <text class="credit-val">{{ creditProfile?.successRate ?? 0 }}%</text>
            </view>
            <view class="credit-row">
              <text class="credit-key">Verified Disputes</text>
              <text class="credit-val">{{ creditProfile?.disputeCount ?? 0 }}</text>
            </view>

            <view v-if="creditProfile?.disputes?.length" class="credit-list">
              <view v-for="d in creditProfile.disputes" :key="d.id" class="credit-item">
                <text class="credit-item-title">{{ d.projectName || 'Project' }}</text>
                <text class="credit-item-sub">{{ d.reason || '' }}</text>
                <text class="credit-item-meta">{{ d.verdict }} · {{ d.date }}</text>
              </view>
            </view>

            <view v-if="creditProfile?.reviews?.length" class="credit-list">
              <view v-for="r in creditProfile.reviews.slice(0, 3)" :key="r.id" class="credit-item">
                <text class="credit-item-title">★{{ r.rating }} · {{ r.projectName || 'Project' }}</text>
                <text class="credit-item-sub">{{ r.comment || '' }}</text>
                <text class="credit-item-meta">from {{ r.from }} · {{ r.date }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="topics-card">
        <text class="topics-title">{{ t('page.contact.topics_title') }}</text>
        <view v-if="topicTags.length > 0" class="topics-list">
          <text v-for="tag in topicTags" :key="tag" class="topic-chip" @click="removeTag(tag)">#{{ tag }} ×</text>
        </view>
        <text v-else class="topics-empty">{{ t('page.contact.topics_empty') }}</text>
        <view class="topics-editor">
          <input
            v-model="newTag"
            class="topic-input"
            :placeholder="t('page.contact.topics_input_placeholder')"
            maxlength="24"
            @confirm="addTag"
          />
          <button class="topic-add-btn" @click="addTag">{{ t('action.add') }}</button>
        </view>
      </view>

      <view class="timeline-card">
        <text class="timeline-title">{{ t('page.profile.menu_notifications') }}</text>
        <view class="timeline-item">
          <view class="timeline-dot" />
          <view class="timeline-main">
            <text class="timeline-text">{{ t('page.contact.detail_recent_topic') }}</text>
            <text class="timeline-time">{{ t('page.contact.timeline_recent_topic_time') }}</text>
          </view>
        </view>
        <view class="timeline-item">
          <view class="timeline-dot soft" />
          <view class="timeline-main">
            <text class="timeline-text">{{ t('page.contact.detail_recent_message') }}</text>
            <text class="timeline-time">{{ t('page.contact.timeline_recent_message_time') }}</text>
          </view>
        </view>
      </view>

      <view class="actions">
        <button class="btn-chat" @click="startChat">{{ t('action.start_chat') }}</button>
      </view>
    </template>
    <BottomNav />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { conversationApi } from '@/api/modules/conversation'
import { creditApi, type CreditProfile, type CreditSubjectType } from '@/api/modules/credits'
import { useUserStore } from '@/store/modules/user'
import BottomNav from '@/components/BottomNav.vue'

const contact = ref<ContactItem | null>(null)
const loading = ref(false)
const friendId = ref(0)
const userStore = useUserStore()
const { t } = useI18n({ useScope: 'global' })
const newTag = ref('')
const topicTags = ref<string[]>([])
const avatarStyle = ref<Record<string, string>>({})

const creditProfile = ref<CreditProfile | null>(null)
const showCreditDetails = ref(false)

const isAgentLike = computed(() => {
  const n = (contact.value?.nickname || '').toLowerCase()
  return n.includes('agent') || n.includes('ai')
})

const subjectType = computed<CreditSubjectType>(() => (isAgentLike.value ? 'AGENT' : 'USER'))

const profileHint = computed(() => {
  const count = topicTags.value.length
  return count > 0
    ? t('page.contact.detail_topic_count', { n: count })
    : t('page.contact.detail_no_topic')
})

const buildAvatarStyle = (seed: string) => {
  const palette = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  for (let i = 0; i < seed.length; i++) h = seed.charCodeAt(i) + ((h << 5) - h)
  const c = palette[Math.abs(h) % palette.length]!
  return { background: `linear-gradient(135deg, ${c}f0, ${c}c8)` }
}

const loadCreditProfile = async () => {
  if (!contact.value) return
  try {
    creditProfile.value = await creditApi.getSubjectCreditProfile(contact.value.id, subjectType.value)
  } catch {
    creditProfile.value = null
  }
}

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
      contact.value = { id, nickname: t('page.contact.default_user_name', { id }), avatarUrl: undefined, status: undefined }
    }
    avatarStyle.value = buildAvatarStyle(contact.value.nickname || String(contact.value.id))
    topicTags.value = contact.value.tags || []
    await loadCreditProfile()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const persistTags = async (nextTags: string[]) => {
  const id = contact.value?.id ?? friendId.value
  if (!id) return
  const saved = await contactApi.updateContactTags(id, nextTags)
  topicTags.value = saved || []
}

const addTag = async () => {
  const id = contact.value?.id ?? friendId.value
  const tag = (newTag.value || '').trim().replace(/\s+/g, ' ')
  if (!id || !tag) return
  const exists = topicTags.value.some((x) => x.toLowerCase() === tag.toLowerCase())
  if (!exists) {
    try {
      await persistTags([...topicTags.value, tag])
    } catch (err: any) {
      uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
      return
    }
  }
  newTag.value = ''
}

const removeTag = async (tag: string) => {
  const id = contact.value?.id ?? friendId.value
  if (!id) return
  const next = topicTags.value.filter((x) => x.toLowerCase() !== tag.toLowerCase())
  try {
    await persistTags(next)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

const startChat = async () => {
  const id = contact.value?.id ?? friendId.value
  if (!id) return
  try {
    const data = await conversationApi.createConversation({ targetUserId: id })
    const title = encodeURIComponent(contact.value?.nickname || data.title || t('common.conversation'))
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
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  padding: 24rpx;
  padding-bottom: var(--page-pad-bottom-tabbar);
  box-sizing: border-box;
}

.profile-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  padding: 24rpx;
  box-shadow: var(--c-shadow-soft);
  margin-bottom: 20rpx;
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-letter {
  font-size: 44rpx;
  font-weight: 800;
  color: #fff;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.name {
  font-size: 34rpx;
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

.meta {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
}

.sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
}

.stats-grid {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.stat-card {
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.7);
  padding: 16rpx 10rpx;
  text-align: center;
}

.stat-value {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.stat-label {
  font-size: 20rpx;
  margin-top: 6rpx;
  color: var(--c-muted);
  display: block;
}

.credit-details-card {
  margin-top: 14rpx;
  background: rgba(255, 255, 255, 0.82);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.credit-details-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.credit-details-title {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.credit-details-toggle {
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 18rpx;
  border-radius: var(--radius-md);
  border: 1rpx solid rgba(91, 103, 241, 0.25);
  background: rgba(91, 103, 241, 0.08);
  color: var(--c-primary);
  font-size: 22rpx;
  font-weight: 700;
}

.credit-details-body {
  margin-top: 14rpx;
}

.credit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0;
  border-bottom: 1rpx dashed rgba(0, 0, 0, 0.06);
}

.credit-row:last-child {
  border-bottom: none;
}

.credit-key {
  font-size: 22rpx;
  color: var(--c-muted);
}

.credit-val {
  font-size: 24rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.credit-list {
  margin-top: 14rpx;
}

.credit-item {
  padding: 12rpx 0;
}

.credit-item-title {
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.credit-item-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: var(--c-muted);
  line-height: 1.45;
}

.credit-item-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 18rpx;
  color: rgba(26, 23, 32, 0.55);
}

.topics-card {
  background: rgba(255, 255, 255, 0.82);
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.topics-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.topics-empty {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
}

.topics-list {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.topic-chip {
  padding: 8rpx 14rpx;
  border-radius: var(--radius-pill);
  background: rgba(3, 2, 19, 0.06);
  color: var(--c-ink);
  font-size: 22rpx;
}

.topics-editor {
  margin-top: 18rpx;
  display: flex;
  gap: 12rpx;
}

.topic-input {
  flex: 1;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-md);
  border: 1rpx solid var(--c-border);
  padding: 0 20rpx;
  font-size: 26rpx;
  color: var(--c-ink);
}

.topic-add-btn {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 24rpx;
  border-radius: var(--radius-md);
  border: 1rpx solid rgba(3, 2, 19, 0.2);
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  margin: 0;
  box-shadow: 0 8rpx 16rpx rgba(3, 2, 19, 0.16);
}

.timeline-card {
  background: rgba(255, 255, 255, 0.82);
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.timeline-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  margin-bottom: 14rpx;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 12rpx 0;
}

.timeline-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: var(--c-primary);
  margin-top: 8rpx;
}

.timeline-dot.soft {
  opacity: 0.45;
}

.timeline-main {
  flex: 1;
  min-width: 0;
}

.timeline-text {
  font-size: 24rpx;
  color: var(--c-ink);
  display: block;
}

.timeline-time {
  font-size: 20rpx;
  margin-top: 6rpx;
  color: var(--c-muted);
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
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  border: 1rpx solid rgba(3, 2, 19, 0.22);
  box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.18);
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 80rpx;
  font-size: 28rpx;
}
</style>
