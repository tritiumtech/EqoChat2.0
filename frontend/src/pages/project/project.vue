<template>
  <scroll-view class="page" scroll-y>
    <!-- List -->
    <ProjectListView
      v-if="selectedProjectId == null"
      :loading="loading"
      :projects="projects"
      :openCreateModal="openCreateModal"
      :openProject="openProject"
      :statusClass="statusClass"
      :statusText="statusText"
    />

    <ProjectDetailView
      v-else
      :projectDetail="projectDetail"
      :pendingBidUpdate="pendingBidUpdate"
      :pendingTaskCount="pendingTaskCount"
      :currentUserIsOwner="currentUserIsOwner"
      :recentActivityList="recentActivityList"
      :onCloseProject="closeProject"
      :onOpenSidebar="openSidebar"
      :onOpenShareModal="openShareModal"
      :onOpenUpdateBidModal="openUpdateBidModal"
      :onApproveBidUpdate="onApproveBidUpdate"
      :onRejectBidUpdate="onRejectBidUpdate"
      :onQuickAction="onQuickAction"
      :onOpenTransferModal="openTransferModal"
      :formatBidK="formatBidK"
      :formatDepositAmount="formatDepositAmount"
      :getMemberInitial="getMemberInitial"
    />

    <view v-if="false" class="list-wrap">
      <view class="header">
        <view class="header-row">
          <view class="header-titles">
            <text class="header-title">{{ t('page.project.title') }}</text>
            <text class="header-sub">{{ t('page.project.subtitle') }}</text>
          </view>
          <button class="icon-btn bordered" @click="openCreateModal">
            <text class="plus">＋</text>
          </button>
        </view>
      </view>

      <view class="content">
        <view v-if="loading" class="empty-wrap">
          <text class="empty-title">{{ t('page.project.loading') }}</text>
        </view>

        <view v-else-if="projects.length === 0" class="empty-wrap">
          <text class="empty-title">{{ t('page.project.empty_title') }}</text>
          <text class="empty-sub">{{ t('page.project.empty_sub') }}</text>
        </view>

        <view v-else class="cards">
          <view
            v-for="p in projects"
            :key="p.id"
            class="project-card"
            @click="openProject(p.id)"
          >
            <view v-if="p.pendingBidUpdate" class="pending-badge">
              <text>{{ t('page.project.pending_bid_badge') }}</text>
            </view>
            <view class="project-card-head">
              <view class="color-dot" :style="{ backgroundColor: p.color }" />
              <text class="project-name">{{ p.name }}</text>
              <view class="project-status" :class="statusClass(p.status)">
                {{ statusText(p.status) }}
              </view>
            <text class="project-chevron">›</text>
            </view>

            <view class="progress">
              <text class="progress-label">{{ t('page.project.progress') }}</text>
              <text class="progress-val">{{ p.progress }}%</text>
              <view class="progress-bar">
                <view class="progress-fill" :style="{ width: `${p.progress}%`, backgroundColor: p.color }" />
              </view>
            </view>

            <view class="project-meta">
              <text class="meta-left">{{ p.humans }}{{ t('page.project.human_abbr') }} + {{ p.agents }}{{ t('page.project.ai_abbr') }}</text>
              <text class="meta-right">{{ p.revenue }}</text>
            </view>

            <view class="project-submeta">
              <text>{{ t('page.project.bid_prefix') }}{{ p.bid }}</text>
              <text>{{ t('page.project.deposit_prefix') }}{{ p.depositPaid ? t('page.project.deposit_paid') : t('page.project.deposit_pending') }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- Detail -->
    <view v-else-if="false" class="detail-wrap">
      <view class="header">
        <view class="header-row">
          <button class="back-btn" @click="closeProject">
            <text class="back-glyph">←</text>
            <text class="back-text">{{ t('page.project.back_to_list') }}</text>
          </button>

          <view class="header-actions">
            <button class="mini-btn tasks-btn" @click="openSidebar('tasks')">
              <text>{{ t('page.project.tabs.tasks') }}</text>
              <view v-if="pendingTaskCount > 0" class="tasks-badge">
                <text>{{ pendingTaskCount }}</text>
              </view>
            </button>
            <button class="mini-btn primary" @click="openShareModal">{{ t('page.project.actions.share') }}</button>
          </view>
        </view>

        <view class="detail-title-row">
          <view class="color-dot big" :style="{ backgroundColor: projectDetail?.color || '#7C3AED' }" />
          <text class="detail-title">{{ projectDetail?.name || '' }}</text>
        </view>

        <view class="detail-sub-row">
          <text class="detail-sub">
            {{ projectDetail?.humans || 0 }}{{ t('page.project.human_abbr') }} + {{ projectDetail?.agents || 0 }}{{ t('page.project.ai_abbr') }} · {{ t('page.project.deadline_prefix') }}{{ projectDetail?.deadline || '-' }}
          </text>
        </view>
      </view>

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
            {{ t('page.project.pending_bid_alert.subtitle', {
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
            <button class="btn-outline" @click="openUpdateBidModal">
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
                {{ projectDetail?.depositPaid ? t('page.project.details.deposit_paid') : t('page.project.details.deposit_not_required') }}
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
                    {{ m.type === 'agent' ? t('page.project.member_type_ai') : t('page.project.member_type_human') }}
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
                @click.stop="openTransferModal(m)"
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

    <view class="foot-pad" />
  </scroll-view>

  <FgTabbar />

  <!-- Project Sidebar Drawer -->
  <ProjectSidebarDrawer
    :open="showSidebar"
    :sidebarTab="sidebarTab"
    :sidebarTasks="sidebarTasks"
    :sidebarPayments="sidebarPayments"
    :sidebarFiles="sidebarFiles"
    :onClose="closeSidebar"
    :onChangeTab="openSidebar"
    :taskIconClass="taskIconClass"
    :taskIconGlyph="taskIconGlyph"
    :priorityPillClass="priorityPillClass"
    :taskPriorityLabel="taskPriorityLabel"
    :paymentPillClass="paymentPillClass"
    :paymentStatusLabel="paymentStatusLabel"
    :formatMoney="formatMoney"
    :openProjectFile="openProjectFile"
  />

  <view v-if="false" class="drawer-mask" @click="closeSidebar">
    <view class="drawer" @click.stop>
      <view class="drawer-head">
        <view class="drawer-head-left">
          <view class="drawer-icon">▦</view>
          <view class="drawer-titles">
            <text class="drawer-title">{{ t('page.project.drawer.title') }}</text>
            <text class="drawer-sub">{{ t('page.project.drawer.subtitle') }}</text>
          </view>
        </view>
        <text class="sheet-close" @click="closeSidebar">✕</text>
      </view>

      <view class="drawer-tabs">
        <button
          class="tab-btn"
          :class="{ active: sidebarTab === 'tasks' }"
          @click="sidebarTab = 'tasks'"
        >{{ t('page.project.tabs.tasks') }}</button>
        <button
          class="tab-btn"
          :class="{ active: sidebarTab === 'payments' }"
          @click="sidebarTab = 'payments'"
        >{{ t('page.project.tabs.payments') }}</button>
        <button
          class="tab-btn"
          :class="{ active: sidebarTab === 'files' }"
          @click="sidebarTab = 'files'"
        >{{ t('page.project.tabs.files') }}</button>
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
                <text class="task-meta-item">{{ task.assignee }}</text>
                <text class="task-meta-sep">·</text>
                <text class="task-meta-item">{{ task.isAgent ? t('page.project.member_type_ai') : t('page.project.member_type_human') }}</text>
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
              <text class="payment-title">{{ payment.recipient }}</text>
              <view class="payment-amount-row">
                <text class="payment-amount">{{ formatMoney(payment.amount) }}</text>
                <view class="payment-pill" :class="paymentPillClass(payment.status)">
                  <text class="payment-pill-text">{{ paymentStatusLabel(payment.status) }}</text>
                </view>
              </view>
              <text class="payment-meta">
                {{ payment.isAgent ? t('page.project.member_type_ai') : t('page.project.member_type_human') }} · {{ payment.date || '' }}
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
                {{ file.isAgent ? t('page.project.member_type_ai') : t('page.project.member_type_human') }} · {{ file.date || '' }}
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

  <!-- Create Project Modal -->
  <CreateProjectModal
    :open="showCreateModal"
    :createName="createName"
    :createBidStr="createBidStr"
    :canCreateProject="canCreateProject"
    :depositHintText="t('page.project.modals.create.deposit_hint', { deposit: calculateDeposit() })"
    @close="closeCreateModal"
    @submit="submitCreateProject"
    @update:createName="createName = $event"
    @update:createBidStr="createBidStr = $event"
  />

  <view v-if="false" class="sheet-mask" @click="closeCreateModal">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.project.modals.create.title') }}</text>
        <text class="sheet-close" @click="closeCreateModal">✕</text>
      </view>
      <view class="sheet-body">
        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create.project_name') }}</text>
          <input class="field-input" v-model="createName" :placeholder="t('page.project.placeholders.project_name')" />
        </view>
        <view class="field">
          <text class="field-label">{{ t('page.project.modals.create.bid') }}</text>
          <input class="field-input" type="number" v-model="createBidStr" :placeholder="t('page.project.placeholders.bid')" />
        </view>
        <view v-if="Number(createBidStr) >= 100" class="hint">
          {{ t('page.project.modals.create.deposit_hint', { deposit: calculateDeposit() }) }}
        </view>
        <view v-else-if="Number(createBidStr) > 0" class="hint">
          {{ t('page.project.modals.create.no_deposit_hint') }}
        </view>
      </view>
      <view class="sheet-foot">
        <button class="btn-secondary" @click="closeCreateModal">{{ t('toast.cancel') }}</button>
        <button class="btn-primary" :disabled="!canCreateProject" @click="submitCreateProject">{{ t('page.project.modals.create.confirm') }}</button>
      </view>
    </view>
  </view>

  <!-- Update Bid Modal -->
  <UpdateBidModal
    :open="showUpdateBidModal"
    :currentBidText="formatBidK(projectDetail?.bid || 0)"
    :updateBidStr="updateBidStr"
    :canUpdateBid="canUpdateBid"
    :hintText="updateBidHintText"
    @close="closeUpdateBidModal"
    @submit="submitUpdateBid"
    @update:updateBidStr="updateBidStr = $event"
  />

  <view v-if="false" class="sheet-mask" @click="closeUpdateBidModal">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.project.modals.update_bid.title') }}</text>
        <text class="sheet-close" @click="closeUpdateBidModal">✕</text>
      </view>
      <view class="sheet-body">
        <view class="modal-current-bid">
          <text class="modal-current-label">{{ t('page.project.details.current_bid_label') }}</text>
          <text class="modal-current-value">{{ formatBidK(projectDetail?.bid || 0) }}</text>
        </view>

        <view class="field">
          <text class="field-label">{{ t('page.project.modals.update_bid.new_bid_label') }}</text>
          <input class="field-input" type="number" v-model="updateBidStr" :placeholder="t('page.project.placeholders.bid')" />
        </view>

        <view class="team-approval-box">
          <view class="team-approval-head">
            <text class="team-approval-icon">ⓘ</text>
            <text class="team-approval-title">{{ t('page.project.modals.update_bid.team_approval_title') }}</text>
          </view>
          <text class="team-approval-body">{{ t('page.project.modals.update_bid.team_approval_body') }}</text>
          <text class="team-approval-body team-approval-warning">{{ t('page.project.modals.update_bid.team_approval_warning') }}</text>
          <view class="hint">
            {{ updateBidHintText }}
          </view>
        </view>
      </view>
      <view class="sheet-foot">
        <button class="btn-secondary" @click="closeUpdateBidModal">{{ t('toast.cancel') }}</button>
        <button class="btn-primary" :disabled="!canUpdateBid" @click="submitUpdateBid">{{ t('page.project.modals.update_bid.confirm') }}</button>
      </view>
    </view>
  </view>

  <!-- Transfer Modal -->
  <TransferOwnershipModal
    :open="showTransferModal"
    :member="selectedTransferMember"
    :getMemberInitial="getMemberInitial"
    @close="closeTransferModal"
    @submit="submitTransferOwnership"
  />

  <view v-if="false" class="sheet-mask" @click="closeTransferModal">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.project.modals.transfer.title') }}</text>
        <text class="sheet-close" @click="closeTransferModal">✕</text>
      </view>
      <view class="sheet-body">
        <view v-if="!selectedTransferMember" class="sheet-empty">{{ t('page.project.modals.transfer.no_members') }}</view>
        <view v-else>
          <view class="transfer-member-card">
            <view
              class="transfer-member-avatar"
              :class="{ 'transfer-avatar-agent': selectedTransferMember.type === 'agent' }"
            >
              <text class="transfer-member-avatar-initial">{{ getMemberInitial(selectedTransferMember.name) }}</text>
            </view>
            <view class="transfer-member-main">
              <text class="transfer-member-name">{{ selectedTransferMember.name }}</text>
              <text class="transfer-member-sub">
                {{ selectedTransferMember.type === 'agent' ? t('page.project.member_type_ai') : t('page.project.member_type_human') }}
              </text>
            </view>
          </view>

          <view class="transfer-info-box">
            <view class="transfer-info-head">
              <text class="transfer-info-icon">👑</text>
              <text class="transfer-info-title">{{ t('page.project.modals.transfer.responsibilities_title') }}</text>
            </view>
            <text class="transfer-info-body">
              {{ t('page.project.modals.transfer.responsibilities_body', { memberName: selectedTransferMember.name }) }}
            </text>

            <view class="transfer-info-list">
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_1') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_2') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_3') }}</text>
              <text class="transfer-li">• {{ t('page.project.modals.transfer.resp_4') }}</text>
            </view>

            <text class="transfer-info-accept">
              {{ t('page.project.modals.transfer.must_accept', { memberName: selectedTransferMember.name }) }}
            </text>
          </view>
        </view>
      </view>
      <view class="sheet-foot">
        <button class="btn-secondary" @click="closeTransferModal">{{ t('toast.cancel') }}</button>
        <button
          class="btn-primary"
          :disabled="!selectedTransferMember"
          @click="submitTransferOwnership"
        >
          {{ t('page.project.modals.transfer.confirm') }}
        </button>
      </view>
    </view>
  </view>

  <!-- Share Modal -->
  <ShareProjectModal
    :open="showShareModal"
    :projectDetail="projectDetail"
    :shareUrl="shareUrl"
    :shareMessage="shareMessage"
    :getMemberInitial="getMemberInitial"
    @close="closeShareModal"
    @copy="copyShareLink"
    @update:shareMessage="shareMessage = $event"
  />

  <view v-if="false" class="sheet-mask" @click="closeShareModal">
    <view class="sheet" @click.stop>
      <view class="sheet-head">
        <text class="sheet-title">{{ t('page.project.modals.share.title') }}</text>
        <text class="sheet-close" @click="closeShareModal">✕</text>
      </view>
      <view class="sheet-body">
        <view class="share-project-top">
          <view class="share-project-avatar" :style="{ background: projectDetail?.color || '#7C3AED' }">
            <text class="share-project-avatar-text">{{ getMemberInitial(projectDetail?.name) }}</text>
          </view>
          <view class="share-project-main">
            <text class="share-project-name">{{ projectDetail?.name || '-' }}</text>
            <text class="share-project-id">{{ t('page.project.modals.share.project_id_label', { id: projectDetail?.id }) }}</text>
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
            v-model="shareMessage"
            :placeholder="t('page.project.modals.share.message_placeholder')"
          ></textarea>
        </view>
      </view>
      <view class="sheet-foot">
        <button class="btn-secondary" @click="closeShareModal">{{ t('page.project.modals.share.close') }}</button>
        <button class="btn-primary" :disabled="!shareUrl" @click="copyShareLink">{{ t('page.project.modals.share.share_project') }}</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from 'vue-i18n'
import FgTabbar from '@/tabbar/index.vue'
import ProjectListView from './components/ProjectListView.vue'
import ProjectDetailView from './components/ProjectDetailView.vue'
import ProjectSidebarDrawer from './components/ProjectSidebarDrawer.vue'
import CreateProjectModal from './components/modals/CreateProjectModal.vue'
import UpdateBidModal from './components/modals/UpdateBidModal.vue'
import TransferOwnershipModal from './components/modals/TransferOwnershipModal.vue'
import ShareProjectModal from './components/modals/ShareProjectModal.vue'
import { useChatStore } from '@/store/modules/chat'
import {
  projectApi,
  type ProjectDetail,
  type ProjectFile,
  type ProjectMember,
  type ProjectPayment,
  type ProjectSummary,
  type ProjectTask,
} from '@/api/modules/project'

const { t } = useI18n({ useScope: 'global' })
const chatStore = useChatStore()

// 页面层同样设置为 shared，避免子组件样式隔离导致布局丢失（小程序端尤其明显）
defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

type SidebarTab = 'tasks' | 'payments' | 'files'

const loading = ref(false)
const projects = ref<ProjectSummary[]>([])

const selectedProjectId = ref<number | null>(null)
const projectDetail = ref<ProjectDetail | null>(null)

const sidebarTab = ref<SidebarTab>('tasks')
const sidebarTasks = ref<ProjectTask[]>([])
const sidebarPayments = ref<ProjectPayment[]>([])
const sidebarFiles = ref<ProjectFile[]>([])
const showSidebar = ref(false)

const showCreateModal = ref(false)
const createName = ref('')
const createBidStr = ref('0')

const showUpdateBidModal = ref(false)
const updateBidStr = ref('0')

const showTransferModal = ref(false)
const selectedTransferMemberId = ref<number | null>(null)

const showShareModal = ref(false)
const shareUrl = ref('')
const shareMessage = ref('')
const shareLoading = ref(false)

const canCreateProject = computed(() => {
  const nameOk = createName.value.trim().length > 0
  const bid = Number(createBidStr.value)
  return nameOk && Number.isFinite(bid) && bid > 0
})

const canUpdateBid = computed(() => {
  const bid = Number(updateBidStr.value)
  return Number.isFinite(bid) && bid > 0
})

const updateBidHintText = computed(() => {
  const bid = Number(updateBidStr.value)
  if (!Number.isFinite(bid) || bid <= 0) return ''
  return bid >= 100 ? t('page.project.modals.update_bid.hint_need_deposit') : t('page.project.modals.update_bid.hint_no_deposit')
})

const pendingTaskCount = computed(() => {
  return sidebarTasks.value.filter((task) => String(task.status || '').toLowerCase() !== 'completed').length
})

const pendingBidUpdate = computed(() => {
  return projectDetail.value?.pendingBidUpdate || null
})

const selectedTransferMember = computed(() => {
  if (!projectDetail.value?.members?.length) return null
  return projectDetail.value.members.find((m) => m.id === selectedTransferMemberId.value) || null
})

const currentUserIsOwner = computed(() => {
  if (!projectDetail.value) return false
  if (chatStore.currentUserId == null) return false
  return projectDetail.value.ownerId === chatStore.currentUserId
})

type RecentActivityItem = {
  id: string
  initial: string
  avatarGradient: string
  text: string
  time: string
}

const recentActivityList = computed<RecentActivityItem[]>(() => {
  if (!projectDetail.value) return []

  const items: RecentActivityItem[] = []

  const completedTask = sidebarTasks.value.find((task) => {
    const s = String(task.status || '').toLowerCase()
    return s.includes('complete')
  })
  if (completedTask) {
    const actor = completedTask.assignee || ''
    items.push({
      id: 'ra_task_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(124, 58, 237, 0.95), rgba(99, 102, 241, 0.85))',
      text: t('page.project.recent_activity.completed_task', { actor, title: completedTask.title || '-' }),
      time: t('page.project.recent_activity.time_2h'),
    })
  }

  const firstFile = sidebarFiles.value[0]
  if (firstFile) {
    const actor = firstFile.uploadedBy || ''
    items.push({
      id: 'ra_file_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(59, 130, 246, 0.85))',
      text: t('page.project.recent_activity.added_file', { actor, fileName: firstFile.name || '-' }),
      time: t('page.project.recent_activity.time_5h'),
    })
  }

  const firstPayment = sidebarPayments.value[0]
  if (firstPayment) {
    const actor = firstPayment.recipient || ''
    items.push({
      id: 'ra_payment_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(220, 38, 38, 0.95), rgba(239, 68, 68, 0.85))',
      text: t('page.project.recent_activity.optimized_performance', { actor }),
      time: t('page.project.recent_activity.time_yesterday'),
    })
  }

  return items
})

function statusClass(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('pause')) return 'status-paused'
  if (s.includes('complete')) return 'status-completed'
  return 'status-active'
}

function statusText(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('pause')) return t('page.project.status.paused')
  if (s.includes('complete')) return t('page.project.status.completed')
  return t('page.project.status.active')
}

function taskStatusLabel(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('complete')) return t('page.project.task_status.completed')
  if (s.includes('in-progress') || s.includes('in_progress')) return t('page.project.task_status.in_progress')
  return t('page.project.task_status.pending')
}

function taskPriorityLabel(priority: any) {
  const s = String(priority || '').toLowerCase()
  if (s.includes('high')) return t('page.project.task_priority.high')
  if (s.includes('low')) return t('page.project.task_priority.low')
  return t('page.project.task_priority.medium')
}

function paymentStatusLabel(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('paid')) return t('page.project.payment_status.paid')
  if (s.includes('invoiced')) return t('page.project.payment_status.invoiced')
  return t('page.project.payment_status.pending')
}

