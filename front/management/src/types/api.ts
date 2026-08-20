/** 后端统一响应结构 Result<T> */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T | null
  traceId?: string
}

/** 统一请求异常，保留业务码、HTTP 状态和链路标识。 */
export class ApiError extends Error {
  readonly code: number
  readonly status?: number
  readonly traceId?: string
  readonly details?: unknown

  constructor(
    message: string,
    options: { code?: number; status?: number; traceId?: string; details?: unknown } = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.code = options.code ?? options.status ?? -1
    this.status = options.status
    this.traceId = options.traceId
    this.details = options.details
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

/** 分页响应结构 */
export interface PageResult<T = unknown> {
  total: number
  pages: number
  current: number
  size: number
  records: T[]
}

/** 通用分页请求参数 */
export interface PageParams {
  page?: number
  size?: number
  orderBy?: string
  orderDir?: 'asc' | 'desc'
}
