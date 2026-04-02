<template>
  <!-- 图片类型：直接显示预览 -->
  <view
    v-if="isImage"
    class="image-attachment"
    @click.stop="handlePreviewImage"
  >
    <image
      class="image-preview"
      :src="downloadUrl"
      mode="aspectFill"
      @error="handleImageError"
    />
    <view class="image-overlay">
      <text class="image-name">{{ fileName || t('common.attachment') }}</text>
      <text class="image-size">{{ fileSize }}</text>
    </view>
  </view>
  
  <!-- 其他文件类型：显示下载按钮 -->
  <view
    v-else
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
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  fileName: string
  fileSize: string
  fileType: string
  downloadUrl?: string
}>()

const { t } = useI18n({ useScope: 'global' })
const fileTypeLower = computed(() => (props.fileType || '').toLowerCase())
const imageLoadError = ref(false)

// 判断是否为图片类型
const isImage = computed(() => {
  if (imageLoadError.value) return false
  const mt = fileTypeLower.value
  const fileName = (props.fileName || '').toLowerCase()
  return /^image\//.test(mt) || /\.(png|jpg|jpeg|gif|webp|bmp|svg)$/i.test(fileName)
})

const iconGradientStyle = computed(() => {
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

const handleImageError = () => {
  imageLoadError.value = true
}

// 图片预览：点击放大
const handlePreviewImage = () => {
  if (!props.downloadUrl) return
  
  // #ifdef H5
  if (typeof window !== 'undefined') {
    window.open(props.downloadUrl, '_blank')
  }
  // #endif
  
  // #ifndef H5
  uni.previewImage({
    urls: [props.downloadUrl],
    current: props.downloadUrl,
  })
  // #endif
}

const handleNoDownload = () => {
  // Phase 2 才会提供真实下载能力，这里给出静默 UX
}

const handleDownload = () => {
  if (!props.downloadUrl) return

  // #ifdef H5
  if (typeof window !== 'undefined' && window.open) {
    window.open(props.downloadUrl, '_blank')
  }
  // #endif

  // #ifndef H5
  uni.downloadFile({
    url: props.downloadUrl,
    success: (res) => {
      if (res.statusCode === 200 && res.tempFilePath) {
        uni.openDocument({
          filePath: res.tempFilePath,
          fileType: props.fileType,
        })
        return
      }
      uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    },
    fail: () => {
      uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    },
  })
  // #endif
}
</script>

<style scoped>
@import "@/styles/tokens.css";

/* 图片附件样式 */
.image-attachment {
  margin-top: 10rpx;
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  max-width: 520rpx;
  background: rgba(255, 255, 255, 0.6);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
}

.image-preview {
  width: 520rpx;
  height: 280rpx;
  display: block;
}

.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10rpx 14rpx;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.2), transparent);
}

.image-name {
  font-size: 22rpx;
  font-weight: 600;
  color: #fff;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-size {
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.85);
  display: block;
  margin-top: 4rpx;
}

/* 其他文件附件样式 */
.attachment {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 14rpx 16rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.6);
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

