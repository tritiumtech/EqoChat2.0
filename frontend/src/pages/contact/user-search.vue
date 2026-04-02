<template>
  <view class="page">
    <!-- 页面头部 -->
    <PageHeader
      :title="t('page.contact.search_user')"
      show-back-icon
      @back-click="goBack"
    />

    <!-- 搜索输入区 -->
    <view class="search-section">
      <view class="search-input-wrap">
        <input
          v-model="searchKeyword"
          class="search-input"
          :placeholder="t('page.contact.search_placeholder')"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <view v-if="searchKeyword" class="clear-btn" @click="clearSearch">
          <text class="clear-icon">✕</text>
        </view>
      </view>
      <button class="search-btn" :disabled="!canSearch" @click="handleSearch">
        <text class="search-btn-text">{{ t('action.search') }}</text>
      </button>
    </view>

    <!-- 搜索提示 -->
    <view class="search-tips">
      <text class="tips-text">{{ t('page.contact.search_tips') }}</text>
    </view>

    <!-- 搜索结果 -->
    <view v-if="hasSearched" class="result-section">
      <!-- 加载中 -->
      <view v-if="loading" class="state-loading">
        <u-loading-icon mode="circle" size="28" color="#030213" />
        <text class="state-text">{{ t('common.loading') }}</text>
      </view>

      <!-- 未找到 -->
      <view v-else-if="!searchResult" class="state-empty">
        <view class="empty-icon-wrap">
          <u-icon name="search" :size="48" color="#717182" />
        </view>
        <text class="empty-title">{{ t('toast.user_not_found') }}</text>
        <text class="empty-sub">{{ t('page.contact.search_tips') }}</text>
      </view>

      <!-- 搜索结果 -->
      <view v-else class="result-card" @click="goToUserProfile(searchResult.id)">
        <view class="user-avatar-wrap">
          <image
            v-if="searchResult.avatarUrl"
            class="user-avatar"
            :src="searchResult.avatarUrl"
            mode="aspectFill"
          />
          <view v-else class="user-avatar avatar-gradient" :style="avatarStyle(searchResult)">
            <text class="avatar-text">{{ (searchResult.nickname || '?').slice(0, 1) }}</text>
          </view>
        </view>
        <view class="user-info">
          <text class="user-name">{{ searchResult.nickname }}</text>
          <text class="user-account">{{ t('page.contact.user_id') }}: {{ searchResult.id }}</text>
          <text v-if="searchResult.bio" class="user-bio">{{ searchResult.bio }}</text>
          <view v-if="searchResult.isFriend" class="friend-badge">
            <text class="friend-badge-text">{{ t('page.world.friend') }}</text>
          </view>
        </view>
        <text class="result-arrow">›</text>
      </view>
    </view>

    <!-- 搜索历史 -->
    <view v-if="!hasSearched && searchHistory.length > 0" class="history-section">
      <view class="history-header">
        <text class="history-title">{{ t('page.contact.search_history') }}</text>
        <text class="history-clear" @click="clearHistory">{{ t('action.clear') }}</text>
      </view>
      <view class="history-list">
        <view
          v-for="(keyword, index) in searchHistory"
          :key="index"
          class="history-item"
          @click="quickSearch(keyword)"
        >
          <text class="history-icon">🕐</text>
          <text class="history-text">{{ keyword }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useI18nWithFormat } from '@/composables/useI18nWithFormat'
import { userApi, type UserSearchResult } from '@/api/modules/user'
import { useUserStore } from '@/store/modules/user'
import { getApiErrorMessage } from '@/utils/request'
import PageHeader from '@/components/PageHeader.vue'

const { t } = useI18nWithFormat()
const userStore = useUserStore()

// 搜索关键词
const searchKeyword = ref('')
const loading = ref(false)
const hasSearched = ref(false)
const searchResult = ref<UserSearchResult | null>(null)
const searchHistory = ref<string[]>([])

// 是否可以搜索（输入不为空即可）
const canSearch = computed(() => {
  return searchKeyword.value.trim().length > 0
})

// 头像样式
const avatarStyle = (user: UserSearchResult) => {
  const hues = ['#14B8A6', '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1', '#EF4444']
  let h = 0
  const s = user.nickname || String(user.id)
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  const c = hues[Math.abs(h) % hues.length]!
  return { background: `linear-gradient(135deg, ${c}f0, ${c}c0)` }
}

// 返回上一页
const goBack = () => {
  uni.navigateBack()
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  hasSearched.value = false
  searchResult.value = null
}

// 执行搜索
const handleSearch = async () => {
  if (!canSearch.value) {
    uni.showToast({ title: t('toast.invalid_account'), icon: 'none' })
    return
  }

  const keyword = searchKeyword.value.trim()
  loading.value = true
  hasSearched.value = true

  try {
    const result = await userApi.searchUserByAccount({
      keyword: keyword
    })
    
    searchResult.value = result
    // 添加到搜索历史
    addToHistory(keyword)
  } catch (err: any) {
    uni.showToast({ title: getApiErrorMessage(err, t('toast.load_failed')), icon: 'none' })
    searchResult.value = null
  } finally {
    loading.value = false
  }
}