function formatBidK(bid: number) {
  const safe = Number.isFinite(bid) ? Math.max(0, bid) : 0
  const k = safe / 1000
  const decimals = Math.abs(k - Math.round(k)) < 0.000001 ? 0 : 1
  return `$${k.toFixed(decimals)}K`
}

function formatDepositAmount(bid: number) {
  const safe = Number.isFinite(bid) ? Math.max(0, bid) : 0
  // 项目 bid 的押金口径为 10%
  return safe * 0.1
}

function getMemberInitial(name?: string) {
  const v = (name || '').trim()
  if (!v) return '?'
  return v.slice(0, 1).toUpperCase()
}

function onQuickAction(_action: 'new_task' | 'invite') {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

function onApproveBidUpdate() {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

function onRejectBidUpdate() {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

function formatMoney(amount: number) {
  const safe = Number.isFinite(amount) ? Math.max(0, amount) : 0
  const v = Math.round(safe).toString()
  const withComma = v.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `$${withComma}`
}

function priorityPillClass(priority: any) {
  const s = String(priority || '').toLowerCase()
  if (s.includes('high')) return 'prio-high'
  if (s.includes('low')) return 'prio-low'
  return 'prio-medium'
}

function paymentPillClass(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('paid')) return 'pay-paid'
  if (s.includes('invoiced')) return 'pay-invoiced'
  return 'pay-pending'
}

function taskIconClass(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('complete')) return 'task-icon-done'
  if (s.includes('in-progress') || s.includes('in_progress')) return 'task-icon-progress'
  return 'task-icon-pending'
}

function taskIconGlyph(status: any) {
  const s = String(status || '').toLowerCase()
  if (s.includes('complete')) return '✓'
  if (s.includes('in-progress') || s.includes('in_progress')) return '⏳'
  return '•'
}

function openProjectFile(downloadUrl?: string) {
  if (!downloadUrl) return
  // #ifdef H5
  if (typeof window !== 'undefined' && window.open) {
    window.open(downloadUrl, '_blank')
  }
  // #endif
  // #ifndef H5
  uni.downloadFile({
    url: downloadUrl,
    success: (res) => {
      if (res.statusCode === 200 && res.tempFilePath) {
        uni.openDocument({ filePath: res.tempFilePath, fileType: '' })
        return
      }
      uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    },
    fail: () => uni.showToast({ title: t('toast.load_failed'), icon: 'none' }),
  })
  // #endif
}

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await projectApi.listMyProjects()
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    projects.value = []
  } finally {
    loading.value = false
  }
}

