const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

class Request {
  private baseURL: string
  
  constructor(options: { baseURL: string }) {
    this.baseURL = options.baseURL
  }
  
  private async request<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any
  ): Promise<T> {
    const token = uni.getStorageSync('token')
    
    return new Promise((resolve, reject) => {
      uni.request({
        url: `${this.baseURL}${url}`,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(res.data as T)
          } else {
            reject(res.data)
          }
        },
        fail: (err) => {
          reject(err)
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