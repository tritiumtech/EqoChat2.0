import { defineUniPages } from '@uni-helper/vite-plugin-uni-pages'
import { tabBar } from './src/tabbar/config'

/**
 * 与 `src/pages.json` 保持一致；当前构建以手写 pages.json 为准（避免 UniPages 扫描误注册组件页）。
 * 若日后启用 `vite-plugin-uni-pages`，请配置 exclude 后再以本文件为单一来源。
 */
export default defineUniPages({
  globalStyle: {
    navigationBarTextStyle: 'black',
    navigationBarTitleText: '%pages.global.title%',
    navigationBarBackgroundColor: '#F8F8F8',
    backgroundColor: '#F8F8F8',
  },
  tabBar,
  pages: [
    {
      path: 'pages/index/index',
      type: 'home',
      style: {},
    },
    {
      path: 'pages/auth/login',
      style: {
        navigationBarTitleText: '%pages.login.title%',
      },
    },
    {
      path: 'pages/auth/register',
      style: {
        navigationBarTitleText: '%pages.register.title%',
      },
    },
    {
      path: 'pages/chat/chat-list',
      style: {
		  navigationBarTitleText: '',
		  navigationStyle: 'custom'
      },
    },
    {
      path: 'pages/chat/chat-room',
      style: {
        navigationBarTitleText: '',
        navigationStyle: 'custom',
        'app-plus': {
          softinputMode: 'adjustResize',
        },
      },
    },
    {
      path: 'pages/contact/contact-list',
      style: {
        navigationBarTitleText: '%pages.contact.title%',
      },
    },
    {
      path: 'pages/contact/contact-detail',
      style: {
        navigationBarTitleText: '%pages.contact.detail.title%',
      },
    },
    {
      path: 'pages/world/world',
      style: {
        navigationBarTitleText: '%pages.world.title%',
      },
    },
    {
      path: 'pages/profile/profile',
      style: {
        navigationBarTitleText: '%pages.profile.title%',
      },
    },
    {
      path: 'pages/project/project',
      style: {
        navigationBarTitleText: '%pages.project.title%',
      },
    },
  ],
})
