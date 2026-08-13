/** 后端统一响应结构 Result<T> */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
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