async function loadProjectAll(id: number) {
  sidebarTab.value = 'tasks'
  const [detail, tasks, payments, files] = await Promise.all([
    projectApi.getProjectDetail(id),
    projectApi.listSidebarTasks(id),
    projectApi.listSidebarPayments(id),
    projectApi.listSidebarFiles(id),
  ])
  projectDetail.value = detail
  sidebarTasks.value = tasks
  sidebarPayments.value = payments
  sidebarFiles.value = files
  selectedTransferMemberId.value = detail?.members?.[0]?.id ?? null
}

function openCreateModal() {
  createName.value = ''
  createBidStr.value = '0'
  showCreateModal.value = true
}

function closeCreateModal() {
  showCreateModal.value = false
}

function closeUpdateBidModal() {
  showUpdateBidModal.value = false
}

function closeTransferModal() {
  showTransferModal.value = false
  selectedTransferMemberId.value = null
}

function closeShareModal() {
  showShareModal.value = false
}

function calculateDeposit() {
  const bid = Number(createBidStr.value)
  if (!Number.isFinite(bid) || bid <= 0) return '$0'
  const dep = (bid * 0.1).toFixed(2)
  return `$${dep}`
}

async function submitCreateProject() {
  if (!canCreateProject.value) return
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    const bid = Math.round(Number(createBidStr.value))
    const created = await projectApi.createProject({ name: createName.value.trim(), bid })
    showCreateModal.value = false
    selectedProjectId.value = created.id
    await loadProjectAll(created.id)
    uni.showToast({ title: t('page.project.toasts.create_success'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function openUpdateBidModal() {
  if (!projectDetail.value) return
  updateBidStr.value = String(projectDetail.value.bid ?? 0)
  showUpdateBidModal.value = true
}

async function submitUpdateBid() {
  if (!canUpdateBid.value || selectedProjectId.value == null) return
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    const newBid = Math.round(Number(updateBidStr.value))
    await projectApi.requestBidUpdate(selectedProjectId.value, { newBid })
    showUpdateBidModal.value = false
    await loadProjectAll(selectedProjectId.value)
    uni.showToast({ title: t('page.project.toasts.update_success'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function openTransferModal(member?: ProjectMember) {
  if (!projectDetail.value) return
  if (member) {
    selectedTransferMemberId.value = member.id
  } else {
    selectedTransferMemberId.value = selectedTransferMemberId.value ?? projectDetail.value.members?.[0]?.id ?? null
  }
  showTransferModal.value = true
}

function selectTransferMember(m: ProjectMember) {
  selectedTransferMemberId.value = m.id
}

async function submitTransferOwnership() {
  if (!selectedProjectId.value || !selectedTransferMember.value) return
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    const toMemberType = selectedTransferMember.value.type === 'agent' ? 'AGENT' : 'HUMAN'
    await projectApi.transferOwnership(selectedProjectId.value, {
      toMemberId: selectedTransferMember.value.id,
      toMemberType: toMemberType as 'AGENT' | 'HUMAN',
    })
    showTransferModal.value = false
    await loadProjectAll(selectedProjectId.value)
    uni.showToast({ title: t('page.project.toasts.transfer_success'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function openShareModal() {
  if (!selectedProjectId.value) return
  showShareModal.value = true
  shareMessage.value = ''
  shareUrl.value = ''
  loadShareLink(selectedProjectId.value)
}

async function loadShareLink(projectId: number) {
  if (shareLoading.value) return
  shareLoading.value = true
  try {
    const resp = await projectApi.shareLink(projectId)
    shareUrl.value = resp.url
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    shareUrl.value = ''
  } finally {
    shareLoading.value = false
  }
}

function copyShareLink() {
  if (!shareUrl.value) return
  uni.setClipboardData({
    data: shareUrl.value,
    success: () => uni.showToast({ title: t('page.project.toasts.share_copied'), icon: 'none' }),
    fail: () => uni.showToast({ title: t('toast.load_failed'), icon: 'none' }),
  })
}

function openProject(id: number) {
  selectedProjectId.value = id
  loadProjectAll(id).catch((err: any) => {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    selectedProjectId.value = null
  })
}

function openSidebar(tab: SidebarTab) {
  sidebarTab.value = tab
  showSidebar.value = true
}

function closeSidebar() {
  showSidebar.value = false
}

function closeProject() {
  selectedProjectId.value = null
  projectDetail.value = null
  sidebarTasks.value = []
  sidebarPayments.value = []
  sidebarFiles.value = []
  showSidebar.value = false
}

onLoad((query) => {
  const raw = (query as any)?.projectId ?? (query as any)?.id
  const id = Number(raw)
  if (!Number.isNaN(id) && id > 0) {
    selectedProjectId.value = id
    loadProjectAll(id).catch(() => {
      selectedProjectId.value = null
    })
  }
})

onShow(async () => {
  await loadProjects()
  // 如果从分享链接带参进入，则在项目详情加载失败时退回列表
  if (selectedProjectId.value != null && projectDetail.value == null) {
    try {
      await loadProjectAll(selectedProjectId.value)
    } catch {
      selectedProjectId.value = null
      projectDetail.value = null
    }
  }
})
</script>

<style scoped src="./project.styles.css"></style>

<!--
旧样式已迁移到 project.styles.css，并由页面/子组件分别通过 scoped src 引入。
下方保留的历史样式块将被移除；如果你仍在本文件看到大量 CSS，请继续向下确认是否仍存在残留。
-->

<!-- <style>
@import '@/styles/tokens.css';

.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, var(--c-bg) 0%, var(--c-bg-2) 100%);
  box-sizing: border-box;
}

.header {
  flex-shrink: 0;
  padding: 16rpx 24rpx 0;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1rpx solid var(--c-border);
  box-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.04);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16rpx;
}

.header-titles {
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.header-sub {
  font-size: 22rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
  display: block;
}

.icon-btn {
  width: 72rpx;
  height: 72rpx;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--c-input-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn.bordered {
  border: 1rpx solid var(--c-border);
  background: var(--c-surface);
}

.plus {
  font-size: 40rpx;
  color: var(--c-primary);
  font-weight: 700;
  line-height: 1;
}

.content {
  padding: 24rpx;
  flex: 1;
  box-sizing: border-box;
}

.cards {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.empty-wrap {
  margin-top: 40rpx;
  background: rgba(255, 255, 255, 0.7);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 28rpx;
}

.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.empty-sub {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: var(--c-muted);
  line-height: 1.5;
}

.project-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  position: relative;
  padding: 20rpx;
  box-shadow: var(--c-shadow-soft);
  backdrop-filter: blur(12rpx);
}

.project-card-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.color-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: #7c3aed;
}

.color-dot.big {
  width: 22rpx;
  height: 22rpx;
}

.project-name {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 800;
  color: var(--c-ink);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.project-chevron {
  font-size: 34rpx;
  color: var(--c-muted);
  line-height: 1;
  margin-left: 6rpx;
}

.project-status {
  font-size: 18rpx;
  padding: 6rpx 14rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  color: var(--c-muted);
  flex-shrink: 0;
}

.project-status.status-active {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.22);
  color: #059669;
}

.project-status.status-paused {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.22);
  color: #b45309;
}

.project-status.status-completed {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.22);
  color: #2563eb;
}

.progress {
  margin-top: 14rpx;
}

.progress-label {
  font-size: 22rpx;
  color: var(--c-muted);
}

.progress-val {
  font-size: 22rpx;
  color: var(--c-muted);
  float: right;
}

.progress-bar {
  margin-top: 8rpx;
  width: 100%;
  height: 10rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
}

.project-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.meta-left {
  font-size: 22rpx;
  color: var(--c-muted);
}

.meta-right {
  font-size: 22rpx;
  color: var(--c-primary);
  font-weight: 700;
}

.project-submeta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 8rpx;
}

.detail-title {
  font-size: 32rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.detail-sub-row {
  padding-bottom: 16rpx;
}

.detail-sub {
  font-size: 22rpx;
  color: var(--c-muted);
}

.back-btn {
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 0;
  margin: 0;
}

.back-glyph {
  font-size: 34rpx;
  color: var(--c-primary);
  font-weight: 800;
}

.back-text {
  font-size: 22rpx;
  color: var(--c-muted);
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 10rpx;
  align-items: center;
}

.mini-btn {
  padding: 10rpx 14rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.6);
  font-size: 20rpx;
  color: var(--c-muted);
  line-height: 1;
}

.mini-btn.primary {
  background: rgba(3, 2, 19, 0.9);
  border-color: rgba(3, 2, 19, 0.9);
  color: #fff;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.stat-card {
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  padding: 18rpx;
  backdrop-filter: blur(12rpx);
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  min-width: 0;
}

.stat-label {
  display: block;
  font-size: 20rpx;
  color: var(--c-muted);
}

.stat-value {
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.stat-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 800;
  flex-shrink: 0;
}

.stat-icon-earned {
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.22);
}

.stat-icon-pending {
  background: rgba(245, 158, 11, 0.12);
  border: 1rpx solid rgba(245, 158, 11, 0.22);
}

.stat-icon-tasks {
  background: rgba(59, 130, 246, 0.12);
  border: 1rpx solid rgba(59, 130, 246, 0.22);
}

.stat-icon-efficiency {
  background: rgba(124, 58, 237, 0.12);
  border: 1rpx solid rgba(124, 58, 237, 0.22);
}

.progress-wrap {
  margin-top: 18rpx;
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  padding: 18rpx;
  backdrop-filter: blur(12rpx);
}

.progress-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tabs {
  margin-top: 18rpx;
  display: flex;
  gap: 12rpx;
  padding: 10rpx;
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12rpx);
}

.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 998;
  display: flex;
  justify-content: flex-end;
}

.drawer {
  width: 520rpx;
  max-width: 90vw;
  height: 100%;
  background: rgba(255, 255, 255, 0.98);
  border-left: 1rpx solid var(--c-border);
  backdrop-filter: blur(12rpx);
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid var(--c-border);
}

.drawer-head-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.drawer-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 16rpx;
  background: rgba(17, 24, 39, 0.10);
  border: 1rpx solid rgba(17, 24, 39, 0.14);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.drawer-titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.drawer-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--c-ink);
}

.drawer-sub {
  font-size: 20rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
}

.drawer-tabs {
  margin: 16rpx 24rpx 0;
  display: flex;
  gap: 12rpx;
}

.drawer-actions {
  display: flex;
  gap: 12rpx;
  padding: 16rpx 24rpx 0;
}

.drawer-body {
  flex: 1;
  padding: 16rpx 24rpx 24rpx;
  box-sizing: border-box;
}

.tab-btn {
  flex: 1;
  height: 54rpx;
  line-height: 54rpx;
  font-size: 22rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid transparent;
  background: transparent;
  color: var(--c-muted);
}

.tab-btn.active {
  background: rgba(3, 2, 19, 0.9);
  color: #fff;
  border-color: rgba(3, 2, 19, 0.9);
}

.sidebar-list {
  margin-top: 18rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.sheet-empty {
  text-align: center;
  color: var(--c-muted);
  padding: 60rpx 0;
}

.row {
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  padding: 18rpx;
  backdrop-filter: blur(12rpx);
}

.row-title {
  display: block;
  font-size: 26rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.row-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--c-muted);
}

.row-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.link-btn {
  margin-top: 14rpx;
  width: 100%;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: var(--radius-pill);
  background: rgba(59, 130, 246, 0.12);
  border: 1rpx solid rgba(59, 130, 246, 0.25);
  color: #2563eb;
  font-size: 22rpx;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.6);
  padding: 16rpx;
  margin-bottom: 14rpx;
  backdrop-filter: blur(12rpx);
}

.member-row.selected {
  border-color: rgba(3, 2, 19, 0.35);
  box-shadow: 0 10rpx 18rpx rgba(3, 2, 19, 0.08);
}

.member-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #7c3aedf0, #6366f1e0);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.member-avatar-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.member-main {
  flex: 1;
  min-width: 0;
}

