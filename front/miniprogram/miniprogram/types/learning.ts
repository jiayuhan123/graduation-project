import type { PageParams } from './api'

export interface PracticeTaskSummary {
  id: string
  studentId: string
  sourceType: string
  sourceId: string | null
  title: string
  mode: string
  status: string
  questionCount: number
  timeLimitSeconds: number | null
  targetAccuracy: number | null
  startedAt: string | null
  completedAt: string | null
}

export interface PracticeQuestion {
  id: string
  questionId: string
  sequenceNo: number
  maxScore: number
}

export interface PracticeDetail {
  task: PracticeTaskSummary
  knowledgePointIds: string[]
  questions: PracticeQuestion[]
}

export interface AnswerRecord {
  id: string
  taskQuestionId: string
  attemptNo: number
  studentAnswer: string | null
  gradingStatus: string
  correct: boolean | null
  score: number | null
  responseMs: number | null
  answerSource: string
  recognitionConfidence: number | null
  submittedAt: string
  gradedAt: string | null
}

export interface ErrorRecord {
  id: string
  answerRecordId: string
  questionId: string
  knowledgePointId: string
  errorTypeId: string | null
  errorPosition: string | null
  diagnosis: string | null
  confidence: number | null
  status: string
  resolvedByAnswerId: string | null
  resolvedAt: string | null
}

export interface Mastery {
  id: string
  knowledgePointId: string
  masteryScore: number
  accuracyRate: number
  avgResponseMs: number | null
  totalAnswerCount: number
  correctAnswerCount: number
  consecutiveCorrectCount: number
  lastPracticedAt: string | null
  nextReviewAt: string | null
}

export interface MasteryChange {
  id: string
  answerRecordId: string
  beforeScore: number
  afterScore: number
  deltaScore: number
  reason: string
  occurredAt: string
}

export interface RecommendationItem {
  id: string
  knowledgePointId: string
  sourceType: string
  sourceId: string | null
  reasonText: string
  priority: number
  recommendedQuestionCount: number
  status: string
  generatedTaskId: string | null
  generatedAt: string
  expiresAt: string | null
  acceptedAt: string | null
  completedAt: string | null
}

export interface PhotoTask {
  id: string
  originalFileId: string
  practiceTaskId: string | null
  status: string
  pageCount: number
  overallConfidence: number | null
  recognizedItemCount: number
  reviewStatus: string
  submittedAt: string
  processedAt: string | null
  reviewedAt: string | null
  failureReason: string | null
}

export interface PhotoItem {
  id: string
  itemNo: number
  cropFileId: string | null
  boundingBox: string | null
  recognizedStem: string | null
  recognizedAnswer: string | null
  matchedQuestionId: string | null
  recognitionConfidence: number | null
  taskQuestionId: string | null
  answerRecordId: string | null
  status: string
}

export interface PhotoDetail {
  task: PhotoTask
  items: PhotoItem[]
}

export interface PracticeTaskQuery extends PageParams {
  status?: string
}

export interface AnswerQuery extends PageParams {
  gradingStatus?: string
}

export interface ErrorRecordQuery extends PageParams {
  status?: string
  knowledgePointId?: string
}

export interface RecommendationQuery extends PageParams {
  status?: string
}

export interface PhotoTaskQuery extends PageParams {
  status?: string
}
