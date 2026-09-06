/** 登录请求参数 */
export interface LoginParams {
  /** 用户名、手机号或邮箱 */
  username: string
  /** 登录密码；生产环境必须通过 HTTPS 传输 */
  password: string
}

/** 登录成功返回数据 */
export interface LoginResult {
  token: string
  tokenName: string
  userId: string
  username: string
  nickname: string | null
  avatarUrl: string | null
  role: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

/** 当前服务端会话。 */
export interface SessionResult {
  userId: string
  token: string
  tokenName: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

/** 本地缓存的用户信息 */
export interface UserInfo {
  userId: string
  username?: string
  nickname?: string | null
  avatarUrl?: string | null
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

export interface WechatWebAuthorizeInput {
  nickname?: string
  realName?: string
  gradeLevel: number
  semester: number
}

export interface WechatWebAuthorizeResult {
  authorizationUrl: string
  state: string
  expiresInSeconds: number
}

export interface WechatWebCallbackInput {
  code: string
  state: string
}

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
