<template>
  <view class="list-wrap">
    <PageHeader
      :title="t('page.project.title')"
      :subtitle="t('page.project.subtitle')"
      action-icon="＋"
      action-variant="bordered"
      action-size="md"
      @action-click="openCreateModal"
    />

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
            <text class="meta-left">
              {{ p.humans }}{{ t('page.project.human_abbr') }} + {{ p.agents }}{{ t('page.project.ai_abbr') }}
            </text>
            <text class="meta-right">{{ p.revenue }}</text>
          </view>

          <view class="project-submeta">
            <text>{{ t('page.project.bid_prefix') }}{{ p.bid }}</text>
            <text>
              {{ t('page.project.deposit_prefix') }}{{
                p.depositPaid ? t('page.project.deposit_paid') : t('page.project.deposit_pending')
              }}
            </text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHeader from '@/components/PageHeader.vue'
import type { ProjectSummary } from '@/api/modules/project'

// 小程序端默认样式隔离：让页面样式可作用到子组件
defineOptions({
  options: {
    styleIsolation: 'shared',
  },
})

type Props = {
  loading: boolean
  projects: ProjectSummary[]
  openCreateModal: () => void
  openProject: (id: number) => void
  statusClass: (status: any) => string
  statusText: (status: any) => string
}

const props = defineProps<Props>()
const { t } = useI18n({ useScope: 'global' })

const { loading, projects } = toRefs(props)

// 用于模板直接绑定函数，保证调用上下文正确
const openCreateModal = props.openCreateModal
const openProject = props.openProject
const statusClass = props.statusClass
const statusText = props.statusText
</script>

<style scoped src="../project.styles.css"></style>

