import { describe, expect, it } from 'vitest'
import { safeRedirect } from '@/router/redirect'

describe('safeRedirect', () => {
  it('keeps internal absolute paths', () => {
    expect(safeRedirect('/users?page=2')).toBe('/users?page=2')
  })

  it.each(['https://example.com', '//example.com', '/login?redirect=/dashboard', 'dashboard'])(
    'rejects unsafe target %s',
    (target) => {
      expect(safeRedirect(target)).toBe('/')
    },
  )

  it('uses fallback for non-string values', () => {
    expect(safeRedirect(['dashboard'], '/dashboard')).toBe('/dashboard')
  })
})
