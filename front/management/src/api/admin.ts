import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AdminUser,
  AdminUserQuery,
  CreateAdminUserInput,
  CreateRoleInput,
  Permission,
  Role,
  StudentProfileInput,
  TeacherProfileInput,
  UpdateAdminUserInput,
  UpdateRoleInput,
} from '@/types/admin'

const USER_BASE_URL = '/api/user/admin/users'
const ADMIN_BASE_URL = '/api/user/admin'

export const pageAdminUsersApi = (params: AdminUserQuery) =>
  request<PageResult<AdminUser>>({ url: USER_BASE_URL, method: 'GET', params })

export const getAdminUserApi = (userId: string) =>
  request<AdminUser>({ url: `${USER_BASE_URL}/${userId}`, method: 'GET' })

export const createAdminUserApi = (data: CreateAdminUserInput) =>
  request<AdminUser>({ url: USER_BASE_URL, method: 'POST', data })

export const updateAdminUserApi = (userId: string, data: UpdateAdminUserInput) =>
  request<AdminUser>({ url: `${USER_BASE_URL}/${userId}`, method: 'PUT', data })

export const updateAdminUserStatusApi = (userId: string, status: number) =>
  request<void>({ url: `${USER_BASE_URL}/${userId}/status`, method: 'PUT', data: { status } })

export const assignAdminUserRolesApi = (userId: string, roleIds: string[]) =>
  request<void>({ url: `${USER_BASE_URL}/${userId}/roles`, method: 'PUT', data: { roleIds } })

export const resetAdminUserPasswordApi = (userId: string, newPassword: string) =>
  request<void>({
    url: `${USER_BASE_URL}/${userId}/password`,
    method: 'PUT',
    data: { newPassword },
  })

export const deleteAdminUserApi = (userId: string) =>
  request<void>({ url: `${USER_BASE_URL}/${userId}`, method: 'DELETE' })

export const upsertStudentProfileApi = (userId: string, data: StudentProfileInput) =>
  request<AdminUser>({
    url: `${USER_BASE_URL}/${userId}/student-profile`,
    method: 'PUT',
    data,
  })

export const upsertTeacherProfileApi = (userId: string, data: TeacherProfileInput) =>
  request<AdminUser>({
    url: `${USER_BASE_URL}/${userId}/teacher-profile`,
    method: 'PUT',
    data,
  })

export const listRolesApi = () => request<Role[]>({ url: `${ADMIN_BASE_URL}/roles`, method: 'GET' })

export const createRoleApi = (data: CreateRoleInput) =>
  request<Role>({ url: `${ADMIN_BASE_URL}/roles`, method: 'POST', data })

export const updateRoleApi = (roleId: string, data: UpdateRoleInput) =>
  request<Role>({ url: `${ADMIN_BASE_URL}/roles/${roleId}`, method: 'PUT', data })

export const updateRoleStatusApi = (roleId: string, status: number) =>
  request<void>({
    url: `${ADMIN_BASE_URL}/roles/${roleId}/status`,
    method: 'PUT',
    data: { status },
  })

export const assignRolePermissionsApi = (roleId: string, permissionIds: string[]) =>
  request<void>({
    url: `${ADMIN_BASE_URL}/roles/${roleId}/permissions`,
    method: 'PUT',
    data: { permissionIds },
  })

export const deleteRoleApi = (roleId: string) =>
  request<void>({ url: `${ADMIN_BASE_URL}/roles/${roleId}`, method: 'DELETE' })

export const listPermissionsApi = () =>
  request<Permission[]>({ url: `${ADMIN_BASE_URL}/permissions`, method: 'GET' })
