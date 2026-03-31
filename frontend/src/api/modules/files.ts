const API_BASE =
  import.meta.env.VITE_API_BASE_URL === undefined || import.meta.env.VITE_API_BASE_URL === ''
    ? import.meta.env.DEV
      ? 'http://localhost:8080'
      : 'http://localhost:8080'
    : import.meta.env.VITE_API_BASE_URL

export interface UploadFileResponse {
  url: string
}

function uploadChatFile(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    if (!token) {
      reject({ message: '未登录' })
      return
    }

    uni.uploadFile({
      url: `${API_BASE}/api/v1/files/uploads`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${token}`,
      },
      success: (res) => {
        let payload: any = res.data
        if (typeof payload === 'string') {
          try {
            payload = JSON.parse(payload)
          } catch {
            reject({ message: '上传响应解析失败' })
            return
          }
        }

        if (payload && payload.code === 200 && payload.data?.url) {
          resolve(String(payload.data.url))
          return
        }
        reject({ message: payload?.message || '上传失败' })
      },
      fail: (err: any) => {
        reject({ message: err?.errMsg || err?.message || '网络错误' })
      },
    })
  })
}

export const filesApi = {
  uploadChatFile,
  // 预留：如需要用 request 管理下载可在后续补充
}

