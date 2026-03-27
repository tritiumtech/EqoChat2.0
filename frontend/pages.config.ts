import { defineUniPages } from '@uni-helper/vite-plugin-uni-pages'

export default defineUniPages({
  globalStyle: {
    navigationBarTextStyle: 'black',
    navigationBarTitleText: 'EqoChat',
    navigationBarBackgroundColor: '#F8F8F8',
    backgroundColor: '#F8F8F8',
  },
  pages: [
    {
      path: 'pages/chat/chat-list',
      style: {
        navigationBarTitleText: '聊天',
      },
    },
    {
      path: 'pages/chat/chat-room',
      style: {
        navigationBarTitleText: '',
        navigationStyle: 'custom',
      },
    },
    {
      path: 'pages/contact/contact-list',
      style: {
        navigationBarTitleText: '联系人',
      },
    },
    {
      path: 'pages/contact/contact-detail',
      style: {
        navigationBarTitleText: '好友详情',
      },
    },
    {
      path: 'pages/world/world',
      style: {
        navigationBarTitleText: '世界',
      },
    },
    {
      path: 'pages/profile/profile',
      style: {
        navigationBarTitleText: '我的',
      },
    },
    {
      path: 'pages/auth/login',
      style: {
        navigationBarTitleText: '登录',
      },
    },
    {
      path: 'pages/auth/register',
      style: {
        navigationBarTitleText: '注册',
      },
    },
  ],
})

