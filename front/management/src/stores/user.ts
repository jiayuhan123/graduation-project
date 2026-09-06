import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { completeWechatWebLoginApi, currentSessionApi, loginApi, logoutApi } from '@/api/user'
import { DEFAULT_TOKEN_NAME, LEGACY_STORAGE_KEYS, STORAGE_KEYS } from '@/constants/storage'
import type {
  LoginParams,
  LoginResult,
  SessionResult,
  UserInfo,
  UserProfile,
  WechatWebCallbackInput,
} from '@/types/user'
import { storage } from '@/utils/storage'

function migrateLegacyStorage(): void {
  if (!storage.getString(STORAGE_KEYS.token)) {
    const legacyToken = storage.getString(LEGACY_STORAGE_KEYS.token)
    if (legacyToken) storage.setString(STORAGE_KEYS.token, legacyToken)
  }
  if (!storage.getString(STORAGE_KEYS.userInfo)) {
    const legacyUser = storage.getString(LEGACY_STORAGE_KEYS.userInfo)
    if (legacyUser) storage.setString(STORAGE_KEYS.userInfo, legacyUser)
  }
  storage.removeMany(Object.values(LEGACY_STORAGE_KEYS))
}

function normalized(values: string[] | undefined): string[] {
  return [...new Set((values ?? []).filter(Boolean))]
}

/** 登录状态、服务端会话和 RBAC 权限。 */
export const useUserStore = defineStore('user', () => {
  migrateLegacyStorage()

  const token = ref(storage.getString(STORAGE_KEYS.token) ?? '')
  const tokenName = ref(storage.getString(STORAGE_KEYS.tokenName) ?? DEFAULT_TOKEN_NAME)
  const userInfo = ref<UserInfo | null>(storage.getJson<UserInfo>(STORAGE_KEYS.userInfo))
  const activeRole = ref(storage.getString(STORAGE_KEYS.activeRole) ?? '')
  const initialized = ref(false)
  let initializationTask: Promise<void> | null = null

  const hasToken = computed(() => Boolean(token.value))
  const isLoggedIn = computed(() => initialized.value && hasToken.value && Boolean(userInfo.value))
  const roles = computed(() => userInfo.value?.roles ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')

  function persistSession(): void {
    if (!token.value || !userInfo.value) return
    storage.setString(STORAGE_KEYS.token, token.value)
    storage.setString(STORAGE_KEYS.tokenName, tokenName.value)
    storage.setJson(STORAGE_KEYS.userInfo, userInfo.value)
    if (activeRole.value) storage.setString(STORAGE_KEYS.activeRole, activeRole.value)
  }

  function normalizeActiveRole(availableRoles: string[], preferred?: string): string {
    if (preferred && availableRoles.includes(preferred)) return preferred
    if (availableRoles.includes('student')) return 'student'
    return availableRoles[0] ?? ''
  }

  function applyLogin(result: LoginResult): void {
    token.value = result.token
    tokenName.value = result.tokenName || DEFAULT_TOKEN_NAME
    userInfo.value = {
      userId: result.userId,
      username: result.username,
      nickname: result.nickname,
      avatarUrl: result.avatarUrl,
      roles: normalized(result.roles.length ? result.roles : [result.role]),
      permissions: normalized(result.permissions),
      timeout: result.timeout,
      expireAt: result.expireAt,
    }
    activeRole.value = normalizeActiveRole(userInfo.value.roles, activeRole.value || result.role)
    initialized.value = true
    persistSession()
  }

  function applySession(result: SessionResult): void {
    const cachedUsername =
      userInfo.value?.userId === result.userId ? userInfo.value.username : undefined
    token.value = result.token || token.value
    tokenName.value = result.tokenName || tokenName.value
    userInfo.value = {
      userId: result.userId,
      username: cachedUsername,
      nickname: userInfo.value?.nickname,
      avatarUrl: userInfo.value?.avatarUrl,
      roles: normalized(result.roles),
      permissions: normalized(result.permissions),
      timeout: result.timeout,
      expireAt: result.expireAt,
    }
    activeRole.value = normalizeActiveRole(userInfo.value.roles, activeRole.value)
    initialized.value = true
    persistSession()
  }

  /** 使用服务端会话校验本地 Token，禁止直接信任缓存中的权限。 */
  function initializeSession(): Promise<void> {
    if (initialized.value) return Promise.resolve()
    if (!token.value) {
      initialized.value = true
      userInfo.value = null
      return Promise.resolve()
    }
    if (!initializationTask) {
      initializationTask = currentSessionApi()
        .then(applySession)
        .finally(() => {
          initializationTask = null
        })
    }
    return initializationTask
  }

  async function login(params: LoginParams): Promise<void> {
    applyLogin(await loginApi(params))
  }

  async function completeWechatWebLogin(params: WechatWebCallbackInput): Promise<void> {
    applyLogin(await completeWechatWebLoginApi(params))
  }

  function setActiveRole(role: string): void {
    if (!roles.value.includes(role)) return
    activeRole.value = role
    storage.setString(STORAGE_KEYS.activeRole, role)
  }

  function syncProfile(profile: UserProfile): void {
    if (!userInfo.value) return
    userInfo.value = {
      ...userInfo.value,
      username: profile.username,
      nickname: profile.nickname,
      avatarUrl: profile.avatarUrl,
      roles: normalized(profile.roles),
      permissions: normalized(profile.permissions),
    }
    activeRole.value = normalizeActiveRole(userInfo.value.roles, activeRole.value)
    persistSession()
  }

  /** 清理浏览器内会话；供 401 和主动退出共用。 */
  function clearSession(): void {
    token.value = ''
    tokenName.value = DEFAULT_TOKEN_NAME
    userInfo.value = null
    activeRole.value = ''
    initialized.value = true
    storage.removeMany(Object.values(STORAGE_KEYS))
  }

  /** 优先注销服务端会话，即使请求失败也确保当前浏览器退出。 */
  async function logout(): Promise<boolean> {
    let serverLoggedOut = true
    if (token.value) {
      try {
        await logoutApi()
      } catch {
        serverLoggedOut = false
      }
    }
    clearSession()
    return serverLoggedOut
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  return {
    token,
    tokenName,
    userInfo,
    initialized,
    hasToken,
    isLoggedIn,
    roles,
    permissions,
    activeRole,
    displayName,
    initializeSession,
    login,
    completeWechatWebLogin,
    setActiveRole,
    syncProfile,
    clearSession,
    logout,
    hasRole,
    hasPermission,
  }
})
