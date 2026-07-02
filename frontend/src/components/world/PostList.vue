<template>
  <z-paging
    ref="pagingRef"
    :auto="true"
    :refresher-enabled="true"
    :loading-more-enabled="hasMore"
    @query="queryPosts"
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
      <view class="sort-row">
        <view class="sort-row-inner">
          <view class="sort-wrap">
            <button class="sort-btn" @click="showSortDropdown = !showSortDropdown">
              <text>{{ sortLabel }}</text>
              <text class="chev">v</text>
            </button>
            <view v-if="showSortDropdown" class="sort-menu">
              <view
                v-for="opt in sortOptions"
                :key="opt.value"
                class="sort-item"
                :class="{ current: sortBy === opt.value }"
                @click="onSortSelect(opt.value)"
              >
                <text>{{ opt.label }}</text>
              </view>
            </view>
          </view>
          <button class="new-post-btn" @click="$emit('new-post')">
            <text class="new-post-plus">+</text>
            <text>{{ t('page.world.new_post_short') }}</text>
          </button>
        </view>
      </view>
    </template>
    <view class="post-list-content">
      <WorldPostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @open-detail="() => $emit('open-detail', post)"
        @upvote="() => $emit('upvote', post)"
        @reply="() => $emit('reply', post)"
        @share="() => $emit('share', post)"
      />
    </view>
    <template #bottom>
      <view class="tabbar-spacer"></view>
    </template>
  </z-paging>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { worldApi, type WorldPost, type WorldSort, type WorldSubjectParams } from '@/api/modules/world'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import WorldPostCard from './WorldPostCard.vue'

const { t } = useI18nWithFormat()

const props = defineProps<{
  tabOptions: { name: string; value: string }[]
  activeTabIndex: number
  sortOptions: { value: WorldSort; label: string }[]
  normalizePost: (post: WorldPost) => WorldPost
  viewer?: WorldSubjectParams
}>()

const emit = defineEmits<{
  (e: 'tab-change', index: number): void
  (e: 'new-post'): void
  (e: 'open-detail', post: WorldPost): void
  (e: 'upvote', post: WorldPost): void
  (e: 'reply', post: WorldPost): void
  (e: 'share', post: WorldPost): void
}>()

const pagingRef = ref<any>(null)
const posts = ref<WorldPost[]>([])
const sortBy = ref<WorldSort>('friends')
const showSortDropdown = ref(false)
const cursorId = ref<number | string | undefined>(undefined)
const hasMore = ref(true)

const sortLabel = computed(() => props.sortOptions.find((o) => o.value === sortBy.value)?.label ?? '')

const queryPosts = async (pageNo: number, pageSize: number) => {
  try {
    const isRefresh = pageNo === 1
    const reqCursorId = isRefresh ? undefined : cursorId.value
    const res = await worldApi.listPosts({
      sort: sortBy.value,
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

const onSortSelect = (value: WorldSort) => {
  sortBy.value = value
  showSortDropdown.value = false
  pagingRef.value?.reload()
}

const onSearchTap = () => {
  uni.showToast({ title: t('toast.coming_soon'), icon: 'none' })
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

.sort-row {
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid var(--c-border);
  background: #fff;
}

.sort-row-inner {
  display: flex;
  align-items: center;
  gap: 16rpx;
  width: 100%;
  max-width: var(--page-content-max);
  margin: 0 auto;
}

.sort-wrap {
  position: relative;
  flex: 1;
  min-width: 0;
}

.sort-btn {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 6rpx 24rpx;
  border-radius: var(--radius-md);
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  font-size: 26rpx;
  color: var(--c-ink);
}

.sort-menu {
  position: absolute;
  left: 0;
  top: calc(100% + 8rpx);
  min-width: 280rpx;
  background: var(--c-surface);
  border: 1rpx solid var(--c-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--c-shadow-soft);
  z-index: 30;
  overflow: hidden;
}

.sort-item {
  padding: 22rpx 26rpx;
  font-size: 26rpx;
  color: var(--c-ink);
}

.sort-item.current {
  background: rgba(3, 2, 19, 0.08);
  color: var(--c-primary);
  font-weight: 600;
}

.new-post-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 22rpx;
  border-radius: var(--radius-md);
  background: var(--c-primary);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  border: none;
  box-shadow: var(--shadow-action);
}

.new-post-plus {
  font-size: 28rpx;
  font-weight: 400;
  line-height: 1;
}

.chev {
  font-size: 20rpx;
  color: var(--c-muted);
}

.post-list-content {
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