.member-name {
  display: block;
  font-size: 26rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.member-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom);
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-bottom: 1rpx solid var(--c-border);
}

.sheet-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--c-ink);
}

.sheet-close {
  font-size: 30rpx;
  color: var(--c-muted);
  font-weight: 700;
}

.sheet-body {
  padding: 16rpx 24rpx 24rpx;
}

.sheet-foot {
  padding: 18rpx 24rpx;
  border-top: 1rpx solid var(--c-border);
  display: flex;
  gap: 16rpx;
}

.btn-primary {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: var(--radius-pill);
  background: rgba(3, 2, 19, 0.9);
  color: #fff;
  border: 1rpx solid rgba(3, 2, 19, 0.9);
  font-size: 22rpx;
  font-weight: 800;
}

.btn-primary[disabled] {
  opacity: 0.5;
}

.btn-secondary {
  flex: 1;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: var(--radius-pill);
  background: rgba(246, 242, 238, 0.9);
  color: var(--c-ink);
  border: 1rpx solid var(--c-border);
  font-size: 22rpx;
  font-weight: 800;
}

.field {
  margin-bottom: 16rpx;
}

.field-label {
  display: block;
  font-size: 22rpx;
  color: var(--c-muted);
  margin-bottom: 10rpx;
}

.field-input {
  width: 100%;
  height: 64rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid var(--c-border);
  background: rgba(246, 242, 238, 0.9);
  padding: 0 20rpx;
  box-sizing: border-box;
  font-size: 22rpx;
}

