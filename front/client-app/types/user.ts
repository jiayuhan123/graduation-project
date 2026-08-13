/** 登录请求参数 */
export interface LoginParams {
  /** 用户名或手机号 */
  username: string
  /** 登录密码（明文传输，服务端 BCrypt 比对） */
  password: string
}

/** 注册请求参数 */
export interface RegisterParams {
  /** 用户名（3-20 位，仅字母/数字/下划线） */
  username: string
  /** 登录密码（6-20 位） */
  password: string
  /** 真实姓名（选填） */
  realName?: string
  /** 手机号（1 开头 11 位数字） */
  phone: string
}

/** 登录成功返回数据 */
export interface LoginResult {
  token: string
  tokenName: string
  userId: number
  username: string
  role: string
  expireAt: string
}

/** 注册成功返回数据 */
export interface RegisterResult {
  id: number
  username: string
}

/** 本地缓存的用户信息 */
export interface UserInfo {
  userId: number
  username: string
  role: string
}
