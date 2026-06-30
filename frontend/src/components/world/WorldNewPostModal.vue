<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

type MentionFriend = {
  targetSubjectId: number
  targetSubjectType: 'HUMAN' | 'AGENT'
  nickname: string
  avatarUrl?: string
}

type MentionSubject = {
  subjectId: number
  subjectType: 'HUMAN' | 'AGENT'
}

const props = defineProps<{
  visible: boolean
  content: string
  friends: MentionFriend[]
  localImagePath: string
  localVideoPath: string
  videoError: string
  mediaTip: string
  canSubmit: boolean
  posting: boolean
  placeholder: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update:content', value: string): void
  (e: 'update:mentionedSubjects', value: MentionSubject[]): void
  (e: 'pick-image'): void
  (e: 'pick-video'): void
  (e: 'clear-media'): void
  (e: 'submit'): void
}>()

function resolveInputValue(payload: any): string {
  if (typeof payload === 'string') return payload
  if (typeof payload?.detail?.value === 'string') return payload.detail.value
  if (typeof payload?.value === 'string') return payload.value
  return ''
}

function resolveCursor(payload: any): number {
  if (typeof payload?.detail?.cursor === 'number') return payload.detail.cursor
  if (typeof payload?.cursor === 'number') return payload.cursor
  return -1
}

const cursorPos = ref(-1)
const localContent = ref('')
const inputFocused = ref(false)
const keepPanelOpen = ref(false)
const inputCursor = computed(() => (cursorPos.value >= 0 ? cursorPos.value : undefined))

watch(
  () => props.content,
  (v) => {
    const next = String(v || '')
    if (next !== localContent.value) {
      localContent.value = next
    }
  },
  { immediate: true },
)

watch(
  () => props.visible,
  (visible) => {
    if (visible) return
    inputFocused.value = false
    keepPanelOpen.value = false
    cursorPos.value = -1
  },
)

function normalizeDeleteMention(prev: string, next: string, cursor: number): string {
  if (!prev) return next
  // 兼容不同平台 cursor 回传：优先使用 next 长度作为回退
  const safeCursor = cursor >= 0 ? cursor : next.length
  // 仅在回删场景（长度减少 1）下触发 token 删除
  if (prev.length !== next.length + 1) return next
  if (cursor < 0) return next
  const mentionRe = /@[A-Za-z0-9_\u4E00-\u9FFF-]+/g
  let m: RegExpExecArray | null
  while ((m = mentionRe.exec(prev)) !== null) {
    const start = m.index
    const end = start + m[0].length
    // 微博/Twitter 习惯：回删发生在 mention 内或 mention 尾部，整段删除（并吞掉后置空格）
    if ((safeCursor >= start && safeCursor < end) || safeCursor === end) {
      const tailHasSpace = prev.charAt(end) === ' '
      return `${prev.slice(0, start)}${prev.slice(tailHasSpace ? end + 1 : end)}`
    }
  }
  return next
}

function handleContentInput(payload: any) {
  const nextRaw = resolveInputValue(payload)
  const nextCursor = resolveCursor(payload)
  const prev = localContent.value || ''
  const next = normalizeDeleteMention(prev, nextRaw, nextCursor)
  localContent.value = next
  if (nextCursor >= 0) {
    cursorPos.value = nextCursor
  }
  emit('update:content', next)
}

