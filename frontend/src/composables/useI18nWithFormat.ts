import { useI18n } from 'vue-i18n'
import { formatTemplate } from '../utils/i18n'
import en from '../locale/en.json'
import zhHans from '../locale/zh-Hans.json'

const messages: Record<string, Record<string, unknown>> = {
  en: en as Record<string, unknown>,
  'zh-Hans': zhHans as Record<string, unknown>,
}

/**
 * 从嵌套对象中获取值
 */
const getNestedValue = (obj: Record<string, unknown>, key: string): string | null => {
  const keys = key.split('.')
  let value: unknown = obj
  for (const k of keys) {
    if (value && typeof value === 'object') {
      value = (value as Record<string, unknown>)[k]
    } else {
      return null
    }
  }
  return typeof value === 'string' ? value : null
}

/**
 * 增强的 i18n hook，支持自定义占位符替换
 * 当 vue-i18n 的插值不工作时，使用此 hook
 */
export const useI18nWithFormat = () => {
  const { t, locale } = useI18n({ useScope: 'global' })

  /**
   * 翻译并格式化模板
   * @param key 翻译键
   * @param params 占位符参数
   * @returns 格式化后的字符串
   */
  const tf = (key: string, params?: Record<string, string | number>): string => {
    // 获取当前语言
    const currentLocale = locale?.value || 'zh-Hans'
    const messageObj = messages[currentLocale]
    
    // 从 JSON 中获取翻译模板
    let template = key
    if (messageObj) {
      const found = getNestedValue(messageObj, key)
      if (found) {
        template = found
      }
    }
    
    // 如果没有参数，直接返回模板
    if (!params) return template
    
    // 格式化占位符
    return formatTemplate(template, params)
  }

  return {
    t,
    tf,
    locale,
  }
}