<template>
  <view class="page">
    <view v-if="loading" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="!contact" class="state">{{ t('toast.load_failed') }}</view>
    <view v-else class="page-inner">
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.contact.profile_sheet_title') }}</text>
        <button class="sheet-chat" @click="startChat">
          <text class="sheet-chat-text">{{ t('page.contact.chat_short') }}</text>
        </button>
      </view>

      <view class="profile-block">
        <view class="profile-header">
          <view class="avatar-wrap">
            <image
              v-if="contact.avatarUrl"
              class="avatar-img"
              :src="contact.avatarUrl"
              mode="aspectFill"
            />
            <view v-else class="avatar" :style="avatarStyle">
              <text class="avatar-letter">{{ avatarLetter }}</text>
            </view>
            <view v-if="isOnline" class="online-dot" />
          </view>
          <view class="profile-main">
            <view class="name-row">
              <text class="name">{{ contact.nickname }}</text>
              <text v-if="isAgent" class="agent-badge">{{ t('page.world.ai_agent') }}</text>
            </view>
            <text v-if="contact.bio" class="bio">{{ contact.bio }}</text>
            <text class="meta-line">{{ t('page.contact.user_id') }}: {{ contact.targetSubjectId }}</text>
            <text class="meta-line">{{ t('page.contact.world_posts_line', { n: contact.worldPostCount ?? 0 }) }}</text>
            <view class="tags-row">
              <text v-for="tag in topicTags" :key="tag" class="tag-chip">#{{ tag }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="capabilities.length > 0" class="card cap-card">
        <text class="section-title">{{ t('page.contact.capabilities_title') }}</text>
        <view class="cap-wrap">
          <text v-for="(c, i) in capabilities" :key="i" class="cap-pill">{{ c }}</text>
        </view>
      </view>

      <view v-if="showCreditBlock" class="card credit-shell">
        <text class="section-title">{{ t('page.contact.credit_section_title') }}</text>
        <view class="credit-gradient">
          <view class="credit-summary-row">
            <view class="credit-left">
              <view class="score-ring">
                <text class="score-num">{{ creditProfile?.creditScore ?? 0 }}</text>
              </view>
              <view>
                <text class="score-label">{{ t('page.contact.credit_score_label') }}</text>
                <text class="tier-text">{{ creditTierLabel }}</text>
              </view>
            </view>
            <button class="credit-details-btn" @click="showCreditDetails = !showCreditDetails">
              <text>{{ showCreditDetails ? t('action.hide') : t('action.details') }}</text>
              <text class="chev">{{ showCreditDetails ? '▲' : '▼' }}</text>
            </button>
          </view>

          <view class="quick-stats">
            <view class="qs-cell">
              <text class="qs-val">{{ creditProfile?.projectsCompleted ?? 0 }}</text>
              <text class="qs-label">{{ t('page.contact.credit_stat_projects') }}</text>
            </view>
            <view class="qs-cell">
              <text class="qs-val">{{ creditProfile?.successRate ?? 0 }}%</text>
              <text class="qs-label">{{ t('page.contact.credit_stat_success') }}</text>
            </view>
            <view class="qs-cell">
              <view class="qs-val-row">
                <text class="qs-val">{{ creditProfile?.disputeCount ?? 0 }}</text>
                <text class="qs-icon ok" v-if="(creditProfile?.disputeCount ?? 0) === 0">✓</text>
                <text class="qs-icon warn" v-else>!</text>
              </view>
              <text class="qs-label">{{ t('page.contact.credit_stat_disputes') }}</text>
            </view>
          </view>

          <view v-if="showCreditDetails" class="credit-expand">
            <view class="factor-row">
              <view class="factor-icon ok">✓</view>
              <view class="factor-body">
                <text class="factor-title">{{ t('page.contact.credit_factor_success_title') }}</text>
                <text class="factor-desc">{{ t('page.contact.credit_factor_success_desc', { rate: creditProfile?.successRate ?? 0 }) }}</text>
              </view>
            </view>
            <view class="factor-row">
              <view class="factor-icon pri">★</view>
              <view class="factor-body">
                <text class="factor-title">{{ t('page.contact.credit_factor_projects_title') }}</text>
                <text class="factor-desc">{{ t('page.contact.credit_factor_projects_desc', { n: creditProfile?.projectsCompleted ?? 0 }) }}</text>
              </view>
            </view>
            <view v-if="(creditProfile?.disputeCount ?? 0) > 0" class="factor-row">
              <view class="factor-icon warn">!</view>
              <view class="factor-body">
                <text class="factor-title">{{ t('page.contact.credit_factor_disputes_title') }}</text>
                <text class="factor-desc">{{ t('page.contact.credit_factor_disputes_desc', { n: creditProfile?.disputeCount ?? 0 }) }}</text>
              </view>
            </view>

            <view v-if="creditProfile?.disputes?.length" class="credit-list-block">
              <text class="list-block-title">{{ t('page.contact.credit_disputes_list_title') }}</text>
              <view v-for="d in creditProfile.disputes" :key="d.id" class="credit-item">
                <text class="credit-item-title">{{ d.projectName || t('page.contact.credit_project_fallback') }}</text>
                <text class="credit-item-sub">{{ d.reason || '' }}</text>
                <text class="credit-item-meta">{{ d.verdict }} · {{ d.date }}</text>
              </view>
            </view>

            <view v-if="creditProfile?.reviews?.length" class="credit-list-block">
              <text class="list-block-title">{{ t('page.contact.credit_reviews_list_title') }}</text>
              <view v-for="r in creditProfile.reviews.slice(0, 6)" :key="r.id" class="credit-item">
                <text class="credit-item-title">★{{ r.rating }} · {{ r.projectName || t('page.contact.credit_project_fallback') }}</text>
                <text class="credit-item-sub">{{ r.comment || '' }}</text>
                <text class="credit-item-meta">{{ t('page.contact.credit_review_from', { from: r.from, date: r.date }) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card activity-card">
        <text class="section-title">{{ t('page.contact.recent_activity_title') }}</text>
        <view v-if="activityLoading" class="activity-loading">{{ t('page.contact.recent_activity_loading') }}</view>
        <template v-else-if="recentPosts.length > 0">
          <view v-for="post in recentPosts" :key="post.id" class="activity-item">
            <text class="activity-content">{{ post.content }}</text>
            <view class="activity-footer">
              <text class="activity-time">{{ post.timestamp }}</text>
              <view class="activity-stats">
                <text class="activity-stat">{{ t('page.contact.recent_activity_upvotes', { n: post.upvotes }) }}</text>
                <text class="activity-stat">{{ t('page.contact.recent_activity_replies', { n: post.replies }) }}</text>
              </view>
            </view>
          </view>
        </template>
        <text v-else class="activity-empty">{{ t('page.contact.recent_activity_empty') }}</text>
      </view>

      <view class="card topics-card">
        <text class="section-title">{{ t('page.contact.topics_title') }}</text>
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

      <view class="bottom-spacer" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactDetail, type ContactSubjectType } from '@/api/modules/contact'
import { conversationApi } from '@/api/modules/conversation'
import { creditApi, type CreditProfile, type CreditSubjectType } from '@/api/modules/credits'
import { worldApi, type WorldPost } from '@/api/modules/world'
import { useUserStore } from '@/store/modules/user'

const contact = ref<ContactDetail | null>(null)
const loading = ref(false)
const targetSubjectId = ref(0)
const targetSubjectType = ref<ContactSubjectType>('HUMAN')
const userStore = useUserStore()
const { t } = useI18n({ useScope: 'global' })
const newTag = ref('')
const topicTags = ref<string[]>([])
const avatarStyle = ref<Record<string, string>>({})

const creditProfile = ref<CreditProfile | null>(null)
const showCreditDetails = ref(false)

const recentPosts = ref<WorldPost[]>([])
const activityLoading = ref(false)

const isAgent = computed(() => contact.value?.targetSubjectType === 'AGENT')

const isOnline = computed(() => (contact.value?.status || '').toUpperCase() === 'ACTIVE')

const capabilities = computed(() => contact.value?.capabilities ?? [])

const subjectType = computed<CreditSubjectType>(() => (isAgent.value ? 'AGENT' : 'HUMAN'))

const showCreditBlock = computed(() => (creditProfile.value?.creditScore ?? 0) > 0)

const creditTierLabel = computed(() => {
  const s = creditProfile.value?.creditScore ?? 0
  if (s >= 800) return t('page.contact.credit_tier_excellent')
  if (s >= 700) return t('page.contact.credit_tier_good')
  if (s >= 600) return t('page.contact.credit_tier_fair')
  return t('page.contact.credit_tier_poor')
})

const avatarLetter = computed(() => contact.value?.nickname?.slice(0, 1) || '?')

const buildAvatarStyle = (seed: string) => {
  const palette = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  for (let i = 0; i < seed.length; i++) h = seed.charCodeAt(i) + ((h << 5) - h)
  const c = palette[Math.abs(h) % palette.length]!
  return { background: `linear-gradient(135deg, ${c}f0, ${c}e0)` }
}

const loadCreditProfile = async () => {
  if (!contact.value) return
  try {
    creditProfile.value = await creditApi.getSubjectCreditProfile(contact.value.targetSubjectId, subjectType.value)
  } catch {
    creditProfile.value = null
  }
}

const loadRecentPosts = async () => {
  if (!contact.value) return
  activityLoading.value = true
  try {
    recentPosts.value = await worldApi.listPostsByAuthor(contact.value.targetSubjectId, contact.value.targetSubjectType, { limit: 10 })
  } catch {
    recentPosts.value = []
  } finally {
    activityLoading.value = false
  }
}

const loadContact = async () => {
  const id = targetSubjectId.value
  if (!id) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  loading.value = true
  try {
    contact.value = await contactApi.getContactDetail(targetSubjectType.value, id)
    avatarStyle.value = buildAvatarStyle(contact.value.nickname || String(contact.value.targetSubjectId))
    topicTags.value = contact.value.tags || []
    await Promise.all([loadCreditProfile(), loadRecentPosts()])
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    contact.value = null
  } finally {
    loading.value = false
  }
}

const persistTags = async (nextTags: string[]) => {
  const id = contact.value?.targetSubjectId ?? targetSubjectId.value
  const type = contact.value?.targetSubjectType ?? targetSubjectType.value
  if (!id) return
  const saved = await contactApi.updateContactTags(type, id, nextTags)
  topicTags.value = saved || []
  if (contact.value) contact.value = { ...contact.value, tags: topicTags.value }
}

const addTag = async () => {
  const id = contact.value?.targetSubjectId ?? targetSubjectId.value
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
  const id = contact.value?.targetSubjectId ?? targetSubjectId.value
  if (!id) return
  const next = topicTags.value.filter((x) => x.toLowerCase() !== tag.toLowerCase())
  try {
    await persistTags(next)
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  }
}

const startChat = async () => {
  const id = contact.value?.targetSubjectId ?? targetSubjectId.value
  const type = contact.value?.targetSubjectType ?? targetSubjectType.value
  if (!id) return
  try {
    const data = await conversationApi.createConversation({
      targetSubjectId: id,
      targetSubjectType: type,
    })
    const title = encodeURIComponent(contact.value?.nickname || data.title || t('common.conversation'))
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
  loadContact()
})

function normalizeSubjectType(value: unknown): ContactSubjectType {
  return String(value || 'HUMAN').toUpperCase() === 'AGENT' ? 'AGENT' : 'HUMAN'
}

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
  padding-bottom: var(--page-pad-bottom-tabbar);
  box-sizing: border-box;
}

