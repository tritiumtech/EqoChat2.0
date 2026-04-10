<template>
  <view class="page">
    <!-- 话题详情 -->
    <TopicDetail
      v-if="selectedTopic"
      ref="topicDetailRef"
      :topic-name="selectedTopic"
      :topic-list="topicList"
      :normalize-post="normalizePost"
      @back="selectedTopic = null"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <!-- Posts Tab -->
    <PostList
      v-else-if="activeTab === 'posts'"
      ref="postListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      :sort-options="sortOptions"
      :normalize-post="normalizePost"
      @tab-change="onTabChange"
      @new-post="openNewPost"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <!-- Topics Tab -->
    <TopicList
      v-else-if="activeTab === 'topics'"
      ref="topicListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      @tab-change="onTabChange"
      @open-topic="openTopic"
      @toggle-follow="toggleFollow"
    />

    <!-- My Tab -->
    <MyPostList
      v-else
      ref="myPostListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      :normalize-post="normalizePost"
      @tab-change="onTabChange"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <!-- Modals -->
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

    <!-- 详情层 -->
    <WorldPostDetail
      v-if="detailTarget"
      ref="detailRef"
      :post="detailTarget"
      @back="detailTarget = null"
      @upvote="detailTarget && toggleUpvote(detailTarget)"
      @reply="detailTarget && onReplyTap(detailTarget)"
      @share="detailTarget && openShare(detailTarget)"
    />

    <FgTabbar v-if="!detailTarget" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { worldApi, type WorldMediaType, type WorldPost, type WorldSort, type WorldTopic } from '@/api/modules/world'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { getApiErrorMessage } from '@/utils/request'
import PostList from '@/components/world/PostList.vue'
import TopicList from '@/components/world/TopicList.vue'
import MyPostList from '@/components/world/MyPostList.vue'
import TopicDetail from '@/components/world/TopicDetail.vue'
import WorldPostDetail from '@/components/world/WorldPostDetail.vue'
import WorldNewPostModal from '@/components/world/WorldNewPostModal.vue'
import WorldReplyModal from '@/components/world/WorldReplyModal.vue'
import WorldShareModal from '@/components/world/WorldShareModal.vue'
import FgTabbar from '@/tabbar/index.vue'

const { t } = useI18nWithFormat()

type SortOption = WorldSort

const activeTab = ref<'posts' | 'topics' | 'my'>('posts')
const selectedTopic = ref<string | null>(null)
const topicList = ref<WorldTopic[]>([])

// New Post Modal
const showNewPostModal = ref(false)
const newPostContent = ref('')
const mentionFriends = ref<ContactItem[]>([])
const mentionedUserIds = ref<number[]>([])
const localImagePath = ref('')
const localVideoPath = ref('')
const videoError = ref('')
const posting = ref(false)

// Reply Modal
const showReplyModal = ref(false)
const replyTarget = ref<WorldPost | null>(null)
const replyContent = ref('')
const replyPosting = ref(false)

// Share Modal
const showShareModal = ref(false)
const shareTarget = ref<WorldPost | null>(null)
const shareLinkUrl = ref('')
const shareNote = ref('')
const shareCopied = ref(false)

// Detail
const detailTarget = ref<WorldPost | null>(null)
const detailRef = ref<InstanceType<typeof WorldPostDetail> | null>(null)

// Refs
const postListRef = ref<InstanceType<typeof PostList> | null>(null)
const topicListRef = ref<InstanceType<typeof TopicList> | null>(null)
const myPostListRef = ref<InstanceType<typeof MyPostList> | null>(null)
const topicDetailRef = ref<InstanceType<typeof TopicDetail> | null>(null)

const sortOptions = computed(() => [
  { value: 'friends' as SortOption, label: t('page.world.sort_friends') },
  { value: 'upvotes' as SortOption, label: t('page.world.sort_upvotes') },
  { value: 'topics' as SortOption, label: t('page.world.sort_topics') },
])

const tabOptions = computed(() => ([
  { name: t('page.world.tab_posts'), value: 'posts' },
  { name: t('page.world.tab_topics'), value: 'topics' },
  { name: t('page.world.tab_my'), value: 'my' },
]))

const activeTabIndex = computed(() => {
  if (activeTab.value === 'topics') return 1
  if (activeTab.value === 'my') return 2
  return 0
})

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

// Tab change handler
function onTabChange(index: number) {
  if (index === 1) {
    activeTab.value = 'topics'
  } else if (index === 2) {
    activeTab.value = 'my'
  } else {
    activeTab.value = 'posts'
  }
}

// Watch tab changes to reload
watch(activeTab, (v) => {
  if (v === 'posts') {
    postListRef.value?.reload()
  } else if (v === 'topics') {
    topicListRef.value?.reload()
  } else if (v === 'my') {
    myPostListRef.value?.reload()
  }
})

// Topic handlers
function openTopic(topic: WorldTopic) {
  selectedTopic.value = topic.name
}

