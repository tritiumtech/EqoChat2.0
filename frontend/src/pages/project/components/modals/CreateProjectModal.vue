<template>
  <view v-if="open" class="modal-mask modal-mask-center" @click="emit('close')">
    <view class="modal-card modal-card-md" @click.stop>
      <view class="modal-head">
        <text class="sheet-title">{{ t('page.project.modals.create.title') }}</text>
        <text class="sheet-close" @click="emit('close')">✕</text>
      </view>

      <view class="modal-body modal-body-spaced">
        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create.project_name') }}</text>
          <input
            class="field-input"
            :value="createName"
            :placeholder="t('page.project.placeholders.project_name')"
            @input="onNameInput"
          />
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create.bid') }}</text>
          <input
            class="field-input"
            type="number"
            :value="createBidStr"
            :placeholder="t('page.project.placeholders.bid')"
            @input="onBidInput"
          />
        </view>

        <view v-if="Number(createBidStr) >= 100" class="hint">{{ depositHintText }}</view>
        <view v-else-if="Number(createBidStr) > 0" class="hint">{{ t('page.project.modals.create.no_deposit_hint') }}</view>
      </view>

      <view class="modal-foot">
        <Button variant="secondary" size="large" shape="round" @click="emit('close')">
          {{ t('toast.cancel') }}
        </Button>
        <Button variant="primary" size="large" shape="round" :disabled="!canCreateProject" @click="emit('submit')">
          {{ t('page.project.modals.create.confirm') }}
        </Button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from '@/components/Button.vue'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  open: boolean
  createName: string
  createBidStr: string
  canCreateProject: boolean
  depositHintText: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
  (e: 'update:createName', value: string): void
  (e: 'update:createBidStr', value: string): void
}>()

const { t } = useI18n({ useScope: 'global' })
const { open, createName, createBidStr, canCreateProject, depositHintText } = toRefs(props)

function resolveInputValue(payload: any): string {
  return typeof payload?.detail?.value === 'string' ? payload.detail.value : ''
}

function onNameInput(payload: any) {
  emit('update:createName', resolveInputValue(payload))
}

function onBidInput(payload: any) {
  emit('update:createBidStr', resolveInputValue(payload))
}
</script>

<style scoped src="../../project.styles.css"></style>

