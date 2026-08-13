import { request } from '@/utils/request'
import type { LoginParams, LoginResult } from '@/types/user'

/** 管理员登录 */
export const loginApi = (data: LoginParams) =>
  request<LoginResult>({ url: '/api/user/login', method: 'POST', data })
