import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type {
  AnswerQuery,
  AnswerFeedback,
  AnswerRecord,
  CreatePracticeTaskInput,
  ErrorRecord,
  ErrorRecordQuery,
  LearningOverview,
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
  StudentLearningReport,
  SubmitAnswerInput,
} from '../types/learning'

const LEARNING_BASE_URL = '/api/learning'

export const createPracticeTaskApi = (data: CreatePracticeTaskInput) =>
  request<PracticeDetail, CreatePracticeTaskInput>({
    path: `${LEARNING_BASE_URL}/practice-tasks`,
    method: 'POST',
    data,
  })

export const createAssignmentTaskApi = (assignmentId: string) =>
  request<PracticeDetail>({
    path: `${LEARNING_BASE_URL}/assignment-tasks/${assignmentId}`,
    method: 'POST',
  })

export const startPracticeTaskApi = (taskId: string) =>
  request<PracticeDetail>({
    path: `${LEARNING_BASE_URL}/practice-tasks/${taskId}/start`,
    method: 'POST',
  })

export const submitPracticeAnswerApi = (
  taskId: string,
  taskQuestionId: string,
  data: SubmitAnswerInput,
) =>
  request<AnswerFeedback, SubmitAnswerInput>({
    path: `${LEARNING_BASE_URL}/practice-tasks/${taskId}/questions/${taskQuestionId}/answers`,
    method: 'POST',
    data,
  })

export const completePracticeTaskApi = (taskId: string) =>
  request<PracticeDetail>({
    path: `${LEARNING_BASE_URL}/practice-tasks/${taskId}/complete`,
    method: 'POST',
  })

export const getLearningOverviewApi = () =>
  request<LearningOverview>({ path: `${LEARNING_BASE_URL}/overview` })

export const getOwnLearningReportApi = () =>
  request<StudentLearningReport>({ path: `${LEARNING_BASE_URL}/reports/me` })

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

export const acceptRecommendationApi = (recommendationId: string) =>
  request<PracticeDetail>({
    path: `${LEARNING_BASE_URL}/recommendations/${recommendationId}/accept`,
    method: 'POST',
  })

export const dismissRecommendationApi = (recommendationId: string) =>
  request<void>({
    path: `${LEARNING_BASE_URL}/recommendations/${recommendationId}`,
    method: 'DELETE',
  })

export const pagePhotoTasksApi = (query: PhotoTaskQuery = {}) =>
  request<PageResult<PhotoTask>>({ path: `${LEARNING_BASE_URL}/photo-marking`, query })

export const getPhotoTaskApi = (markingTaskId: string) =>
  request<PhotoDetail>({ path: `${LEARNING_BASE_URL}/photo-marking/${markingTaskId}` })
