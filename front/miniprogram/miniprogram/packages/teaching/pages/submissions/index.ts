import {
  listAssignmentSubmissionsApi,
  pageCreatedAssignmentsApi,
  pageStudentAssignmentsApi,
  updateSubmissionCommentApi,
} from '../../../../api/teaching'
import { getSession } from '../../../../services/session'
import type {
  AssignmentSummary,
  StudentAssignment,
  SubmissionDetail,
} from '../../../../types/teaching'

const emptyComments: Record<string, string> = {}

Page({
  data: {
    loading: true,
    isTeacher: false,
    assignments: [] as AssignmentSummary[],
    assignmentIndex: 0,
    submissions: [] as SubmissionDetail[],
    studentAssignments: [] as StudentAssignment[],
    comments: emptyComments,
  },

  onShow() {
    this.setData({ isTeacher: getSession()?.activeRole === 'teacher' })
    void this.load()
  },

  onPullDownRefresh() {
    void this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      if (this.data.isTeacher) {
        const page = await pageCreatedAssignmentsApi({ page: 1, size: 100 })
        const assignments = page.records.filter(
          (item) => item.status === 'PUBLISHED' || item.status === 'CLOSED',
        )
        const assignmentIndex = Math.min(
          this.data.assignmentIndex,
          Math.max(assignments.length - 1, 0),
        )
        this.setData({ assignments, assignmentIndex, studentAssignments: [] })
        await this.loadSubmissions()
      } else {
        const page = await pageStudentAssignmentsApi({ page: 1, size: 100 })
        this.setData({ loading: false, submissions: [], studentAssignments: page.records })
      }
    } catch (error) {
      this.setData({ loading: false })
      await this.showError(error, '提交记录加载失败')
    }
  },

  async loadSubmissions() {
    const assignment = this.data.assignments[this.data.assignmentIndex]
    if (!assignment) {
      this.setData({ loading: false, submissions: [] })
      return
    }
    const submissions = await listAssignmentSubmissionsApi(assignment.id)
    this.setData({
      loading: false,
      submissions,
      comments: Object.fromEntries(
        submissions.map((item) => [item.submission.id, item.submission.teacherComment ?? '']),
      ),
    })
  },

  selectAssignment(event: WechatMiniprogram.PickerChange) {
    this.setData({ assignmentIndex: Number(event.detail.value) })
    void this.loadSubmissions()
  },

  onCommentInput(event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { id: string }>) {
    const id = event.currentTarget.dataset.id
    if (id) this.setData({ [`comments.${id}`]: event.detail.value })
  },

  async saveComment(event: WechatMiniprogram.BaseEvent) {
    const id = String(event.currentTarget.dataset.id ?? '')
    const submission = this.data.submissions.find((item) => item.submission.id === id)
    if (!submission) return
    try {
      const updated = await updateSubmissionCommentApi(
        id,
        this.data.comments[id] ?? '',
        submission.submission.version,
      )
      const submissions = this.data.submissions.map((item) =>
        item.submission.id === id ? updated : item,
      )
      this.setData({ submissions })
      await wx.showToast({ title: '评语已保存', icon: 'success' })
    } catch (error) {
      await this.showError(error, '保存评语失败')
    }
  },

  async showError(error: unknown, fallback: string) {
    await wx.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
  },
})
