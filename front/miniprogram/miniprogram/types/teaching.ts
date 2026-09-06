import type { PageParams } from './api'

export interface ClassMembership {
  classId: string
  classCode: string
  name: string
  schoolName: string | null
  academicYear: string
  gradeLevel: number
  semester: number
  classStatus: string
  membershipRole: string
  membershipStatus: string
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
  status: string
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
  assignmentType: string
  status: string
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
  status: string
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
  classId: string
  title: string
  description?: string
  assignmentType: 'UNIFIED' | 'LAYERED'
  startAt: string
  dueAt: string
  settings?: string
  questions: AssignmentQuestionInput[]
}

export interface ClassQuery extends PageParams {
  status?: string
}

export interface AssignmentQuery extends PageParams {
  status?: string
}

export interface SubmissionQuery extends PageParams {
  status?: string
}
