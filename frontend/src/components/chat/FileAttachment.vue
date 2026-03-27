<template>
  <view
    class="attachment"
    :class="{ 'with-download': !!downloadUrl }"
  >
    <view class="file-icon" :style="iconGradientStyle">
      <text class="file-icon-glyph">📄</text>
    </view>
    <view class="file-meta">
      <text class="file-name">{{ fileName || t('common.attachment') }}</text>
      <text class="file-sub">
        <text>{{ fileSize || '-' }}</text>
        <text class="dot">·</text>
        <text>{{ fileType || '-' }}</text>
      </text>
    </view>
    <button
      v-if="downloadUrl"
      class="download-btn"
      @click.stop="handleDownload"
    >
      <text class="download-glyph">⬇</text>
    </button>
    <button
      v-else
      class="download-btn disabled"
      disabled
      @click.stop="handleNoDownload"
    >
      <text class="download-glyph">⬇</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  fileName: string
  fileSize: string
  fileType: string
  downloadUrl?: string
}>()

const { t } = useI18n()
const fileTypeLower = computed(() => (props.fileType || '').toLowerCase())

const iconGradientStyle = computed(() => {
  // 粗略对齐 Figma：根据文件扩展名/类型决定渐变色
  const mt = fileTypeLower.value
  const map: Array<[RegExp, string]> = [
    [/pdf/, 'linear-gradient(135deg, rgba(239,68,68,0.95), rgba(225,29,72,0.95))'],
    [/doc|word/, 'linear-gradient(135deg, rgba(59,130,246,0.95), rgba(37,99,235,0.95))'],
    [/xls|excel/, 'linear-gradient(135deg, rgba(16,185,129,0.95), rgba(5,150,105,0.95))'],
    [/fig|figma/, 'linear-gradient(135deg, rgba(124,58,237,0.95), rgba(79,70,229,0.95))'],
    [/image|png|jpg|jpeg|gif|webp/, 'linear-gradient(135deg, rgba(14,165,233,0.95), rgba(59,130,246,0.95))'],
  ]
  const hit = map.find(([re]) => re.test(mt))
  return { backgroundImage: hit?.[1] ?? 'linear-gradient(135deg, rgba(148,163,184,0.95), rgba(100,116,139,0.95))' }
})

const handleNoDownload = () => {
  // Phase 2 才会提供真实下载能力，这里给出静默 UX
}

const handleDownload = () => {
  if (!props.downloadUrl) return
  // uni-app 在各端对外链行为不同：优先使用 openDocument / downloadFile（保持后续可替换）
  uni.showToast({ title: t('toast.download_coming_soon'), icon: 'none' })
}
</script>

<style scoped>
@import "@/styles/tokens.css";

.attachment {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 14rpx 16rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  background: rgba(246, 242, 238, 0.6);
  max-width: 100%;
}

.file-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-icon-glyph {
  color: #fff;
  font-size: 26rpx;
}

.file-meta {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-sub {
  display: block;
  font-size: 20rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dot {
  margin: 0 8rpx;
}

.download-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.75);
}

.download-btn.disabled {
  opacity: 0.6;
}

.download-glyph {
  font-size: 28rpx;
  color: var(--c-primary);
}
</style>

