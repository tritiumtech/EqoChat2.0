<script setup lang="ts">
import { computed } from 'vue'
import type { WorldPost } from '@/api/modules/world'

const props = defineProps<{
  visible: boolean
  post: WorldPost | null
  copied: boolean
  shareNote: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'channel', channel: string): void
  (e: 'copy'): void
  (e: 'update:shareNote', value: string): void
}>()

const channels = computed(() => [
  // 优先使用 uview-plus 内置 u-icon，保证图标风格统一
  { key: 'twitter', labelKey: 'page.world.twitter', cls: 'tw', iconName: 'twitter' },
  { key: 'linkedin', labelKey: 'page.world.linkedin', cls: 'in', iconText: 'in' },
  { key: 'facebook', labelKey: 'page.world.facebook', cls: 'fb', iconName: 'facebook' },
  // whatsapp / telegram 在内置图标库中没有精准品牌图标，这里用通用电话/分享图标占位统一风格
  { key: 'whatsapp', labelKey: 'page.world.whatsapp', cls: 'wa', iconName: 'phone-fill' },
  { key: 'telegram', labelKey: 'page.world.telegram', cls: 'tg', iconName: 'share' },
  { key: 'email', labelKey: 'page.world.email', cls: 'mail', iconName: 'email-fill' },
])

function isHttpUrlAvatar(avatar: string) {
  return /^https?:\/\//i.test(avatar || '') || avatar.startsWith('/api/')
}

function avatarStyle(avatar: string) {
  const c = /^#[0-9A-Fa-f]{3,6}$/.test(avatar || '') ? avatar : '#7c3aed'
  return { background: `linear-gradient(135deg, ${c}f0, ${c}e0)` }
}
</script>

<template>
  <u-popup
    :show="props.visible && !!props.post"
    mode="bottom"
    round="16"
    bg-color="#ffffff"
    :safe-area-inset-bottom="true"
    @close="emit('close')"
  >
    <view v-if="props.post" class="modal-sheet share-sheet">
      <view class="modal-head">
        <text class="modal-title">{{ $t('page.world.share_modal_title') }}</text>
        <view class="modal-close-icon" @click="emit('close')">
          <u-icon name="close" size="16" color="#030213" />
        </view>
      </view>
      <view class="modal-body">
        <view class="share-ref">
          <view class="ref-row">
            <image v-if="isHttpUrlAvatar(props.post.author.avatar)" class="ref-avatar-img" :src="props.post.author.avatar" mode="aspectFill" />
            <view v-else class="ref-avatar" :style="avatarStyle(props.post.author.avatar)">
              <text class="ref-avatar-letter">{{ props.post.author.name.slice(0, 1) }}</text>
            </view>
            <view class="ref-meta">
              <view class="ref-name-row">
                <text class="ref-name">{{ props.post.author.name }}</text>
                <text v-if="props.post.author.ai" class="ref-ai-chip">{{ $t('page.world.ai_agent') }}</text>
              </view>
              <text class="ref-time">{{ props.post.timestamp }}</text>
            </view>
          </view>
          <text class="share-ref-text">{{ props.post.content }}</text>
        </view>

        <text class="modal-label">{{ $t('page.world.share_to') }}</text>
        <u-grid :col="3" :border="false" class="share-grid">
          <u-grid-item v-for="item in channels" :key="item.key" @click="emit('channel', item.key)">
            <view class="share-cell">
              <view class="share-emoji" :class="item.cls">
                <u-icon
                  v-if="item.iconName"
                  :name="item.iconName"
                  :size="24"
                  color="#ffffff"
                />
                <text v-else class="share-icon-text">{{ item.iconText }}</text>
              </view>
              <text class="share-label">{{ $t(item.labelKey) }}</text>
            </view>
          </u-grid-item>
        </u-grid>

        <u-button class="copy-btn" shape="circle" color="#ffffff" @click="emit('copy')">
          {{ props.copied ? $t('page.world.link_copied') : $t('page.world.copy_link') }}
        </u-button>

        <text class="modal-label">{{ $t('page.world.share_message_placeholder') }}</text>
        <u-textarea
          :value="props.shareNote"
          class="share-note"
          height="88"
          :maxlength="500"
          placeholder=""
          @input="emit('update:shareNote', ($event as any).detail.value)"
        />
      </view>
    </view>
  </u-popup>
</template>

<style scoped>
@import "@/styles/tokens.css";

.modal-sheet { width: 100%; min-height: 72vh; background: var(--c-surface); display: flex; flex-direction: column; }
.share-sheet { min-height: 74vh; }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 28rpx; border-bottom: 1rpx solid var(--c-border); }
.modal-title { font-size: 32rpx; font-weight: 700; color: var(--c-ink); }
.modal-close-icon { width: 56rpx; height: 56rpx; border-radius: 14rpx; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.04); border: 1rpx solid rgba(0,0,0,0.06); flex-shrink: 0; margin-left: auto; }
.modal-body { flex: 1; padding: 16rpx 20rpx 20rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.share-ref { margin-bottom: 8rpx; border-radius: 18rpx; background: rgba(3,2,19,0.03); border: 1rpx solid rgba(0,0,0,0.08); padding: 14rpx; }
.ref-row { display: flex; align-items: flex-start; gap: 10rpx; margin-bottom: 6rpx; }
.ref-avatar, .ref-avatar-img { width: 48rpx; height: 48rpx; border-radius: 50%; flex-shrink: 0; }
.ref-avatar { display: flex; align-items: center; justify-content: center; }
.ref-avatar-letter { color: #fff; font-size: 18rpx; font-weight: 700; }
.ref-meta { flex: 1; min-width: 0; }
.ref-name-row { display: flex; align-items: center; gap: 8rpx; }
.ref-name { font-size: 22rpx; font-weight: 600; color: var(--c-ink); }
.ref-ai-chip { padding: 2rpx 8rpx; border-radius: 8rpx; font-size: 18rpx; color: var(--c-violet); border: 1rpx solid rgba(124,58,237,0.22); background: rgba(124,58,237,0.1); }
.ref-time { font-size: 18rpx; color: var(--c-muted); display: block; margin-top: 2rpx; }
.share-ref-text { font-size: 22rpx; color: var(--c-ink); line-height: 1.45; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.modal-label { display: block; margin-top: 12rpx; margin-bottom: 10rpx; font-size: 22rpx; color: var(--c-muted); font-weight: 600; }
.share-grid { margin: 0 -8rpx; }
.share-cell { width: 100%; display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 10rpx 2rpx; border-radius: 16rpx; border: 1rpx solid rgba(0,0,0,0.08); background: rgba(255,255,255,0.95); font-size: 20rpx; box-sizing: border-box; }
.share-emoji { width: 52rpx; height: 52rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22rpx; font-weight: 700; color: #fff; }
.share-emoji.tw { background: #1da1f2; }
.share-emoji.in { background: #0a66c2; font-size: 22rpx; }
.share-emoji.fb { background: #1877f2; }
.share-emoji.wa { background: #25d366; }
.share-emoji.tg { background: #0088cc; }
.share-emoji.mail { background: var(--c-primary); }
.share-label { color: var(--c-ink); text-align: center; font-size: 20rpx; line-height: 1.2; }
.share-icon-text { font-size: 20rpx; font-weight: 700; color: #fff; }
.copy-btn { width: 100%; margin-top: 12rpx; border: 1rpx solid rgba(0,0,0,0.1); background: rgba(3,2,19,0.05); font-size: 24rpx; color: var(--c-ink); font-weight: 600; }
.share-note { margin-top: 4rpx; }
</style>
