<template>
  <view v-if="open" class="drawer-mask" @click="onClose">
    <view class="drawer" @click.stop>
      <view class="drawer-head">
        <view class="drawer-head-left">
          <view class="drawer-icon">▦</view>
          <view class="drawer-titles">
            <text class="drawer-title">{{ t('page.project.drawer.title') }}</text>
            <text class="drawer-sub">{{ t('page.project.drawer.subtitle') }}</text>
          </view>
        </view>
        <text class="sheet-close" @click="onClose">x</text>
      </view>

      <view class="drawer-tabs">
        <Button
          class="tab-btn"
          :class="{ active: sidebarTab === 'tasks' }"
          variant="ghost"
          size="mini"
          @click="onChangeTab('tasks')"
        >{{ t('page.project.tabs.tasks') }}</Button>
        <Button
          class="tab-btn"
          :class="{ active: sidebarTab === 'payments' }"
          variant="ghost"
          size="mini"
          @click="onChangeTab('payments')"
        >{{ t('page.project.tabs.payments') }}</Button>
        <Button
          class="tab-btn"
          :class="{ active: sidebarTab === 'files' }"
          variant="ghost"
          size="mini"
          @click="onChangeTab('files')"
        >{{ t('page.project.tabs.files') }}</Button>
      </view>

      <scroll-view class="drawer-body" scroll-y>
        <view v-if="sidebarTab === 'tasks'">
          <view v-if="sidebarTasks.length === 0" class="sheet-empty">{{ t('page.project.empty_tasks') }}</view>
          <view v-for="task in sidebarTasks" :key="task.id" class="task-card">
            <view class="task-icon-wrap" :class="taskIconClass(task.status)">
              <text class="task-icon">{{ taskIconGlyph(task.status) }}</text>
            </view>
            <view class="task-main">
              <view class="task-title-row">
                <text class="task-title">{{ task.title }}</text>
                <view class="priority-pill" :class="priorityPillClass(task.priority)">
                  <text class="priority-text">{{ taskPriorityLabel(task.priority) }}</text>
                </view>
              </view>
              <view class="task-meta-row">
                <text class="task-meta-item">{{ task.assigneeDisplayName }}</text>
                <text class="task-meta-sep">·</text>
                <text class="task-meta-item">{{ subjectTypeLabel(task.assigneeSubjectType) }}</text>
                <text class="task-meta-sep">·</text>
                <text class="task-meta-date">{{ task.deadline || '-' }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-else-if="sidebarTab === 'payments'">
          <view v-if="sidebarPayments.length === 0" class="sheet-empty">{{ t('page.project.empty_payments') }}</view>
          <view v-for="payment in sidebarPayments" :key="payment.id" class="payment-card">
            <view class="payment-icon-wrap">
              <text class="payment-icon">$</text>
            </view>
            <view class="payment-main">
              <text class="payment-title">{{ payment.recipientDisplayName }}</text>
              <view class="payment-amount-row">
                <text class="payment-amount">{{ formatMoney(payment.amount) }}</text>
                <view class="payment-pill" :class="paymentPillClass(payment.status)">
                  <text class="payment-pill-text">{{ paymentStatusLabel(payment.status) }}</text>
                </view>
              </view>
              <text class="payment-meta">
                {{ subjectTypeLabel(payment.recipientSubjectType) }} · {{ payment.date || '' }}
              </text>
            </view>
          </view>
        </view>

        <view v-else>
          <view v-if="sidebarFiles.length === 0" class="sheet-empty">{{ t('page.project.empty_files') }}</view>
          <view v-for="file in sidebarFiles" :key="file.id" class="file-card">
            <view class="file-icon-wrap">
              <text class="file-icon">📄</text>
            </view>
            <view class="file-main">
              <text class="file-name">{{ file.name }}</text>
              <text class="file-sub">
                {{ file.type }} · {{ file.size || '-' }}
              </text>
              <text class="file-meta">
                {{ subjectTypeLabel(file.uploaderSubjectType) }} · {{ file.date || '' }}
              </text>
              <button v-if="file.downloadUrl" class="link-btn" @click="openProjectFile(file.downloadUrl)">
                {{ t('page.project.download_open') }}
              </button>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import Button from '@/components/Button.vue'
import type { ProjectFile, ProjectPayment, ProjectTask } from '@/api/modules/project'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

type SidebarTab = 'tasks' | 'payments' | 'files'

type Props = {
  open: boolean
  sidebarTab: SidebarTab
  sidebarTasks: ProjectTask[]
  sidebarPayments: ProjectPayment[]
  sidebarFiles: ProjectFile[]
  onClose: () => void
  onChangeTab: (tab: SidebarTab) => void
  taskIconClass: (status: any) => string
  taskIconGlyph: (status: any) => string
  priorityPillClass: (priority: any) => string
  taskPriorityLabel: (priority: any) => string
  paymentPillClass: (status: any) => string
  paymentStatusLabel: (status: any) => string
  formatMoney: (amount: number) => string
  openProjectFile: (downloadUrl?: string) => void
}

const props = defineProps<Props>()
const { t } = useI18n({ useScope: 'global' })

const { open, sidebarTab, sidebarTasks, sidebarPayments, sidebarFiles } = toRefs(props)

const onClose = props.onClose
const onChangeTab = props.onChangeTab
const taskIconClass = props.taskIconClass
const taskIconGlyph = props.taskIconGlyph
const priorityPillClass = props.priorityPillClass
const taskPriorityLabel = props.taskPriorityLabel
const paymentPillClass = props.paymentPillClass
const paymentStatusLabel = props.paymentStatusLabel
const formatMoney = props.formatMoney
const openProjectFile = props.openProjectFile

const subjectTypeLabel = (type: unknown) => {
  if (type === 'AGENT') return t('page.project.member_type_ai')
  if (type === 'HUMAN') return t('page.project.member_type_human')
  return String(type || '').toUpperCase()
}
</script>

<style scoped src="../project.styles.css"></style>
