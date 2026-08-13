import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

/** Sa-Token 在本地存储中的 Key */
export const TOKEN_KEY = 'satoken'

/** axios 实例（开发环境由 vite proxy 转发 /api 到网关） */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
})

/** 请求拦截器：自动携带 satoken */
service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers[TOKEN_KEY] = token
  }
  return config
})

/** 响应拦截器：统一解包后端 Result<T> */
service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult
    if (body.code === 200) {
      return body as never
    }
    if (body.code === 401) {
      // 登录过期：清除 token 并跳转登录页
      localStorage.removeItem(TOKEN_KEY)
      ElMessage.warning('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error(body.message))
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  (error: AxiosError) => {
    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

/**
 * 类型安全的请求方法，直接返回业务数据 T
 *
 * @example
 * const user = await request<LoginResult>({ url: '/api/user/login', method: 'POST', data })
 */
export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const res = (await service.request(config)) as unknown as ApiResult<T>
  return res.data
}

export default service
