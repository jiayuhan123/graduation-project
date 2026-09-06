import { DEFAULT_TOKEN_NAME, STORAGE_KEYS } from '../constants/storage'
import { preferredRole } from '../constants/roles'
import type { LoginResult, StoredSession } from '../types/auth'
import { getStorage, removeStorage, setStorage } from './storage'

export function getSession(): StoredSession | null {
  const session = getStorage<StoredSession>(STORAGE_KEYS.session)
  if (!session) return null
  const activeRole = preferredRole(session.roles, session.activeRole)
  return activeRole === session.activeRole ? session : { ...session, activeRole }
}

export function saveSession(result: LoginResult): StoredSession {
  const session: StoredSession = {
    token: result.token,
    tokenName: result.tokenName || DEFAULT_TOKEN_NAME,
    userId: result.userId,
    roles: result.roles,
    permissions: result.permissions,
    activeRole: preferredRole(result.roles, result.role),
    expireAt: result.expireAt,
  }
  setStorage(STORAGE_KEYS.session, session)
  return session
}

export function setActiveRole(activeRole: string): StoredSession | null {
  const session = getSession()
  if (!session || !session.roles.includes(activeRole)) return null
  const updated = { ...session, activeRole }
  setStorage(STORAGE_KEYS.session, updated)
  return updated
}

export function clearSession(): void {
  removeStorage(STORAGE_KEYS.session)
}
