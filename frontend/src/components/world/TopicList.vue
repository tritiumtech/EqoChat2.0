<template>
  <z-paging
    ref="pagingRef"
    :auto="true"
    :refresher-enabled="true"
    :loading-more-enabled="hasMore"
    @query="queryTopics"
    class="paging-wrap"
  >
    <template #top>
      <PageHeader
        :title="t('page.world.title')"
        action-icon="⌕"
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
    </template>
    <view class="topic-list-content">
      <WorldTopicCard
        v-for="topic in topics"
        :key="topic.id"
        :topic="topic"
        :posts-label="t('page.world.posts')"
        :followers-label="t('page.world.followers')"
        :follow-text="t('page.world.follow')"
        :followed-text="t('page.world.followed')"
        @open="() => $emit('open-topic', topic)"
        @toggle-follow="() => $emit('toggle-follow', topic)"
      />
    </view>
    <template #bottom>
      <view style="height: 100rpx;"></view>
    </template>
  </z-paging>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { worldApi, type WorldTopic } from '@/api/modules/world'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import WorldTopicCard from './WorldTopicCard.vue'

const { t } = useI18nWithFormat()

const props = defineProps<{
  tabOptions: { name: string; value: string }[]
  activeTabIndex: number
}>()

const emit = defineEmits<{
  (e: 'tab-change', index: number): void
  (e: 'open-topic', topic: WorldTopic): void
  (e: 'toggle-follow', topic: WorldTopic): void
}>()

const pagingRef = ref<any>(null)
const topics = ref<WorldTopic[]>([])
const cursorId = ref<number | string | undefined>(undefined)
const hasMore = ref(true)

const queryTopics = async (pageNo: number, pageSize: number) => {
  try {
    const isRefresh = pageNo === 1
    const reqCursorId = isRefresh ? undefined : cursorId.value
    const res = await worldApi.listTopics({ cursorId: reqCursorId, limit: pageSize })
    const list = res.items
    
    if (isRefresh) {
      topics.value = list
    } else {
      topics.value = [...topics.value, ...list]
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

.topic-list-content {
  padding: 20rpx 24rpx 0;
}
</style>
