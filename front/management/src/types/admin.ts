import type { PageParams } from '@/types/api'

export interface StudentProfile {
  userId: string
  studentNo: string | null
  gradeLevel: number
  semester: number
  updatedAt: string
}

export interface TeacherProfile {
  userId: string
  teacherNo: string | null
  title: string | null
  introduction: string | null
  updatedAt: string
}

export interface RoleSummary {
  id: string
  code: string
  name: string
}

export interface Permission {
  id: string
  code: string
  name: string
  description: string | null
}

export interface Role extends RoleSummary {
  description: string | null
  status: number
  builtIn: boolean
  permissions: Permission[]
  createdAt: string
  updatedAt: string
}

export interface AdminUser {
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

export interface StudentProfileInput {
  studentNo?: string
  gradeLevel: number
  semester: number
}

export interface TeacherProfileInput {
  teacherNo?: string
  title?: string
  introduction?: string
}

export interface CreateAdminUserInput {
  username: string
  password: string
  nickname?: string
  realName?: string
  email?: string
  phone?: string
  status?: number
  roleIds: string[]
  studentProfile?: StudentProfileInput
  teacherProfile?: TeacherProfileInput
}

export interface UpdateAdminUserInput {
  nickname?: string
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
}

export interface CreateRoleInput {
  code: string
  name: string
  description?: string
}

export interface UpdateRoleInput {
  name: string
  description?: string
}
