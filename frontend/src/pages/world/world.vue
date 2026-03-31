<template>
  <view class="page">
    <!-- 话题详情 -->
    <template v-if="selectedTopic">
      <view class="header">
        <view class="header-row">
          <button class="icon-btn" @click="selectedTopic = null">
            <text class="icon-chevron">‹</text>
          </button>
          <view class="header-titles">
            <text class="header-title topic-hash">#{{ selectedTopic }}</text>
            <text class="header-sub">{{ topicMeta?.posts }} {{ t('page.world.posts') }} · {{ topicMeta?.followers }} {{ t('page.world.followers') }}</text>
          </view>
        </view>
      </view>
      <!-- #ifdef H5 -->
      <view class="scroll-feed native-scroll">
        <WorldPostCard
          v-for="post in topicPosts"
          :key="post.id"
          :post="post"
          @upvote="() => toggleUpvote(post)"
          @reply="() => onReplyTap(post)"
          @share="openShare(post)"
        />
      </view>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <scroll-view class="scroll-feed" scroll-y>
        <WorldPostCard
          v-for="post in topicPosts"
          :key="post.id"
          :post="post"
          @upvote="() => toggleUpvote(post)"
          @reply="() => onReplyTap(post)"
          @share="openShare(post)"
        />
      </scroll-view>
      <!-- #endif -->
    </template>

    <template v-else>
      <view class="header">
        <view class="header-top">
          <text class="header-title">{{ t('page.world.title') }}</text>
          <button class="icon-btn bordered" @click="onSearchTap">
            <text class="search-glyph">⌕</text>
          </button>
        </view>
        <view class="tabs">
          <u-tabs
            :list="tabOptions"
            :current="activeTabIndex"
            key-name="name"
            :scrollable="false"
            :line-color="'#030213'"
            :active-style="{ color: '#030213', fontWeight: 600 }"
            :inactive-style="{ color: '#717182' }"
            :item-style="{ height: '88rpx' }"
            @change="onTabChange"
          />
        </view>
      </view>

      <template v-if="activeTab === 'posts'">
        <view class="sort-row">
          <view class="sort-row-inner">
            <view class="sort-wrap">
              <button class="sort-btn" @click="showSortDropdown = !showSortDropdown">
                <text>{{ sortLabel }}</text>
                <text class="chev">▼</text>
              </button>
              <view v-if="showSortDropdown" class="sort-menu">
                <view
                  v-for="opt in sortOptions"
                  :key="opt.value"
                  class="sort-item"
                  :class="{ current: sortBy === opt.value }"
                  @click="onSortDropdownSelect(opt.value)"
                >
                  <text>{{ opt.label }}</text>
                </view>
              </view>
            </view>
            <button class="new-post-btn" @click="openNewPost">
              <text class="new-post-plus">＋</text>
              <text>{{ t('page.world.new_post_short') }}</text>
            </button>
          </view>
        </view>
        <!-- #ifdef H5 -->
        <view class="scroll-feed native-scroll">
          <view v-if="sortedPosts.length === 0" class="feed-empty">
            <text>{{ t('common.empty_conversation') }}</text>
          </view>
          <WorldPostCard
            v-for="post in sortedPosts"
            :key="post.id"
            :post="post"
            @upvote="() => toggleUpvote(post)"
            @reply="() => onReplyTap(post)"
            @share="openShare(post)"
          />
          <view v-if="sortedPosts.length > 0" class="feed-end">
            <text>没有更多内容了</text>
          </view>
        </view>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <scroll-view class="scroll-feed" scroll-y>
          <view v-if="sortedPosts.length === 0" class="feed-empty">
            <text>{{ t('common.empty_conversation') }}</text>
          </view>
          <WorldPostCard
            v-for="post in sortedPosts"
            :key="post.id"
            :post="post"
            @upvote="() => toggleUpvote(post)"
            @reply="() => onReplyTap(post)"
            @share="openShare(post)"
          />
          <view v-if="sortedPosts.length > 0" class="feed-end">
            <text>没有更多内容了</text>
          </view>
        </scroll-view>
        <!-- #endif -->
      </template>

      <!-- #ifdef H5 -->
      <view v-else class="scroll-feed scroll-topics native-scroll">
        <view v-if="topicList.length === 0" class="feed-empty">
          <text>{{ t('common.empty_conversation') }}</text>
        </view>
        <WorldTopicCard
          v-for="topic in topicList"
          :key="topic.id"
          :topic="topic"
          :posts-label="t('page.world.posts')"
          :followers-label="t('page.world.followers')"
          :follow-text="t('page.world.follow')"
          :followed-text="t('page.world.followed')"
          @open="openTopic"
          @toggle-follow="toggleFollow"
        />
        <view v-if="topicList.length > 0" class="feed-end">
          <text>没有更多内容了</text>
        </view>
      </view>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <scroll-view v-else class="scroll-feed scroll-topics" scroll-y>
        <view v-if="topicList.length === 0" class="feed-empty">
          <text>{{ t('common.empty_conversation') }}</text>
        </view>
        <WorldTopicCard
          v-for="topic in topicList"
          :key="topic.id"
          :topic="topic"
          :posts-label="t('page.world.posts')"
          :followers-label="t('page.world.followers')"
          :follow-text="t('page.world.follow')"
          :followed-text="t('page.world.followed')"
          @open="openTopic"
          @toggle-follow="toggleFollow"
        />
        <view v-if="topicList.length > 0" class="feed-end">
          <text>没有更多内容了</text>
        </view>
      </scroll-view>
      <!-- #endif -->
    </template>

    <WorldNewPostModal
      :visible="showNewPostModal"
      :content="newPostContent"
      :friends="mentionFriends"
      :local-image-path="localImagePath"
      :local-video-path="localVideoPath"
      :video-error="videoError"
      :media-tip="mediaTip"
      :can-submit="canSubmitPost"
      :posting="posting"
      :placeholder="t('page.world.new_post_placeholder')"
      @close="closeNewPost"
      @update:content="onNewPostContentChange"
      @update:mentioned-ids="onMentionedIdsChange"
      @pick-image="pickImage"
      @pick-video="pickVideo"
      @clear-media="clearMedia"
      @submit="submitNewPost"
    />

    <WorldReplyModal
      :visible="showReplyModal"
      :post="replyTarget"
      :content="replyContent"
      :can-submit="canSubmitReply"
      :posting="replyPosting"
      :placeholder="t('placeholder.message')"
      @close="closeReply"
      @update:content="(v) => (replyContent = v)"
      @submit="submitReply"
    />

    <WorldShareModal
      :visible="showShareModal"
      :post="shareTarget"
      :copied="shareCopied"
      :share-note="shareNote"
      @close="closeShare"
      @channel="shareChannel"
      @copy="copyShareLink"
      @update:share-note="(v) => (shareNote = v)"
    />
    <BottomNav />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import { worldApi, type WorldMediaType, type WorldPost, type WorldSort, type WorldTopic } from '@/api/modules/world'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { getApiErrorMessage } from '@/utils/request'
