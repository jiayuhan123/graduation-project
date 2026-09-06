import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type { AdminUserQuery, AdminUserSummary, RoleDetail } from '../types/admin-user'

const ADMIN_BASE_URL = '/api/user/admin'

export const listAdminUsersApi = (query: AdminUserQuery) =>
  request<PageResult<AdminUserSummary>>({ path: `${ADMIN_BASE_URL}/users`, query })

export const listRolesApi = () => request<RoleDetail[]>({ path: `${ADMIN_BASE_URL}/roles` })
