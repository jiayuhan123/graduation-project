import { request } from '@/utils/request'
import type {
  ChangePasswordInput,
  InitializePasswordInput,
  LoginParams,
  LoginResult,
  SessionResult,
  StudentProfile,
  TeacherProfile,
  UpdateProfileInput,
  UpdateStudentProfileInput,
  UpdateTeacherProfileInput,
  UserProfile,
  WechatWebAuthorizeInput,
  WechatWebAuthorizeResult,
  WechatWebCallbackInput,
} from '@/types/user'

/** 管理员登录 */
export const loginApi = (data: LoginParams) =>
  request<LoginResult>({ url: '/api/user/login', method: 'POST', data })

/** 创建微信开放平台网站应用扫码授权会话。 */
export const beginWechatWebLoginApi = (data: WechatWebAuthorizeInput) =>
  request<WechatWebAuthorizeResult>({
    url: '/api/user/login/wechat/web/authorize',
    method: 'POST',
    data,
    withCredentials: true,
    skipUnauthorizedHandler: true,
  })

/** 使用微信回调 code 完成登录。 */
export const completeWechatWebLoginApi = (data: WechatWebCallbackInput) =>
  request<LoginResult>({
    url: '/api/user/login/wechat/web/callback',
    method: 'POST',
    data,
    withCredentials: true,
    skipUnauthorizedHandler: true,
  })

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

export const getCurrentUserApi = () => request<UserProfile>({ url: '/api/user/me', method: 'GET' })

export const updateCurrentUserApi = (data: UpdateProfileInput) =>
  request<UserProfile>({ url: '/api/user/me', method: 'PUT', data })

export const getStudentProfileApi = () =>
  request<StudentProfile>({ url: '/api/user/me/student-profile', method: 'GET' })

export const updateStudentProfileApi = (data: UpdateStudentProfileInput) =>
  request<StudentProfile>({ url: '/api/user/me/student-profile', method: 'PUT', data })

export const getTeacherProfileApi = () =>
  request<TeacherProfile>({ url: '/api/user/me/teacher-profile', method: 'GET' })

export const updateTeacherProfileApi = (data: UpdateTeacherProfileInput) =>
  request<TeacherProfile>({ url: '/api/user/me/teacher-profile', method: 'PUT', data })

export const initializePasswordApi = (data: InitializePasswordInput) =>
  request<void>({ url: '/api/user/me/password/initialize', method: 'PUT', data })

export const changePasswordApi = (data: ChangePasswordInput) =>
  request<void>({ url: '/api/user/me/password', method: 'PUT', data })