// 添加到搜索历史
const addToHistory = (keyword: string) => {
  // 去重并限制数量
  searchHistory.value = [keyword, ...searchHistory.value.filter(k => k !== keyword)].slice(0, 10)
  
  // 保存到本地存储
  uni.setStorageSync('user_search_history', JSON.stringify(searchHistory.value))
}

// 快速搜索
const quickSearch = (keyword: string) => {
  searchKeyword.value = keyword
  handleSearch()
}

// 清空历史
const clearHistory = () => {
  searchHistory.value = []
  uni.removeStorageSync('user_search_history')
}

// 跳转到用户资料页
const goToUserProfile = (userId: number) => {
  uni.navigateTo({ url: `/pages/contact/user-profile?id=${userId}` })
}

// 加载搜索历史
const loadSearchHistory = () => {
  try {
    const history = uni.getStorageSync('user_search_history')
    if (history) {
      searchHistory.value = JSON.parse(history)
    }
  } catch {
    searchHistory.value = []
  }
}

onShow(() => {
  if (!userStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/auth/login' })
    return
  }
  uni.setNavigationBarTitle({ title: t('page.contact.search_user') })
  loadSearchHistory()
})
</script>

<style scoped>
@import "@/styles/tokens.css";

.page {
  min-height: 100vh;
  background: #ffffff;
  box-sizing: border-box;
}

/* 搜索输入区 */
.search-section {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 32rpx;
  background: #ffffff;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f8f8fb;
  border: 2rpx solid rgba(3, 2, 19, 0.12);
  border-radius: var(--radius-lg);
  padding: 0 20rpx;
  height: 88rpx;
}

.search-input {
  flex: 1;
  height: 88rpx;
  background: transparent;
  font-size: 30rpx;
  color: var(--c-ink);
}

.clear-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn:active {
  opacity: 0.7;
}

.clear-icon {
  font-size: 24rpx;
  color: var(--c-muted);
}

.search-btn {
  height: 88rpx;
  padding: 0 32rpx;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--c-primary) 0%, var(--c-primary-deep) 100%);
  border: none;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn[disabled] {
  opacity: 0.45;
}

.search-btn-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #fff;
}

/* 搜索提示 */
.search-tips {
  padding: 0 32rpx 24rpx;
  background: #ffffff;
}

.tips-text {
  font-size: 24rpx;
  color: var(--c-muted);
}

/* 结果区域 */
.result-section {
  padding: 24rpx 32rpx;
}

.state-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  padding: 80rpx 0;
}

.state-text {
  font-size: 26rpx;
  color: var(--c-muted);
}

.state-empty {
  text-align: center;
  padding: 80rpx 32rpx;
}

.empty-icon-wrap {
  width: 128rpx;
  height: 128rpx;
  margin: 0 auto 24rpx;
  border-radius: 50%;
  background: rgba(243, 243, 245, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--c-ink);
  display: block;
}

.empty-sub {
  font-size: 24rpx;
  color: var(--c-muted);
  margin-top: 12rpx;
  display: block;
}

/* 搜索结果卡片 */
.result-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  box-shadow: var(--c-shadow-soft);
}

.result-card:active {
  background: rgba(3, 2, 19, 0.03);
}

.user-avatar-wrap {
  flex-shrink: 0;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-gradient {
  box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.06);
}

.avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--c-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-account {
  font-size: 24rpx;
  color: var(--c-muted);
}

.user-bio {
  font-size: 24rpx;
  color: var(--c-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-badge {
  display: inline-flex;
  align-self: flex-start;
  padding: 4rpx 12rpx;
  border-radius: var(--radius-pill);
  background: rgba(16, 185, 129, 0.12);
  border: 1rpx solid rgba(16, 185, 129, 0.25);
}

.friend-badge-text {
  font-size: 20rpx;
  color: #059669;
  font-weight: 600;
}

.result-arrow {
  font-size: 36rpx;
  color: var(--c-muted);
}

/* 搜索历史 */
.history-section {
  padding: 24rpx 32rpx;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.history-title {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--c-ink);
}

.history-clear {
  font-size: 24rpx;
  color: var(--c-primary);
}

.history-clear:active {
  opacity: 0.7;
}

.history-list {
  display: flex;
  flex-direction: column;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
}

.history-item:last-child {
  border-bottom: none;
}

.history-item:active {
  opacity: 0.7;
}

.history-icon {
  font-size: 28rpx;
}

.history-text {
  flex: 1;
  font-size: 28rpx;
  color: var(--c-ink);
}
</style>
