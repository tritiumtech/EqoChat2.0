import { reactive } from 'vue'
import type { CustomTabBarItem, CustomTabBarItemBadge } from './types'

import {
  customTabbarList as _tabbarList,
  customTabbarEnable,
  selectedTabbarStrategy,
  TABBAR_STRATEGY_MAP,
} from './config'

const BULGE_ENABLE = false

const tabbarList = reactive<CustomTabBarItem[]>(
  _tabbarList.map((item) => ({
    ...item,
    pagePath: item.pagePath.startsWith('/') ? item.pagePath : `/${item.pagePath}`,
  })),
)

if (customTabbarEnable && BULGE_ENABLE) {
  if (tabbarList.length % 2) {
    console.error('Bulge tabbar requires an even number of configured items.')
  }
  tabbarList.splice(tabbarList.length / 2, 0, {
    isBulge: true,
  } as CustomTabBarItem)
}

export function isPageTabbar(path: string) {
  if (selectedTabbarStrategy === TABBAR_STRATEGY_MAP.NO_TABBAR) {
    return false
  }
  const _path = path.split('?')[0]
  return tabbarList.some((item) => item.pagePath === _path)
}

const tabbarStore = reactive({
  curIdx: Number(uni.getStorageSync('app-tabbar-index')) || 0,
  prevIdx: Number(uni.getStorageSync('app-tabbar-index')) || 0,
  setCurIdx(idx: number) {
    this.curIdx = idx
    uni.setStorageSync('app-tabbar-index', idx)
  },
  setTabbarItemBadge(idx: number, badge: CustomTabBarItemBadge) {
    if (tabbarList[idx]) {
      tabbarList[idx].badge = badge
    }
  },
  setAutoCurIdx(path: string) {
    if (path === '/') {
      this.setCurIdx(0)
      return
    }
    const index = tabbarList.findIndex((item) => item.pagePath === path)
    if (index === -1) {
      const pagesPathList = getCurrentPages().map((item) =>
        item.route.startsWith('/') ? item.route : `/${item.route}`,
      )
      const flag = tabbarList.some((item) => pagesPathList.includes(item.pagePath))
      if (!flag) {
        this.setCurIdx(0)
      }
    } else {
      this.setCurIdx(index)
    }
  },
  restorePrevIdx() {
    if (this.prevIdx === this.curIdx) return
    this.setCurIdx(this.prevIdx)
    this.prevIdx = Number(uni.getStorageSync('app-tabbar-index')) || 0
  },
})

export { tabbarList, tabbarStore }
