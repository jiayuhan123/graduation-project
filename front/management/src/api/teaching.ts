import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AssignmentDetail,
  AssignmentInput,
  AssignmentQuery,
  AssignmentSummary,
  ClassDetail,
  ClassInput,
  ClassMembership,
  ClassQuery,
  ClassStudent,
  QuestionCandidate,
  StudentAssignment,
  SubmissionDetail,
} from '@/types/teaching'

const TEACHER_URL = '/api/teaching/teacher'
const STUDENT_URL = '/api/teaching/student'

export const pageTeacherClassesApi = (params: ClassQuery = {}) =>
  request<PageResult<ClassMembership>>({ url: `${TEACHER_URL}/classes`, method: 'GET', params })

export const getTeacherClassApi = (classId: string) =>
  request<ClassDetail>({ url: `${TEACHER_URL}/classes/${classId}`, method: 'GET' })

export const createTeachingClassApi = (data: ClassInput) =>
  request<ClassMembership>({ url: `${TEACHER_URL}/classes`, method: 'POST', data })

export const updateTeachingClassApi = (classId: string, data: ClassInput) =>
  request<ClassMembership>({ url: `${TEACHER_URL}/classes/${classId}`, method: 'PUT', data })

export const updateTeachingClassStatusApi = (
  classId: string,
  status: 'ACTIVE' | 'ARCHIVED',
  version: number,
) =>
  request<ClassMembership>({
    url: `${TEACHER_URL}/classes/${classId}/status`,
    method: 'PUT',
    data: { status, version },
  })

export const addClassStudentApi = (classId: string, studentId: string, studentNoInClass?: string) =>
  request<ClassStudent>({
    url: `${TEACHER_URL}/classes/${classId}/students`,
    method: 'POST',
    data: { studentId, studentNoInClass },
  })

export const removeClassStudentApi = (classId: string, studentId: string) =>
  request<void>({
    url: `${TEACHER_URL}/classes/${classId}/students/${studentId}`,
    method: 'DELETE',
  })

export const pageStudentClassesApi = (params: ClassQuery = {}) =>
  request<PageResult<ClassMembership>>({ url: `${STUDENT_URL}/classes`, method: 'GET', params })

export const joinTeachingClassApi = (classCode: string, studentNoInClass?: string) =>
  request<ClassMembership>({
    url: `${STUDENT_URL}/classes/join`,
    method: 'POST',
    data: { classCode, studentNoInClass },
  })

export const leaveTeachingClassApi = (classId: string) =>
  request<void>({ url: `${STUDENT_URL}/classes/${classId}/membership`, method: 'DELETE' })

export const pageTeacherAssignmentsApi = (params: AssignmentQuery = {}) =>
  request<PageResult<AssignmentSummary>>({
    url: `${TEACHER_URL}/assignments`,
    method: 'GET',
    params,
  })

export const getTeacherAssignmentApi = (assignmentId: string) =>
  request<AssignmentDetail>({ url: `${TEACHER_URL}/assignments/${assignmentId}`, method: 'GET' })

export const createAssignmentApi = (data: AssignmentInput) =>
  request<AssignmentDetail>({ url: `${TEACHER_URL}/assignments`, method: 'POST', data })

export const updateAssignmentApi = (assignmentId: string, data: AssignmentInput) =>
  request<AssignmentDetail>({
    url: `${TEACHER_URL}/assignments/${assignmentId}`,
    method: 'PUT',
    data,
  })

export const publishAssignmentApi = (
  assignmentId: string,
  version: number,
  studentLayers: Array<{ studentId: string; layerCode: string }>,
) =>
  request<AssignmentDetail>({
    url: `${TEACHER_URL}/assignments/${assignmentId}/publish`,
    method: 'POST',
    data: { version, studentLayers },
  })

export const changeAssignmentStatusApi = (
  assignmentId: string,
  action: 'CLOSE' | 'CANCEL',
  version: number,
) =>
  request<AssignmentDetail>({
    url: `${TEACHER_URL}/assignments/${assignmentId}/status`,
    method: 'POST',
    data: { action, version },
  })

export const pageQuestionCandidatesApi = (params: {
  page?: number
  size?: number
  keyword?: string
  questionType?: string
  difficulty?: number
}) =>
  request<PageResult<QuestionCandidate>>({
    url: `${TEACHER_URL}/question-candidates`,
    method: 'GET',
    params,
  })

export const listAssignmentSubmissionsApi = (assignmentId: string, status?: string) =>
  request<SubmissionDetail[]>({
    url: `${TEACHER_URL}/assignments/${assignmentId}/submissions`,
    method: 'GET',
    params: { status },
  })

export const updateSubmissionCommentApi = (
  submissionId: string,
  teacherComment: string,
  version: number,
) =>
  request<SubmissionDetail>({
    url: `${TEACHER_URL}/submissions/${submissionId}/comment`,
    method: 'PUT',
    data: { teacherComment, version },
  })

export const pageStudentAssignmentsApi = (params: AssignmentQuery = {}) =>
  request<PageResult<StudentAssignment>>({
    url: `${STUDENT_URL}/assignments`,
    method: 'GET',
    params,
  })

export const getStudentAssignmentApi = (assignmentId: string) =>
  request<AssignmentDetail>({ url: `${STUDENT_URL}/assignments/${assignmentId}`, method: 'GET' })
