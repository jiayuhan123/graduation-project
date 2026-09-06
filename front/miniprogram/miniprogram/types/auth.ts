export interface WechatLoginInput {
  code: string
  nickname?: string
  realName?: string
  gradeLevel?: number
  semester?: number
}

export interface PasswordLoginInput {
  username: string
  password: string
}

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

export interface SessionResult {
  userId: string
  token: string
  tokenName: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

export interface StoredSession {
  token: string
  tokenName: string
  userId: string
  roles: string[]
  permissions: string[]
  activeRole: string
  expireAt: string | null
}
