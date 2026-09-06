import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type { AuditLog, AuditLogQuery } from '../types/audit'

export const pageMyAuditLogsApi = (query: AuditLogQuery = {}) =>
  request<PageResult<AuditLog>>({ path: '/api/audits/me', query })
