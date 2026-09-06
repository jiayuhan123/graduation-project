export type EnvironmentVersion = 'develop' | 'trial' | 'release'

export interface AppConfig {
  environment: EnvironmentVersion
  apiBaseUrl: string
  requestTimeout: number
}

const API_BASE_URLS: Record<EnvironmentVersion, string> = {
  develop: 'http://127.0.0.1:10000',
  trial: '',
  release: '',
}

function resolveEnvironment(): EnvironmentVersion {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion
  } catch {
    return 'develop'
  }
}

const environment = resolveEnvironment()

/** 体验版和正式版域名在部署确定后配置，禁止把密钥写入小程序。 */
export const appConfig: AppConfig = Object.freeze({
  environment,
  apiBaseUrl: API_BASE_URLS[environment],
  requestTimeout: 15_000,
})
