import { getLearningOverviewApi, pagePracticeTasksApi } from '../../api/learning'
import { pageCreatedAssignmentsApi } from '../../api/teaching'
import { getSession } from '../../services/session'
import type { LearningOverview } from '../../types/learning'
import { navigateToPage, syncTabBar } from '../../utils/navigation'
import { shortDate, statusLabel } from '../../utils/display'

interface RecentItem {
  id: string
  title: string
  meta: string
  status: string
  url: string
}

Page({
  data: {
    role: '',
    loading: true,
    loadError: '',
    overview: null as LearningOverview | null,
    accuracy: '—',
    records: [] as RecentItem[],
    canManageUsers: false,
    canManageRoles: false,
  },
  loadRevision: 0,
  onShow() {
    void this.loadContent()
  },
  onPullDownRefresh() {
    void this.loadContent().finally(() => wx.stopPullDownRefresh())
  },
  async loadContent() {
    const session = getSession()
    if (!session) {
      void wx.reLaunch({ url: '/pages/login/index' })
      return
    }
    const revision = ++this.loadRevision
    const role = session.activeRole
    syncTabBar(role)
    void wx.setNavigationBarTitle({
      title: role === 'teacher' ? '教学' : role === 'admin' ? '管理' : '学习',
    })
    this.setData({
      role,
      loading: true,
      loadError: '',
      overview: null,
      accuracy: '—',
      records: [],
      canManageUsers: session.permissions.includes('system:user:manage'),
      canManageRoles: session.permissions.includes('system:role:manage'),
    })
    if (role === 'student') {
      const [overview, tasks] = await Promise.allSettled([
        getLearningOverviewApi(),
        pagePracticeTasksApi({ page: 1, size: 10 }),
      ])
      if (revision !== this.loadRevision) return
      this.setData({
        loading: false,
        overview: overview.status === 'fulfilled' ? overview.value : null,
        accuracy:
          overview.status === 'fulfilled' && overview.value.answerCount
            ? `${Math.round(overview.value.accuracyRate * 100)}%`
            : '—',
        records:
          tasks.status === 'fulfilled'
            ? tasks.value.records.map((task) => ({
                id: task.id,
                title: task.title,
                meta: `${task.questionCount} 题 · ${shortDate(task.completedAt || task.startedAt)}`,
                status: statusLabel(task.status),
                url: `/packages/learning/pages/practice-session/index?taskId=${task.id}`,
              }))
            : [],
        loadError:
          overview.status === 'rejected' || tasks.status === 'rejected'
            ? '部分学习记录未能加载，请重试。'
            : '',
      })
      return
    }
    if (role === 'teacher') {
      try {
        const result = await pageCreatedAssignmentsApi({ page: 1, size: 10 })
        if (revision !== this.loadRevision) return
        this.setData({
          records: result.records.map((item) => ({
            id: item.id,
            title: item.title,
            meta: `截止 ${shortDate(item.dueAt)} · ${item.assignmentType === 'LAYERED' ? '分层作业' : '统一作业'}`,
            status: statusLabel(item.status),
            url: `/packages/teaching/pages/assignments/index?assignmentId=${item.id}`,
          })),
        })
      } catch {
        if (revision === this.loadRevision)
          this.setData({ loadError: '作业记录未能加载，请重试。' })
      }
    }
    if (revision === this.loadRevision) this.setData({ loading: false })
  },
  openPage(event: WechatMiniprogram.BaseEvent) {
    const url = String(event.currentTarget.dataset.url ?? '')
    if (url) navigateToPage(url)
  },
})
