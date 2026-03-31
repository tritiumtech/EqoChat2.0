import { createI18n } from 'vue-i18n'

import en from './en.json'
import zhHans from './zh-Hans.json'

import type { LocaleType } from '../utils/i18n'
import { initLocale, normalizeLocale } from '../utils/i18n'

const refreshCurrentPages = () => {
  try {
    const pages = getCurrentPages()
    pages.forEach((page: any) => {
      if (page?.$vm && typeof page.$vm.$forceUpdate === 'function') {
        page.$vm.$forceUpdate()
      }
    })
  } catch {
    // ignore
  }
}

const messagesFromFiles = {
  en,
  'zh-Hans': zhHans,
}

export const i18n = createI18n({
  // 与 techjewelryapp 保持一致：模板可直接使用 $t
  legacy: false,
  locale: initLocale(),
  fallbackLocale: 'en',
  messages: messagesFromFiles,
  globalInjection: true,
})

export const setLocale = (locale: LocaleType) => {
  const next = normalizeLocale(locale)
  const g = i18n.global as any
  // legacy:false => composition 模式，locale 是 ref
  if (g?.locale && 'value' in g.locale) {
    g.locale.value = next
  } else {
    g.locale = next
  }

  try {
    uni.setLocale(next)
  } catch {
    // Some environments might throw, vue-i18n still works.
  }

  uni.setStorageSync('locale', next)
  setTimeout(refreshCurrentPages, 150)
}

uni.onLocaleChange((event) => {
  const next = normalizeLocale(event.locale)
  const g = i18n.global as any
  const cur = g?.locale && 'value' in g.locale ? g.locale.value : g?.locale
  if (cur === next) return

  if (g?.locale && 'value' in g.locale) {
    g.locale.value = next
  } else {
    g.locale = next
  }
  uni.setStorageSync('locale', next)
  setTimeout(refreshCurrentPages, 150)
})

