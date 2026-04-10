<template>
  <view class="detail-wrap">
    <PageHeader
      :title="projectDetail?.name || ''"
      has-back-row
      :back-text="t('page.project.back_to_list')"
      @back-click="onCloseProject"
    >
      <template #title-prefix>
        <view class="color-dot big" :style="{ backgroundColor: projectDetail?.color || '#7C3AED' }" />
      </template>

      <template #back-actions>
        <button class="mini-btn tasks-btn" @click="onOpenSidebar('tasks')">
          <text>{{ t('page.project.tabs.tasks') }}</text>
          <view v-if="pendingTaskCount > 0" class="tasks-badge">
            <text>{{ pendingTaskCount }}</text>
          </view>
        </button>
      </template>

      <template #actions>
        <button class="mini-btn share-btn" @click="onOpenShareModal">
          {{ t('page.project.actions.share') }}
        </button>
      </template>

      <template #subtitle-custom>
        <view class="detail-sub-row">
          <text class="detail-sub">
            {{ tf('page.project.detail_subtitle', {
              humans: projectDetail?.humans || 0,
              agents: projectDetail?.agents || 0,
              deadline: projectDetail?.deadline || '-',
            }) }}
          </text>
        </view>
      </template>
    </PageHeader>

    <view class="content">
      <view class="stats-grid">
        <view class="stat-card">
          <view class="stat-icon stat-icon-earned">💵</view>
          <text class="stat-label">{{ t('page.project.stats.earned') }}</text>
          <text class="stat-value">{{ projectDetail?.stats?.earned || '$0' }}</text>
        </view>
        <view class="stat-card">
          <view class="stat-icon stat-icon-pending">⏳</view>
          <text class="stat-label">{{ t('page.project.stats.pending') }}</text>
          <text class="stat-value">{{ projectDetail?.stats?.pending || '$0' }}</text>
        </view>
        <view class="stat-card">
          <view class="stat-icon stat-icon-tasks">✅</view>
          <text class="stat-label">{{ t('page.project.stats.tasks') }}</text>
          <text class="stat-value">
            {{ projectDetail?.stats?.tasksCompleted || 0 }} / {{ projectDetail?.stats?.tasksTotal || 0 }}
          </text>
        </view>
        <view class="stat-card">
          <view class="stat-icon stat-icon-efficiency">📈</view>
          <text class="stat-label">{{ t('page.project.stats.efficiency') }}</text>
          <text class="stat-value">{{ projectDetail?.stats?.efficiency || '+0%' }}</text>
        </view>
      </view>

      <view class="progress-wrap">
        <view class="progress-top">
          <text class="progress-label">{{ t('page.project.progress') }}</text>
          <text class="progress-val">{{ projectDetail?.progress || 0 }}%</text>
        </view>
        <view class="progress-bar">
          <view
            class="progress-fill"
            :style="{ width: `${projectDetail?.progress || 0}%`, backgroundColor: projectDetail?.color || '#7C3AED' }"
          />
        </view>
      </view>

      <!-- Pending bid update -->
      <view v-if="pendingBidUpdate" class="bid-alert">
        <view class="bid-alert-head">
          <view class="bid-alert-icon">!</view>
          <text class="bid-alert-title">{{ t('page.project.pending_bid_alert.title') }}</text>
        </view>

        <text class="bid-alert-sub">
          {{ tf('page.project.pending_bid_alert.subtitle', {
            newBid: formatBidK(pendingBidUpdate.newBid),
            currentBid: formatBidK(projectDetail?.bid || 0),
          }) }}
        </text>

        <view class="bid-alert-stats">
          <text class="bid-approve">
            ✓ {{ pendingBidUpdate.approvals?.length || 0 }} {{ t('page.project.pending_bid_alert.approved') }}
          </text>
          <text class="bid-sep">•</text>
          <text class="bid-reject">
            ✗ {{ pendingBidUpdate.rejections?.length || 0 }} {{ t('page.project.pending_bid_alert.rejected') }}
          </text>
          <text class="bid-sep">•</text>
          <text class="bid-pending">
            {{ pendingBidUpdate.pending || 0 }} {{ t('page.project.pending_bid_alert.pending') }}
          </text>
        </view>

        <view class="bid-alert-actions">
          <button class="btn-approve" @click="onApproveBidUpdate">
            {{ t('page.project.pending_bid_alert.approve_change') }}
          </button>
          <button class="btn-reject" @click="onRejectBidUpdate">
            {{ t('page.project.pending_bid_alert.reject_change') }}
          </button>
        </view>
      </view>

      <!-- Project Details -->
      <view class="card project-details">
        <view class="card-head">
          <text class="card-title">{{ t('page.project.details.title') }}</text>
          <button class="btn-outline btn-outline-sm" @click="onOpenUpdateBidModal">
            <text class="btn-outline-icon">✎</text>
            {{ t('page.project.details.update_bid_button') }}
          </button>
        </view>

        <view class="details-rows">
          <view class="detail-row">
            <text class="detail-label">{{ t('page.project.details.current_bid_label') }}</text>
            <text class="detail-value">{{ formatBidK(projectDetail?.bid || 0) }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">{{ t('page.project.details.deposit_status_label') }}</text>
            <view class="pill" :class="projectDetail?.depositPaid ? 'pill-green' : 'pill-amber'">
              {{
                projectDetail?.depositPaid ? t('page.project.details.deposit_paid') : t('page.project.details.deposit_not_required')
              }}
            </view>
          </view>
          <view v-if="projectDetail?.depositPaid" class="detail-row">
            <text class="detail-label">{{ t('page.project.details.deposit_amount_label') }}</text>
            <text class="detail-value">{{ formatBidK(formatDepositAmount(projectDetail?.bid || 0)) }}</text>
          </view>
          <view class="detail-row">
            <text class="detail-label">{{ t('page.project.details.team_members_label') }}</text>
            <text class="detail-value">{{ projectDetail?.members?.length || 0 }}</text>
          </view>
        </view>
      </view>

      <!-- Quick Actions -->
      <view class="quick-actions">
        <text class="section-title">{{ t('page.project.quick_actions.title') }}</text>
        <view class="quick-grid">
          <button class="quick-item" @click="onQuickAction('new_task')">
            <view class="quick-icon quick-icon-blue">＋</view>
            <view class="quick-text">
              <text class="quick-title">{{ t('page.project.quick_actions.new_task_title') }}</text>
              <text class="quick-sub">{{ t('page.project.quick_actions.new_task_subtitle') }}</text>
            </view>
          </button>
          <button class="quick-item" @click="onQuickAction('invite')">
            <view class="quick-icon quick-icon-green">⤴</view>
            <view class="quick-text">
              <text class="quick-title">{{ t('page.project.quick_actions.invite_title') }}</text>
              <text class="quick-sub">{{ t('page.project.quick_actions.invite_subtitle') }}</text>
            </view>
          </button>
        </view>
      </view>

      <!-- Team Members -->
      <view class="card team-members">
        <text class="card-title-sm">{{ t('page.project.team_members.title') }}</text>
        <view class="member-list">
          <view v-for="m in projectDetail?.members || []" :key="m.id" class="member-row">
            <view class="member-avatar">
              <text class="member-initial">{{ getMemberInitial(m.name) }}</text>
            </view>
            <view class="member-info">
              <view class="member-top">
                <text class="member-name">{{ m.name }}</text>
                <view v-if="m.id === projectDetail?.ownerId" class="chip chip-owner">
                  {{ t('page.project.member_owner_label') }}
                </view>
                <view v-else class="chip chip-type">
                  {{ isAgentType(m.type) ? t('page.project.member_type_ai') : t('page.project.member_type_human') }}
                </view>
              </view>
              <view class="member-bottom">
                <view class="chip chip-online">
                  {{ m.isOnline ? t('page.project.member_online') : t('page.project.member_offline') }}
                </view>
              </view>
            </view>
            <button
              v-if="currentUserIsOwner && m.id !== projectDetail?.ownerId"
              class="member-transfer-btn"
              @click.stop="onOpenTransferModal(m)"
            >
              <text class="member-transfer-btn-glyph">⇄</text>
              <text class="member-transfer-btn-text">{{ t('page.project.actions.transfer') }}</text>
            </button>
          </view>
        </view>
      </view>

      <!-- Recent Activity -->
      <view class="card recent-activity">
        <text class="card-title-sm">{{ t('page.project.recent_activity.title') }}</text>
        <view class="activity-list">
          <view v-if="recentActivityList.length === 0" class="sheet-empty">{{ t('page.project.recent_activity.empty') }}</view>
          <view v-for="a in recentActivityList" :key="a.id" class="activity-item">
            <view class="activity-avatar" :style="{ background: a.avatarGradient }">
              <text class="activity-avatar-text">{{ a.initial }}</text>
            </view>
            <view class="activity-main">
              <text class="activity-text">{{ a.text }}</text>
              <text class="activity-time">{{ a.time }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import PageHeader from '@/components/PageHeader.vue'
import type { ProjectDetail, ProjectMember } from '@/api/modules/project'

defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

export type ProjectSidebarTab = 'tasks' | 'payments' | 'files'

export type RecentActivityItem = {
  id: string
  initial: string
  avatarGradient: string
  text: string
  time: string
}

type Props = {
  projectDetail: ProjectDetail | null
  pendingBidUpdate: ProjectDetail['pendingBidUpdate']
  pendingTaskCount: number
  currentUserIsOwner: boolean
  recentActivityList: RecentActivityItem[]
  onCloseProject: () => void
  onOpenSidebar: (tab: ProjectSidebarTab) => void
  onOpenShareModal: () => void
  onOpenUpdateBidModal: () => void
  onApproveBidUpdate: () => void
  onRejectBidUpdate: () => void
  onQuickAction: (action: 'new_task' | 'invite') => void
  onOpenTransferModal: (member: ProjectMember) => void
  formatBidK: (bid: number) => string
  formatDepositAmount: (bid: number) => number
  getMemberInitial: (name?: string) => string
}

const props = defineProps<Props>()
const { t, tf } = useI18nWithFormat()

const { projectDetail, pendingBidUpdate, pendingTaskCount, currentUserIsOwner, recentActivityList } = toRefs(props)

const onCloseProject = props.onCloseProject
const onOpenSidebar = props.onOpenSidebar
const onOpenShareModal = props.onOpenShareModal
const onOpenUpdateBidModal = props.onOpenUpdateBidModal
const onApproveBidUpdate = props.onApproveBidUpdate
const onRejectBidUpdate = props.onRejectBidUpdate
const onQuickAction = props.onQuickAction
const onOpenTransferModal = props.onOpenTransferModal

const formatBidK = props.formatBidK
const formatDepositAmount = props.formatDepositAmount
const getMemberInitial = props.getMemberInitial

const isAgentType = (type: unknown) => String(type || '').trim().toLowerCase() === 'agent'
</script>

<style scoped src="../project.styles.css"></style>
