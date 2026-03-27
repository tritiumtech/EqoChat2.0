<script setup lang="ts">
const props = defineProps<{
  visible: boolean
  content: string
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

function handleContentInput(payload: any) {
  emit('update:content', resolveInputValue(payload))
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
        <text class="modal-title">{{ $t('page.world.new_post_title') }}</text>
        <view class="modal-close-icon" @click="emit('close')">
          <u-icon name="close" size="16" color="#030213" />
        </view>
      </view>
      <view class="modal-body">
        <u-textarea
          :value="props.content"
          class="post-textarea"
          :placeholder="props.placeholder"
          height="100"
          :maxlength="8000"
          border="surround"
          @input="handleContentInput"
          @change="handleContentInput"
        />
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
.post-textarea { width: 100%; }
.modal-label { display: block; margin-top: 12rpx; margin-bottom: 10rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 600; }
.media-picker-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10rpx; }
.media-pick-card { border-radius: 16rpx; border: 1rpx solid rgba(0,0,0,0.1); background: var(--c-surface); min-height: 96rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; padding: 8rpx; }
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
