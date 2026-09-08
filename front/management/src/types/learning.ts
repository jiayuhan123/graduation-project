import type { PageParams } from '@/types/api'

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

export interface PracticeQuestion {
  id: string
  questionId: string
  sequenceNo: number
  questionType: string
  stem: string
  options: string | null
  difficulty: number
  estimatedSeconds: number | null
  maxScore: number
  latestAnswer: AnswerRecord | null
  solutionVisible: boolean
  standardAnswer: string | null
  analysis: string | null
}

export interface PracticeDetail {
  task: PracticeTaskSummary
  knowledgePointIds: string[]
  questions: PracticeQuestion[]
}

export interface CreatePracticeTaskInput {
  knowledgePointId: string
  difficulty?: number
  questionCount?: number
  mode?: 'NORMAL' | 'TIMED' | 'SPECIAL' | 'REVIEW'
  timeLimitSeconds?: number
  targetAccuracy?: number
}

export interface AnswerFeedback {
  taskId: string
  taskQuestionId: string
  answer: AnswerRecord
  solutionVisible: boolean
  standardAnswer: string | null
  analysis: string | null
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

export interface LearningOverview {
  studentId: string
  taskCount: number
  completedTaskCount: number
  answerCount: number
  correctAnswerCount: number
  accuracyRate: number
  openErrorCount: number
  weakKnowledgePointCount: number
  pendingRecommendationCount: number
}

export interface StudentLearningReport {
  studentId: string
  username: string
  displayName: string
  avatarUrl: string | null
  overview: LearningOverview
  recentTasks: PracticeTaskSummary[]
  weakKnowledgePoints: Mastery[]
  recentErrors: ErrorRecord[]
  recommendations: RecommendationItem[]
}

export interface RecommendationQuery extends PageParams {
  status?: string
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

export interface ReviewPhotoItemInput {
  decision: 'APPROVE' | 'CORRECT' | 'REJECT'
  recognizedStem?: string
  recognizedAnswer?: string
  matchedQuestionId?: string
  comment?: string
}

export interface PhotoTaskQuery extends PageParams {
  status?: string
}

export interface PhotoReviewTask {
  task: PhotoTask
  studentId: string
  studentName: string
  pendingItemCount: number
}

export interface PhotoQuestionReference {
  questionId: string
  code: string
  stem: string
  standardAnswer: string
}

export interface PhotoReviewItem {
  item: PhotoItem
  matchedQuestion: PhotoQuestionReference | null
}

export interface PhotoReviewHistory {
  id: string
  markingItemId: string | null
  reviewerId: string
  reviewerName: string
  decision: 'APPROVE' | 'CORRECT' | 'REJECT'
  beforeData: string | null
  afterData: string | null
  comment: string | null
  reviewedAt: string
}

export interface PhotoReviewDetail {
  summary: PhotoReviewTask
  items: PhotoReviewItem[]
  history: PhotoReviewHistory[]
}

export interface PhotoReviewWorkbenchQuery extends PageParams {
  classId?: string
  studentId?: string
  status?: string
  reviewStatus?: string
  submittedFrom?: string
  submittedTo?: string
}

export interface BatchPhotoReviewResult {
  approvedItemCount: number
  completedTaskCount: number
}

export interface ClassLearningMetrics {
  studentCount: number
  taskCount: number
  completedTaskCount: number
  completionRate: number
  answerCount: number
  correctAnswerCount: number
  accuracyRate: number
  openErrorCount: number
}

export interface ClassWeakKnowledgePoint {
  knowledgePointId: string
  knowledgePointName: string
  averageMastery: number
  averageAccuracy: number
  affectedStudentCount: number
  assessedStudentCount: number
}

export interface ClassFrequentQuestion {
  questionId: string
  questionCode: string | null
  stem: string
  errorCount: number
  affectedStudentCount: number
}

export interface ClassFrequentErrorType {
  errorTypeId: string
  errorTypeName: string
  category: string | null
  errorCount: number
  affectedStudentCount: number
}

export interface ClassDailyTrend {
  date: string
  answerCount: number
  correctAnswerCount: number
  accuracyRate: number
}

export interface ClassStudentSnapshot {
  studentId: string
  username: string | null
  displayName: string
  avatarUrl: string | null
  taskCount: number
  completedTaskCount: number
  answerCount: number
  correctAnswerCount: number
  accuracyRate: number
  openErrorCount: number
  weakKnowledgePointCount: number
  lastActivityAt: string | null
}

export interface ClassStudentAlert {
  studentId: string
  displayName: string
  code: string
  message: string
  severity: 'HIGH' | 'MEDIUM'
}

export interface ClassLearningDashboard {
  classId: string
  className: string
  schoolName: string | null
  gradeLevel: number
  semester: number
  rangeDays: number
  rangeStart: string
  rangeEnd: string
  metrics: ClassLearningMetrics
  weakKnowledgePoints: ClassWeakKnowledgePoint[]
  frequentWrongQuestions: ClassFrequentQuestion[]
  frequentErrorTypes: ClassFrequentErrorType[]
  dailyTrends: ClassDailyTrend[]
  students: ClassStudentSnapshot[]
  alerts: ClassStudentAlert[]
}

export interface ClassPracticeCreationResult {
  createdCount: number
  taskIds: string[]
}
