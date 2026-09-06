import { request } from '../services/request'
import type {
  StudentProfile,
  TeacherProfile,
  ChangePasswordInput,
  InitializePasswordInput,
  UpdateProfileInput,
  UpdateStudentProfileInput,
  UpdateTeacherProfileInput,
  UserProfile,
  WechatCodeInput,
} from '../types/account'

const ACCOUNT_BASE_URL = '/api/user/me'

export const getCurrentUserApi = () => request<UserProfile>({ path: ACCOUNT_BASE_URL })

export const updateCurrentUserApi = (data: UpdateProfileInput) =>
  request<UserProfile, UpdateProfileInput>({ path: ACCOUNT_BASE_URL, method: 'PUT', data })

export const getStudentProfileApi = () =>
  request<StudentProfile>({ path: `${ACCOUNT_BASE_URL}/student-profile` })

export const updateStudentProfileApi = (data: UpdateStudentProfileInput) =>
  request<StudentProfile, UpdateStudentProfileInput>({
    path: `${ACCOUNT_BASE_URL}/student-profile`,
    method: 'PUT',
    data,
  })

export const getTeacherProfileApi = () =>
  request<TeacherProfile>({ path: `${ACCOUNT_BASE_URL}/teacher-profile` })

export const updateTeacherProfileApi = (data: UpdateTeacherProfileInput) =>
  request<TeacherProfile, UpdateTeacherProfileInput>({
    path: `${ACCOUNT_BASE_URL}/teacher-profile`,
    method: 'PUT',
    data,
  })

export const initializePasswordApi = (data: InitializePasswordInput) =>
  request<void, InitializePasswordInput>({
    path: `${ACCOUNT_BASE_URL}/password/initialize`,
    method: 'PUT',
    data,
  })

export const changePasswordApi = (data: ChangePasswordInput) =>
  request<void, ChangePasswordInput>({
    path: `${ACCOUNT_BASE_URL}/password`,
    method: 'PUT',
    data,
  })

export const bindWechatApi = (data: WechatCodeInput) =>
  request<void, WechatCodeInput>({
    path: `${ACCOUNT_BASE_URL}/wechat`,
    method: 'POST',
    data,
  })

export const unbindWechatApi = () =>
  request<void>({
    path: `${ACCOUNT_BASE_URL}/wechat`,
    method: 'DELETE',
  })
