import type { Directive } from 'vue'
import { hasAnyPermission } from '@/auth/permission'
import { useUserStore } from '@/stores/user'

export type PermissionValue = string | string[]

/** 按钮级权限只控制界面展示，后端仍必须执行最终授权。 */
export const permissionDirective: Directive<HTMLElement, PermissionValue> = {
  mounted(element, binding) {
    const userStore = useUserStore()
    if (!hasAnyPermission(userStore.permissions, binding.value)) element.remove()
  },
}
