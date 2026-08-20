import { describe, expect, it } from 'vitest'
import { hasAccess, hasAnyPermission } from '@/auth/permission'

describe('permission helpers', () => {
  const subject = {
    roles: ['admin'],
    permissions: ['user:read', 'user:update'],
  }

  it('allows empty requirements', () => {
    expect(hasAccess(subject, {})).toBe(true)
  })

  it('requires both configured categories', () => {
    expect(hasAccess(subject, { roles: ['admin'], permissions: ['user:read'] })).toBe(true)
    expect(hasAccess(subject, { roles: ['admin'], permissions: ['user:delete'] })).toBe(false)
  })

  it('matches any button permission', () => {
    expect(hasAnyPermission(subject.permissions, ['user:delete', 'user:update'])).toBe(true)
  })
})
