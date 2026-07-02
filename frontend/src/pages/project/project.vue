<template>
  <view class="page">
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
  </view>

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


  <!-- Create Project Modal -->
  <CreateProjectModal
    :open="showCreateModal"
    :createName="createName"
    :createBidStr="createBidStr"
    :canCreateProject="canCreateProject"
    :depositHintText="tf('page.project.modals.create.deposit_hint', { deposit: calculateDeposit() })"
    @close="closeCreateModal"
    @submit="submitCreateProject"
    @update:createName="createName = $event"
    @update:createBidStr="createBidStr = $event"
  />


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


  <!-- Transfer Modal -->
  <TransferOwnershipModal
    :open="showTransferModal"
    :member="selectedTransferMember"
    :getMemberInitial="getMemberInitial"
    @close="closeTransferModal"
    @submit="submitTransferOwnership"
  />


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

  <!-- Create Task Modal -->
  <CreateTaskModal
    :open="showCreateTaskModal"
    :projectId="selectedProjectId"
    :creating="creatingTask"
    :members="projectDetail?.members || []"
    @close="closeCreateTaskModal"
    @submit="submitCreateTask"
  />

</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import FgTabbar from '@/tabbar/index.vue'
import ProjectListView from './components/ProjectListView.vue'
import ProjectDetailView from './components/ProjectDetailView.vue'
import ProjectSidebarDrawer from './components/ProjectSidebarDrawer.vue'
import CreateProjectModal from './components/modals/CreateProjectModal.vue'
import CreateTaskModal from './components/modals/CreateTaskModal.vue'
import UpdateBidModal from './components/modals/UpdateBidModal.vue'
import TransferOwnershipModal from './components/modals/TransferOwnershipModal.vue'
import ShareProjectModal from './components/modals/ShareProjectModal.vue'
import { useUserStore } from '@/store/modules/user'
import { useActiveSubjectStore } from '@/store/modules/activeSubject'
import {
  projectApi,
  type ProjectDetail,
  type ProjectFile,
  type ProjectMember,
  type ProjectPayment,
  type ProjectBusinessSubjectType,
  type ProjectSummary,
  type ProjectTask,
} from '@/api/modules/project'

const { t, tf } = useI18nWithFormat()
const userStore = useUserStore()
const activeSubjectStore = useActiveSubjectStore()

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
const selectedTransferMemberKey = ref<string | null>(null)

const showShareModal = ref(false)
const shareUrl = ref('')
const shareMessage = ref('')
const shareLoading = ref(false)

const showCreateTaskModal = ref(false)
const creatingTask = ref(false)
const loadedSubjectKey = ref('')
let projectLoadSeq = 0

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
  const targetKey = selectedTransferMemberKey.value
  if (!targetKey) return null
  return projectDetail.value.members.find((m) => memberSubjectKey(m) === targetKey) || null
})

const currentUserIsOwner = computed(() => {
  if (!projectDetail.value) return false
  const subject = activeSubjectStore.currentSubject
  if (!subject) return false
  return Number(projectDetail.value.ownerSubjectId) === Number(subject.subjectId)
    && String(projectDetail.value.ownerSubjectType || '').toUpperCase() === subject.subjectType
})

const currentSubjectKey = computed(() => {
  const subject = activeSubjectStore.currentSubject
  return subject ? `${subject.subjectType}:${subject.subjectId}` : ''
})

const isBusinessSubjectType = (type: unknown): type is ProjectBusinessSubjectType => type === 'HUMAN' || type === 'AGENT'

