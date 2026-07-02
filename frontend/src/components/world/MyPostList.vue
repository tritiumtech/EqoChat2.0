<template>
  <z-paging
    ref="pagingRef"
    :auto="true"
    :refresher-enabled="true"
    :loading-more-enabled="hasMore"
    @query="queryMyPosts"
    class="paging-wrap"
  >
    <template #top>
      <PageHeader
        :title="t('page.world.title')"
        action-icon="S"
        action-variant="bordered"
        action-size="md"
        :has-tabs="true"
        @action-click="onSearchTap"
      >
        <template #tabs>
          <u-tabs
            :list="tabOptions"
            :current="activeTabIndex"
            key-name="name"
            :scrollable="false"
            :line-color="'#030213'"
            :active-style="{ color: '#030213', fontWeight: 600 }"
            :inactive-style="{ color: '#717182' }"
            :item-style="{ height: '88rpx' }"
            @change="(e: any) => onTabChange(e.index)"
          />
        </template>
      </PageHeader>
      <TimelineHeader
        :title="t('page.world.timeline_title')"
        :total-posts="posts.length"
        :show-index="showTimelineIndex"
        :timeline-structure="timelineStructure"
        :expanded-years="expandedYears"
        :expanded-months="expandedMonths"
        @update:show-index="showTimelineIndex = $event"
        @toggle-year="toggleYear"
        @toggle-month="toggleMonth"
      />
    </template>

    <view class="my-post-list-content">
      <template v-if="postsByMonth.length > 0">
        <MonthGroup
          v-for="(monthGroup, groupIndex) in postsByMonth"
          :key="`${monthGroup.year}-${monthGroup.month}`"
          :month-group="monthGroup"
          :group-index="groupIndex"
          :all-groups="postsByMonth"
          @post-click="$emit('open-detail', $event)"
          @post-upvote="$emit('upvote', $event)"
          @post-reply="$emit('reply', $event)"
          @post-share="$emit('share', $event)"
        />
        <TimelineEnd :label="t('page.world.timeline_end')" />
      </template>
    </view>
    <template #bottom>
      <view class="tabbar-spacer"></view>
    </template>
  </z-paging>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { worldApi, type WorldPost, type WorldSubjectParams } from '@/api/modules/world'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import TimelineHeader from './TimelineHeader.vue'
import MonthGroup from './MonthGroup.vue'
import TimelineEnd from './TimelineEnd.vue'

const { t } = useI18nWithFormat()

const props = defineProps<{
  tabOptions: { name: string; value: string }[]
  activeTabIndex: number
  normalizePost: (post: WorldPost) => WorldPost
  viewer?: WorldSubjectParams
}>()

const emit = defineEmits<{
  (e: 'tab-change', index: number): void
  (e: 'open-detail', post: WorldPost): void
  (e: 'upvote', post: WorldPost): void
  (e: 'reply', post: WorldPost): void
  (e: 'share', post: WorldPost): void
}>()

const pagingRef = ref<any>(null)
const posts = ref<WorldPost[]>([])
const cursorId = ref<number | string | undefined>(undefined)
const hasMore = ref(true)

// Timeline state
const showTimelineIndex = ref(false)
const expandedYears = ref<Set<string>>(new Set())
const expandedMonths = ref<Set<string>>(new Set())

// Timeline computed properties
const timelineStructure = computed(() => {
  const structure: { [year: string]: { [month: string]: string[] } } = {}
  posts.value.forEach((post) => {
    const date = new Date(post.timestamp)
    const year = String(date.getFullYear())
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    if (!structure[year]) structure[year] = {}
    if (!structure[year][month]) structure[year][month] = []
    if (!structure[year][month].includes(day)) structure[year][month].push(day)
  })
  return structure
})

const postsByMonth = computed(() => {
  const groups: { [key: string]: { year: string; month: string; day: number; posts: WorldPost[] } } = {}
  posts.value.forEach((post) => {
    const date = new Date(post.timestamp)
    const year = String(date.getFullYear())
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = date.getDate()
    const key = `${year}-${month}`
    if (!groups[key]) {
      groups[key] = { year, month, day, posts: [] }
    }
    groups[key].posts.push(post)
  })
  return Object.values(groups).sort((a, b) => {
    if (a.year !== b.year) return Number(b.year) - Number(a.year)
    return Number(b.month) - Number(a.month)
  })
})

const queryMyPosts = async (pageNo: number, pageSize: number) => {
  try {
    const isRefresh = pageNo === 1
    const reqCursorId = isRefresh ? undefined : cursorId.value
    const res = await worldApi.listMyPosts({
      cursorId: reqCursorId,
      limit: pageSize,
      viewerSubjectId: props.viewer?.subjectId,
      viewerSubjectType: props.viewer?.subjectType,
    })
    const list = res.items.map(props.normalizePost)

    if (isRefresh) {
      posts.value = list
    } else {
      posts.value = [...posts.value, ...list]
    }

    hasMore.value = res.hasMore
    if (res.nextCursorId) {
      cursorId.value = res.nextCursorId
    }

    pagingRef.value?.complete(list, res.hasMore)
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
    pagingRef.value?.complete(false, false)
  }
}

const onTabChange = (index: number) => {
  emit('tab-change', index)
}

const onSearchTap = () => {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
}

const toggleYear = (year: string) => {
  const newSet = new Set(expandedYears.value)
  if (newSet.has(year)) {
    newSet.delete(year)
  } else {
    newSet.add(year)
  }
  expandedYears.value = newSet
}

const toggleMonth = (month: string) => {
  const newSet = new Set(expandedMonths.value)
  if (newSet.has(month)) {
    newSet.delete(month)
  } else {
    newSet.add(month)
  }
  expandedMonths.value = newSet
}

const reload = () => {
  pagingRef.value?.reload()
}

defineExpose({
  reload,
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.paging-wrap {
  flex: 1;
  min-height: 0;
}

.my-post-list-content {
  width: 100%;
  max-width: var(--page-content-max);
  margin: 0 auto;
  padding: 20rpx 24rpx 0;
  box-sizing: border-box;
}

.tabbar-spacer {
  height: var(--page-pad-bottom-tabbar);
}
</style>
