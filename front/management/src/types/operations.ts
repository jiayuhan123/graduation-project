export interface UserOperationsStatistics {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  studentUsers: number
  teacherUsers: number
  adminUsers: number
}

export interface TeachingOperationsStatistics {
  totalClasses: number
  activeClasses: number
  teacherMemberships: number
  activeStudentMemberships: number
  totalAssignments: number
  publishedAssignments: number
  totalSubmissions: number
  completedSubmissions: number
}

export interface ContentOperationsStatistics {
  totalModules: number
  activeModules: number
  totalKnowledgePoints: number
  activeKnowledgePoints: number
  totalQuestions: number
  enabledQuestions: number
  draftQuestions: number
  disabledQuestions: number
  pendingFeedback: number
}

export interface LearningOperationsStatistics {
  totalPracticeTasks: number
  completedPracticeTasks: number
  totalAnswers: number
  correctAnswers: number
  answerAccuracyRate: number
  totalOcrTasks: number
  completedOcrTasks: number
  failedOcrTasks: number
  pendingReviewTasks: number
  totalRecognizedItems: number
  manuallyReviewedItems: number
  approvedItems: number
  correctedItems: number
  rejectedItems: number
  ocrFailureRate: number
  manualReviewRate: number
  reviewAccuracyRate: number
}

export interface GameOperationsStatistics {
  totalPkSessions: number
  activePkSessions: number
  completedPkSessions: number
  cancelledPkSessions: number
  expiredPkSessions: number
  abnormalPkSessions: number
  completionRate: number
  abnormalRate: number
}

export interface AuditLogItem {
  id: string
  operatorId: string
  source: string
  action: string
  targetType: string
  targetId: string
  detail: string | null
  occurredAt: string
}
