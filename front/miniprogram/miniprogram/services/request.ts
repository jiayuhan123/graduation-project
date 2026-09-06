import { appConfig } from '../config/environment'
import { ApiError, type ApiResult } from '../types/api'
import { clearSession, getSession } from './session'

type RequestMethod = WechatMiniprogram.RequestOption<ApiResult<unknown>>['method']
type UnauthorizedHandler = () => void | Promise<void>

export interface RequestOptions<TBody = unknown> {
  path: string
  method?: RequestMethod
  query?: object
  data?: TBody
  header?: Record<string, string>
  skipAuth?: boolean
  skipUnauthorizedHandler?: boolean
}

export interface UploadOptions {
  path: string
  filePath: string
  name?: string
  formData?: Record<string, string>
  header?: Record<string, string>
}

export interface DownloadResult {
  tempFilePath: string
  statusCode: number
}

let unauthorizedHandler: UnauthorizedHandler | undefined
let unauthorizedTask: Promise<void> | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

function buildUrl(path: string, query?: object): string {
  if (!appConfig.apiBaseUrl) throw new ApiError('当前环境尚未配置 API 域名')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const entries = Object.entries(query ?? {}).filter(([, value]) => value !== undefined)
  if (!entries.length) return `${appConfig.apiBaseUrl}${normalizedPath}`

  const search = entries
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')
  return `${appConfig.apiBaseUrl}${normalizedPath}?${search}`
}

function isApiResult<T>(value: unknown): value is ApiResult<T> {
  if (!value || typeof value !== 'object') return false
  const body = value as Partial<ApiResult<T>>
  return typeof body.code === 'number' && typeof body.message === 'string' && 'data' in body
}

async function handleUnauthorized(): Promise<void> {
  clearSession()
  if (!unauthorizedTask) {
    unauthorizedTask = Promise.resolve(unauthorizedHandler?.()).finally(() => {
      unauthorizedTask = null
    })
  }
  await unauthorizedTask
}

export function request<TResponse, TBody = unknown>(
  options: RequestOptions<TBody>,
): Promise<TResponse> {
  return new Promise((resolve, reject) => {
    const session = getSession()
    const header: Record<string, string> = {
      'content-type': 'application/json',
      ...options.header,
    }
    if (!options.skipAuth && session?.token) header[session.tokenName] = session.token

    wx.request<ApiResult<TResponse>>({
      url: buildUrl(options.path, options.query),
      method: options.method ?? 'GET',
      data: options.data as WechatMiniprogram.IAnyObject | undefined,
      header,
      timeout: appConfig.requestTimeout,
      success: (response) => {
        const body = response.data
        if (!isApiResult<TResponse>(body)) {
          reject(new ApiError('服务响应格式异常', { status: response.statusCode }))
          return
        }

        if (response.statusCode === 401 || body.code === 401) {
          const error = new ApiError(body.message || '登录已过期', {
            code: body.code,
            status: response.statusCode,
            traceId: body.traceId,
          })
          if (options.skipUnauthorizedHandler) reject(error)
          else
            void handleUnauthorized().then(
              () => reject(error),
              () => reject(error),
            )
          return
        }

        if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 200) {
          reject(
            new ApiError(body.message || '请求失败', {
              code: body.code,
              status: response.statusCode,
              traceId: body.traceId,
              details: body.data,
            }),
          )
          return
        }

        resolve(body.data as TResponse)
      },
      fail: (error) => reject(new ApiError(error.errMsg || '网络请求失败')),
    })
  })
}

export function uploadFile<TResponse>(options: UploadOptions): Promise<TResponse> {
  return new Promise((resolve, reject) => {
    const session = getSession()
    const header: Record<string, string> = { ...options.header }
    if (session?.token) header[session.tokenName] = session.token

    void wx.uploadFile({
      url: buildUrl(options.path),
      filePath: options.filePath,
      name: options.name ?? 'file',
      formData: options.formData,
      header,
      timeout: appConfig.requestTimeout,
      success: (response) => {
        let body: unknown
        try {
          body = JSON.parse(response.data) as unknown
        } catch {
          reject(new ApiError('服务响应格式异常', { status: response.statusCode }))
          return
        }

        if (!isApiResult<TResponse>(body)) {
          reject(new ApiError('服务响应格式异常', { status: response.statusCode }))
          return
        }

        if (response.statusCode === 401 || body.code === 401) {
          const error = new ApiError(body.message || '登录已过期', {
            code: body.code,
            status: response.statusCode,
            traceId: body.traceId,
          })
          void handleUnauthorized().then(
            () => reject(error),
            () => reject(error),
          )
          return
        }

        if (response.statusCode < 200 || response.statusCode >= 300 || body.code !== 200) {
          reject(
            new ApiError(body.message || '上传失败', {
              code: body.code,
              status: response.statusCode,
              traceId: body.traceId,
              details: body.data,
            }),
          )
          return
        }

        resolve(body.data as TResponse)
      },
      fail: (error) => reject(new ApiError(error.errMsg || '文件上传失败')),
    })
  })
}

export function downloadFile(path: string): Promise<DownloadResult> {
  return new Promise((resolve, reject) => {
    const session = getSession()
    const header: Record<string, string> = {}
    if (session?.token) header[session.tokenName] = session.token

    void wx.downloadFile({
      url: buildUrl(path),
      header,
      timeout: appConfig.requestTimeout,
      success: (response) => {
        if (response.statusCode === 401) {
          const error = new ApiError('登录已过期', { status: response.statusCode })
          void handleUnauthorized().then(
            () => reject(error),
            () => reject(error),
          )
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new ApiError('文件下载失败', { status: response.statusCode }))
          return
        }
        resolve({ tempFilePath: response.tempFilePath, statusCode: response.statusCode })
      },
      fail: (error) => reject(new ApiError(error.errMsg || '文件下载失败')),
    })
  })
}
