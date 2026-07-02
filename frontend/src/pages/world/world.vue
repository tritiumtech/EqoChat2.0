<template>
  <view class="page">
    <!-- 话题详情 -->
    <TopicDetail
      v-if="worldReady && selectedTopic"
      ref="topicDetailRef"
      :topic-name="selectedTopic"
      :topic-list="topicList"
      :normalize-post="normalizePost"
      :viewer="activeWorldSubject"
      @back="selectedTopic = null"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <!-- Posts Tab -->
    <PostList
      v-else-if="worldReady && activeTab === 'posts'"
      ref="postListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      :sort-options="sortOptions"
      :normalize-post="normalizePost"
      :viewer="activeWorldSubject"
      @tab-change="onTabChange"
      @new-post="openNewPost"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <!-- Topics Tab -->
    <TopicList
      v-else-if="worldReady && activeTab === 'topics'"
      ref="topicListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      :viewer="activeWorldSubject"
      @tab-change="onTabChange"
      @open-topic="openTopic"
      @toggle-follow="toggleFollow"
    />

    <!-- My Tab -->
    <MyPostList
      v-else-if="worldReady"
      ref="myPostListRef"
      :tab-options="tabOptions"
      :active-tab-index="activeTabIndex"
      :normalize-post="normalizePost"
      :viewer="activeWorldSubject"
      @tab-change="onTabChange"
      @open-detail="detailTarget = $event"
      @upvote="toggleUpvote"
      @reply="onReplyTap"
      @share="openShare"
    />

    <view v-else class="state">{{ t('common.loading') }}</view>

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
      @update:mentioned-subjects="onMentionedSubjectsChange"
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
      :viewer="activeWorldSubject"
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
import { worldApi, type WorldMediaType, type WorldMentionedSubject, type WorldPost, type WorldSort, type WorldTopic } from '@/api/modules/world'
import { contactApi, type ContactItem } from '@/api/modules/contact'
import { getApiErrorMessage } from '@/utils/request'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import { useUserStore } from '@/store/modules/user'
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
const activeSubjectStore = useActiveSubjectStore()
const userStore = useUserStore()

type SortOption = WorldSort

const activeTab = ref<'posts' | 'topics' | 'my'>('posts')
const worldReady = ref(false)
const selectedTopic = ref<string | null>(null)
const topicList = ref<WorldTopic[]>([])

// New Post Modal
const showNewPostModal = ref(false)
const newPostContent = ref('')
const mentionFriends = ref<ContactItem[]>([])
const mentionedSubjects = ref<WorldMentionedSubject[]>([])
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

const activeWorldSubject = computed(() => {
  const subject = activeSubjectStore.currentSubject
  return subject ? { subjectId: subject.subjectId, subjectType: subject.subjectType } : undefined
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

watch(
  () => activeSubjectStore.currentSubject
    ? `${activeSubjectStore.currentSubject.subjectType}:${activeSubjectStore.currentSubject.subjectId}`
    : '',
  () => {
    postListRef.value?.reload()
    topicListRef.value?.reload()
    myPostListRef.value?.reload()
    topicDetailRef.value?.reload()
    detailRef.value?.loadReplies()
    loadMentionFriends()
  },
)

// Topic handlers
function openTopic(topic: WorldTopic) {
  selectedTopic.value = topic.name
}

async function toggleFollow(topic: WorldTopic) {
  try {
    const wasFollowing = Boolean(topic.followed ?? topic.favorite)
    const result = await worldApi.followTopic(topic.name, !wasFollowing, activeWorldSubject.value)
    const following = typeof result?.following === 'boolean' ? result.following : !wasFollowing
    topic.followed = following
    topic.favorite = following
    if (following !== wasFollowing) {
      topic.followers = Math.max(0, topic.followers + (following ? 1 : -1))
    }
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
    await worldApi.upvotePost(post.id, activeWorldSubject.value)
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
  const actor = activeWorldSubject.value
  if (!actor?.subjectId) {
    uni.showToast({ title: t('auth.login_required'), icon: 'none' })
    return
  }
  replyPosting.value = true
  try {
    await worldApi.replyToPost(replyTarget.value.id, replyContent.value.trim(), actor.subjectId, actor.subjectType)
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
  mentionedSubjects.value = []
  localImagePath.value = ''
  localVideoPath.value = ''
  videoError.value = ''
}

function onNewPostContentChange(value: string) {
  newPostContent.value = value
}

function onMentionedSubjectsChange(subjects: WorldMentionedSubject[]) {
  mentionedSubjects.value = subjects
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
  const actor = activeWorldSubject.value
  if (!actor?.subjectId) {
    uni.showToast({ title: t('auth.login_required'), icon: 'none' })
    return
  }
  posting.value = true
  try {
    const mediaType: WorldMediaType = localVideoPath.value ? 'VIDEO' : localImagePath.value ? 'IMAGE' : 'TEXT'
    await worldApi.createPost({
      actorSubjectId: actor.subjectId,
      actorSubjectType: actor.subjectType,
      content: newPostContent.value.trim(),
      mediaType,
      imageUrl: localImagePath.value || undefined,
      videoUrl: localVideoPath.value || undefined,
      mentionedSubjects: mentionedSubjects.value,
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
  if (!activeSubjectStore.currentSubject) {
    mentionFriends.value = []
    return
  }
  try {
    mentionFriends.value = await contactApi.listContacts(activeSubjectStore.contactOwnerParams())
  } catch {
    mentionFriends.value = []
  }
}

onShow(async () => {
  worldReady.value = false
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.world.title') })
  await activeSubjectStore.ensureLoaded()
  if (!activeSubjectStore.currentSubject) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  worldReady.value = true
  loadMentionFriends()
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--c-page);
  box-sizing: border-box;
}

.state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-muted);
  font-size: 26rpx;
}
</style>
