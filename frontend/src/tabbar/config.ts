import type { TabBar } from '@uni-helper/vite-plugin-uni-pages'
import type { CustomTabBarItem, NativeTabBarItem } from './types'

/**
 * tabbar 策略（与 techjewelry 一致）
 * 0 无 | 1 纯原生 | 2 自定义+缓存+hide 原生 | 3 自定义无缓存
 */
export const TABBAR_STRATEGY_MAP = {
  NO_TABBAR: 0,
  NATIVE_TABBAR: 1,
  CUSTOM_TABBAR_WITH_CACHE: 2,
  CUSTOM_TABBAR_WITHOUT_CACHE: 3,
} as const

export type TabbarStrategy = (typeof TABBAR_STRATEGY_MAP)[keyof typeof TABBAR_STRATEGY_MAP]

// 显式标注为“联合类型”，避免被推断成单一字面量导致对比判断恒为 false
export let selectedTabbarStrategy: TabbarStrategy = TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITH_CACHE

export const nativeTabbarList: NativeTabBarItem[] = []

export const customTabbarList: CustomTabBarItem[] = [
  {
    pagePath: 'pages/chat/chat-list',
    nativeTabText: '%pages.chat.title%',
    text: 'nav.chat',
    iconType: 'image',
    icon: '/static/images/chat.png',
    iconActive: '/static/images/chat_selected.png',
  },
  {
    pagePath: 'pages/project/project',
    nativeTabText: '%pages.project.title%',
    text: 'nav.project',
    iconType: 'image',
    icon: '/static/images/project.png',
    iconActive: '/static/images/project_selected.png',
  },
  {
    pagePath: 'pages/world/world',
    nativeTabText: '%pages.world.title%',
    text: 'nav.world',
    iconType: 'image',
    icon: '/static/images/world.png',
    iconActive: '/static/images/world_selected.png',
  },
  {
    pagePath: 'pages/contact/contact-list',
    nativeTabText: '%pages.contact.title%',
    text: 'nav.contacts',
    iconType: 'image',
    icon: '/static/images/contacts.png',
    iconActive: '/static/images/contacts_selected.png',
  },
  {
    pagePath: 'pages/profile/profile',
    nativeTabText: '%pages.profile.title%',
    text: 'nav.me',
    iconType: 'image',
    icon: '/static/images/me.png',
    // TODO: 当前仅有未选中图，先复用同一张，后续补齐 me.png 后替换
    iconActive: '/static/images/me_selected.png',
  },
]

export const tabbarCacheEnable = [
  TABBAR_STRATEGY_MAP.NATIVE_TABBAR,
  TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITH_CACHE,
].includes(selectedTabbarStrategy)

export const customTabbarEnable = [
  TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITH_CACHE,
  TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITHOUT_CACHE,
].includes(selectedTabbarStrategy)

/** 自定义+缓存 时需隐藏系统 tabBar，仅显示自定义层 */
export const needHideNativeTabbar =
  selectedTabbarStrategy === TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITH_CACHE

const _tabbarList = customTabbarEnable
  ? customTabbarList.map((item) => ({
      text: item.nativeTabText,
      pagePath: item.pagePath.replace(/^\//, ''),
    }))
  : nativeTabbarList

const _tabbar: TabBar = {
  custom: selectedTabbarStrategy === TABBAR_STRATEGY_MAP.CUSTOM_TABBAR_WITH_CACHE,
  color: '#888888',
  selectedColor: '#030213',
  backgroundColor: '#FFFFFF',
  borderStyle: 'black',
  list: _tabbarList as unknown as TabBar['list'],
}

export const tabBar = tabbarCacheEnable ? _tabbar : undefined

export const isNativeTabbar =
  (selectedTabbarStrategy as TabbarStrategy) === TABBAR_STRATEGY_MAP.NATIVE_TABBAR