import WorldPostCard from '@/components/world/WorldPostCard.vue'
import WorldTopicCard from '@/components/world/WorldTopicCard.vue'
import WorldNewPostModal from '@/components/world/WorldNewPostModal.vue'
import WorldReplyModal from '@/components/world/WorldReplyModal.vue'
import WorldShareModal from '@/components/world/WorldShareModal.vue'
import BottomNav from '@/components/BottomNav.vue'

const { t } = useI18n({ useScope: 'global' })

type SortOption = WorldSort

const activeTab = ref<'posts' | 'topics'>('posts')
const sortBy = ref<SortOption>('friends')
const showSortDropdown = ref(false)
const selectedTopic = ref<string | null>(null)

const posts = ref<WorldPost[]>([])
const topicList = ref<WorldTopic[]>([])
const topicPosts = ref<WorldPost[]>([])
const loading = ref(false)

const showNewPostModal = ref(false)
const newPostContent = ref('')
const mentionFriends = ref<ContactItem[]>([])
const mentionedUserIds = ref<number[]>([])
const localImagePath = ref('')
const localVideoPath = ref('')
const videoError = ref('')
const posting = ref(false)

const showReplyModal = ref(false)
const replyTarget = ref<WorldPost | null>(null)
const replyContent = ref('')
const replyPosting = ref(false)

const showShareModal = ref(false)
const shareTarget = ref<WorldPost | null>(null)
const shareLinkUrl = ref('')
const shareNote = ref('')
const shareCopied = ref(false)

