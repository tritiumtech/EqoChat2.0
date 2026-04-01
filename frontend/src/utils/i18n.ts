import en from '../locale/en.json'
import zhHans from '../locale/zh-Hans.json'

export type LocaleType = 'en' | 'zh-Hans'

const messages: Record<LocaleType, Record<string, unknown>> = {
  en: en as Record<string, unknown>,
  'zh-Hans': zhHans as Record<string, unknown>,
}

export const normalizeLocale = (locale?: string): LocaleType => {
  if (!locale) return 'zh-Hans'
  const s = String(locale).toLowerCase()
  if (s.startsWith('zh')) return 'zh-Hans'
  return 'en'
}

/**
 * 获取当前语言
 */
export const getCurrentLocale = (): LocaleType => {
  const cached = uni.getStorageSync('locale')
  if (cached) {
    return normalizeLocale(cached)
  }
  try {
    const system = uni.getSystemInfoSync().language
    return normalizeLocale(system)
  } catch {
    return 'zh-Hans'
  }
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

/**
 * 简单的占位符替换函数
 * 将模板中的 {key} 替换为对应的值
 * 例如: formatTemplate("Lv.{n} · {name}", { n: 1, name: "Observer" }) => "Lv.1 · Observer"
 */
export const formatTemplate = (template: string, params: Record<string, string | number>): string => {
  if (!template) return ''
  return template.replace(/\{(\w+)\}/g, (match, key) => {
    const value = params[key]
    return value !== undefined ? String(value) : match
  })
}

/**
 * 简单的翻译函数（用于非 Vue 组件中）
 * 支持嵌套键，如 'auth.session_kicked'
 */
export const t = (key: string, defaultValue?: string): string => {
  const locale = getCurrentLocale()
  const messageObj = messages[locale]
  if (!messageObj) return defaultValue || key

  const keys = key.split('.')
  let value: unknown = messageObj

  for (const k of keys) {
    if (value && typeof value === 'object') {
      value = (value as Record<string, unknown>)[k]
    } else {
      return defaultValue || key
    }
  }

  return typeof value === 'string' ? value : (defaultValue || key)
}

