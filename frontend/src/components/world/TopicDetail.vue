<template>
  <z-paging
    ref="pagingRef"
    :auto="true"
    :refresher-enabled="true"
    :loading-more-enabled="hasMore"
    @query="queryTopicPosts"
    class="paging-wrap"
  >
    <template #top>
      <PageHeader
        :title="'#' + topicName"
        :subtitle="topicMeta ? `${topicMeta.posts} ${t('page.world.posts')} · ${topicMeta.followers} ${t('page.world.followers')}` : ''"
        :has-back-row="true"
        @back-click="$emit('back')"
      />
    </template>
    <WorldPostCard
      v-for="post in posts"
      :key="post.id"
      :post="post"
      @open-detail="$emit('open-detail', $event)"
      @upvote="$emit('upvote', $event)"
      @reply="$emit('reply', $event)"
      @share="$emit('share', $event)"
    />
    <template #bottom>
      <view style="height: 100rpx;"></view>
    </template>
  </z-paging>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { worldApi, type WorldPost, type WorldTopic } from '@/api/modules/world'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'
import WorldPostCard from './WorldPostCard.vue'

const { t } = useI18nWithFormat()

const props = defineProps<{
  topicName: string
  topicList: WorldTopic[]
  normalizePost: (post: WorldPost) => WorldPost
}>()

const emit = defineEmits<{
  back: []
  'open-detail': [post: WorldPost]
  upvote: [post: WorldPost]
  reply: [post: WorldPost]
  share: [post: WorldPost]
}>()

const pagingRef = ref<any>(null)
const posts = ref<WorldPost[]>([])
const cursorId = ref<number | string | undefined>(undefined)
const hasMore = ref(true)

const topicMeta = computed(() => props.topicList.find((x) => x.name === props.topicName))

const queryTopicPosts = async (pageNo: number, pageSize: number) => {
  if (!props.topicName) {
    pagingRef.value?.complete([], false)
    return
  }
  try {
    const isRefresh = pageNo === 1
    const reqCursorId = isRefresh ? undefined : cursorId.value
    const res = await worldApi.listTopicPosts(props.topicName, { cursorId: reqCursorId, limit: pageSize })
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

const reload = () => {
  pagingRef.value?.reload()
}

watch(() => props.topicName, () => {
  reload()
})

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
</style>
