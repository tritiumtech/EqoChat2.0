<template>
  <view class="page">
    <PageHeader 
      :title="t('page.contact.title')"
      action-icon="＋"
      action-variant="primary"
      action-size="md"
      @action-click="goToSearch"
    >
      <template #search>
        <view class="search-shell">
          <SearchBar v-model="searchQuery" :placeholder="t('page.contact.search_placeholder')" />
        </view>
      </template>
    </PageHeader>

    <!-- 新的朋友入口 -->
    <view class="new-friends-entry" @click="goToFriendRequests">
      <view class="new-friends-icon">👋</view>
      <view class="new-friends-body">
        <text class="new-friends-title">{{ t('page.contact.new_friends') }}</text>
        <text v-if="pendingRequestCount > 0" class="new-friends-subtitle">
          {{ tf('page.contact.pending_requests_count', { n: pendingRequestCount }) }}
        </text>
      </view>
      <view class="new-friends-arrow">
        <text v-if="pendingRequestCount > 0" class="new-friends-badge">{{ pendingRequestCount > 99 ? '99+' : pendingRequestCount }}</text>
        <text class="chev">›</text>
      </view>
    </view>

    <view class="chips-scroll">
      <view class="chips-inner">
        <button
          class="chip"
          :class="{ active: tagFilter === 'all' }"
          @click="tagFilter = 'all'"
        >
          {{ t('page.contact.filter_all') }}
        </button>
        <button
          class="chip"
          v-for="topic in topicFilters"
          :key="topic"
          :class="{ active: tagFilter === topic }"
          @click="tagFilter = topic"
        >
          #{{ topic }}
        </button>
      </view>
    </view>

    <view class="list-zone">
      <view class="main-scroll">
        <view v-if="loading" class="state state-loading">
          <u-loading-icon mode="circle" size="28" color="#030213" />
          <text class="state-text">{{ t('common.loading') }}</text>
        </view>
        <view v-else-if="letters.length === 0" class="empty">
          <view class="empty-icon-wrap">
            <u-icon name="search" :size="36" color="#717182" />
          </view>
          <text class="empty-title">{{ t('page.contact.empty_filter') }}</text>
          <text class="empty-sub">{{ t('page.contact.empty_filter_hint') }}</text>
        </view>
        <block v-else>
          <view v-for="letter in letters" :key="letter" :id="letterId(letter)" class="section">
            <view class="letter-head">
              <text class="letter-text">{{ letter }}</text>
            </view>
            <view
              v-for="item in grouped[letter]"
              :key="item.id"
              class="contact-row"
              @click="goDetail(item.id)"
            >
              <view class="avatar-wrap">
                <view
                  class="avatar"
                  :class="{ 'avatar--photo': !!item.avatarUrl }"
                  :style="item.avatarUrl ? undefined : avatarStyle(item)"
                >
                  <image
                    v-if="item.avatarUrl"
                    class="avatar-img"
                    :src="item.avatarUrl"
                    mode="aspectFill"
                  />
                  <template v-else>
                    <view class="avatar-shine" />
                    <text class="avatar-letter">{{ (item.nickname || '?').slice(0, 1) }}</text>
                  </template>
                </view>
              </view>
              <view class="info">
                <view class="name-row">
                  <text class="name">{{ item.nickname }}</text>
                </view>
                <text class="role">
                  {{ item.tags?.length ? `#${item.tags[0]}` : `${t('page.contact.user_id')}: ${item.id}` }}
                </text>
              </view>
            </view>
          </view>
        </block>
        <view class="scroll-pad" />
      </view>

      <view v-if="letters.length > 0" class="index-bar">
        <button
          v-for="letter in letters"
          :key="letter"
          class="index-letter"
          @click="scrollToLetter(letter)"
        >
          {{ letter }}
        </button>
      </view>
    </view>

    <FgTabbar />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onPageScroll, onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { contactApi, type ContactItem as Contact } from '@/api/modules/contact'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import SearchBar from '@/components/SearchBar.vue'
import PageHeader from '@/components/PageHeader.vue'
import FgTabbar from '@/tabbar/index.vue'

const contacts = ref<Contact[]>([])
const receivedRequests = ref<FriendRequestItem[]>([])
const loading = ref(false)
const userStore = useUserStore()
const { t, tf } = useI18nWithFormat()

const searchQuery = ref('')
const tagFilter = ref('all')
const pageScrollTop = ref(0)
let queryTimer: number | null = null

// 待处理的好友申请数量（仅统计 PENDING）
const pendingRequestCount = computed(() =>
  receivedRequests.value.filter((item) => String(item.status || '').toUpperCase() === 'PENDING').length
)

const avatarHue = (s: string) => {
  const hues = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return hues[Math.abs(h) % hues.length]!
}

const avatarStyle = (item: Contact) => {
  const c = avatarHue(item.nickname || String(item.id))
  return { background: `linear-gradient(135deg, ${c}f0, ${c}c0)` }
}

