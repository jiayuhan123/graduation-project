import { passwordLoginApi, wechatLoginApi } from '../../api/auth'
import { saveSession, getSession } from '../../services/session'

function requestWechatCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success: ({ code }) => {
        if (code) resolve(code)
        else reject(new Error('微信登录凭证获取失败'))
      },
      fail: ({ errMsg }) => reject(new Error(errMsg || '微信登录凭证获取失败')),
    })
  })
}

Page({
  data: {
    loginMode: 'wechat',
    username: '',
    password: '',
    nickname: '',
    realName: '',
    grades: ['一年级', '二年级', '三年级', '四年级', '五年级', '六年级'],
    semesters: ['上学期', '下学期'],
    gradeIndex: 2,
    semesterIndex: 0,
    submitting: false,
  },

  onLoad() {
    if (getSession()) void wx.switchTab({ url: '/pages/index/index' })
  },

  onNicknameInput(event: WechatMiniprogram.Input) {
    this.setData({ nickname: event.detail.value })
  },

  switchLoginMode(event: WechatMiniprogram.BaseEvent) {
    const loginMode = String(event.currentTarget.dataset.mode ?? '')
    if (loginMode !== 'wechat' && loginMode !== 'password') return
    this.setData({ loginMode })
  },

  onUsernameInput(event: WechatMiniprogram.Input) {
    this.setData({ username: event.detail.value })
  },

  onPasswordInput(event: WechatMiniprogram.Input) {
    this.setData({ password: event.detail.value })
  },

  onRealNameInput(event: WechatMiniprogram.Input) {
    this.setData({ realName: event.detail.value })
  },

  onGradeChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ gradeIndex: Number(event.detail.value) })
  },

  onSemesterChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ semesterIndex: Number(event.detail.value) })
  },

  async handleLogin() {
    if (this.data.submitting) return
    if (this.data.loginMode === 'password' && (!this.data.username.trim() || !this.data.password)) {
      await wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const result =
        this.data.loginMode === 'wechat'
          ? await wechatLoginApi({
              code: await requestWechatCode(),
              nickname: this.data.nickname.trim() || undefined,
              realName: this.data.realName.trim() || undefined,
              gradeLevel: this.data.gradeIndex + 1,
              semester: this.data.semesterIndex + 1,
            })
          : await passwordLoginApi({
              username: this.data.username.trim(),
              password: this.data.password,
            })
      saveSession(result)
      await wx.showToast({ title: '登录成功', icon: 'success' })
      await wx.switchTab({ url: '/pages/index/index' })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '登录失败，请稍后重试',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
