<template>
  <view v-if="open" class="modal-mask modal-mask-center" @click="emit('close')">
    <view class="modal-card modal-card-md" @click.stop>
      <view class="modal-head">
        <text class="sheet-title">{{ t('page.project.modals.transfer.title') }}</text>
        <text class="sheet-close" @click="emit('close')">✕</text>
      </view>

      <view class="modal-body modal-body-spaced">
        <view v-if="!member" class="sheet-empty">{{ t('page.project.modals.transfer.no_members') }}</view>

        <view v-else>
          <view class="transfer-member-card">
            <view
              class="transfer-member-avatar"
              :class="{ 'transfer-avatar-agent': isAgentType(member.type) }"
            >
              <text class="transfer-member-avatar-initial">{{ getMemberInitial(member.name) }}</text>
            </view>

            <view class="transfer-member-main">
              <text class="transfer-member-name">{{ member.name }}</text>
              <text class="transfer-member-sub">
                {{ isAgentType(member.type) ? t('page.project.member_type_ai') : t('page.project.member_type_human') }}
              </text>
            </view>
          </view>

          <view class="transfer-info-box">
            <view class="transfer-info-head">
              <text class="transfer-info-icon">👑</text>
              <text class="transfer-info-title">{{ t('page.project.modals.transfer.responsibilities_title') }}</text>
            </view>

            <text class="transfer-info-body">
              {{ tf('page.project.modals.transfer.responsibilities_body', { memberName: member.name }) }}
            </text>

            <view class="transfer-info-list">
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_1') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_2') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_3') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_4') }}</text>
            </view>

            <text class="transfer-info-accept">
              {{ tf('page.project.modals.transfer.must_accept', { memberName: member.name }) }}
            </text>
          </view>
        </view>
      </view>

      <view class="modal-foot">
        <button class="btn-secondary btn-modal" @click="emit('close')">{{ t('toast.cancel') }}</button>
        <button class="btn-primary btn-modal" :disabled="!member" @click="emit('submit')">
          {{ t('page.project.modals.transfer.confirm') }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import type { ProjectMember } from '@/api/modules/project'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

const props = defineProps<{
  open: boolean
  member: ProjectMember | null
  getMemberInitial: (name?: string) => string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit'): void
}>()

const { t, tf } = useI18nWithFormat()
const getMemberInitial = props.getMemberInitial

const { open, member } = toRefs(props)
const isAgentType = (type: unknown) => String(type || '').trim().toLowerCase() === 'agent'
</script>

<style scoped src="../../project.styles.css"></style>
