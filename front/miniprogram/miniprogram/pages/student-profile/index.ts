import { getStudentProfileApi, updateStudentProfileApi } from '../../api/account'

Page({
  data: {
    loading: true,
    saving: false,
    studentNo: '',
    grades: ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'],
    semesters: ['上学期', '下学期'],
    gradeIndex: 0,
    semesterIndex: 0,
  },

  onLoad() {
    void this.loadProfile()
  },

  async loadProfile() {
    try {
      const profile = await getStudentProfileApi()
      this.setData({
        loading: false,
        studentNo: profile.studentNo ?? '',
        gradeIndex: Math.max(0, profile.gradeLevel - 1),
        semesterIndex: Math.max(0, profile.semester - 1),
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '学生资料加载失败',
        icon: 'none',
      })
    }
  },

  onGradeChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ gradeIndex: Number(event.detail.value) })
  },

  onSemesterChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ semesterIndex: Number(event.detail.value) })
  },

  async handleSave() {
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      await updateStudentProfileApi({
        gradeLevel: this.data.gradeIndex + 1,
        semester: this.data.semesterIndex + 1,
      })
      await wx.showToast({ title: '学生资料已更新', icon: 'success' })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '保存失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saving: false })
    }
  },
})
