export interface ErrorContext {
  source: 'vue' | 'router' | 'window' | 'promise'
  route?: string
  info?: string
}

/** 统一错误上报入口；接入监控平台时只需替换此处。 */
export function reportError(error: unknown, context: ErrorContext): void {
  console.error('[management-error]', context, error)
}
