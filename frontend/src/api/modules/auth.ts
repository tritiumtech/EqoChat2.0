import request from '../request'

export const authApi = {
  // 发送验证码
  sendVerifyCode(phone: string) {
    return request.post('/api/v1/auth/verify-code', { phone })
  },
  
  // 注册
  register(data: {
    phone: string
    code: string
    password: string
    nickname: string
  }) {
    return request.post('/api/v1/auth/register', data)
  },
  
  // 登录
  login(data: { phone: string; password: string }) {
    return request.post('/api/v1/auth/login', data)
  },
  
  // 刷新token
  refreshToken() {
    return request.post('/api/v1/auth/refresh')
  }
}