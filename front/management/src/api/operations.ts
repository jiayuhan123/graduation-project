import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AuditLogItem,
  ContentOperationsStatistics,
  GameOperationsStatistics,
  LearningOperationsStatistics,
  TeachingOperationsStatistics,
  UserOperationsStatistics,
} from '@/types/operations'

export const getUserOperationsStatisticsApi = () =>
  request<UserOperationsStatistics>({
    url: '/api/user/admin/users/statistics',
    method: 'GET',
  })

export const getTeachingOperationsStatisticsApi = () =>
  request<TeachingOperationsStatistics>({
    url: '/api/teaching/admin/statistics',
    method: 'GET',
  })

export const getContentOperationsStatisticsApi = () =>
  request<ContentOperationsStatistics>({
    url: '/api/content/admin/statistics',
    method: 'GET',
  })

export const getLearningOperationsStatisticsApi = () =>
  request<LearningOperationsStatistics>({
    url: '/api/learning/admin/statistics',
    method: 'GET',
  })

export const getGameOperationsStatisticsApi = () =>
  request<GameOperationsStatistics>({
    url: '/api/games/admin/statistics',
    method: 'GET',
  })

export const pageAdminAuditLogsApi = (params: {
  page?: number
  size?: number
  operatorId?: string
  source?: string
  action?: string
  targetType?: string
  targetId?: string
  occurredFrom?: string
  occurredTo?: string
}) =>
  request<PageResult<AuditLogItem>>({
    url: '/api/audits/admin',
    method: 'GET',
    params,
  })

export const pageMyAuditLogsApi = (params: { page?: number; size?: number; action?: string }) =>
  request<PageResult<AuditLogItem>>({
    url: '/api/audits/me',
    method: 'GET',
    params,
  })
