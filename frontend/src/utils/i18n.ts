export type LocaleType = 'en' | 'zh-Hans'

export const normalizeLocale = (locale?: string): LocaleType => {
  if (!locale) return 'zh-Hans'
  const s = String(locale).toLowerCase()
  if (s.startsWith('zh')) return 'zh-Hans'
  return 'en'
}

/**
 * 初始化语言设置：
 * 1) 优先用 storage 的 `locale`
 * 2) 再用系统语言
 * 3) 兜底 `zh-Hans`
 *
 * 并同步 uni 的 locale，保证 pages.json 的 `%pages.xxx%` 正确命中。
 */
export const initLocale = (): LocaleType => {
  const cached = uni.getStorageSync('locale')
  if (cached) {
    const next = normalizeLocale(cached)
    try {
      uni.setLocale(next)
      uni.setStorageSync('locale', next)
    } catch {
      // ignore
    }
    return next
  }

  try {
    const system = uni.getSystemInfoSync().language
    return normalizeLocale(system)
  } catch {
    return 'zh-Hans'
  }
}

