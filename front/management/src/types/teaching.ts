import type { PageParams } from '@/types/api'

export type ClassStatus = 'ACTIVE' | 'ARCHIVED'
export type MembershipStatus = 'ACTIVE' | 'LEFT'
export type AssignmentType = 'UNIFIED' | 'LAYERED'
export type AssignmentStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED'
export type SubmissionStatus = 'ASSIGNED' | 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED' | 'OVERDUE'

export interface ClassMembership {
  classId: string
  classCode: string
  name: string
  schoolName: string | null
  academicYear: string
  gradeLevel: number
  semester: number
  classStatus: ClassStatus
  membershipRole: string
  membershipStatus: MembershipStatus
  joinedAt: string
  createdBy: string
  version: number
}

export interface ClassStudent {
  studentId: string
  username: string | null
  displayName: string | null
  avatarUrl: string | null
  studentNoInClass: string | null
  status: MembershipStatus
  joinedAt: string
  leftAt: string | null
}

export interface ClassDetail {
  classInfo: ClassMembership
  students: ClassStudent[]
}

export interface QuestionCandidate {
  id: string
  code: string
  questionType: string
  stem: string
  options: string | null
  standardAnswer: string
  analysis: string | null
  difficulty: number
  estimatedSeconds: number | null
}

export interface AssignmentSummary {
  id: string
  classId: string
  teacherId: string
  title: string
  description: string | null
  assignmentType: AssignmentType
  status: AssignmentStatus
  startAt: string
  dueAt: string
  totalScore: number
  settings: string | null
  publishedAt: string | null
  version: number
}

export interface AssignmentQuestion {
  id: string
  layerCode: string
  questionId: string
  sequenceNo: number
  score: number
  questionCode: string
  questionType: string
  stem: string
  options: string | null
  standardAnswer: string | null
  analysis: string | null
  difficulty: number
}

export interface AssignmentDetail {
  assignment: AssignmentSummary
  questions: AssignmentQuestion[]
}

export interface Submission {
  id: string
  assignmentId: string
  studentId: string
  layerCode: string
  practiceTaskId: string | null
  status: SubmissionStatus
  score: number | null
  correctCount: number | null
  totalCount: number | null
  startedAt: string | null
  submittedAt: string | null
  gradedAt: string | null
  teacherComment: string | null
  version: number
}

export interface SubmissionDetail {
  submission: Submission
  username: string | null
  displayName: string | null
  avatarUrl: string | null
}

export interface StudentAssignment {
  assignment: AssignmentSummary
  submission: Submission
}

export interface ClassInput {
  name: string
  schoolName?: string
  academicYear: string
  gradeLevel: number
  semester: number
  version?: number
}

export interface AssignmentQuestionInput {
  questionId: string
  layerCode: string
  sequenceNo: number
  score: number
}

export interface AssignmentInput {
  classId?: string
  title: string
  description?: string
  assignmentType: AssignmentType
  startAt: string
  dueAt: string
  settings?: string
  questions: AssignmentQuestionInput[]
  version?: number
}

export interface ClassQuery extends PageParams {
  status?: MembershipStatus
}

export interface AssignmentQuery extends PageParams {
  status?: AssignmentStatus | SubmissionStatus
}
