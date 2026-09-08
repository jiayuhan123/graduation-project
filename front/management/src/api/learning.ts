import { download, request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AnswerFeedback,
  CreatePracticeTaskInput,
  PracticeDetail,
  PhotoDetail,
  PhotoTask,
  PhotoTaskQuery,
  RecommendationItem,
  RecommendationQuery,
  ReviewPhotoItemInput,
  StudentLearningReport,
  BatchPhotoReviewResult,
  ClassLearningDashboard,
  ClassPracticeCreationResult,
  PhotoReviewDetail,
  PhotoReviewTask,
  PhotoReviewWorkbenchQuery,
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

export const getPhotoTaskApi = (markingTaskId: string) =>
  request<PhotoDetail>({
    url: `${LEARNING_URL}/photo-marking/${markingTaskId}`,
    method: 'GET',
  })

export const confirmPhotoItemApi = (
  markingTaskId: string,
  markingItemId: string,
  data: ReviewPhotoItemInput,
) =>
  request<PhotoDetail>({
    url: `${LEARNING_URL}/photo-marking/${markingTaskId}/items/${markingItemId}/confirm`,
    method: 'POST',
    data,
  })

export const pageStudentPhotoTasksApi = (studentId: string, params: PhotoTaskQuery = {}) =>
  request<PageResult<PhotoTask>>({
    url: `${LEARNING_URL}/photo-reviews/students/${studentId}`,
    method: 'GET',
    params,
  })

export const getStudentPhotoTaskApi = (studentId: string, markingTaskId: string) =>
  request<PhotoDetail>({
    url: `${LEARNING_URL}/photo-reviews/students/${studentId}/${markingTaskId}`,
    method: 'GET',
  })

export const reviewStudentPhotoItemApi = (
  studentId: string,
  markingTaskId: string,
  markingItemId: string,
  data: ReviewPhotoItemInput,
) =>
  request<PhotoDetail>({
    url: `${LEARNING_URL}/photo-reviews/students/${studentId}/${markingTaskId}/items/${markingItemId}`,
    method: 'POST',
    data,
  })

export const getClassLearningDashboardApi = (classId: string, days = 30) =>
  request<ClassLearningDashboard>({
    url: `${LEARNING_URL}/class-reports/${classId}`,
    method: 'GET',
    params: { days },
  })

export const createClassTargetedPracticeApi = (classId: string, data: CreatePracticeTaskInput) =>
  request<ClassPracticeCreationResult>({
    url: `${LEARNING_URL}/class-reports/${classId}/practice-tasks`,
    method: 'POST',
    data,
  })

export const pagePhotoReviewWorkbenchApi = (params: PhotoReviewWorkbenchQuery) =>
  request<PageResult<PhotoReviewTask>>({
    url: `${LEARNING_URL}/photo-reviews`,
    method: 'GET',
    params,
  })

export const getPhotoReviewWorkbenchDetailApi = (markingTaskId: string) =>
  request<PhotoReviewDetail>({
    url: `${LEARNING_URL}/photo-reviews/${markingTaskId}`,
    method: 'GET',
  })

export const reviewPhotoWorkbenchItemApi = (
  markingTaskId: string,
  markingItemId: string,
  data: ReviewPhotoItemInput,
) =>
  request<PhotoReviewDetail>({
    url: `${LEARNING_URL}/photo-reviews/${markingTaskId}/items/${markingItemId}`,
    method: 'POST',
    data,
  })

export const retryPhotoRecognitionApi = (markingTaskId: string) =>
  request<PhotoReviewDetail>({
    url: `${LEARNING_URL}/photo-reviews/${markingTaskId}/retry`,
    method: 'POST',
  })

export const batchApprovePhotoReviewApi = (markingTaskIds: string[]) =>
  request<BatchPhotoReviewResult>({
    url: `${LEARNING_URL}/photo-reviews/batch-approve`,
    method: 'POST',
    data: { markingTaskIds },
  })

export const downloadPhotoReviewFileApi = (ownerId: string, fileId: string) =>
  download({
    url: `/api/files/reviews/${fileId}`,
    method: 'GET',
    params: { ownerId },
  })