.hint {
  margin-top: 12rpx;
  background: rgba(245, 158, 11, 0.08);
  border: 1rpx solid rgba(245, 158, 11, 0.22);
  color: #b45309;
  border-radius: var(--radius-lg);
  padding: 12rpx 14rpx;
  font-size: 20rpx;
}

.share-box {
  border: 1rpx solid var(--c-border);
  background: rgba(246, 242, 238, 0.6);
  border-radius: var(--radius-lg);
  padding: 16rpx;
}

.share-label {
  display: block;
  font-size: 20rpx;
  color: var(--c-muted);
}

.share-url {
  display: block;
  margin-top: 10rpx;
  word-break: break-all;
  font-size: 20rpx;
  color: var(--c-ink);
  font-weight: 700;
}

.foot-pad {
  height: 48rpx;
}

/* ===== Project Detail (ProjectPage.tsx parity) ===== */
.tasks-btn {
  position: relative;
  padding-right: 44rpx;
}

.tasks-badge {
  position: absolute;
  top: -10rpx;
  right: -14rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 6rpx;
  border-radius: 20rpx;
  background: rgba(239, 68, 68, 0.95);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 800;
}

.card {
  background: rgba(255, 255, 255, 0.6);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 20rpx;
  box-shadow: var(--c-shadow-soft);
  backdrop-filter: blur(12rpx);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.card-title {
  font-size: 26rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.card-title-sm {
  display: block;
  font-size: 24rpx;
  font-weight: 900;
  color: var(--c-ink);
  margin-bottom: 16rpx;
}

.bid-alert {
  margin-top: 22rpx;
  background: rgba(245, 158, 11, 0.06);
  border: 1rpx solid rgba(245, 158, 11, 0.25);
  border-radius: var(--radius-lg);
  padding: 20rpx;
}

.bid-alert-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.bid-alert-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 26rpx;
}

.bid-alert-title {
  font-size: 24rpx;
  font-weight: 900;
  color: #b45309;
}

.bid-alert-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.bid-alert-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 12rpx;
}

