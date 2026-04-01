import { useI18n } from 'vue-i18n'
import { formatTemplate } from '../utils/i18n'

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
    const template = t(key)
    if (!params) return template
    return formatTemplate(template, params)
  }

  return {
    t,
    tf,
    locale,
  }
}
