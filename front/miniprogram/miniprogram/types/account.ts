export interface StudentProfile {
  userId: string
  studentNo: string | null
  gradeLevel: number
  semester: number
  updatedAt: string
}

export interface TeacherProfile {
  userId: string
  teacherNo: string | null
  title: string | null
  introduction: string | null
  updatedAt: string
}

export interface UserProfile {
  id: string
  username: string
  nickname: string | null
  realName: string | null
  email: string | null
  phone: string | null
  avatarUrl: string | null
  status: number
  roles: string[]
  permissions: string[]
  wechatBound: boolean
  passwordSet: boolean
  studentProfile: StudentProfile | null
  teacherProfile: TeacherProfile | null
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UpdateProfileInput {
  nickname?: string
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
}

export interface UpdateStudentProfileInput {
  gradeLevel: number
  semester: number
}

export interface UpdateTeacherProfileInput {
  title?: string
  introduction?: string
}

export interface ChangePasswordInput {
  oldPassword: string
  newPassword: string
}

export interface InitializePasswordInput {
  newPassword: string
}

export interface WechatCodeInput {
  code: string
}
