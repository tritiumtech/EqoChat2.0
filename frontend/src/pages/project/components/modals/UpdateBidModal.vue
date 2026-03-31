<template>
  <view v-if="open" class="modal-mask modal-mask-center" @click="emit('close')">
    <view class="modal-card modal-card-md" @click.stop>
      <view class="modal-head">
        <text class="sheet-title">{{ t('page.project.modals.update_bid.title') }}</text>
        <text class="sheet-close" @click="emit('close')">✕</text>
      </view>

      <view class="modal-body modal-body-spaced">
        <view class="modal-current-bid">
          <text class="modal-current-label">{{ t('page.project.details.current_bid_label') }}</text>
          <text class="modal-current-value">{{ currentBidText }}</text>
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.update_bid.new_bid_label') }}</text>
          <input
            class="field-input"
            type="number"
            :value="updateBidStr"
            :placeholder="t('page.project.placeholders.bid')"
            @input="onBidInput"
          />
        </view>

        <view class="team-approval-box">
          <view class="team-approval-head">
            <text class="team-approval-icon">ⓘ</text>
            <text class="team-approval-title">{{ t('page.project.modals.update_bid.team_approval_title') }}</text>
          </view>
          <text class="team-approval-body">{{ t('page.project.modals.update_bid.team_approval_body') }}</text>
          <text class="team-approval-body team-approval-warning">{{ t('page.project.modals.update_bid.team_approval_warning') }}</text>
          <view class="hint">{{ hintText }}</view>
        </view>
      </view>

      <view class="modal-foot">
        <button class="btn-secondary btn-modal" @click="emit('close')">{{ t('toast.cancel') }}</button>
        <button class="btn-primary btn-modal" :disabled="!canUpdateBid" @click="emit('submit')">
          {{ t('page.project.modals.update_bid.confirm') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  open: boolean
  currentBidText: string
  updateBidStr: string
  canUpdateBid: boolean
  hintText: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
  (e: 'update:updateBidStr', value: string): void
}>()

const { t } = useI18n({ useScope: 'global' })
const { open, currentBidText, updateBidStr, canUpdateBid, hintText } = toRefs(props)

function resolveInputValue(payload: any): string {
  return typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

function onBidInput(payload: any) {
  emit('update:updateBidStr', resolveInputValue(payload))
}
</script>

<style scoped src="../../project.styles.css"></style>

