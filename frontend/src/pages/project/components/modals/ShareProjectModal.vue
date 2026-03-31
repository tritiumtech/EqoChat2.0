<template>
  <view v-if="open" class="modal-mask modal-mask-center" @click="emit('close')">
    <view class="modal-card modal-card-md" @click.stop>
      <view class="modal-head">
        <text class="sheet-title">{{ t('page.project.modals.share.title') }}</text>
        <text class="sheet-close" @click="emit('close')">✕</text>
      </view>

      <view class="modal-body modal-body-spaced">
        <view class="share-top-card">
          <view class="share-project-top">
            <view class="share-project-avatar" :style="{ background: projectDetail?.color || '#7C3AED' }">
              <view class="share-avatar-overlay" />
              <text class="share-project-avatar-text">{{ getMemberInitial(projectDetail?.name) }}</text>
            </view>
            <view class="share-project-main">
              <text class="share-project-name">{{ projectDetail?.name || '-' }}</text>
              <text class="share-project-id">{{ t('page.project.modals.share.project_id_label', { id: projectDetail?.id }) }}</text>
            </view>
          </view>
        </view>

        <view class="share-box">
          <text class="share-label">{{ t('page.project.modals.share.link_label') }}</text>
          <text class="share-url">{{ shareUrl || '-' }}</text>
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.share.message_label') }}</text>
          <textarea
            class="field-textarea"
            :value="shareMessage"
            :placeholder="t('page.project.modals.share.message_placeholder')"
            @input="onTextareaInput"
          ></textarea>
        </view>
      </view>

      <view class="modal-foot">
        <button class="btn-secondary btn-modal" @click="emit('close')">{{ t('page.project.modals.share.close') }}</button>
        <button class="btn-primary btn-modal" :disabled="!shareUrl" @click="emit('copy')">
          {{ t('page.project.modals.share.share_project') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProjectDetail } from '@/api/modules/project'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  open: boolean
  projectDetail: ProjectDetail | null
  shareUrl: string
  shareMessage: string
  getMemberInitial: (name?: string) => string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'copy'): void
  (e: 'update:shareMessage', value: string): void
}>()

const { t } = useI18n({ useScope: 'global' })
const { open, projectDetail, shareUrl, shareMessage } = toRefs(props)

function resolveTextareaValue(payload: any): string {
  return typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

function onTextareaInput(payload: any) {
  emit('update:shareMessage', resolveTextareaValue(payload))
}

const getMemberInitial = props.getMemberInitial
</script>

<style scoped src="../../project.styles.css"></style>

