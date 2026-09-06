import { MessagePlugin } from 'tdesign-vue-next'
import type { Router } from 'vue-router'
import { hasAccess } from '@/auth/permission'
import { appConfig } from '@/config/app'
import { safeRedirect } from '@/router/redirect'
import { useUserStore } from '@/stores/user'
import { isApiError } from '@/types/api'
import { reportError } from '@/utils/error-reporter'

function loginLocation(fullPath: string) {
  return { name: 'login', query: { redirect: safeRedirect(fullPath) } }
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const userStore = useUserStore()
    document.title = `${to.meta.title} - ${appConfig.name}`

    if (to.meta.requiresAuth === false) {
      if (to.name === 'login' && userStore.hasToken) {
        try {
          await userStore.initializeSession()
          if (userStore.isLoggedIn) return { name: 'dashboard' }
        } catch (error) {
          if (isApiError(error) && (error.code === 401 || error.status === 401)) {
            userStore.clearSession()
          }
        }
      }
      return true
    }

    if (!userStore.hasToken) return loginLocation(to.fullPath)

    try {
      await userStore.initializeSession()
    } catch (error) {
      if (isApiError(error) && (error.code === 401 || error.status === 401)) {
        userStore.clearSession()
        void MessagePlugin.warning('登录已过期，请重新登录')
        return loginLocation(to.fullPath)
      }
      void MessagePlugin.error('会话校验失败，请检查网络后重试')
      return false
    }

    if (
      !hasAccess(
        {
          roles: userStore.activeRole ? [userStore.activeRole] : userStore.roles,
          permissions: userStore.permissions,
        },
        {
          roles: to.meta.roles,
          permissions: to.meta.permissions,
        },
      )
    ) {
      return { name: 'forbidden', query: { from: to.fullPath } }
    }
    return true
  })

  router.onError((error, to) => {
    reportError(error, { source: 'router', route: to.fullPath })
  })
}
