function localStorageOrNull(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage
  } catch {
    return null
  }
}

/** 对 localStorage 的容错、类型化封装。 */
export const storage = {
  getString(key: string): string | null {
    return localStorageOrNull()?.getItem(key) ?? null
  },

  setString(key: string, value: string): void {
    localStorageOrNull()?.setItem(key, value)
  },

  getJson<T>(key: string): T | null {
    const value = this.getString(key)
    if (!value) return null
    try {
      return JSON.parse(value) as T
    } catch {
      this.remove(key)
      return null
    }
  },

  setJson(key: string, value: unknown): void {
    this.setString(key, JSON.stringify(value))
  },

  remove(key: string): void {
    localStorageOrNull()?.removeItem(key)
  },

  removeMany(keys: readonly string[]): void {
    keys.forEach((key) => this.remove(key))
  },
}
