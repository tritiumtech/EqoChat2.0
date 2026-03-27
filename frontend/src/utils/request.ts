const BASE_URL =
  import.meta.env.VITE_API_BASE_URL === undefined || import.meta.env.VITE_API_BASE_URL === ''
    ? (import.meta.env.DEV ? 'http://localhost:8080' : 'http://localhost:8080')
    : import.meta.env.VITE_API_BASE_URL

type ApiResponse<T> = {
  code: number
  message: string
  errorCode?: string
  data: T
  timestamp: number
}

type ApiError = {
  code?: number
  message: string
  errorCode?: string
  timestamp?: number
}

class Request {
  private baseURL: string
  
  constructor(options: { baseURL: string }) {
    this.baseURL = options.baseURL
  }

  private isPublicPath(url: string) {
    const cleanUrl = url.split('?')[0]
    return (
      cleanUrl.startsWith('/api/v1/auth/login') ||
      cleanUrl.startsWith('/api/v1/auth/register') ||
      cleanUrl.startsWith('/api/v1/auth/verify-code') ||
      cleanUrl.startsWith('/api/v1/auth/refresh') ||
      cleanUrl.startsWith('/api/v1/health')
    )
  }
  
  private async request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any
  ): Promise<T> {
    const token = uni.getStorageSync('token')
    const locale = uni.getStorageSync('locale') || 'zh-CN'
    
    const normalizeError = (payload: any): ApiError => {
      if (!payload || typeof payload !== 'object') {
        return { message: String(payload || '请求失败') }
      }
      const rawData = typeof payload.data === 'string' ? (() => {
        try { return JSON.parse(payload.data) } catch { return null }
      })() : payload.data
      const source = rawData && typeof rawData === 'object' ? rawData : payload
      const message = source.message || source.msg || source.error || payload.message || payload.msg || '请求失败'
      return {
        code: payload.code ?? source.code,
        message: typeof message === 'string' ? message : '请求失败',
        errorCode: payload.errorCode ?? source.errorCode,
        timestamp: payload.timestamp ?? source.timestamp
      }
    }

    return new Promise((resolve, reject) => {
      if (!token && !this.isPublicPath(url)) {
        uni.reLaunch({ url: '/pages/auth/login' })
        reject({ message: '未登录' })
        return
      }

      uni.request({
        url: `${this.baseURL}${url}`,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          'Accept-Language': locale,
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        success: (res) => {
          if (res.statusCode === 401) {
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/auth/login' })
            reject({ message: '未登录' })
            return
          }
          let payload = res.data
          if (typeof payload === 'string') {
            try { payload = JSON.parse(payload) } catch { /* ignore */ }
          }
          if (res.statusCode >= 200 && res.statusCode < 300) {
            const typedPayload = payload as ApiResponse<T> | T
            if (
              typedPayload &&
              typeof typedPayload === 'object' &&
              'code' in typedPayload &&
              typeof (typedPayload as ApiResponse<T>).code === 'number'
            ) {
              const apiPayload = typedPayload as ApiResponse<T>
              if (apiPayload.code === 200) {
                resolve(apiPayload.data as T)
              } else {
                reject(normalizeError(apiPayload))
              }
            } else {
              resolve(typedPayload as T)
            }
          } else {
            reject(normalizeError(payload ?? res.data))
          }
        },
        fail: (err: any) => {
          const msg = err?.message || err?.msg || err?.errMsg || '网络请求失败'
          reject({ message: typeof msg === 'string' ? msg : '网络请求失败' })
        }
      })
    })
  }
  
  get<T>(url: string, params?: any) {
    const queryString = params ? '?' + this.serializeParams(params) : ''
    return this.request<T>('GET', url + queryString)
  }
  
  post<T>(url: string, data?: any) {
    return this.request<T>('POST', url, data)
  }
  
  put<T>(url: string, data?: any) {
    return this.request<T>('PUT', url, data)
  }
  
  delete<T>(url: string) {
    return this.request<T>('DELETE', url)
  }

  /** 兼容各端的查询参数序列化（避免依赖 URLSearchParams） */
  private serializeParams(params: any): string {
    const parts: string[] = []
    const append = (key: string, value: any) => {
      if (value === undefined || value === null) return
      parts.push(
        encodeURIComponent(key) + '=' + encodeURIComponent(String(value))
      )
    }

    Object.keys(params || {}).forEach((key) => {
      const value = params[key]
      if (Array.isArray(value)) {
        value.forEach((v) => append(key, v))
      } else if (typeof value === 'object') {
        // 简单对象按 JSON 字符串处理
        append(key, JSON.stringify(value))
      } else {
        append(key, value)
      }
    })

    return parts.join('&')
  }
}

/** 从 API 错误对象中提取用户可见的提示文案 */
export function getApiErrorMessage(err: any, fallback = '请求失败'): string {
  if (err == null) return fallback
  if (typeof err === 'string') return err || fallback
  const msg = err.message ?? err.msg ?? err.errMsg ?? err.data?.message ?? err.data?.msg
  return (typeof msg === 'string' && msg.trim()) ? msg : fallback
}

export default new Request({ baseURL: BASE_URL })