.bid-approve {
  color: #16a34a;
  font-weight: 700;
  font-size: 20rpx;
}

.bid-reject {
  color: #ef4444;
  font-weight: 700;
  font-size: 20rpx;
}

.bid-pending {
  color: var(--c-muted);
  font-weight: 700;
  font-size: 20rpx;
}

.bid-sep {
  color: rgba(0, 0, 0, 0.25);
  font-size: 20rpx;
}

.bid-alert-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 14rpx;
}

.btn-approve,
.btn-reject {
  flex: 1;
  border-radius: var(--radius-lg);
  padding: 14rpx 10rpx;
  font-size: 20rpx;
  font-weight: 800;
  border: 1rpx solid transparent;
}

.btn-approve {
  background: rgba(16, 185, 129, 0.14);
  border-color: rgba(16, 185, 129, 0.35);
  color: #059669;
}

.btn-reject {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.30);
  color: #dc2626;
}

.details-rows {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.detail-label {
  font-size: 20rpx;
  color: var(--c-muted);
  flex-shrink: 0;
}

.detail-value {
  font-size: 22rpx;
  color: var(--c-ink);
  font-weight: 900;
  text-align: right;
}

.pill {
  font-size: 20rpx;
  padding: 8rpx 16rpx;
  border-radius: var(--radius-pill);
  font-weight: 800;
  text-align: center;
}

.pill-green {
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.28);
  color: #059669;
}

