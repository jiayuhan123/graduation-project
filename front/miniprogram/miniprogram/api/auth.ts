import { request } from '../services/request'
import type {
  LoginResult,
  PasswordLoginInput,
  SessionResult,
  WechatLoginInput,
} from '../types/auth'

const USER_BASE_URL = '/api/user'

export const wechatLoginApi = (data: WechatLoginInput) =>
  request<LoginResult, WechatLoginInput>({
    path: `${USER_BASE_URL}/login/wechat`,
    method: 'POST',
    data,
    skipAuth: true,
  })

export const passwordLoginApi = (data: PasswordLoginInput) =>
  request<LoginResult, PasswordLoginInput>({
    path: `${USER_BASE_URL}/login`,
    method: 'POST',
    data,
    skipAuth: true,
  })

export const currentSessionApi = () =>
  request<SessionResult>({
    path: `${USER_BASE_URL}/session`,
    skipUnauthorizedHandler: true,
  })

export const logoutApi = () =>
  request<void>({
    path: `${USER_BASE_URL}/logout`,
    method: 'POST',
    skipUnauthorizedHandler: true,
  })
