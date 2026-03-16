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
      if (payload && typeof payload === 'object') {
        const message = payload.message || payload.msg || payload.error || '请求失败'
        return {
          code: payload.code,
          message,
          errorCode: payload.errorCode,
          timestamp: payload.timestamp
        }
      }
      return { message: String(payload || '请求失败') }
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
          if (res.statusCode >= 200 && res.statusCode < 300) {
            const payload = res.data as ApiResponse<T> | T
            if (
              payload &&
              typeof payload === 'object' &&
              'code' in payload &&
              typeof (payload as ApiResponse<T>).code === 'number'
            ) {
              const apiPayload = payload as ApiResponse<T>
              if (apiPayload.code === 200) {
                resolve(apiPayload.data as T)
              } else {
                reject(normalizeError(apiPayload))
              }
            } else {
              resolve(payload as T)
            }
          } else {
            reject(normalizeError(res.data))
          }
        },
        fail: (err) => {
          reject(normalizeError(err))
        }
      })
    })
  }
  
  get<T>(url: string, params?: any) {
    const queryString = params ? '?' + new URLSearchParams(params).toString() : ''
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
}

export default new Request({ baseURL: BASE_URL })
