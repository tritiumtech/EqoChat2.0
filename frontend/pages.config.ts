import { defineUniPages } from '@uni-helper/vite-plugin-uni-pages'

export default defineUniPages({
  globalStyle: {
    navigationBarTextStyle: 'black',
    navigationBarTitleText: '%pages.global.title%',
    navigationBarBackgroundColor: '#F8F8F8',
    backgroundColor: '#F8F8F8',
  },
  pages: [
    {
      path: 'pages/chat/chat-list',
      style: {
        navigationBarTitleText: '%pages.chat.title%',
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
      path: 'pages/project/project',
      style: {
        navigationBarTitleText: '%pages.project.title%',
      },
    },
  ],
})

