import { beforeEach, describe, expect, it } from 'vitest'
import { storage } from '@/utils/storage'

describe('storage', () => {
  beforeEach(() => localStorage.clear())

  it('round-trips JSON values', () => {
    storage.setJson('test', { enabled: true })
    expect(storage.getJson<{ enabled: boolean }>('test')).toEqual({ enabled: true })
  })

  it('removes malformed JSON', () => {
    localStorage.setItem('broken', '{')
    expect(storage.getJson('broken')).toBeNull()
    expect(localStorage.getItem('broken')).toBeNull()
  })
})
