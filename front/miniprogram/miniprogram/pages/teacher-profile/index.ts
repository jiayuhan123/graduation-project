import { getTeacherProfileApi, updateTeacherProfileApi } from '../../api/account'

Page({
  data: {
    loading: true,
    saving: false,
    teacherNo: '',
    title: '',
    introduction: '',
  },

  onLoad() {
    void this.loadProfile()
  },

  async loadProfile() {
    try {
      const profile = await getTeacherProfileApi()
      this.setData({
        loading: false,
        teacherNo: profile.teacherNo ?? '',
        title: profile.title ?? '',
        introduction: profile.introduction ?? '',
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '教师资料加载失败',
        icon: 'none',
      })
    }
  },

  onTitleInput(event: WechatMiniprogram.Input) {
    this.setData({ title: event.detail.value })
  },

  onIntroductionInput(event: WechatMiniprogram.Input) {
    this.setData({ introduction: event.detail.value })
  },

  async handleSave() {
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      await updateTeacherProfileApi({
        title: this.data.title.trim(),
        introduction: this.data.introduction.trim(),
      })
      await wx.showToast({ title: '教师资料已更新', icon: 'success' })
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
