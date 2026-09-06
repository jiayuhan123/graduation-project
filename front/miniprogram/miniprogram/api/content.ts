import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type {
  ContentModule,
  ContentModuleQuery,
  KnowledgePoint,
  KnowledgePointQuery,
} from '../types/content'

const CONTENT_BASE_URL = '/api/content'

export const pageContentModulesApi = (query: ContentModuleQuery = {}) =>
  request<PageResult<ContentModule>>({ path: `${CONTENT_BASE_URL}/modules`, query })

export const getContentModuleApi = (moduleId: string) =>
  request<ContentModule>({ path: `${CONTENT_BASE_URL}/modules/${moduleId}` })

export const listKnowledgePointsApi = (query: KnowledgePointQuery) =>
  request<KnowledgePoint[]>({ path: `${CONTENT_BASE_URL}/knowledge-points`, query })

export const getKnowledgePointApi = (knowledgePointId: string) =>
  request<KnowledgePoint>({ path: `${CONTENT_BASE_URL}/knowledge-points/${knowledgePointId}` })