.pill-amber {
  background: rgba(245, 158, 11, 0.12);
  border: 1rpx solid rgba(245, 158, 11, 0.28);
  color: #b45309;
}

.quick-actions {
  margin-top: 22rpx;
}

.section-title {
  display: block;
  font-size: 24rpx;
  font-weight: 900;
  color: var(--c-ink);
  margin-bottom: 16rpx;
}

.quick-grid {
  display: flex;
  gap: 16rpx;
}

.quick-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.55);
  box-shadow: var(--c-shadow-soft);
  backdrop-filter: blur(12rpx);
}

.quick-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.quick-icon-blue {
  background: rgba(59, 130, 246, 0.14);
  border: 1rpx solid rgba(59, 130, 246, 0.25);
  color: #2563eb;
}

.quick-icon-green {
  background: rgba(16, 185, 129, 0.14);
  border: 1rpx solid rgba(16, 185, 129, 0.25);
  color: #059669;
}

.quick-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.quick-title {
  font-size: 22rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.quick-sub {
  font-size: 20rpx;
  color: var(--c-muted);
  margin-top: 6rpx;
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: rgba(124, 58, 237, 0.06);
  border: 1rpx solid rgba(124, 58, 237, 0.25);
  border-radius: var(--radius-lg);
  padding: 10rpx 16rpx;
  font-size: 20rpx;
  font-weight: 900;
  color: var(--c-primary);
}

.btn-outline-icon {
  font-size: 22rpx;
  line-height: 1;
}

.team-members .member-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.team-members .member-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 6rpx 0;
}

.team-members .member-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(124, 58, 237, 0.12);
  border: 1rpx solid rgba(124, 58, 237, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.team-members .member-initial {
  font-weight: 900;
  font-size: 24rpx;
  color: var(--c-ink);
}

.team-members .member-info {
  flex: 1;
  min-width: 0;
}

.team-members .member-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.team-members .member-name {
  font-size: 22rpx;
  font-weight: 900;
  color: var(--c-ink);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.team-members .member-bottom {
  margin-top: 8rpx;
}

.team-members .chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6rpx 12rpx;
  border-radius: var(--radius-pill);
  font-size: 18rpx;
  font-weight: 900;
  border: 1rpx solid transparent;
}

.team-members .chip-owner {
  background: rgba(245, 158, 11, 0.15);
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.25);
}

.team-members .chip-type {
  background: rgba(59, 130, 246, 0.10);
  color: #2563eb;
  border-color: rgba(59, 130, 246, 0.18);
}

.team-members .chip-online {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border-color: rgba(16, 185, 129, 0.24);
}
 
/* Drawer cards (Tasks / Payments / Files) */
.task-card,
.payment-card,
.file-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: var(--c-shadow-soft);
  backdrop-filter: blur(12rpx);
  margin-bottom: 14rpx;
}

.task-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1rpx solid transparent;
}

.task-icon {
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1;
}

.task-icon-pending {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.25);
}

.task-icon-progress {
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  border-color: rgba(59, 130, 246, 0.25);
}

.task-icon-done {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  border-color: rgba(16, 185, 129, 0.25);
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.task-title {
  font-size: 22rpx;
  font-weight: 900;
  color: var(--c-ink);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  flex: 1;
}

.priority-pill {
  padding: 6rpx 14rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid transparent;
  font-size: 18rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.priority-text {
  font-size: 18rpx;
}

.prio-high {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.28);
  color: #dc2626;
}

.prio-medium {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.28);
  color: #b45309;
}

.prio-low {
  background: rgba(148, 163, 184, 0.18);
  border-color: rgba(148, 163, 184, 0.28);
  color: #64748b;
}

