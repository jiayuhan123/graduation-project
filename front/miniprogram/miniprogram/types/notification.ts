import type { PageParams } from './api'

export interface Notification {
  id: string
  type: string
  title: string
  content: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationQuery extends PageParams {
  read?: boolean
}
