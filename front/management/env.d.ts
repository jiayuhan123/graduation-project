/// <reference types="vite/client" />

/** Vite 环境变量类型声明 */
interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE?: string
  /** 后端网关基础地址（生产环境使用） */
  readonly VITE_API_BASE_URL?: string
  /** 请求超时时间，单位毫秒 */
  readonly VITE_REQUEST_TIMEOUT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