const sortOptions = computed(() => [
  { value: 'friends' as SortOption, label: t('page.world.sort_friends') },
  { value: 'upvotes' as SortOption, label: t('page.world.sort_upvotes') },
  { value: 'topics' as SortOption, label: t('page.world.sort_topics') },
])
const tabOptions = computed(() => ([
  { name: t('page.world.tab_posts'), value: 'posts' },
  { name: t('page.world.tab_topics'), value: 'topics' },
]))
const activeTabIndex = computed(() => (activeTab.value === 'posts' ? 0 : 1))

const sortLabel = computed(() => sortOptions.value.find((o) => o.value === sortBy.value)?.label ?? '')

const topicMeta = computed(() => topicList.value.find((x) => x.name === selectedTopic.value))
const sortedPosts = computed(() => posts.value)

const canSubmitPost = computed(() => {
  const text = String(newPostContent.value || '').trim()
  return !!(text || localImagePath.value || localVideoPath.value)
})

const canSubmitReply = computed(() => {
  return !!String(replyContent.value || '').trim()
})

const mediaTip = computed(() => {
  if (localVideoPath.value) return t('page.world.media_tip_video')
  if (localImagePath.value) return t('page.world.media_tip_image')
  return t('page.world.media_tip_text')
})

function normalizeMediaUrl(url?: string): string | undefined {
  const value = String(url || '').trim()
  if (!value || value === 'null' || value === 'undefined') return undefined
  if (value === '-' || value === '--' || value === 'N/A' || value === 'n/a') return undefined
  // 明显非 URL 的垃圾值，直接降级避免出现空媒体占位
  if (!/^https?:\/\//i.test(value) && !value.startsWith('/api/') && !value.startsWith('/uploads/')) {
    return undefined
  }
  return value
}

function normalizePost(post: WorldPost): WorldPost {
  const imageUrl = normalizeMediaUrl(post.imageUrl)
  const videoUrl = normalizeMediaUrl(post.videoUrl)
  const rawType = String(post.mediaType || 'TEXT').toUpperCase()
  const mediaType =
    rawType === 'IMAGE' ? (imageUrl ? 'IMAGE' : 'TEXT')
      : rawType === 'VIDEO' ? (videoUrl ? 'VIDEO' : 'TEXT')
        : 'TEXT'
  return {
    ...post,
    mediaType,
    imageUrl,
    videoUrl,
  }
}

function openExternalUrl(url: string) {
  // #ifdef H5
  if (typeof window !== 'undefined' && window.open) {
    window.open(url, '_blank')
    return
  }
  // #endif
  if (typeof plus !== 'undefined' && plus.runtime?.openURL) {
    plus.runtime.openURL(url)
    return
  }
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: t('page.world.open_browser_tip'), icon: 'none' }),
  })
}

function openNewPost() {
  showSortDropdown.value = false
  showNewPostModal.value = true
}

function onTabChange(item: any, index?: number) {
  const idx = typeof item?.index === 'number' ? item.index : (typeof index === 'number' ? index : 0)
  activeTab.value = idx === 1 ? 'topics' : 'posts'
}

function onSortDropdownSelect(value: SortOption) {
  showSortDropdown.value = false
  if (value === 'friends' || value === 'upvotes' || value === 'topics') {
    sortBy.value = value
  }
}

function closeNewPost() {
  showNewPostModal.value = false
  newPostContent.value = ''
  mentionedUserIds.value = []
  localImagePath.value = ''
  localVideoPath.value = ''
  videoError.value = ''
}

function clearMedia() {
  localImagePath.value = ''
  localVideoPath.value = ''
  videoError.value = ''
}

function onNewPostContentChange(value: string) {
  newPostContent.value = String(value || '')
}

function onMentionedIdsChange(value: number[]) {
  mentionedUserIds.value = Array.isArray(value) ? value : []
}

function pickImage() {
  videoError.value = ''
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: (res) => {
      const p = res.tempFilePaths?.[0]
      if (p) {
        localImagePath.value = p
        localVideoPath.value = ''
      }
    },
  })
}

function pickVideo() {
  videoError.value = ''
  uni.chooseVideo({
    maxDuration: 15,
    sourceType: ['album', 'camera'],
    success: (res) => {
      const d = typeof res.duration === 'number' ? res.duration : Number(res.duration)
      if (d > 15) {
        videoError.value = t('page.world.video_too_long')
        return
      }
      const p = res.tempFilePath
      if (p) {
        localVideoPath.value = p
        localImagePath.value = ''
      }
    },
    fail: (err: any) => {
      const msg = String(err?.errMsg || '')
      if (msg.includes('cancel') || msg.includes('取消')) return
    },
  })
}

