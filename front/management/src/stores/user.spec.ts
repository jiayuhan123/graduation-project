import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { currentSessionApi, loginApi, logoutApi } from '@/api/user'
import { STORAGE_KEYS } from '@/constants/storage'
import { useUserStore } from '@/stores/user'
import { storage } from '@/utils/storage'

vi.mock('@/api/user', () => ({
  currentSessionApi: vi.fn(),
  loginApi: vi.fn(),
  logoutApi: vi.fn(),
}))

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('persists roles and permissions after login', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'token-1',
      tokenName: 'satoken',
      userId: '1',
      username: 'admin',
      nickname: null,
      avatarUrl: null,
      role: 'admin',
      roles: ['admin'],
      permissions: ['user:read'],
      timeout: 3600,
      expireAt: '2026-08-20T22:00:00',
    })

    const store = useUserStore()
    await store.login({ username: 'admin', password: 'secret' })

    expect(store.isLoggedIn).toBe(true)
    expect(store.roles).toEqual(['admin'])
    expect(store.permissions).toEqual(['user:read'])
    expect(storage.getString(STORAGE_KEYS.token)).toBe('token-1')
  })

  it('replaces cached permissions with the server session', async () => {
    storage.setString(STORAGE_KEYS.token, 'token-1')
    storage.setJson(STORAGE_KEYS.userInfo, {
      userId: '1',
      username: 'admin',
      roles: ['forged-role'],
      permissions: ['forged:permission'],
      timeout: 3600,
      expireAt: null,
    })
    vi.mocked(currentSessionApi).mockResolvedValue({
      userId: '1',
      token: 'token-1',
      tokenName: 'satoken',
      roles: ['admin'],
      permissions: ['user:read'],
      timeout: 3500,
      expireAt: null,
    })

    const store = useUserStore()
    await store.initializeSession()

    expect(store.roles).toEqual(['admin'])
    expect(store.permissions).toEqual(['user:read'])
    expect(store.userInfo?.username).toBe('admin')
    expect(logoutApi).not.toHaveBeenCalled()
  })
})
