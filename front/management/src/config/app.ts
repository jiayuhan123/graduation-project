function parsePositiveInteger(value: string | undefined, fallback: number): number {
  if (!value) return fallback
  const parsed = Number.parseInt(value, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

/** 应用运行配置。VITE_ 配置会进入浏览器构建产物，禁止放置密钥。 */
export const appConfig = Object.freeze({
  name: import.meta.env.VITE_APP_TITLE?.trim() || '项目管理后台',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL?.trim() || '',
  requestTimeout: parsePositiveInteger(import.meta.env.VITE_REQUEST_TIMEOUT, 15_000),
})
