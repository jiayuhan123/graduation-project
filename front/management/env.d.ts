/// <reference types="vite/client" />

/** Vite 环境变量类型声明 */
interface ImportMetaEnv {
  /** 后端网关基础地址（生产环境使用） */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
