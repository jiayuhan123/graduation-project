import { request } from '@/utils/request'
import type { LoginParams, LoginResult, RegisterParams, RegisterResult } from '@/types/user'

/** 登录 */
export const loginApi = (data: LoginParams) =>
  request<LoginResult>({ url: '/api/user/login', method: 'POST', data })

/** 注册 */
export const registerApi = (data: RegisterParams) =>
  request<RegisterResult>({ url: '/api/user/register', method: 'POST', data })
