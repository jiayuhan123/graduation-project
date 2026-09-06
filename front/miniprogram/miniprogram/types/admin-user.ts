import type { StudentProfile, TeacherProfile } from './account'
import type { PageParams } from './api'

export interface RoleSummary {
  id: string
  code: string
  name: string
}

export interface AdminUserSummary {
  id: string
  username: string
  nickname: string | null
  realName: string | null
  email: string | null
  phone: string | null
  avatarUrl: string | null
  status: number
  roles: RoleSummary[]
  wechatBound: boolean
  studentProfile: StudentProfile | null
  teacherProfile: TeacherProfile | null
  lastLoginAt: string | null
  lastLoginIp: string | null
  passwordChangedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminUserQuery extends PageParams {
  keyword?: string
  status?: number
  roleCode?: string
}

export interface PermissionDetail {
  id: string
  code: string
  name: string
  description: string | null
}

export interface RoleDetail {
  id: string
  code: string
  name: string
  description: string | null
  status: number
  builtIn: boolean
  permissions: PermissionDetail[]
  createdAt: string
  updatedAt: string
}
