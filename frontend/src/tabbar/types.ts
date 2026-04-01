import type { TabBar } from '@uni-helper/vite-plugin-uni-pages'

/** 原生 tabbar 单项（与 pages.json 中 list 项一致） */
export type NativeTabBarItem = TabBar['list'][number]

export type CustomTabBarItemBadge = number | 'dot'

/**
 * 自定义 tabbar 单项：展示用文案走 `text`（vue-i18n，如 nav.chat）；
 * 原生 tabBar 占位文案走 `nativeTabText`（pages.json 的 %pages.*%）。
 */
export interface CustomTabBarItem {
  /** vue-i18n key，如 nav.chat */
  text: string
  /** pages.json tabBar.list 的 text，如 %pages.chat.title% */
  nativeTabText: string
  pagePath: string
  subPagePath?: string
  iconType: 'emoji' | 'image'
  icon: string
  iconActive?: string
  showTabbar?: boolean
  badge?: CustomTabBarItemBadge
  isBulge?: boolean
}
