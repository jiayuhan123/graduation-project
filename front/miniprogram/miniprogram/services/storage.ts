export function getStorage<T>(key: string): T | null {
  const value: unknown = wx.getStorageSync(key)
  return value === undefined || value === null || value === '' ? null : (value as T)
}

export function setStorage<T>(key: string, value: T): void {
  wx.setStorageSync(key, value)
}

export function removeStorage(key: string): void {
  wx.removeStorageSync(key)
}
