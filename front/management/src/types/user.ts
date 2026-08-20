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
  userId: number
  username: string
  role: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

/** 当前服务端会话。 */
export interface SessionResult {
  userId: number
  token: string
  tokenName: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}

/** 本地缓存的用户信息 */
export interface UserInfo {
  userId: number
  username?: string
  roles: string[]
  permissions: string[]
  timeout: number
  expireAt: string | null
}
