import {
  pageEnrolledClassesApi,
  pageTeachingClassesApi,
  pageStudentAssignmentsApi,
  pageCreatedAssignmentsApi,
} from '../../api/teaching'
import { getSession } from '../../services/session'
import type { ClassMembership } from '../../types/teaching'
import { navigateToPage, syncTabBar } from '../../utils/navigation'
import { shortDate, statusLabel } from '../../utils/display'

interface AssignmentItem {
  id: string
  title: string
  date: string
  status: string
}

Page({
  data: {
    role: '',
    loading: true,
    loadError: '',
    classes: [] as ClassMembership[],
    assignments: [] as AssignmentItem[],
    canManageRoles: false,
    permissions: [] as string[],
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
    void wx.setNavigationBarTitle({ title: role === 'admin' ? '权限' : '班级' })
    this.setData({
      role,
      loading: true,
      loadError: '',
      classes: [],
      assignments: [],
      canManageRoles: session.permissions.includes('system:role:manage'),
      permissions: session.permissions,
    })
    if (role !== 'student' && role !== 'teacher') {
      this.setData({ loading: false })
      return
    }
    const [classes, assignments] = await Promise.allSettled([
      role === 'teacher'
        ? pageTeachingClassesApi({ page: 1, size: 100 })
        : pageEnrolledClassesApi({ page: 1, size: 100 }),
      role === 'teacher'
        ? pageCreatedAssignmentsApi({ page: 1, size: 5 }).then((result) =>
            result.records.map((item) => ({
              id: item.id,
              title: item.title,
              date: shortDate(item.dueAt),
              status: statusLabel(item.status),
            })),
          )
        : pageStudentAssignmentsApi({ page: 1, size: 5 }).then((result) =>
            result.records.map((item) => ({
              id: item.assignment.id,
              title: item.assignment.title,
              date: shortDate(item.assignment.dueAt),
              status: statusLabel(item.submission.status),
            })),
          ),
    ])
    if (revision !== this.loadRevision) return
    this.setData({
      loading: false,
      classes:
        classes.status === 'fulfilled'
          ? classes.value.records.filter(
              (item) => item.membershipStatus === 'ACTIVE' && item.classStatus === 'ACTIVE',
            )
          : [],
      assignments: assignments.status === 'fulfilled' ? assignments.value : [],
      loadError:
        classes.status === 'rejected' || assignments.status === 'rejected'
          ? '部分班级信息未能加载，请重试。'
          : '',
    })
  },
  openPage(event: WechatMiniprogram.BaseEvent) {
    const url = String(event.currentTarget.dataset.url ?? '')
    if (url) navigateToPage(url)
  },
})