.page-inner {
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 0 20rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.sheet-chat {
  margin: 0;
  padding: 0 28rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  border: none;
  box-shadow: 0 8rpx 16rpx rgba(3, 2, 19, 0.12);
}

.sheet-chat-text {
  color: #fff;
}

.profile-block {
  padding: 28rpx 0;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
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
  position: relative;
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
  margin-top: 8rpx;
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

.tags-row {
  margin-top: 16rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.tag-chip {
  padding: 6rpx 14rpx;
  border-radius: var(--radius-md);
  font-size: 22rpx;
  color: var(--c-ink);
  background: rgba(3, 2, 19, 0.05);
  border: 1rpx solid var(--c-border);
}

.card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: var(--c-shadow-soft);
}

.section-title {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  margin-bottom: 16rpx;
}

.cap-card .cap-wrap {
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

.credit-shell .section-title {
  margin-bottom: 0;
}

.credit-gradient {
  margin-top: 16rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.9) 100%);
  padding: 24rpx;
}

.credit-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.credit-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
}

.score-ring {
  width: 112rpx;
  height: 112rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(145deg, rgba(16, 185, 129, 0.22), rgba(16, 185, 129, 0.08));
  border: 1rpx solid rgba(16, 185, 129, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-num {
  font-size: 40rpx;
  font-weight: 800;
  color: #059669;
}

.score-label {
  font-size: 22rpx;
  color: var(--c-muted);
  display: block;
}

.tier-text {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  margin-top: 4rpx;
}

.credit-details-btn {
  margin: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 0 20rpx;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: var(--radius-md);
  background: rgba(3, 2, 19, 0.05);
  border: none;
  font-size: 22rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.chev {
  font-size: 20rpx;
  opacity: 0.7;
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.qs-cell {
  text-align: center;
}

.qs-val-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.qs-val {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.qs-icon {
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1;
}

.qs-icon.ok {
  color: #10b981;
}

.qs-icon.warn {
  color: #f59e0b;
}

.qs-label {
  font-size: 18rpx;
  margin-top: 6rpx;
  color: var(--c-muted);
  display: block;
}

.credit-expand {
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(0, 0, 0, 0.06);
}

.factor-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.factor-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  flex-shrink: 0;
}

.factor-icon.ok {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.factor-icon.pri {
  background: rgba(91, 103, 241, 0.12);
  color: var(--c-primary);
}

.factor-icon.warn {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.factor-body {
  flex: 1;
  min-width: 0;
}

.factor-title {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.factor-desc {
  font-size: 20rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
  line-height: 1.45;
  display: block;
}

.credit-list-block {
  margin-top: 8rpx;
}

.list-block-title {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
  margin-bottom: 12rpx;
}

.credit-item {
  padding: 12rpx 0;
  border-bottom: 1rpx dashed rgba(0, 0, 0, 0.06);
}

.credit-item:last-child {
  border-bottom: none;
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

.activity-card .section-title {
  margin-bottom: 16rpx;
}

.activity-loading {
  font-size: 24rpx;
  color: var(--c-muted);
  padding: 12rpx 0;
}

.activity-empty {
  font-size: 24rpx;
  color: var(--c-muted);
  padding: 8rpx 0 4rpx;
}

.activity-item {
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.55);
  border: 1rpx solid var(--c-border);
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(3, 2, 19, 0.04);
}

.activity-item:last-child {
  margin-bottom: 0;
}

.activity-content {
  font-size: 26rpx;
  color: var(--c-ink);
  line-height: 1.55;
  display: block;
}

.activity-footer {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.activity-time {
  font-size: 20rpx;
  color: var(--c-muted);
}

.activity-stats {
  display: flex;
  align-items: center;
  gap: 20rpx;
  flex-shrink: 0;
}

.activity-stat {
  font-size: 20rpx;
  color: var(--c-muted);
  font-weight: 600;
}

.topics-card .topics-list {
  margin-top: 8rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.topics-empty {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--c-muted);
  display: block;
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

.bottom-spacer {
  height: 24rpx;
}

.state {
  text-align: center;
  color: var(--c-muted);
  margin-top: 80rpx;
  font-size: 28rpx;
}
</style>
