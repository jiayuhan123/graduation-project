import type { App } from 'vue'
import { permissionDirective } from '@/directives/permission'

export function setupDirectives(app: App): void {
  app.directive('permission', permissionDirective)
}
