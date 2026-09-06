import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type {
  AnswerQuery,
  AnswerRecord,
  ErrorRecord,
  ErrorRecordQuery,
  Mastery,
  MasteryChange,
  PhotoDetail,
  PhotoTask,
  PhotoTaskQuery,
  PracticeDetail,
  PracticeTaskQuery,
  PracticeTaskSummary,
  RecommendationItem,
  RecommendationQuery,
} from '../types/learning'

const LEARNING_BASE_URL = '/api/learning'

export const pagePracticeTasksApi = (query: PracticeTaskQuery = {}) =>
  request<PageResult<PracticeTaskSummary>>({
    path: `${LEARNING_BASE_URL}/practice-tasks`,
    query,
  })

export const getPracticeTaskApi = (taskId: string) =>
  request<PracticeDetail>({ path: `${LEARNING_BASE_URL}/practice-tasks/${taskId}` })

export const pageAnswersApi = (query: AnswerQuery = {}) =>
  request<PageResult<AnswerRecord>>({ path: `${LEARNING_BASE_URL}/answers`, query })

export const pageErrorsApi = (query: ErrorRecordQuery = {}) =>
  request<PageResult<ErrorRecord>>({ path: `${LEARNING_BASE_URL}/errors`, query })

export const listMasteryApi = (knowledgePointId?: string) =>
  request<Mastery[]>({ path: `${LEARNING_BASE_URL}/mastery`, query: { knowledgePointId } })

export const listMasteryChangesApi = (knowledgePointId: string) =>
  request<MasteryChange[]>({
    path: `${LEARNING_BASE_URL}/mastery/${knowledgePointId}/changes`,
  })

export const pageRecommendationsApi = (query: RecommendationQuery = {}) =>
  request<PageResult<RecommendationItem>>({
    path: `${LEARNING_BASE_URL}/recommendations`,
    query,
  })

export const pagePhotoTasksApi = (query: PhotoTaskQuery = {}) =>
  request<PageResult<PhotoTask>>({ path: `${LEARNING_BASE_URL}/photo-marking`, query })

export const getPhotoTaskApi = (markingTaskId: string) =>
  request<PhotoDetail>({ path: `${LEARNING_BASE_URL}/photo-marking/${markingTaskId}` })
