import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    title: string
    requiresAuth?: boolean
    roles?: string[]
    permissions?: string[]
    hidden?: boolean
  }
}
