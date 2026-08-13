/** 登录请求参数 */
export interface LoginParams {
  /** 用户名或手机号 */
  username: string
  /** 登录密码（明文传输，服务端 BCrypt 比对） */
  password: string
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

/** 本地缓存的用户信息 */
export interface UserInfo {
  userId: number
  username: string
  role: string
}
