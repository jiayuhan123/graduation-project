const STATUS_LABELS: Record<string, string> = {
  CREATED: '未开始',
  PENDING: '待完成',
  NOT_STARTED: '未开始',
  ASSIGNED: '待完成',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  SUBMITTED: '已提交',
  GRADED: '已批改',
  OVERDUE: '已逾期',
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CLOSED: '已结束',
  CANCELLED: '已取消',
  CANCELED: '已取消',
  ACTIVE: '进行中',
  ARCHIVED: '已归档',
}

export function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? '待更新'
}

export function shortDate(value: string | null): string {
  if (!value) return '尚未开始'
  return value.replace('T', ' ').slice(5, 16)
}