const mentionState = computed(() => {
  const content = localContent.value || ''
  const cursor = cursorPos.value >= 0 ? cursorPos.value : content.length
  const left = content.slice(0, cursor)
  const m = left.match(/(?:^|\s)@([^\s@#]*)$/)
  if (!m) return { active: false, keyword: '', start: -1, end: -1 }
  const keyword = (m[1] || '').trim().toLowerCase()
  const start = left.lastIndexOf('@')
  return { active: true, keyword, start, end: cursor }
})

const mentionCandidates = computed(() => {
  if (!mentionState.value.active) return []
  const keyword = mentionState.value.keyword
  const all = props.friends || []
  if (!keyword) return all.slice(0, 8)
  return all
    .filter((f) => (f.nickname || '').toLowerCase().includes(keyword))
    .slice(0, 8)
})

function handleInputFocus(payload: any) {
  inputFocused.value = true
  const cursor = resolveCursor(payload)
  if (cursor >= 0) {
    cursorPos.value = cursor
  }
}

function handleInputBlur() {
  inputFocused.value = false
  setTimeout(() => {
    keepPanelOpen.value = false
  }, 260)
}

function findMentionRange(content: string, cursor: number): { start: number; end: number } | null {
  const safeCursor = cursor >= 0 ? cursor : content.length
  const left = content.slice(0, safeCursor)
  const matched = left.match(/(?:^|\s)@([^\s@#]*)$/)
  if (!matched) return null
  const start = left.lastIndexOf('@')
  if (start < 0) return null
  return { start, end: safeCursor }
}

function preparePickMention() {
  keepPanelOpen.value = true
}

function selectMention(friend: MentionFriend) {
  preparePickMention()
  insertMention(friend)
}

function insertMention(friend: MentionFriend) {
  const content = localContent.value || ''
  const state = mentionState.value
  const range = state.active && state.start >= 0
    ? { start: state.start, end: state.end }
    : findMentionRange(content, cursorPos.value)
  if (!range) return

  const nickname = String(friend.nickname || '').trim()
  if (!nickname) return

  const beforeRaw = content.slice(0, range.start)
  const afterRaw = content.slice(range.end)
  const before = beforeRaw.length > 0 && !/\s$/.test(beforeRaw) ? `${beforeRaw} ` : beforeRaw
  const after = afterRaw.replace(/^\s+/, '')
  const mentionToken = `@${nickname}`
  const inserted = `${before}${mentionToken}${after ? ` ${after}` : ' '}`
  const nextCursor = before.length + mentionToken.length + 1

  localContent.value = inserted
  cursorPos.value = nextCursor
  emit('update:content', inserted)
  keepPanelOpen.value = false
  nextTick(() => {
    inputFocused.value = true
  })
}

function normalizeSubjectType(value?: string): 'HUMAN' | 'AGENT' {
  return String(value || 'HUMAN').toUpperCase() === 'AGENT' ? 'AGENT' : 'HUMAN'
}

function collectMentionedSubjects(content: string, friends: MentionFriend[]): MentionSubject[] {
  if (!content) return []
  if (!friends?.length) return []
  const nameToSubject = new Map<string, MentionSubject>()
  friends.forEach((f) => {
    if (!f?.nickname) return
    nameToSubject.set(f.nickname, {
      subjectId: f.targetSubjectId,
      subjectType: normalizeSubjectType(f.targetSubjectType),
    })
  })
  const out: MentionSubject[] = []
  const seen = new Set<string>()
  const re = /@([A-Za-z0-9_\u4E00-\u9FFF-]+)/g
  let m: RegExpExecArray | null
  while ((m = re.exec(content)) !== null) {
    const subject = nameToSubject.get(m[1] || '')
    if (!subject) continue
    const key = `${subject.subjectType}:${subject.subjectId}`
    if (seen.has(key)) continue
    seen.add(key)
    out.push(subject)
  }
  return out
}

watch(
  () => [localContent.value, props.friends] as const,
  ([content, friends]) => {
    emit('update:mentionedSubjects', collectMentionedSubjects(content || '', friends || []))
  },
  { immediate: true, deep: true },
)

/** 与 WorldPostCard 一致：@ 与 #话题 均按话题色高亮 */
const contentPreviewParts = computed(() => {
  const content = localContent.value || ''
  const re = /(@[A-Za-z0-9_\u4E00-\u9FFF-]+|#[A-Za-z0-9_\u4E00-\u9FFF-]+)/g
  const out: Array<{ text: string; highlight: boolean }> = []
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(content)) !== null) {
    if (m.index > last) out.push({ text: content.slice(last, m.index), highlight: false })
    out.push({ text: m[0], highlight: true })
    last = m.index + m[0].length
  }
  if (last < content.length) out.push({ text: content.slice(last), highlight: false })
  return out
})

const showMentionPanel = computed(() => {
  return mentionState.value.active && mentionCandidates.value.length > 0 && (inputFocused.value || keepPanelOpen.value)
})
</script>

<template>
  <u-popup
    :show="props.visible"
    mode="bottom"
    round="16"
    bg-color="#ffffff"
    :safe-area-inset-bottom="true"
    @close="emit('close')"
  >
    <view class="modal-sheet">
      <view class="modal-head">
        <text class="modal-title">{{ $t('page.world.new_post_title') }}</text>
        <view class="modal-close-icon" @click="emit('close')">
          <u-icon name="close" size="16" color="#030213" />
        </view>
      </view>
      <view class="modal-body">
        <textarea
          :value="localContent"
          class="post-textarea"
          :placeholder="props.placeholder"
          :maxlength="8000"
          :cursor="inputCursor"
          :focus="inputFocused"
          @focus="handleInputFocus"
          @blur="handleInputBlur"
          @input="handleContentInput"
        />
        <view v-if="localContent" class="mention-preview">
          <text
            v-for="(part, idx) in contentPreviewParts"
            :key="idx"
            :class="part.highlight ? 'preview-highlight' : 'preview-plain'"
          >{{ part.text }}</text>
        </view>
        <view v-if="showMentionPanel" class="mention-panel">
          <view
            v-for="f in mentionCandidates"
            :key="`${f.targetSubjectType}:${f.targetSubjectId}`"
            class="mention-item"
            @touchstart.stop="preparePickMention"
            @mousedown.stop="preparePickMention"
            @touchend.stop.prevent="selectMention(f)"
            @tap.stop.prevent="selectMention(f)"
            @click.stop.prevent="selectMention(f)"
          >
            <image v-if="f.avatarUrl" class="mention-avatar-img" :src="f.avatarUrl" mode="aspectFill" />
            <view v-else class="mention-avatar">{{ (f.nickname || '?').slice(0, 1) }}</view>
            <text class="mention-name">@{{ f.nickname }}</text>
          </view>
        </view>
        <text class="modal-label">{{ $t('page.world.add_media') }}</text>
        <view class="media-picker-grid">
          <u-button class="media-pick-card" :class="{ active: props.localImagePath }" shape="circle" color="#ffffff" @click="emit('pick-image')">
            <view class="media-pick-icon">🖼</view>
            <text class="media-pick-title">{{ $t('page.world.pick_image') }}</text>
          </u-button>
          <u-button class="media-pick-card" :class="{ active: props.localVideoPath }" shape="circle" color="#ffffff" @click="emit('pick-video')">
            <view class="media-pick-icon">🎬</view>
            <text class="media-pick-title">{{ $t('page.world.pick_video') }}</text>
          </u-button>
        </view>
        <u-button v-if="props.localImagePath || props.localVideoPath" class="clear-media-btn" shape="circle" color="#fff2f2" @click="emit('clear-media')">
          {{ $t('page.world.clear_media') }}
        </u-button>
        <view v-if="props.localImagePath" class="media-preview">
          <image class="preview-img" :src="props.localImagePath" mode="aspectFill" />
        </view>
        <view v-if="props.localVideoPath" class="media-preview">
          <video class="preview-vid" :src="props.localVideoPath" controls />
        </view>
        <view v-if="props.videoError" class="media-error">
          <text>{{ props.videoError }}</text>
        </view>
        <view class="media-tip">
          <text>{{ props.mediaTip }}</text>
        </view>
      </view>
      <view class="modal-foot">
        <u-button class="submit-btn" shape="circle" color="#030213" :disabled="!props.canSubmit || props.posting" @click="emit('submit')">
          {{ props.posting ? $t('page.world.posting') : $t('page.world.publish') }}
        </u-button>
      </view>
    </view>
  </u-popup>
</template>

<style scoped>
@import "@/styles/tokens.css";

.modal-sheet { width: 100%; min-height: 74vh; background: var(--c-surface); display: flex; flex-direction: column; }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 28rpx; border-bottom: 1rpx solid var(--c-border); }
.modal-title { font-size: 32rpx; font-weight: 700; color: var(--c-ink); }
.modal-close-icon { width: 56rpx; height: 56rpx; border-radius: 14rpx; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.04); border: 1rpx solid rgba(0, 0, 0, 0.06); flex-shrink: 0; margin-left: auto; }
.modal-body { flex: 1; padding: 16rpx 20rpx 20rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.modal-foot { padding: 16rpx 20rpx calc(20rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid var(--c-border); }
.post-textarea {
  width: 100%;
  min-height: 220rpx;
  padding: 20rpx;
  box-sizing: border-box;
  border-radius: 14rpx;
  border: 1rpx solid var(--c-border);
  background: #fff;
  color: var(--c-ink);
  font-size: 28rpx;
  line-height: 1.5;
}
.mention-preview {
  margin-top: 8rpx;
  padding: 10rpx 12rpx;
  border-radius: 12rpx;
  background: rgba(124, 58, 237, 0.07);
  border: 1rpx solid rgba(124, 58, 237, 0.2);
  line-height: 1.5;
}
.preview-plain {
  color: var(--c-muted);
  font-size: 22rpx;
}
.preview-highlight {
  color: var(--c-violet);
  font-size: 22rpx;
  font-weight: 600;
}
.mention-panel {
  position: relative;
  z-index: 99;
  margin-top: 10rpx;
  border: 1rpx solid var(--c-border);
  background: #fff;
  border-radius: 14rpx;
  max-height: 260rpx;
  overflow: hidden;
  pointer-events: auto;
}
.mention-item {
  width: 100%;
  box-sizing: border-box;
  background: #fff;
  border-radius: 0;
  text-align: left;
  position: relative;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 14rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.05);
}
.mention-item:active {
  background: rgba(59, 91, 253, 0.08);
}
.mention-item:last-child {
  border-bottom: none;
}
.mention-avatar,
.mention-avatar-img {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.mention-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(3, 2, 19, 0.1);
  color: var(--c-primary);
  font-size: 20rpx;
  font-weight: 700;
}
.mention-name {
  font-size: 24rpx;
  color: var(--c-ink);
}
.modal-label { display: block; margin-top: 12rpx; margin-bottom: 10rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 600; }
.media-picker-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10rpx; }
.media-pick-card { border-radius: 16rpx; border: 1rpx solid rgba(0,0,0,0.1); background: var(--c-surface); min-height: 96rpx; display: flex;  align-items: center; justify-content: center; gap: 8rpx; padding: 8rpx; }
.media-pick-card.active { border-color: var(--c-secondary); background: rgba(91, 103, 241, 0.08); }
.media-pick-icon { width: 44rpx; height: 44rpx; border-radius: 50%; background: rgba(91, 103, 241, 0.14); display: flex; align-items: center; justify-content: center; font-size: 22rpx; }
.media-pick-title { font-size: 22rpx; color: var(--c-ink); font-weight: 600; }
.clear-media-btn { margin-top: 10rpx; border-radius: 12rpx; border: 1rpx solid rgba(239,68,68,0.3); color: var(--c-destructive); font-size: 22rpx; }
.media-preview { margin-top: 10rpx; border-radius: var(--radius-md); overflow: hidden; border: 1rpx solid rgba(0,0,0,0.08); }
.preview-img { width: 100%; height: 180rpx; }
.preview-vid { width: 100%; height: 180rpx; background: #000; }
.media-error { margin-top: 10rpx; padding: 10rpx 14rpx; border-radius: var(--radius-md); background: rgba(220,38,38,0.08); border: 1rpx solid rgba(220,38,38,0.2); font-size: 22rpx; color: var(--c-destructive); }
.media-tip { margin-top: 10rpx; padding: 10rpx 14rpx; border-radius: var(--radius-md); background: rgba(59,130,246,0.06); border: 1rpx solid rgba(59,130,246,0.15); font-size: 20rpx; color: var(--c-muted); line-height: 1.4; }
.submit-btn { width: 100%; font-size: 26rpx; font-weight: 600; box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.24); }
</style>
