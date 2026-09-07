import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AnswerFeedback,
  CreatePracticeTaskInput,
  PracticeDetail,
  PhotoTask,
  PhotoTaskQuery,
  RecommendationItem,
  RecommendationQuery,
  StudentLearningReport,
} from '@/types/learning'

const LEARNING_URL = '/api/learning'

export const createPracticeTaskApi = (data: CreatePracticeTaskInput) =>
  request<PracticeDetail>({ url: `${LEARNING_URL}/practice-tasks`, method: 'POST', data })

export const startPracticeTaskApi = (taskId: string) =>
  request<PracticeDetail>({
    url: `${LEARNING_URL}/practice-tasks/${taskId}/start`,
    method: 'POST',
  })

export const createAssignmentTaskApi = (assignmentId: string) =>
  request<PracticeDetail>({
    url: `${LEARNING_URL}/assignment-tasks/${assignmentId}`,
    method: 'POST',
  })

export const submitPracticeAnswerApi = (
  taskId: string,
  taskQuestionId: string,
  data: { answer: string; responseMs?: number },
) =>
  request<AnswerFeedback>({
    url: `${LEARNING_URL}/practice-tasks/${taskId}/questions/${taskQuestionId}/answers`,
    method: 'POST',
    data,
  })

export const completePracticeTaskApi = (taskId: string) =>
  request<PracticeDetail>({
    url: `${LEARNING_URL}/practice-tasks/${taskId}/complete`,
    method: 'POST',
  })

export const getOwnLearningReportApi = () =>
  request<StudentLearningReport>({ url: `${LEARNING_URL}/reports/me`, method: 'GET' })

export const getStudentLearningReportApi = (studentId: string) =>
  request<StudentLearningReport>({
    url: `${LEARNING_URL}/reports/students/${studentId}`,
    method: 'GET',
  })

export const pageRecommendationsApi = (params: RecommendationQuery = {}) =>
  request<PageResult<RecommendationItem>>({
    url: `${LEARNING_URL}/recommendations`,
    method: 'GET',
    params,
  })

export const acceptRecommendationApi = (recommendationId: string) =>
  request<PracticeDetail>({
    url: `${LEARNING_URL}/recommendations/${recommendationId}/accept`,
    method: 'POST',
  })

export const dismissRecommendationApi = (recommendationId: string) =>
  request<void>({
    url: `${LEARNING_URL}/recommendations/${recommendationId}`,
    method: 'DELETE',
  })

export const pagePhotoTasksApi = (params: PhotoTaskQuery = {}) =>
  request<PageResult<PhotoTask>>({
    url: `${LEARNING_URL}/photo-marking`,
    method: 'GET',
    params,
  })
