import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { MessagePlugin } from 'tdesign-vue-next'
import { appConfig } from '@/config/app'
import { DEFAULT_TOKEN_NAME, STORAGE_KEYS } from '@/constants/storage'
import { ApiError, type ApiResult } from '@/types/api'
import { storage } from '@/utils/storage'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 调用方自行展示错误时跳过全局消息。 */
    skipErrorMessage?: boolean
    /** 会话探测、退出接口自行处理 401 时跳过全局退出回调。 */
    skipUnauthorizedHandler?: boolean
    /** 响应拦截器内部保存的链路标识。 */
    __traceId?: string
  }
}

type UnauthorizedHandler = () => void | Promise<void>

let unauthorizedHandler: UnauthorizedHandler | undefined
let unauthorizedTask: Promise<void> | null = null

/** 注册全局登录失效处理，避免请求层直接依赖 Router 或 Pinia。 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

/** axios 实例（开发环境由 Vite proxy 转发 /api 到网关）。 */
const service = axios.create({
  baseURL: appConfig.apiBaseUrl,
  timeout: appConfig.requestTimeout,
})

service.interceptors.request.use((config) => {
  const token = storage.getString(STORAGE_KEYS.token)
  const tokenName = storage.getString(STORAGE_KEYS.tokenName) || DEFAULT_TOKEN_NAME
  if (token) config.headers.set(tokenName, token)
  return config
})

function isApiResult(value: unknown): value is ApiResult {
  if (!value || typeof value !== 'object') return false
  const body = value as Partial<ApiResult>
  return typeof body.code === 'number' && typeof body.message === 'string' && 'data' in body
}

function businessError(
  body: ApiResult,
  status: number,
  config: InternalAxiosRequestConfig,
): ApiError {
  return new ApiError(body.message || '请求失败', {
    code: body.code,
    status,
    traceId: body.traceId || (config.__traceId as string | undefined),
    details: body.data,
  })
}

function transportMessage(error: AxiosError): string {
  if (error.code === AxiosError.ECONNABORTED || error.code === 'ETIMEDOUT')
    return '请求超时，请稍后重试'
  if (!error.response) return navigator.onLine ? '网络异常，请稍后重试' : '网络已断开'

  const messages: Record<number, string> = {
    400: '请求参数错误',
    401: '登录已过期，请重新登录',
    403: '没有权限执行此操作',
    404: '请求的资源不存在',
    429: '请求过于频繁，请稍后重试',
    500: '服务暂时不可用',
    502: '网关暂时不可用',
    503: '服务暂时不可用',
    504: '网关响应超时',
  }
  return messages[error.response.status] || error.message || '请求失败'
}

async function apiErrorFromAxios(error: AxiosError): Promise<ApiError> {
  let data: unknown = error.response?.data
  if (data instanceof Blob && data.type.includes('json')) {
    try {
      data = JSON.parse(await data.text()) as unknown
    } catch {
      // 保留通用 HTTP 错误信息。
    }
  }

  const body = isApiResult(data) ? data : undefined
  const status = error.response?.status
  const traceId = error.response?.headers['x-trace-id'] as string | undefined
  return new ApiError(body?.message || transportMessage(error), {
    code: body?.code ?? status,
    status,
    traceId: body?.traceId || traceId,
    details: body?.data,
  })
}

async function handleUnauthorized(): Promise<void> {
  if (!unauthorizedTask) {
    unauthorizedTask = Promise.resolve(unauthorizedHandler?.())
      .catch((error: unknown) => console.error('[auth] unauthorized handler failed', error))
      .finally(() => {
        unauthorizedTask = null
      })
    void MessagePlugin.warning('登录已过期，请重新登录')
  }
  await unauthorizedTask
}

async function handleRequestError(
  error: ApiError,
  config: InternalAxiosRequestConfig,
): Promise<void> {
  if (error.code === 401 || error.status === 401) {
    if (!config.skipUnauthorizedHandler) await handleUnauthorized()
    return
  }
  if (!config.skipErrorMessage) void MessagePlugin.error(error.message)
}

service.interceptors.response.use(
  async (response) => {
    response.config.__traceId = response.headers['x-trace-id'] as string | undefined
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }

    if (!isApiResult(response.data)) {
      const error = new ApiError('服务响应格式异常', {
        status: response.status,
        traceId: response.config.__traceId as string | undefined,
      })
      await handleRequestError(error, response.config)
      return Promise.reject(error)
    }

    if (response.data.code !== 200) {
      const error = businessError(response.data, response.status, response.config)
      await handleRequestError(error, response.config)
      return Promise.reject(error)
    }
    return response
  },
  async (unknownError: unknown) => {
    if (!axios.isAxiosError(unknownError)) return Promise.reject(unknownError)
    const error = await apiErrorFromAxios(unknownError)
    const config = unknownError.config ?? ({ headers: {} } as InternalAxiosRequestConfig)
    await handleRequestError(error, config)
    return Promise.reject(error)
  },
)

/** 类型安全的 JSON 请求，直接返回后端 Result<T> 的 data。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await service.request<ApiResult<T>>(config)
  return response.data.data as T
}

/** multipart/form-data 上传。 */
export function upload<T>(
  url: string,
  file: File,
  data: Record<string, string | Blob> = {},
  config: AxiosRequestConfig = {},
): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(data).forEach(([key, value]) => formData.append(key, value))
  return request<T>({ ...config, url, method: 'POST', data: formData })
}

/** 文件下载；若服务返回 JSON 错误体，仍按统一异常处理。 */
export async function download(config: AxiosRequestConfig): Promise<Blob> {
  const response = await service.request<Blob>({ ...config, responseType: 'blob' })
  const contentType = response.headers['content-type'] as string | undefined
  if (contentType?.includes('json')) {
    try {
      const body = JSON.parse(await response.data.text()) as unknown
      if (isApiResult(body) && body.code !== 200) {
        const error = businessError(body, response.status, response.config)
        await handleRequestError(error, response.config)
        throw error
      }
    } catch (error) {
      if (error instanceof ApiError) throw error
      throw new ApiError('下载响应格式异常', { status: response.status })
    }
  }
  return response.data
}

export default service
