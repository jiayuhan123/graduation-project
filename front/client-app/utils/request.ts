/** 网关统一地址 */
export const BASE_URL = 'http://localhost:10000'

/** Sa-Token 在本地存储中的 Key */
export const TOKEN_KEY = 'satoken'

/** uni.request 请求参数 */
export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: object
  header?: Record<string, string>
}

/** 后端统一响应结构 Result<T> */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 全局请求封装
 * - 自动携带 satoken
 * - 统一处理业务错误码与 401 登录过期
 * - 成功时直接 resolve 业务数据 data
 */
export function request<T = unknown>(options: RequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    // 1. 获取本地 token
    const token = uni.getStorageSync(TOKEN_KEY) as string

    // 2. 组装 headers
    const header: Record<string, string> = { ...options.header }
    if (token) {
      header[TOKEN_KEY] = token
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success: (res) => {
        const { statusCode } = res
        const body = res.data as ApiResult<T>
        // HTTP 状态码 200
        if (statusCode === 200) {
          if (body.code === 200) {
            resolve(body.data) // 直接返回实际数据
          } else if (body.code === 401) {
            // 未授权，清除 token 并跳转登录页
            uni.removeStorageSync(TOKEN_KEY)
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/login/login' })
            }, 1500)
            reject(new Error(body.message))
          } else {
            // 业务报错
            uni.showToast({ title: body.message || '请求失败', icon: 'none' })
            reject(new Error(body.message))
          }
        } else {
          uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
          reject(new Error('网络异常'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '请求接口失败', icon: 'none' })
        reject(err)
      }
    })
  })
}
