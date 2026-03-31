<template>
  <view class="page">
    <view class="head">
      <view class="head-row">
        <text class="screen-title">{{ t('page.contact.title') }}</text>
        <button class="add-btn" @click="showAddModal = true; addFormError = ''">
          <text class="add-glyph">+</text>
        </button>
      </view>
      <SearchBar v-model="searchQuery" :placeholder="t('page.contact.search_placeholder')" />
    </view>

    <scroll-view class="chips-scroll" scroll-x :show-scrollbar="false">
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
    </scroll-view>

    <view v-if="receivedRequests.length > 0" class="requests-bar">
      <text class="requests-title">{{ t('page.contact.friend_requests') }} ({{ receivedRequests.length }})</text>
      <scroll-view class="req-scroll" scroll-x :show-scrollbar="false">
        <view v-for="req in receivedRequests" :key="req.id" class="req-card">
          <text class="req-name">{{ req.requesterNickname || `ID ${req.requesterId}` }}</text>
          <view class="req-actions">
            <button class="req-btn accept" @click="handleAccept(req.id)">{{ t('action.accept') }}</button>
            <button class="req-btn reject" @click="handleReject(req.id)">{{ t('action.reject') }}</button>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="list-zone">
      <!-- #ifdef H5 -->
      <view
        class="main-scroll native-scroll"
        :scroll-into-view="scrollIntoView"
      >
        <view v-if="loading" class="state">{{ t('common.loading') }}</view>
        <view v-else-if="letters.length === 0" class="empty">
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
                <view class="avatar" :style="avatarStyle(item)">
                  <text class="avatar-letter">{{ (item.nickname || '?').slice(0, 1) }}</text>
                </view>
              </view>
              <view class="info">
                <text class="name">{{ item.nickname }}</text>
                <text class="role">
                  {{ item.tags?.length ? `#${item.tags[0]}` : `${t('page.contact.user_id')}: ${item.id}` }}
                </text>
              </view>
            </view>
          </view>
        </block>
        <view class="scroll-pad" />
      </view>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <scroll-view
        class="main-scroll"
        scroll-y
        :scroll-into-view="scrollIntoView"
        scroll-with-animation
      >
        <view v-if="loading" class="state">{{ t('common.loading') }}</view>
        <view v-else-if="letters.length === 0" class="empty">
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
                <view class="avatar" :style="avatarStyle(item)">
                  <text class="avatar-letter">{{ (item.nickname || '?').slice(0, 1) }}</text>
                </view>
              </view>
              <view class="info">
                <text class="name">{{ item.nickname }}</text>
                <text class="role">
                  {{ item.tags?.length ? `#${item.tags[0]}` : `${t('page.contact.user_id')}: ${item.id}` }}
                </text>
              </view>
            </view>
          </view>
        </block>
        <view class="scroll-pad" />
      </scroll-view>
      <!-- #endif -->

      <view v-if="letters.length > 0" class="index-bar">
        <text
          v-for="letter in letters"
          :key="letter"
          class="index-letter"
          @click="scrollToLetter(letter)"
        >{{ letter }}</text>
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

    <BottomNav />
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { contactApi, type ContactItem as Contact } from '@/api/modules/contact'
import { friendRequestApi, type FriendRequestItem } from '@/api/modules/friendRequest'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import ModalSheet from '@/components/ModalSheet.vue'
import SearchBar from '@/components/SearchBar.vue'
import BottomNav from '@/components/BottomNav.vue'

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
const scrollIntoView = ref('')
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
  scrollIntoView.value = ''
  nextTick(() => {
    scrollIntoView.value = letterId(letter)
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
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  box-sizing: border-box;
}

.head {
  flex-shrink: 0;
  padding: 20rpx 24rpx 12rpx;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.04);
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.screen-title {
  font-size: 40rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.add-btn {
  width: 80rpx;
  height: 80rpx;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--c-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.24);
}

.add-glyph {
  font-size: 44rpx;
  font-weight: 300;
  line-height: 1;
  color: #fff;
}

.chips-scroll {
  flex-shrink: 0;
  white-space: nowrap;
  border-bottom: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.75);
}

.chips-inner {
  display: inline-flex;
  gap: 16rpx;
  padding: 16rpx 24rpx 20rpx;
}

.chip {
  flex-shrink: 0;
  padding: 12rpx 24rpx;
  border-radius: var(--radius-md);
  font-size: 24rpx;
  font-weight: 600;
  background: #f3f3f5;
  color: var(--c-muted);
  border: none;
  margin: 0;
}

.chip.active {
  background: var(--c-primary);
  color: #fff;
}

.requests-bar {
  flex-shrink: 0;
  padding: 16rpx 0 8rpx;
  background: rgba(255, 255, 255, 0.85);
  border-bottom: 1rpx solid var(--c-border);
}

.requests-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: var(--c-muted);
  padding: 0 24rpx 12rpx;
}

.req-scroll {
  white-space: nowrap;
}

.req-card {
  display: inline-flex;
  flex-direction: column;
  gap: 12rpx;
  margin-left: 24rpx;
  padding: 16rpx 20rpx;
  min-width: 240rpx;
  background: var(--c-surface);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-lg);
  vertical-align: top;
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.05);
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
  flex: 1;
  height: 0;
  position: relative;
}

.main-scroll {
  height: 100%;
  padding: 8rpx 24rpx 24rpx 20rpx;
  padding-right: 56rpx;
  padding-bottom: var(--page-pad-bottom-tabbar);
  box-sizing: border-box;
}

.native-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.section {
  margin-bottom: 16rpx;
}

.letter-head {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 12rpx 8rpx;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1rpx solid var(--c-border);
  margin-bottom: 8rpx;
}

.letter-text {
  font-size: 22rpx;
  font-weight: 800;
  color: var(--c-primary);
  letter-spacing: 2rpx;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 12rpx;
  border-radius: var(--radius-lg);
}

.contact-row:active {
  background: rgba(3, 2, 19, 0.05);
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--c-shadow-soft);
}

.avatar-letter {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--c-ink);
  display: block;
}

.role {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
  display: block;
}

.index-bar {
  position: absolute;
  right: 4rpx;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 2rpx;
  z-index: 5;
}

.index-letter {
  font-size: 18rpx;
  font-weight: 700;
  color: var(--c-primary);
  padding: 4rpx 8rpx;
}

.state,
.empty {
  text-align: center;
  padding: 80rpx 32rpx;
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
  background: #f3f3f5;
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
