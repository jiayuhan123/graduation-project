import { request } from '@/utils/request'
import type { LoginParams, LoginResult, SessionResult } from '@/types/user'

/** 管理员登录 */
export const loginApi = (data: LoginParams) =>
  request<LoginResult>({ url: '/api/user/login', method: 'POST', data })

/** 获取服务端当前会话、角色和权限。 */
export const currentSessionApi = () =>
  request<SessionResult>({
    url: '/api/user/session',
    method: 'GET',
    skipErrorMessage: true,
    skipUnauthorizedHandler: true,
  })

/** 注销服务端当前会话。 */
export const logoutApi = () =>
  request<void>({
    url: '/api/user/logout',
    method: 'POST',
    skipErrorMessage: true,
    skipUnauthorizedHandler: true,
  })
