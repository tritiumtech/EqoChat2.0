<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { WorldPost } from '@/api/modules/world'

const props = defineProps<{
  visible: boolean
  post: WorldPost | null
  content: string
  canSubmit: boolean
  posting: boolean
  placeholder: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'update:content', value: string): void
  (e: 'submit'): void
}>()

const localContent = ref('')

watch(
  () => props.content,
  (v) => {
    const next = String(v || '')
    if (next !== localContent.value) localContent.value = next
  },
  { immediate: true },
)

watch(
  () => props.visible,
  (v) => {
    if (v) return
    localContent.value = ''
  },
)

function resolveTextareaValue(payload: any): string {
  return typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

const canShowContext = computed(() => !!props.post?.content)

function onTextareaInput(payload: any) {
  const next = resolveTextareaValue(payload)
  localContent.value = next
  emit('update:content', next)
}
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
        <text class="modal-title">{{ $t('page.world.reply') }}</text>
        <view class="modal-close-icon" @click="emit('close')">
          <u-icon name="close" size="16" color="#030213" />
        </view>
      </view>

      <view class="modal-body">
        <view v-if="props.post && canShowContext" class="reply-context">
          <text class="reply-context-title">{{ props.post.author.name }}</text>
          <text class="reply-context-content">{{ props.post.content }}</text>
        </view>

        <textarea
          :value="localContent"
          class="post-textarea"
          :placeholder="props.placeholder"
          :maxlength="8000"
          @input="onTextareaInput"
        />
      </view>

      <view class="modal-foot">
        <u-button
          class="submit-btn"
          shape="circle"
          color="#030213"
          :disabled="!props.canSubmit || props.posting"
          @click="emit('submit')"
        >
          {{ props.posting ? $t('page.world.posting') : $t('page.world.publish') }}
        </u-button>
      </view>
    </view>
  </u-popup>
</template>

<style scoped>
@import "@/styles/tokens.css";

.modal-sheet {
  width: 100%;
  min-height: 52vh;
  background: var(--c-surface);
  display: flex;
  flex-direction: column;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid var(--c-border);
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.modal-close-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.04);
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  margin-left: auto;
}

.modal-body {
  flex: 1;
  padding: 16rpx 20rpx 20rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.modal-foot {
  padding: 16rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid var(--c-border);
}

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

.reply-context {
  border-radius: var(--radius-md);
  background: rgba(124, 58, 237, 0.07);
  border: 1rpx solid rgba(124, 58, 237, 0.2);
  padding: 16rpx 18rpx;
  margin-bottom: 12rpx;
}

.reply-context-title {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: var(--c-ink);
  margin-bottom: 6rpx;
}

.reply-context-content {
  display: block;
  font-size: 22rpx;
  color: var(--c-muted);
  line-height: 1.45;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.submit-btn {
  width: 100%;
  font-size: 26rpx;
  font-weight: 600;
  box-shadow: 0 10rpx 20rpx rgba(3, 2, 19, 0.24);
}
</style>

