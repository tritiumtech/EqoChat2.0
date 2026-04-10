---
name: "z-paging-pagination"
description: "Integrates z-paging component for pull-to-refresh and load-more functionality in UniApp. Invoke when implementing pagination, infinite scroll, or pull-to-refresh features."
---

# z-paging Pagination Integration

## 1. 核心流程

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  z-paging 组件   │────▶│   @query 事件   │────▶│   调用 API      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         ▲                                               │
         │                                               ▼
         │                                      ┌─────────────────┐
         │                                      │  更新本地数组    │
         │                                      └─────────────────┘
         │                                               │
         │                                               ▼
         │                                      ┌─────────────────┐
         └──────────────────────────────────────│  complete()     │
                                                └─────────────────┘
```

## 2. 标准模板

```vue
<template>
  <z-paging
    ref="pagingRef"
    :auto="true"
    :refresher-enabled="true"
    :loading-more-enabled="hasMore"
    @query="queryList"
  >
    <template #top>
      <PageHeader />
    </template>
    
    <view class="content">
      <Card v-for="item in list" :key="item.id" :data="item" />
    </view>
    
    <template #bottom>
      <view style="height: 100rpx;"></view>
    </template>
  </z-paging>
</template>

<script setup>
const list = ref([])
const hasMore = ref(true)
const pagingRef = ref(null)

const queryList = async (pageNo, pageSize) => {
  try {
    const res = await api.getList({ pageNo, pageSize })
    
    if (pageNo === 1) {
      list.value = res.items
    } else {
      list.value = [...list.value, ...res.items]
    }
    
    pagingRef.value?.complete(res.items, res.hasMore)
  } catch (err) {
    pagingRef.value?.complete(false, false)
  }
}

const reload = () => pagingRef.value?.reload()
defineExpose({ reload })
</script>

<style>
.content { padding: 20rpx 24rpx 0; }
</style>
```

## 3. 关键要点

| 要点 | 正确做法 | 错误做法 |
|------|---------|---------|
| **头部** | 放在 `#top` slot | 放在 z-paging 外部 |
| **底部间距** | 使用 `#bottom` slot | 不加间距 |
| **数据更新** | 手动更新数组 + `complete()` | 使用 `v-model` |
| **Tab 切换** | `@change="(e) => onTabChange(e.index)"` | `@change="onTabChange"` |
| **事件传递** | `@click="() => $emit('select', item)"` | `@click="$emit('select', $event)"` |

## 4. 组件拆分

父组件控制刷新：
```vue
<PostList ref="postListRef" />

watch(activeTab, (v) => {
  if (v === 'posts') postListRef.value?.reload()
})
```

子组件暴露方法：
```vue
const reload = () => pagingRef.value?.reload()
defineExpose({ reload })
```

## 5. 后端接口

```java
@Data
public class PageResponse<T> {
    private List<T> items;
    private Boolean hasMore;
    private Long nextCursorId;
}
```
