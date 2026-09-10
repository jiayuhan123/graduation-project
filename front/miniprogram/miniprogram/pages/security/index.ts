import {
  bindWechatApi,
  changePasswordApi,
  getCurrentUserApi,
  initializePasswordApi,
  unbindWechatApi,
} from '../../api/account'
import { clearSession } from '../../services/session'

type PasswordField = 'oldPassword' | 'newPassword' | 'confirmPassword'

function wechatPresentation(wechatBound: boolean, passwordSet: boolean) {
  return {
    wechatDescription: wechatBound
      ? passwordSet
        ? '可使用微信快捷登录，也可以安全解绑'
        : '微信是当前账号唯一的登录方式'
      : '绑定后可直接使用微信快捷登录',
    wechatActionText: wechatBound ? '解除绑定' : '立即绑定',
    wechatActionTheme: wechatBound ? 'danger' : 'primary',
    wechatActionVariant: wechatBound ? 'outline' : 'base',
    wechatActionDisabled: wechatBound && !passwordSet,
  }
}

Page({
  data: {
    mode: 'initialize',
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    saving: false,
    bindingWechat: false,
    wechatBound: false,
    passwordSet: false,
    ...wechatPresentation(false, false),
  },

  onLoad() {
    void this.loadBindingState()
  },

  async loadBindingState() {
    try {
      const profile = await getCurrentUserApi()
      this.setData({
        wechatBound: profile.wechatBound,
        passwordSet: profile.passwordSet,
        mode: profile.passwordSet ? 'change' : 'initialize',
        ...wechatPresentation(profile.wechatBound, profile.passwordSet),
      })
    } catch {
      // 页面其余密码功能仍可独立使用。
    }
  },

  onPasswordInput(
    event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { field: PasswordField }>,
  ) {
    const field = event.currentTarget.dataset.field
    this.setData({ [field]: event.detail.value })
  },

  async handleSave() {
    if (this.data.saving) return
    const { mode, oldPassword, newPassword, confirmPassword } = this.data
    if (mode === 'change' && !oldPassword) {
      await wx.showToast({ title: '请输入当前密码', icon: 'none' })
      return
    }
    if (newPassword.length < 6 || newPassword.length > 72) {
      await wx.showToast({ title: '新密码长度应为 6-72 位', icon: 'none' })
      return
    }
    if (newPassword !== confirmPassword) {
      await wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
      return
    }

    this.setData({ saving: true })
    try {
      if (mode === 'initialize') await initializePasswordApi({ newPassword })
      else await changePasswordApi({ oldPassword, newPassword })

      clearSession()
      await wx.showModal({
        title: '密码设置成功',
        content: '为了账号安全，请重新登录。',
        showCancel: false,
        confirmColor: '#285c49',
      })
      await wx.reLaunch({ url: '/pages/login/index' })
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '密码设置失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saving: false })
    }
  },

  async handleWechatAction() {
    if (this.data.bindingWechat) return
    if (this.data.wechatBound) {
      if (!this.data.passwordSet) {
        await wx.showToast({ title: '请先设置密码再解绑微信', icon: 'none' })
        return
      }
      const result = await wx.showModal({
        title: '解除微信绑定',
        content: '解绑后将不能再使用当前微信快捷登录。',
        confirmText: '确认解绑',
        confirmColor: '#d54941',
      })
      if (!result.confirm) return
    }

    this.setData({ bindingWechat: true })
    try {
      if (this.data.wechatBound) {
        await unbindWechatApi()
        this.setData({ wechatBound: false, ...wechatPresentation(false, true) })
        await wx.showToast({ title: '微信已解绑', icon: 'success' })
      } else {
        const { code } = await wx.login()
        if (!code) throw new Error('微信登录凭证获取失败')
        await bindWechatApi({ code })
        this.setData({ wechatBound: true, ...wechatPresentation(true, this.data.passwordSet) })
        await wx.showToast({ title: '微信绑定成功', icon: 'success' })
      }
    } catch (error) {
      await wx.showToast({
        title: error instanceof Error ? error.message : '微信身份操作失败',
        icon: 'none',
      })
    } finally {
      this.setData({ bindingWechat: false })
    }
  },
})
