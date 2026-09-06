import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type {
  AssignmentDetail,
  AssignmentQuery,
  AssignmentSummary,
  AssignmentInput,
  ClassDetail,
  ClassInput,
  ClassMembership,
  ClassQuery,
  ClassStudent,
  QuestionCandidate,
  StudentAssignment,
  Submission,
  SubmissionDetail,
  SubmissionQuery,
} from '../types/teaching'

const TEACHING_BASE_URL = '/api/teaching'

export const pageTeachingClassesApi = (query: ClassQuery = {}) =>
  request<PageResult<ClassMembership>>({
    path: `${TEACHING_BASE_URL}/teacher/classes`,
    query,
  })

export const pageEnrolledClassesApi = (query: ClassQuery = {}) =>
  request<PageResult<ClassMembership>>({
    path: `${TEACHING_BASE_URL}/student/classes`,
    query,
  })

export const pageCreatedAssignmentsApi = (query: AssignmentQuery = {}) =>
  request<PageResult<AssignmentSummary>>({
    path: `${TEACHING_BASE_URL}/teacher/assignments`,
    query,
  })

export const getAssignmentApi = (assignmentId: string) =>
  request<AssignmentDetail>({ path: `${TEACHING_BASE_URL}/teacher/assignments/${assignmentId}` })

export const pageMySubmissionsApi = (query: SubmissionQuery = {}) =>
  request<PageResult<Submission>>({
    path: `${TEACHING_BASE_URL}/submissions/mine`,
    query,
  })

export const createTeachingClassApi = (data: ClassInput) =>
  request<ClassMembership, ClassInput>({
    path: `${TEACHING_BASE_URL}/teacher/classes`,
    method: 'POST',
    data,
  })

export const getTeachingClassApi = (classId: string) =>
  request<ClassDetail>({ path: `${TEACHING_BASE_URL}/teacher/classes/${classId}` })

export const addClassStudentApi = (classId: string, studentId: string, studentNoInClass?: string) =>
  request<ClassStudent, { studentId: string; studentNoInClass?: string }>({
    path: `${TEACHING_BASE_URL}/teacher/classes/${classId}/students`,
    method: 'POST',
    data: { studentId, studentNoInClass },
  })

export const removeClassStudentApi = (classId: string, studentId: string) =>
  request<void>({
    path: `${TEACHING_BASE_URL}/teacher/classes/${classId}/students/${studentId}`,
    method: 'DELETE',
  })

export const updateTeachingClassStatusApi = (
  classId: string,
  status: 'ACTIVE' | 'ARCHIVED',
  version: number,
) =>
  request<ClassMembership, { status: 'ACTIVE' | 'ARCHIVED'; version: number }>({
    path: `${TEACHING_BASE_URL}/teacher/classes/${classId}/status`,
    method: 'PUT',
    data: { status, version },
  })

export const joinTeachingClassApi = (classCode: string, studentNoInClass?: string) =>
  request<ClassMembership, { classCode: string; studentNoInClass?: string }>({
    path: `${TEACHING_BASE_URL}/student/classes/join`,
    method: 'POST',
    data: { classCode, studentNoInClass },
  })

export const leaveTeachingClassApi = (classId: string) =>
  request<void>({
    path: `${TEACHING_BASE_URL}/student/classes/${classId}/membership`,
    method: 'DELETE',
  })

export const pageQuestionCandidatesApi = (query: {
  page?: number
  size?: number
  keyword?: string
}) =>
  request<PageResult<QuestionCandidate>>({
    path: `${TEACHING_BASE_URL}/teacher/question-candidates`,
    query,
  })

export const createAssignmentApi = (data: AssignmentInput) =>
  request<AssignmentDetail, AssignmentInput>({
    path: `${TEACHING_BASE_URL}/teacher/assignments`,
    method: 'POST',
    data,
  })

export const publishAssignmentApi = (
  assignmentId: string,
  version: number,
  studentLayers: Array<{ studentId: string; layerCode: string }> = [],
) =>
  request<
    AssignmentDetail,
    { version: number; studentLayers: Array<{ studentId: string; layerCode: string }> }
  >({
    path: `${TEACHING_BASE_URL}/teacher/assignments/${assignmentId}/publish`,
    method: 'POST',
    data: { version, studentLayers },
  })

export const changeAssignmentStatusApi = (
  assignmentId: string,
  action: 'CLOSE' | 'CANCEL',
  version: number,
) =>
  request<AssignmentDetail, { action: 'CLOSE' | 'CANCEL'; version: number }>({
    path: `${TEACHING_BASE_URL}/teacher/assignments/${assignmentId}/status`,
    method: 'POST',
    data: { action, version },
  })

export const pageStudentAssignmentsApi = (query: AssignmentQuery = {}) =>
  request<PageResult<StudentAssignment>>({
    path: `${TEACHING_BASE_URL}/student/assignments`,
    query,
  })

export const getStudentAssignmentApi = (assignmentId: string) =>
  request<AssignmentDetail>({ path: `${TEACHING_BASE_URL}/student/assignments/${assignmentId}` })

export const listAssignmentSubmissionsApi = (assignmentId: string, status?: string) =>
  request<SubmissionDetail[]>({
    path: `${TEACHING_BASE_URL}/teacher/assignments/${assignmentId}/submissions`,
    query: { status },
  })

export const updateSubmissionCommentApi = (
  submissionId: string,
  teacherComment: string,
  version: number,
) =>
  request<SubmissionDetail, { teacherComment: string; version: number }>({
    path: `${TEACHING_BASE_URL}/teacher/submissions/${submissionId}/comment`,
    method: 'PUT',
    data: { teacherComment, version },
  })
