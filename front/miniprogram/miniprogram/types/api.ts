export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T | null
  traceId?: string
}

export interface PageResult<T = unknown> {
  total: number
  pages: number
  page: number
  size: number
  records: T[]
}

export interface PageParams {
  page?: number
  size?: number
}

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