async function submitNewPost() {
  if (!canSubmitPost.value || posting.value) return
  posting.value = true
  try {
    let imageUrl: string | undefined
    let videoUrl: string | undefined
    let mediaType: WorldMediaType = 'TEXT'
    if (localImagePath.value) {
      uni.showLoading({ title: t('page.world.posting'), mask: true })
      imageUrl = await worldApi.uploadMedia(localImagePath.value)
      mediaType = 'IMAGE'
    } else if (localVideoPath.value) {
      uni.showLoading({ title: t('page.world.posting'), mask: true })
      videoUrl = await worldApi.uploadMedia(localVideoPath.value)
      mediaType = 'VIDEO'
    }
    const created = await worldApi.createPost({
      content: newPostContent.value.trim(),
      mediaType,
      mentionedUserIds: mentionedUserIds.value,
      imageUrl,
      videoUrl,
    })
    posts.value = [normalizePost(created), ...posts.value]
    closeNewPost()
    uni.showToast({ title: t('toast.post_published'), icon: 'success' })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.create_failed')), icon: 'none' })
  } finally {
    posting.value = false
    uni.hideLoading()
  }
}

function onReplyTap(post: WorldPost) {
  showReplyModal.value = true
  replyTarget.value = post
  replyContent.value = ''
  replyPosting.value = false
}

function closeReply() {
  showReplyModal.value = false
  replyTarget.value = null
  replyContent.value = ''
  replyPosting.value = false
}

async function submitReply() {
  if (!replyTarget.value || !canSubmitReply.value || replyPosting.value) return
  replyPosting.value = true
  try {
    await worldApi.createReply(replyTarget.value.id, { content: replyContent.value.trim() })
    // 刷新当前列表，确保 replyCount 即时一致
    if (selectedTopic.value) {
      await openTopic(selectedTopic.value)
    } else {
      await loadPosts()
    }
    uni.showToast({ title: t('toast.post_published'), icon: 'success' })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.create_failed')), icon: 'none' })
  } finally {
    replyPosting.value = false
    closeReply()
  }
}

async function openShare(post: WorldPost) {
  showSortDropdown.value = false
  shareTarget.value = post
  shareNote.value = ''
  shareCopied.value = false
  shareLinkUrl.value = ''
  showShareModal.value = true
  try {
    const { url } = await worldApi.getShareLink(post.id)
    shareLinkUrl.value = url
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  }
}

function closeShare() {
  showShareModal.value = false
  shareTarget.value = null
  shareNote.value = ''
  shareLinkUrl.value = ''
  shareCopied.value = false
}

function shareBodyText() {
  const note = shareNote.value.trim()
  if (note) return note
  return (shareTarget.value?.content || '').trim()
}

function shareChannel(channel: string) {
  const link = shareLinkUrl.value
  if (!link) {
    uni.showToast({ title: t('page.world.share_link_loading'), icon: 'none' })
    return
  }
  const text = shareBodyText()
  let url = ''
  switch (channel) {
    case 'twitter':
      url = `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}&url=${encodeURIComponent(link)}`
      break
    case 'linkedin':
      url = `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(link)}`
      break
    case 'facebook':
      url = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(link)}`
      break
    case 'email':
      url = `mailto:?subject=${encodeURIComponent('EqoChat')}&body=${encodeURIComponent(`${text}\n\n${link}`)}`
      openExternalUrl(url)
      return
    case 'whatsapp':
      url = `https://wa.me/?text=${encodeURIComponent(`${text} ${link}`)}`
      break
    case 'telegram':
      url = `https://t.me/share/url?url=${encodeURIComponent(link)}&text=${encodeURIComponent(text)}`
      break
    default:
      return
  }
  openExternalUrl(url)
}

