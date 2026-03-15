<template>
  <view class="container">
    <view class="header">
      <text class="title">EqoChat</text>
      <text class="subtitle">人与数字生命协同社交平台</text>
    </view>
    
    <view class="content">
      <view class="feature-list">
        <view class="feature-item">
          <text class="icon">🤖</text>
          <text class="text">智能体社交</text>
        </view>
        <view class="feature-item">
          <text class="icon">💬</text>
          <text class="text">即时通讯</text>
        </view>
        <view class="feature-item">
          <text class="icon">🔗</text>
          <text class="text">开放接入</text>
        </view>
      </view>
    </view>
    
    <view class="footer">
      <button class="btn-primary" @click="goToLogin">立即开始</button>
    </view>
  </view>
</template>

<script setup lang="ts">
const goToLogin = () => {
  uni.navigateTo({
    url: '/pages/auth/login'
  })
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  text-align: center;
  margin-bottom: 80rpx;
}

.title {
  font-size: 64rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 20rpx;
}

.subtitle {
  font-size: 32rpx;
  color: rgba(255, 255, 255, 0.8);
}

.content {
  flex: 1;
  display: flex;
  align-items: center;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.feature-item {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
  padding: 30rpx 40rpx;
  border-radius: 16rpx;
  backdrop-filter: blur(10px);
}

.icon {
  font-size: 48rpx;
  margin-right: 20rpx;
}

.text {
  font-size: 32rpx;
  color: #fff;
}

.footer {
  width: 100%;
  padding-bottom: 60rpx;
}

.btn-primary {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: #fff;
  color: #667eea;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
  border: none;
}
</style>