const memberSubjectKey = (member: ProjectMember) => `${member.memberSubjectType}:${member.memberSubjectId}`

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
    const actor = completedTask.assigneeDisplayName || ''
    items.push({
      id: 'ra_task_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(124, 58, 237, 0.95), rgba(99, 102, 241, 0.85))',
      text: tf('page.project.recent_activity.completed_task', { actor, title: completedTask.title || '-' }),
      time: t('page.project.recent_activity.time_2h'),
    })
  }

  const firstFile = sidebarFiles.value[0]
  if (firstFile) {
    const actor = firstFile.uploaderDisplayName || ''
    items.push({
      id: 'ra_file_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(59, 130, 246, 0.85))',
      text: tf('page.project.recent_activity.added_file', { actor, fileName: firstFile.name || '-' }),
      time: t('page.project.recent_activity.time_5h'),
    })
  }

  const firstPayment = sidebarPayments.value[0]
  if (firstPayment) {
    const actor = firstPayment.recipientDisplayName || ''
    items.push({
      id: 'ra_payment_1',
      initial: getMemberInitial(actor),
      avatarGradient: 'linear-gradient(135deg, rgba(220, 38, 38, 0.95), rgba(239, 68, 68, 0.85))',
      text: tf('page.project.recent_activity.optimized_performance', { actor }),
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
  if (safe < 1000) {
    const decimals = Number.isInteger(safe) ? 0 : 2
    return `$${safe.toFixed(decimals)}`
  }
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

function onQuickAction(action: 'new_task' | 'invite') {
  if (action === 'new_task') {
    openCreateTaskModal()
  } else {
    if (!selectedProjectId.value) {
      uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
      return
    }
    openShareModal()
  }
}

async function onApproveBidUpdate() {
  if (selectedProjectId.value == null || !pendingBidUpdate.value) {
    uni.showToast({ title: t('page.project.toasts.no_pending_bid_update'), icon: 'none' })
    return
  }
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  const nextBid = Math.round(Number(pendingBidUpdate.value.newBid))
  if (!Number.isFinite(nextBid) || nextBid <= 0) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    await projectApi.requestBidUpdate(selectedProjectId.value, {
      newBid: nextBid,
      ...activeSubjectStore.projectActorParams(),
    })
    await loadProjectAll(selectedProjectId.value)
    uni.showToast({ title: t('page.project.toasts.update_success'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function onRejectBidUpdate() {
  if (selectedProjectId.value == null || !pendingBidUpdate.value) {
    uni.showToast({ title: t('page.project.toasts.no_pending_bid_update'), icon: 'none' })
    return
  }
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  const currentBid = Math.round(Number(projectDetail.value?.bid))
  if (!Number.isFinite(currentBid) || currentBid <= 0) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    await projectApi.requestBidUpdate(selectedProjectId.value, {
      newBid: currentBid,
      ...activeSubjectStore.projectActorParams(),
    })
    await loadProjectAll(selectedProjectId.value)
    uni.showToast({ title: t('page.project.toasts.bid_change_rejected'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    uni.hideLoading()
  }
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
    await activeSubjectStore.ensureLoaded()
    projects.value = await projectApi.listMyProjects(activeSubjectStore.projectViewerParams())
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
    projects.value = []
  } finally {
    loading.value = false
  }
}

async function loadProjectAll(id: number) {
  const requestSeq = ++projectLoadSeq
  sidebarTab.value = 'tasks'
  await activeSubjectStore.ensureLoaded()
  const viewer = activeSubjectStore.projectViewerParams()
  const requestedSubjectKey = currentSubjectKey.value
  const [detail, tasks, payments, files] = await Promise.all([
    projectApi.getProjectDetail(id, viewer),
    projectApi.listSidebarTasks(id, viewer),
    projectApi.listSidebarPayments(id, viewer),
    projectApi.listSidebarFiles(id, viewer),
  ])
  if (requestSeq !== projectLoadSeq || requestedSubjectKey !== currentSubjectKey.value) {
    return
  }
  projectDetail.value = detail
  sidebarTasks.value = tasks
  sidebarPayments.value = payments
  sidebarFiles.value = files
  loadedSubjectKey.value = requestedSubjectKey
  selectedTransferMemberKey.value = detail?.members?.[0] ? memberSubjectKey(detail.members[0]) : null
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
  selectedTransferMemberKey.value = null
}

function closeShareModal() {
  showShareModal.value = false
}

function openCreateTaskModal() {
  showCreateTaskModal.value = true
}

function closeCreateTaskModal() {
  showCreateTaskModal.value = false
}

async function submitCreateTask(payload: {
  title: string
  value: number
  deadline: string
  priority: string
  assigneeSubjectId: number
  assigneeSubjectType: ProjectBusinessSubjectType
}) {
  if (!selectedProjectId.value) return
  creatingTask.value = true
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    await projectApi.createTask(selectedProjectId.value, {
      title: payload.title,
      value: payload.value,
      deadline: payload.deadline,
      priority: payload.priority as 'low' | 'medium' | 'high',
      assigneeSubjectId: payload.assigneeSubjectId,
      assigneeSubjectType: payload.assigneeSubjectType,
      ...activeSubjectStore.projectActorParams(),
    })
    showCreateTaskModal.value = false
    sidebarTasks.value = await projectApi.listSidebarTasks(
      selectedProjectId.value,
      activeSubjectStore.projectViewerParams(),
    )
    uni.showToast({ title: t('page.project.toasts.task_created'), icon: 'none' })
  } catch (err: any) {
    uni.showToast({ title: err?.message || t('toast.load_failed'), icon: 'none' })
  } finally {
    creatingTask.value = false
    uni.hideLoading()
  }
}

function calculateDeposit() {
  const bid = Number(createBidStr.value)
  if (!Number.isFinite(bid) || bid <= 0) return '$0'
  const dep = (bid * 0.1).toFixed(2)
  return `$${dep}`
}

async function submitCreateProject() {
  if (!canCreateProject.value) return
  await activeSubjectStore.ensureLoaded()
  const owner = activeSubjectStore.projectOwnerParams()
  if (!owner) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    const bid = Math.round(Number(createBidStr.value))
    const created = await projectApi.createProject({
      name: createName.value.trim(),
      bid,
      ...owner,
    })
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
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  updateBidStr.value = String(projectDetail.value.bid ?? 0)
  showUpdateBidModal.value = true
}

async function submitUpdateBid() {
  if (!canUpdateBid.value || selectedProjectId.value == null) return
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    const newBid = Math.round(Number(updateBidStr.value))
    await projectApi.requestBidUpdate(selectedProjectId.value, {
      newBid,
      ...activeSubjectStore.projectActorParams(),
    })
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
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  if (member) {
    selectedTransferMemberKey.value = memberSubjectKey(member)
  } else {
    selectedTransferMemberKey.value = selectedTransferMemberKey.value
      ?? (projectDetail.value.members?.[0] ? memberSubjectKey(projectDetail.value.members[0]) : null)
  }
  showTransferModal.value = true
}

async function submitTransferOwnership() {
  if (!selectedProjectId.value || !selectedTransferMember.value) return
  if (!currentUserIsOwner.value) {
    uni.showToast({ title: t('page.project.toasts.owner_only'), icon: 'none' })
    return
  }
  const newOwnerSubjectId = Number(selectedTransferMember.value.memberSubjectId)
  if (!Number.isFinite(newOwnerSubjectId) || newOwnerSubjectId <= 0) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  const newOwnerSubjectType = selectedTransferMember.value.memberSubjectType
  if (!isBusinessSubjectType(newOwnerSubjectType)) {
    uni.showToast({ title: t('toast.load_failed'), icon: 'none' })
    return
  }
  try {
    uni.showLoading({ title: t('common.loading'), mask: true })
    await projectApi.transferOwnership(selectedProjectId.value, {
      newOwnerSubjectId,
      newOwnerSubjectType,
      ...activeSubjectStore.projectActorParams(),
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
    await activeSubjectStore.ensureLoaded()
    const resp = await projectApi.shareLink(projectId, activeSubjectStore.projectViewerParams())
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
  projectLoadSeq += 1
  selectedProjectId.value = null
  projectDetail.value = null
  sidebarTasks.value = []
  sidebarPayments.value = []
  sidebarFiles.value = []
  loadedSubjectKey.value = ''
  showSidebar.value = false
}

watch(currentSubjectKey, async (next, prev) => {
  if (!prev || next === prev || selectedProjectId.value == null) return
  projectLoadSeq += 1
  projectDetail.value = null
  sidebarTasks.value = []
  sidebarPayments.value = []
  sidebarFiles.value = []
  loadedSubjectKey.value = ''
  showSidebar.value = false
  try {
    await loadProjectAll(selectedProjectId.value)
  } catch {
    selectedProjectId.value = null
    projectDetail.value = null
    sidebarTasks.value = []
    sidebarPayments.value = []
    sidebarFiles.value = []
    loadedSubjectKey.value = ''
  }
})

onLoad((query) => {
  if (!userStore.isLoggedIn) return
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
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  await activeSubjectStore.ensureLoaded()
  if (!activeSubjectStore.currentSubject) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  await loadProjects()
  if (selectedProjectId.value != null && loadedSubjectKey.value !== currentSubjectKey.value) {
    try {
      await loadProjectAll(selectedProjectId.value)
    } catch {
      selectedProjectId.value = null
      projectDetail.value = null
      sidebarTasks.value = []
      sidebarPayments.value = []
      sidebarFiles.value = []
      loadedSubjectKey.value = ''
    }
    return
  }
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
