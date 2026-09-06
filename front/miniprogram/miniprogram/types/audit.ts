import type { PageParams } from './api'

export interface AuditLog {
  id: string
  source: string
  action: string
  targetType: string
  targetId: string
  detail: string
  occurredAt: string
}

export interface AuditLogQuery extends PageParams {
  action?: string
}
