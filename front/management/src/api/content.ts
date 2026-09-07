import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ContentModule,
  ContentModuleInput,
  ContentModuleQuery,
  EnabledStatus,
  ErrorType,
  ErrorTypeInput,
  ErrorTypeQuery,
  KnowledgePoint,
  KnowledgePointInput,
  KnowledgePointQuery,
  QuestionDetail,
  QuestionInput,
  QuestionQuery,
  QuestionSummary,
} from '@/types/content'

const ADMIN_CONTENT_URL = '/api/content/admin'
const CONTENT_URL = '/api/content'

export const pageLearningModulesApi = (params: ContentModuleQuery) =>
  request<PageResult<ContentModule>>({ url: `${CONTENT_URL}/modules`, method: 'GET', params })

export const listLearningKnowledgePointsApi = (params: KnowledgePointQuery) =>
  request<KnowledgePoint[]>({
    url: `${CONTENT_URL}/knowledge-points`,
    method: 'GET',
    params,
  })

export const pageContentModulesApi = (params: ContentModuleQuery) =>
  request<PageResult<ContentModule>>({ url: `${ADMIN_CONTENT_URL}/modules`, method: 'GET', params })

export const getContentModuleApi = (moduleId: string) =>
  request<ContentModule>({ url: `${ADMIN_CONTENT_URL}/modules/${moduleId}`, method: 'GET' })

export const createContentModuleApi = (data: ContentModuleInput) =>
  request<ContentModule>({ url: `${ADMIN_CONTENT_URL}/modules`, method: 'POST', data })

export const updateContentModuleApi = (moduleId: string, data: ContentModuleInput) =>
  request<ContentModule>({ url: `${ADMIN_CONTENT_URL}/modules/${moduleId}`, method: 'PUT', data })

export const updateContentModuleStatusApi = (
  moduleId: string,
  status: EnabledStatus,
  version: number,
) =>
  request<ContentModule>({
    url: `${ADMIN_CONTENT_URL}/modules/${moduleId}/status`,
    method: 'PUT',
    data: { status, version },
  })

export const deleteContentModuleApi = (moduleId: string, version: number) =>
  request<void>({
    url: `${ADMIN_CONTENT_URL}/modules/${moduleId}`,
    method: 'DELETE',
    params: { version },
  })

export const listKnowledgePointsApi = (params: KnowledgePointQuery = {}) =>
  request<KnowledgePoint[]>({
    url: `${ADMIN_CONTENT_URL}/knowledge-points`,
    method: 'GET',
    params,
  })

export const getKnowledgePointApi = (knowledgePointId: string) =>
  request<KnowledgePoint>({
    url: `${ADMIN_CONTENT_URL}/knowledge-points/${knowledgePointId}`,
    method: 'GET',
  })

export const createKnowledgePointApi = (data: KnowledgePointInput) =>
  request<KnowledgePoint>({ url: `${ADMIN_CONTENT_URL}/knowledge-points`, method: 'POST', data })

export const updateKnowledgePointApi = (knowledgePointId: string, data: KnowledgePointInput) =>
  request<KnowledgePoint>({
    url: `${ADMIN_CONTENT_URL}/knowledge-points/${knowledgePointId}`,
    method: 'PUT',
    data,
  })

export const updateKnowledgePointStatusApi = (
  knowledgePointId: string,
  status: EnabledStatus,
  version: number,
) =>
  request<KnowledgePoint>({
    url: `${ADMIN_CONTENT_URL}/knowledge-points/${knowledgePointId}/status`,
    method: 'PUT',
    data: { status, version },
  })

export const deleteKnowledgePointApi = (knowledgePointId: string, version: number) =>
  request<void>({
    url: `${ADMIN_CONTENT_URL}/knowledge-points/${knowledgePointId}`,
    method: 'DELETE',
    params: { version },
  })

export const pageQuestionsApi = (params: QuestionQuery) =>
  request<PageResult<QuestionSummary>>({
    url: `${ADMIN_CONTENT_URL}/questions`,
    method: 'GET',
    params,
  })

export const getQuestionApi = (questionId: string) =>
  request<QuestionDetail>({ url: `${ADMIN_CONTENT_URL}/questions/${questionId}`, method: 'GET' })

export const createQuestionApi = (data: QuestionInput) =>
  request<QuestionDetail>({ url: `${ADMIN_CONTENT_URL}/questions`, method: 'POST', data })

export const updateQuestionApi = (questionId: string, data: QuestionInput) =>
  request<QuestionDetail>({
    url: `${ADMIN_CONTENT_URL}/questions/${questionId}`,
    method: 'PUT',
    data,
  })

export const reviewQuestionApi = (
  questionId: string,
  data: { decision: 'APPROVE' | 'RETURN' | 'DISABLE'; remark?: string; version: number },
) =>
  request<QuestionDetail>({
    url: `${ADMIN_CONTENT_URL}/questions/${questionId}/review`,
    method: 'POST',
    data,
  })

export const deleteQuestionApi = (questionId: string, version: number) =>
  request<void>({
    url: `${ADMIN_CONTENT_URL}/questions/${questionId}`,
    method: 'DELETE',
    params: { version },
  })

export const listErrorTypesApi = (params: ErrorTypeQuery = {}) =>
  request<ErrorType[]>({ url: `${ADMIN_CONTENT_URL}/error-types`, method: 'GET', params })

export const getErrorTypeApi = (errorTypeId: string) =>
  request<ErrorType>({ url: `${ADMIN_CONTENT_URL}/error-types/${errorTypeId}`, method: 'GET' })

export const createErrorTypeApi = (data: ErrorTypeInput) =>
  request<ErrorType>({ url: `${ADMIN_CONTENT_URL}/error-types`, method: 'POST', data })

export const updateErrorTypeApi = (errorTypeId: string, data: ErrorTypeInput) =>
  request<ErrorType>({
    url: `${ADMIN_CONTENT_URL}/error-types/${errorTypeId}`,
    method: 'PUT',
    data,
  })

export const updateErrorTypeStatusApi = (
  errorTypeId: string,
  status: EnabledStatus,
  version: number,
) =>
  request<ErrorType>({
    url: `${ADMIN_CONTENT_URL}/error-types/${errorTypeId}/status`,
    method: 'PUT',
    data: { status, version },
  })

export const deleteErrorTypeApi = (errorTypeId: string, version: number) =>
  request<void>({
    url: `${ADMIN_CONTENT_URL}/error-types/${errorTypeId}`,
    method: 'DELETE',
    params: { version },
  })
