import { downloadPrivateFileApi } from '../api/file'

const MANAGED_AVATAR_PREFIX = 'file:'
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
const displayCache = new Map<string, string>()

export function createAvatarReference(fileId: string): string {
  return `${MANAGED_AVATAR_PREFIX}${fileId}`
}

export function getAvatarFileId(reference: string | null | undefined): string | null {
  if (!reference?.startsWith(MANAGED_AVATAR_PREFIX)) return null
  const fileId = reference.slice(MANAGED_AVATAR_PREFIX.length)
  return UUID_PATTERN.test(fileId) ? fileId : null
}

export function directAvatarDisplayUrl(reference: string | null | undefined): string {
  return getAvatarFileId(reference) ? '' : (reference ?? '')
}

export async function resolveAvatarDisplayUrl(
  reference: string | null | undefined,
): Promise<string> {
  if (!reference) return ''
  const fileId = getAvatarFileId(reference)
  if (!fileId) return reference

  const cached = displayCache.get(reference)
  if (cached) return cached

  const result = await downloadPrivateFileApi(fileId)
  displayCache.set(reference, result.tempFilePath)
  return result.tempFilePath
}

export function removeAvatarFromCache(reference: string | null | undefined): void {
  if (reference) displayCache.delete(reference)
}
