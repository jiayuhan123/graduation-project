import {
  addClassStudentApi,
  createTeachingClassApi,
  getTeachingClassApi,
  joinTeachingClassApi,
  leaveTeachingClassApi,
  pageEnrolledClassesApi,
  pageTeachingClassesApi,
  removeClassStudentApi,
  updateTeachingClassStatusApi,
} from '../../../../api/teaching'
import { getSession } from '../../../../services/session'
import type { ClassDetail, ClassMembership } from '../../../../types/teaching'

const currentYear = new Date().getFullYear()

Page({
  data: {
    loading: true,
    submitting: false,
    isTeacher: false,
    classes: [] as ClassMembership[],
    detail: null as ClassDetail | null,
    showCreate: false,
    joinCode: '',
    joinStudentNo: '',
    className: '',
    schoolName: '',
    academicYear: `${currentYear}-${currentYear + 1}`,
    gradeLevel: 1,
    semester: 1,
    gradeOptions: [1, 2, 3, 4, 5, 6],
    semesterOptions: ['上学期', '下学期'],
    studentId: '',
    studentNo: '',
  },

  onShow() {
    const isTeacher = getSession()?.activeRole === 'teacher'
    this.setData({ isTeacher, detail: null, showCreate: false })
    void this.loadClasses()
  },

  onPullDownRefresh() {
    void this.loadClasses().finally(() => wx.stopPullDownRefresh())
  },

  async loadClasses() {
    this.setData({ loading: true })
    try {
      const result = this.data.isTeacher
        ? await pageTeachingClassesApi({ page: 1, size: 100 })
        : await pageEnrolledClassesApi({ page: 1, size: 100 })
      this.setData({ loading: false, classes: result.records })
    } catch (error) {
      this.setData({ loading: false })
      await this.showError(error, '班级加载失败')
    }
  },

  onInput(event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { field: string }>) {
    const field = event.currentTarget.dataset.field
    if (!field) return
    this.setData({ [field]: event.detail.value })
  },

  selectGrade(event: WechatMiniprogram.PickerChange) {
    this.setData({ gradeLevel: Number(event.detail.value) + 1 })
  },

  selectSemester(event: WechatMiniprogram.PickerChange) {
    this.setData({ semester: Number(event.detail.value) + 1 })
  },

  toggleCreate() {
    this.setData({ showCreate: !this.data.showCreate, detail: null })
  },

  async createClass() {
    if (!this.data.className.trim()) {
      await wx.showToast({ title: '请填写班级名称', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const created = await createTeachingClassApi({
        name: this.data.className.trim(),
        schoolName: this.data.schoolName.trim() || undefined,
        academicYear: this.data.academicYear.trim(),
        gradeLevel: this.data.gradeLevel,
        semester: this.data.semester,
      })
      this.setData({ showCreate: false, className: '', schoolName: '' })
      await wx.showModal({
        title: '班级创建成功',
        content: `班级码：${created.classCode}`,
        showCancel: false,
      })
      await this.loadClasses()
    } catch (error) {
      await this.showError(error, '创建班级失败')
    } finally {
      this.setData({ submitting: false })
    }
  },

  async joinClass() {
    const classCode = this.data.joinCode.trim().toUpperCase()
    if (!classCode) {
      await wx.showToast({ title: '请输入班级码', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await joinTeachingClassApi(classCode, this.data.joinStudentNo.trim() || undefined)
      this.setData({ joinCode: '', joinStudentNo: '' })
      await wx.showToast({ title: '加入成功', icon: 'success' })
      await this.loadClasses()
    } catch (error) {
      await this.showError(error, '加入班级失败')
    } finally {
      this.setData({ submitting: false })
    }
  },

  async leaveClass(event: WechatMiniprogram.BaseEvent) {
    const classId = String(event.currentTarget.dataset.id ?? '')
    const result = await wx.showModal({
      title: '退出班级',
      content: '退出后不会删除已经分配的历史作业，确定继续吗？',
    })
    if (!result.confirm || !classId) return
    try {
      await leaveTeachingClassApi(classId)
      await wx.showToast({ title: '已退出', icon: 'success' })
      await this.loadClasses()
    } catch (error) {
      await this.showError(error, '退出班级失败')
    }
  },

  async openStudents(event: WechatMiniprogram.BaseEvent) {
    const classId = String(event.currentTarget.dataset.id ?? '')
    if (!classId) return
    try {
      this.setData({ detail: await getTeachingClassApi(classId), showCreate: false })
    } catch (error) {
      await this.showError(error, '学生名单加载失败')
    }
  },

  closeStudents() {
    this.setData({ detail: null, studentId: '', studentNo: '' })
  },

  async toggleClassStatus(event: WechatMiniprogram.BaseEvent) {
    const classId = String(event.currentTarget.dataset.id ?? '')
    const version = Number(event.currentTarget.dataset.version)
    const currentStatus = String(event.currentTarget.dataset.status)
    const nextStatus: 'ACTIVE' | 'ARCHIVED' = currentStatus === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE'
    if (nextStatus === 'ARCHIVED') {
      const confirmed = await wx.showModal({
        title: '归档班级',
        content: '归档后学生不能再通过班级码加入，确定继续吗？',
      })
      if (!confirmed.confirm) return
    }
    try {
      await updateTeachingClassStatusApi(classId, nextStatus, version)
      await wx.showToast({ title: nextStatus === 'ACTIVE' ? '已恢复' : '已归档', icon: 'success' })
      await this.loadClasses()
    } catch (error) {
      await this.showError(error, '班级状态修改失败')
    }
  },

  async addStudent() {
    if (!this.data.detail || !this.data.studentId.trim()) {
      await wx.showToast({ title: '请输入学生用户 ID', icon: 'none' })
      return
    }
    try {
      await addClassStudentApi(
        this.data.detail.classInfo.classId,
        this.data.studentId.trim(),
        this.data.studentNo.trim() || undefined,
      )
      const detail = await getTeachingClassApi(this.data.detail.classInfo.classId)
      this.setData({ detail, studentId: '', studentNo: '' })
      await wx.showToast({ title: '添加成功', icon: 'success' })
    } catch (error) {
      await this.showError(error, '添加学生失败')
    }
  },

  async removeStudent(event: WechatMiniprogram.BaseEvent) {
    if (!this.data.detail) return
    const studentId = String(event.currentTarget.dataset.id ?? '')
    const confirmed = await wx.showModal({
      title: '移出学生',
      content: '确定将该学生移出当前班级吗？',
    })
    if (!confirmed.confirm || !studentId) return
    try {
      await removeClassStudentApi(this.data.detail.classInfo.classId, studentId)
      const detail = await getTeachingClassApi(this.data.detail.classInfo.classId)
      this.setData({ detail })
      await wx.showToast({ title: '已移出', icon: 'success' })
    } catch (error) {
      await this.showError(error, '移出学生失败')
    }
  },

  async showError(error: unknown, fallback: string) {
    await wx.showToast({ title: error instanceof Error ? error.message : fallback, icon: 'none' })
  },
})