const topicFilters = computed(() => {
  const topics = new Set<string>()
  for (const c of contacts.value) {
    for (const tag of c.tags || []) {
      if (tag?.trim()) topics.add(tag.trim())
    }
  }
  return Array.from(topics).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const filteredContacts = computed(() => {
  if (tagFilter.value === 'all') return contacts.value
  return contacts.value.filter((x) => (x.tags || []).includes(tagFilter.value))
})

const grouped = computed(() => {
  const map: Record<string, Contact[]> = {}
  for (const c of filteredContacts.value) {
    const name = c.nickname?.trim() || '?'
    const letter = name.charAt(0).toUpperCase()
    const key = /[A-Z0-9]/.test(letter) ? letter : '#'
    if (!map[key]) map[key] = []
    map[key]!.push(c)
  }
  for (const k of Object.keys(map)) {
    map[k]!.sort((a, b) => (a.nickname || '').localeCompare(b.nickname || '', 'zh-CN'))
  }
  return map
})

const letters = computed(() => Object.keys(grouped.value).sort((a, b) => a.localeCompare(b, 'en')))

const letterAnchors: Record<string, string> = {
  '#': 'sym',
}

const letterId = (letter: string) => 'letter-' + (letterAnchors[letter] ?? letter)

const scrollToLetter = (letter: string) => {
  const id = letterId(letter)
  nextTick(() => {
    uni.createSelectorQuery()
      .select(`#${id}`)
      .boundingClientRect((rect: any) => {
        if (!rect) return
        const target = Math.max(0, Number(pageScrollTop.value) + Number(rect.top))
        uni.pageScrollTo({ scrollTop: target, duration: 150 })
      })
      .exec()
  })
}

const goDetail = (id: number) => {
  uni.navigateTo({ url: '/pages/contact/contact-detail?id=' + id })
}

// 跳转到搜索用户页面
const goToSearch = () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.navigateTo({ url: '/pages/contact/user-search' })
}

// 跳转到新的朋友页面
const goToFriendRequests = () => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.navigateTo({ url: '/pages/contact/friend-requests' })
}

const fetchContacts = async () => {
  loading.value = true
  try {
    const q = searchQuery.value.trim()
    const list = await contactApi.listContacts({ q: q || undefined })
    contacts.value = list.map((item) => ({ ...item, tags: item.tags || [] }))
    if (tagFilter.value !== 'all' && !topicFilters.value.includes(tagFilter.value)) {
      tagFilter.value = 'all'
    }
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
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

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.title') })
  fetchContacts()
  fetchReceivedRequests()
})

onPageScroll((e) => {
  pageScrollTop.value = e.scrollTop
})

watch(searchQuery, () => {
  if (queryTimer) clearTimeout(queryTimer)
  queryTimer = setTimeout(() => {
    fetchContacts()
  }, 220) as unknown as number
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  box-sizing: border-box;
}

.search-shell {
  width: 100%;
}

.search-shell :deep(.u-search__content) {
  background: rgba(243, 243, 245, 0.65) !important;
  border: 1rpx solid rgba(0, 0, 0, 0.06) !important;
  border-radius: var(--radius-lg) !important;
}

.search-shell :deep(.u-search__content__input) {
  font-size: 26rpx !important;
}

/* 新的朋友入口 */
.new-friends-entry {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 32rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.new-friends-entry:active {
  background: rgba(3, 2, 19, 0.03);
}

.new-friends-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #10B981 0%, #059669 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  flex-shrink: 0;
}

.new-friends-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.new-friends-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.new-friends-subtitle {
  font-size: 24rpx;
  color: var(--c-muted);
}

.new-friends-arrow {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.new-friends-badge {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: rgba(239, 68, 68, 0.95);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chev {
  font-size: 36rpx;
  color: var(--c-muted);
}

.chips-scroll {
  flex-shrink: 0;
  white-space: nowrap;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  background: #ffffff;
}

.chips-scroll::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.chips-inner {
  display: inline-flex;
  gap: 16rpx;
  padding: 20rpx 32rpx 24rpx;
}

.chip {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  border-radius: var(--radius-md);
  font-size: 24rpx;
  font-weight: 500;
  background: rgba(243, 243, 245, 0.85);
  color: var(--c-muted);
  border: none;
  margin: 0;
  line-height: 1.3;
}

.chip.active {
  background: var(--c-primary);
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(3, 2, 19, 0.12);
}

.list-zone {
  flex: 0 0 auto;
  min-height: 0;
  position: relative;
}

.main-scroll {
  min-height: 0;
  padding: 16rpx 32rpx 24rpx 24rpx;
  padding-right: 72rpx;
  padding-bottom: var(--page-pad-bottom-tabbar);
  box-sizing: border-box;
}

.section {
  margin-bottom: 24rpx;
}

.letter-head {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 12rpx 8rpx 16rpx;
  margin: 0 -8rpx 12rpx;
  padding-left: 16rpx;
  padding-right: 16rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.letter-text {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--c-primary);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 20rpx 16rpx;
  border-radius: var(--radius-lg);
  transition: background 0.15s ease;
}

.contact-row:active {
  background: rgba(3, 2, 19, 0.05);
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  position: relative;
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.06);
}

.avatar--photo {
  background: #ececf0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-lg);
}

.avatar-shine {
  position: absolute;
  inset: 0;
  padding: 1rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.35) 0%, transparent 50%);
  pointer-events: none;
}

.avatar-letter {
  position: relative;
  z-index: 1;
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
}

.info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 4rpx;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--c-ink);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role {
  font-size: 28rpx;
  color: var(--c-muted);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.index-bar {
  position: absolute;
  right: 4rpx;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  padding: 8rpx 0;
  z-index: 5;
}

.index-letter {
  width: 40rpx;
  height: 40rpx;
  padding: 0;
  margin: 0;
  line-height: 40rpx;
  font-size: 20rpx;
  font-weight: 700;
  color: var(--c-primary);
  background: transparent;
  border: none;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.index-letter:active {
  background: rgba(3, 2, 19, 0.06);
}

.state,
.empty {
  text-align: center;
  padding: 80rpx 32rpx;
}

.state-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.state-text {
  font-size: 26rpx;
  color: var(--c-muted);
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
}

.empty-sub {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 12rpx;
  display: block;
  line-height: 1.5;
}

.scroll-pad {
  height: 48rpx;
}
</style>
