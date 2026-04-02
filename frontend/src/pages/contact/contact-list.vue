<template>
  <view class="page">
    <PageHeader 
      :title="t('page.contact.title')"
      action-icon="＋"
      action-variant="primary"
      action-size="md"
      @action-click="showAddModal = true; addFormError = ''"
    >
      <template #search>
        <view class="search-shell">
          <SearchBar v-model="searchQuery" :placeholder="t('page.contact.search_placeholder')" />
        </view>
      </template>
    </PageHeader>

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

    <view v-if="receivedRequests.length > 0" class="requests-bar">
      <text class="requests-title">{{ t('page.contact.friend_requests') }} ({{ receivedRequests.length }})</text>
      <view class="req-scroll">
        <view v-for="req in receivedRequests" :key="req.id" class="req-card">
          <text class="req-name">{{ req.requesterNickname || `ID ${req.requesterId}` }}</text>
          <view class="req-actions">
            <button class="req-btn accept" @click="handleAccept(req.id)">{{ t('action.accept') }}</button>
            <button class="req-btn reject" @click="handleReject(req.id)">{{ t('action.reject') }}</button>
          </view>
        </view>
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

    <FgTabbar />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { onPageScroll, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem as Contact } from '@/api/modules/contact'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import ModalSheet from '@/components/ModalSheet.vue'
import SearchBar from '@/components/SearchBar.vue'
import PageHeader from '@/components/PageHeader.vue'
import FgTabbar from '@/tabbar/index.vue'

const contacts = ref<Contact[]>([])
const receivedRequests = ref<FriendRequestItem[]>([])
const loading = ref(false)
const showAddModal = ref(false)
const addFormError = ref('')
const addForm = reactive({ friendId: '', requestMessage: '' })
const userStore = useUserStore()
const { t } = useI18n({ useScope: 'global' })

const searchQuery = ref('')
const tagFilter = ref('all')
const pageScrollTop = ref(0)
let queryTimer: number | null = null

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
  /* 对齐 Chatinterfacedesign theme：--background #ffffff（ContactsPage 主底为白） */
  background: #ffffff;
  box-sizing: border-box;
}

.head {
  flex-shrink: 0;
  padding: calc(var(--status-bar-height) + 20rpx) 32rpx 16rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1rpx 0 rgba(0, 0, 0, 0.04);
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.screen-title {
  font-size: 40rpx;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--c-ink);
}

.add-btn {
  width: 80rpx;
  height: 80rpx;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.22);
}

.add-btn:active {
  opacity: 0.92;
  transform: scale(0.96);
}

/* 对齐 ContactsPage：搜索框浅底 + 细边框 + 圆角 */
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

.requests-bar {
  flex-shrink: 0;
  padding: 16rpx 0 12rpx;
  background: #ffffff;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.requests-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: var(--c-muted);
  padding: 0 32rpx 12rpx;
}

.req-scroll {
  white-space: nowrap;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.req-scroll::-webkit-scrollbar {
  display: none;
}

.req-card {
  display: inline-flex;
  flex-direction: column;
  gap: 12rpx;
  margin-left: 32rpx;
  padding: 20rpx 24rpx;
  min-width: 260rpx;
  background: rgba(243, 243, 245, 0.85);
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  border-radius: var(--radius-lg);
  vertical-align: top;
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.04);
}

.req-card:last-child {
  margin-right: 32rpx;
}

.req-name {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.req-actions {
  display: flex;
  gap: 12rpx;
}

.req-btn {
  flex: 1;
  padding: 8rpx 16rpx;
  font-size: 22rpx;
  font-weight: 600;
  border-radius: var(--radius-pill);
  border: none;
  margin: 0;
}

.req-btn.accept {
  background: rgba(16, 185, 129, 0.2);
  color: #047857;
}

.req-btn.reject {
  background: rgba(26, 23, 32, 0.06);
  color: var(--c-muted);
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

.native-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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

.modal-header {
  text-align: center;
  padding: 12rpx 0 24rpx;
}

.modal-icon-wrap {
  width: 96rpx;
  height: 96rpx;
  margin: 0 auto 20rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
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

.optional {
  font-weight: 400;
  color: var(--c-muted);
  font-size: 24rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  background: #f8f8fb;
  border: 2rpx solid rgba(3, 2, 19, 0.12);
  border-radius: var(--radius-lg);
  padding: 0 24rpx;
}

.input-prefix {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-primary);
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
  background: #f8f8fb;
  border: 2rpx solid rgba(3, 2, 19, 0.12);
  border-radius: var(--radius-lg);
  padding: 24rpx;
  min-height: 160rpx;
}

.modal-form .textarea {
  width: 100%;
  min-height: 120rpx;
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
  color: var(--c-ink);
  background: rgba(3, 2, 19, 0.06);
  border-radius: var(--radius-lg);
  border: 1rpx solid rgba(3, 2, 19, 0.12);
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-send {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  border-radius: var(--radius-lg);
  border: 1rpx solid rgba(3, 2, 19, 0.2);
  margin: 0;
  box-shadow: 0 8rpx 18rpx rgba(3, 2, 19, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-send-text {
  color: #fff;
  font-weight: 700;
}

.btn-send[disabled] {
  opacity: 0.45;
  box-shadow: none;
}
</style>
