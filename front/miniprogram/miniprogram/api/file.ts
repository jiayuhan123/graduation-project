import { downloadFile, request, uploadFile } from '../services/request'
import type { FileMetadata } from '../types/file'

const FILE_BASE_URL = '/api/files'

export const uploadPrivateFileApi = (filePath: string) =>
  uploadFile<FileMetadata>({ path: FILE_BASE_URL, filePath })

export const getFileMetadataApi = (fileId: string) =>
  request<FileMetadata>({ path: `${FILE_BASE_URL}/${fileId}/metadata` })

export const downloadPrivateFileApi = (fileId: string) => downloadFile(`${FILE_BASE_URL}/${fileId}`)

export const deletePrivateFileApi = (fileId: string) =>
  request<void>({ path: `${FILE_BASE_URL}/${fileId}`, method: 'DELETE' })