async function toggleFollow(topic: WorldTopic) {
  try {
    await worldApi.followTopic(topic.name, !topic.followed)
    topic.followed = !topic.followed
    topic.followers += topic.followed ? 1 : -1
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.error')), icon: 'none' })
  }
}

// Post handlers
function normalizeMediaUrl(url?: string): string | undefined {
  const value = String(url || '').trim()
  if (!value || value === 'null' || value === 'undefined') return undefined
  if (value === '-' || value === '--' || value === 'N/A' || value === 'n/a') return undefined
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

async function toggleUpvote(post: WorldPost) {
  try {
    await worldApi.upvotePost(post.id)
    post.upvoted = !post.upvoted
    post.upvotes += post.upvoted ? 1 : -1
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.error')), icon: 'none' })
  }
}

// Reply handlers
function onReplyTap(post: WorldPost) {
  replyTarget.value = post
  showReplyModal.value = true
}

function closeReply() {
  showReplyModal.value = false
  replyTarget.value = null
  replyContent.value = ''
}

async function submitReply() {
  if (!replyTarget.value || !replyContent.value.trim()) return
  replyPosting.value = true
  try {
    await worldApi.replyToPost(replyTarget.value.id, replyContent.value.trim())
    replyTarget.value.replies++
    closeReply()
    uni.showToast({ title: t('toast.success'), icon: 'success' })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.error')), icon: 'none' })
  } finally {
    replyPosting.value = false
  }
}

// Share handlers
function openShare(post: WorldPost) {
  shareTarget.value = post
  showShareModal.value = true
}

function closeShare() {
  showShareModal.value = false
  shareTarget.value = null
  shareLinkUrl.value = ''
  shareNote.value = ''
}

function shareChannel(channel: string) {
  uni.showToast({ title: `${t('page.world.share_to')} ${channel}`, icon: 'none' })
  closeShare()
}

function copyShareLink() {
  const post = shareTarget.value
  if (!post) return
  const link = shareLinkUrl.value || `https://eqochat.app/world/post/${post.id}`
  const text = `${post.content?.slice(0, 80) || ''}${(post.content?.length || 0) > 80 ? '...' : ''}\n${link}`
  uni.setClipboardData({
    data: text,
    success: () => {
      shareCopied.value = true
      setTimeout(() => {
        shareCopied.value = false
      }, 2000)
    },
  })
}

// New Post handlers
function openNewPost() {
  showNewPostModal.value = true
}

function closeNewPost() {
  showNewPostModal.value = false
  newPostContent.value = ''
  mentionedUserIds.value = []
  localImagePath.value = ''
  localVideoPath.value = ''
  videoError.value = ''
}

function onNewPostContentChange(value: string) {
  newPostContent.value = value
}

function onMentionedIdsChange(ids: number[]) {
  mentionedUserIds.value = ids
}

async function pickImage() {
  try {
    const res = await uni.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })
    const tempPath = (res as any).tempFilePaths?.[0]
    if (tempPath) {
      const uploadRes = await uni.uploadFile({
        url: '/api/files/upload',
        filePath: tempPath,
        name: 'file',
      })
      const data = JSON.parse((uploadRes as any).data)
      localImagePath.value = data.url
      localVideoPath.value = ''
      videoError.value = ''
    }
  } catch {
    uni.showToast({ title: t('toast.error'), icon: 'none' })
  }
}

async function pickVideo() {
  try {
    const res = await uni.chooseVideo({ sourceType: ['album', 'camera'], maxDuration: 60 })
    const tempPath = (res as any).tempFilePath
    if (tempPath) {
      const uploadRes = await uni.uploadFile({
        url: '/api/files/upload',
        filePath: tempPath,
        name: 'file',
      })
      const data = JSON.parse((uploadRes as any).data)
      localVideoPath.value = data.url
      localImagePath.value = ''
      videoError.value = ''
    }
  } catch {
    uni.showToast({ title: t('toast.error'), icon: 'none' })
  }
}

function clearMedia() {
  localImagePath.value = ''
  localVideoPath.value = ''
  videoError.value = ''
}

async function submitNewPost() {
  if (!canSubmitPost.value) return
  posting.value = true
  try {
    const mediaType: WorldMediaType = localVideoPath.value ? 'VIDEO' : localImagePath.value ? 'IMAGE' : 'TEXT'
    await worldApi.createPost({
      content: newPostContent.value.trim(),
      mediaType,
      imageUrl: localImagePath.value || undefined,
      videoUrl: localVideoPath.value || undefined,
      mentionedUserIds: mentionedUserIds.value,
    })
    closeNewPost()
    postListRef.value?.reload()
    uni.showToast({ title: t('toast.success'), icon: 'success' })
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.error')), icon: 'none' })
  } finally {
    posting.value = false
  }
}

// Lifecycle
const loadMentionFriends = async () => {
  try {
    mentionFriends.value = await contactApi.listContacts()
  } catch {
    mentionFriends.value = []
  }
}

onShow(() => {
  uni.setNavigationBarTitle({ title: t('page.world.title') })
  loadMentionFriends()
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
</style>