.task-meta-row {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  color: var(--c-muted);
  font-size: 20rpx;
}

.task-meta-sep {
  color: rgba(0, 0, 0, 0.25);
}

.task-meta-date {
  color: var(--c-muted);
}

.payment-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.25);
  color: #059669;
}

.payment-icon {
  font-size: 28rpx;
  font-weight: 900;
}

.payment-main {
  flex: 1;
  min-width: 0;
}

.payment-title {
  display: block;
  font-size: 22rpx;
  font-weight: 900;
  color: var(--c-ink);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.payment-amount-row {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.payment-amount {
  font-size: 22rpx;
  font-weight: 900;
  color: #059669;
}

.payment-pill {
  padding: 6rpx 14rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid transparent;
  font-size: 18rpx;
  font-weight: 900;
  flex-shrink: 0;
}

.payment-pill-text {
  font-size: 18rpx;
}

.pay-paid {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.28);
  color: #059669;
}

.pay-invoiced {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.26);
  color: #2563eb;
}

.pay-pending {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.26);
  color: #b45309;
}

.payment-meta {
  margin-top: 10rpx;
  display: block;
  font-size: 20rpx;
  color: var(--c-muted);
}

.file-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(59, 130, 246, 0.12);
  border: 1rpx solid rgba(59, 130, 246, 0.25);
  color: #2563eb;
}

.file-icon {
  font-size: 26rpx;
  font-weight: 900;
}

.file-main {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  font-size: 22rpx;
  font-weight: 900;
  color: var(--c-ink);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.file-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.file-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

/* Update Bid Modal (ProjectPage.tsx parity) */
.modal-current-bid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 18rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  margin-bottom: 14rpx;
}

.modal-current-label {
  font-size: 20rpx;
  color: var(--c-muted);
  font-weight: 700;
}

.modal-current-value {
  font-size: 22rpx;
  color: var(--c-ink);
  font-weight: 900;
}

.team-approval-box {
  margin-top: 14rpx;
  padding: 18rpx;
  border-radius: var(--radius-lg);
  background: rgba(59, 130, 246, 0.08);
  border: 1rpx solid rgba(59, 130, 246, 0.22);
}

.team-approval-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.team-approval-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.12);
  border: 1rpx solid rgba(59, 130, 246, 0.24);
  color: #2563eb;
  font-size: 26rpx;
  font-weight: 900;
}

.team-approval-title {
  font-size: 22rpx;
  font-weight: 900;
  color: #2563eb;
}

.team-approval-body {
  display: block;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: var(--c-muted);
  line-height: 1.5;
}

.team-approval-warning {
  margin-top: 10rpx;
  color: #d97706;
}

.pending-badge {
  position: absolute;
  top: -10rpx;
  right: -14rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 10rpx;
  border-radius: 20rpx;
  background: rgba(245, 158, 11, 0.95);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 18rpx rgba(245, 158, 11, 0.16);
  border: 1rpx solid rgba(245, 158, 11, 0.28);
  white-space: nowrap;
}

.member-transfer-btn {
  flex-shrink: 0;
  height: 56rpx;
  padding: 0 14rpx;
  border-radius: var(--radius-pill);
  border: 1rpx solid rgba(3, 2, 19, 0.10);
  background: rgba(3, 2, 19, 0.06);
  color: var(--c-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.member-transfer-btn-glyph {
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1;
}

.member-transfer-btn-text {
  font-size: 20rpx;
  font-weight: 900;
  line-height: 1;
}

/* Recent Activity */
.recent-activity {
  margin-top: 22rpx;
}

.activity-list {
  margin-top: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 18rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  box-shadow: var(--c-shadow-soft);
  backdrop-filter: blur(12rpx);
}

.activity-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-avatar-text {
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.activity-main {
  flex: 1;
  min-width: 0;
}

.activity-text {
  display: block;
  font-size: 20rpx;
  color: var(--c-ink);
  font-weight: 700;
  line-height: 1.45;
}

.activity-time {
  display: block;
  margin-top: 8rpx;
  font-size: 18rpx;
  color: var(--c-muted);
}

/* Transfer modal */
.transfer-member-card {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx;
  border-radius: var(--radius-lg);
  border: 1rpx solid var(--c-border);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(12rpx);
  margin-bottom: 14rpx;
}

.transfer-member-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.95), rgba(37, 99, 235, 0.85));
  box-shadow: var(--c-shadow-soft);
}

.transfer-avatar-agent {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.95), rgba(99, 102, 241, 0.85));
}

.transfer-member-avatar-initial {
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.transfer-member-main {
  flex: 1;
  min-width: 0;
}

.transfer-member-name {
  display: block;
  font-size: 24rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.transfer-member-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.transfer-info-box {
  padding: 18rpx;
  border-radius: var(--radius-lg);
  background: rgba(59, 130, 246, 0.08);
  border: 1rpx solid rgba(59, 130, 246, 0.22);
}

.transfer-info-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.transfer-info-icon {
  width: 44rpx;
  height: 44rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.12);
  border: 1rpx solid rgba(59, 130, 246, 0.24);
  color: #2563eb;
  font-size: 26rpx;
  font-weight: 900;
}

.transfer-info-title {
  font-size: 22rpx;
  font-weight: 900;
  color: #2563eb;
}

.transfer-info-body {
  display: block;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: var(--c-muted);
  line-height: 1.5;
}

.transfer-info-list {
  margin-top: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.transfer-li {
  font-size: 20rpx;
  color: #2563eb;
  font-weight: 800;
}

.transfer-info-accept {
  display: block;
  margin-top: 12rpx;
  font-size: 20rpx;
  color: #2563eb;
  font-weight: 800;
  line-height: 1.5;
}

/* Share modal */
.share-project-top {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-bottom: 16rpx;
}

.share-project-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--c-shadow-soft);
}

.share-project-avatar-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.share-project-main {
  flex: 1;
  min-width: 0;
}

.share-project-name {
  display: block;
  font-size: 24rpx;
  font-weight: 900;
  color: var(--c-ink);
}

.share-project-id {
  display: block;
  margin-top: 8rpx;
  font-size: 20rpx;
  color: var(--c-muted);
}

.field-textarea {
  width: 100%;
  min-height: 120rpx;
  border: 1rpx solid var(--c-border);
  background: rgba(246, 242, 238, 0.9);
  padding: 16rpx 20rpx;
  box-sizing: border-box;
  font-size: 22rpx;
}

</style> -->
