<template>
  <view />
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()

onShow(() => {
  // 首页仅作为“登录态分流”的空壳页面
  // - 已登录：进入 tabbar 首页（chat-list）
  // - 未登录：reLaunch 到登录页，避免返回键回到本页
  if (userStore.isLoggedIn) {
    uni.switchTab({ url: '/pages/chat/chat-list' })
    return
  }
  uni.reLaunch({ url: '/pages/auth/login' })
})
</script>

<style scoped>
</style>
