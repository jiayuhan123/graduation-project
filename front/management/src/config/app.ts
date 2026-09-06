function parsePositiveInteger(value: string | undefined, fallback: number): number {
  if (!value) return fallback
  const parsed = Number.parseInt(value, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

function parseBoolean(value: string | undefined, fallback = false): boolean {
  if (!value) return fallback
  return value.trim().toLowerCase() === 'true'
}

/** 应用运行配置。VITE_ 配置会进入浏览器构建产物，禁止放置密钥。 */
export const appConfig = Object.freeze({
  name: import.meta.env.VITE_APP_TITLE?.trim() || '数学成长空间',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL?.trim() || '',
  requestTimeout: parsePositiveInteger(import.meta.env.VITE_REQUEST_TIMEOUT, 15_000),
  wechatWebLoginEnabled: parseBoolean(import.meta.env.VITE_WECHAT_WEB_LOGIN_ENABLED),
})
