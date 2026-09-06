import { request } from '../services/request'
import type { PageResult } from '../types/api'
import type { Notification, NotificationQuery } from '../types/notification'

const NOTIFICATION_BASE_URL = '/api/notifications'

export const pageNotificationsApi = (query: NotificationQuery = {}) =>
  request<PageResult<Notification>>({ path: NOTIFICATION_BASE_URL, query })

export const getUnreadNotificationCountApi = () =>
  request<number>({ path: `${NOTIFICATION_BASE_URL}/unread-count` })

export const markNotificationReadApi = (notificationId: string) =>
  request<void>({ path: `${NOTIFICATION_BASE_URL}/${notificationId}/read`, method: 'PUT' })

export const markAllNotificationsReadApi = () =>
  request<void>({ path: `${NOTIFICATION_BASE_URL}/read-all`, method: 'PUT' })