function copyShareLink() {
  const link = shareLinkUrl.value
  if (!link) {
    uni.showToast({ title: t('page.world.share_link_loading'), icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: link,
    success: () => {
      shareCopied.value = true
      setTimeout(() => {
        shareCopied.value = false
      }, 2000)
    },
  })
}

const loadPosts = async () => {
  loading.value = true
  try {
    const list = await worldApi.listPosts({ sort: sortBy.value, limit: 30 })
    posts.value = list.map(normalizePost)
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadTopics = async () => {
  try {
    topicList.value = await worldApi.listTopics({ limit: 50 })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  }
}

const loadMentionFriends = async () => {
  try {
    mentionFriends.value = await contactApi.listContacts()
  } catch {
    mentionFriends.value = []
  }
}

const openTopic = async (name: string) => {
  selectedTopic.value = name
  try {
    const list = await worldApi.listTopicPosts(name, { limit: 30 })
    topicPosts.value = list.map(normalizePost)
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
    topicPosts.value = []
  }
}

async function toggleUpvote(post: WorldPost) {
  try {
    const res = await worldApi.toggleUpvote(post.id)
    const nextUpvoted = !!res?.upvoted
    const delta = nextUpvoted === post.upvoted ? 0 : (nextUpvoted ? 1 : -1)
    const update = (p: WorldPost) => ({ ...p, upvoted: nextUpvoted, upvotes: Math.max(0, (p.upvotes || 0) + delta) })
    posts.value = posts.value.map((p) => (p.id === post.id ? update(p) : p))
    topicPosts.value = topicPosts.value.map((p) => (p.id === post.id ? update(p) : p))
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  }
}

async function toggleFollow(topic: WorldTopic) {
  try {
    const res = await worldApi.toggleFollow(topic.name)
    const nextFollowing = !!res?.following
    topicList.value = topicList.value.map((row) => {
      if (row.id !== topic.id) return row
      const delta = nextFollowing === row.favorite ? 0 : (nextFollowing ? 1 : -1)
      return {
        ...row,
        favorite: nextFollowing,
        followers: Math.max(0, (row.followers || 0) + delta),
      }
    })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
  }
}

function onSearchTap() {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('page.world.title') })
  loadMentionFriends()
  loadTopics()
  loadPosts()
})


watch(sortBy, () => {
  if (activeTab.value === 'posts') loadPosts()
})

watch(activeTab, (v) => {
  if (v === 'posts') loadPosts()
  else loadTopics()
})

watch(selectedTopic, (v) => {
  if (v) showSortDropdown.value = false
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

.header {
  flex-shrink: 0;
  padding: 16rpx 24rpx 0;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.04);
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12rpx;
}

.header-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-bottom: 16rpx;
}

.header-titles {
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.header-sub {
  font-size: 22rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
  display: block;
}

.icon-btn {
  width: 72rpx;
  height: 72rpx;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--c-input-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn.bordered {
  border: 1rpx solid var(--c-border);
  background: var(--c-surface);
}

.icon-chevron {
  font-size: 44rpx;
  font-weight: 300;
  color: var(--c-ink);
  line-height: 1;
}

.search-glyph {
  font-size: 32rpx;
  color: var(--c-ink);
}

.tabs {
  display: block;
  border-top: 1rpx solid var(--c-border);
}

.sort-row {
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.9);
}

.sort-row-inner {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.sort-wrap {
  position: relative;
  flex: 1;
  min-width: 0;
}

.sort-btn {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 6rpx 24rpx;
  border-radius: var(--radius-md);
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  font-size: 26rpx;
  color: var(--c-ink);
}

.sort-menu {
  position: absolute;
  left: 0;
  top: calc(100% + 8rpx);
  min-width: 280rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  z-index: 30;
  overflow: hidden;
}

.sort-item {
  padding: 22rpx 26rpx;
  font-size: 26rpx;
  color: var(--c-ink);
}

.sort-item.current {
  background: rgba(3, 2, 19, 0.08);
  color: var(--c-primary);
  font-weight: 600;
}

.new-post-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 22rpx;
  border-radius: var(--radius-md);
  background: var(--c-primary);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  border: none;
  box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.24);
}

.new-post-plus {
  font-size: 28rpx;
  font-weight: 400;
  line-height: 1;
}

.chev {
  font-size: 20rpx;
  color: var(--c-muted);
}


.scroll-feed {
  flex: 1;
  height: 0;
  padding: 24rpx;
  padding-bottom: var(--page-pad-bottom-tabbar);
  box-sizing: border-box;
}

.native-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.scroll-topics {
  padding-top: 16rpx;
}

.feed-empty {
  text-align: center;
  color: var(--c-muted);
  font-size: 24rpx;
  padding: 80rpx 0 24rpx;
}

.feed-end {
  text-align: center;
  color: var(--c-muted);
  font-size: 22rpx;
  padding: 8rpx 0 24rpx;
}

.topic-hash {
  color: var(--c-violet);
}
</style>
