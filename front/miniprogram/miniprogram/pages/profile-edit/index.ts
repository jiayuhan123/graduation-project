import { getCurrentUserApi, updateCurrentUserApi } from '../../api/account'
import { deletePrivateFileApi, uploadPrivateFileApi } from '../../api/file'
import {
  createAvatarReference,
  getAvatarFileId,
  removeAvatarFromCache,
  resolveAvatarDisplayUrl,
} from '../../services/avatar'

type EditableField = 'nickname' | 'realName' | 'phone' | 'email'

Page({
  data: {
    loading: true,
    saving: false,
    nickname: '',
    realName: '',
    phone: '',
    email: '',
    avatarReference: '',
    avatarUrl: '',
    pendingAvatarPath: '',
    avatarText: '用',
  },

  onLoad() {
    void this.loadProfile()
  },

  async loadProfile() {
    try {
      const profile = await getCurrentUserApi()
      const displayName = profile.nickname || profile.realName || profile.username
      const avatarUrl = await resolveAvatarDisplayUrl(profile.avatarUrl).catch(() => '')
      this.setData({
        loading: false,
        nickname: profile.nickname ?? '',
        realName: profile.realName ?? '',
        phone: profile.phone ?? '',
        email: profile.email ?? '',
        avatarReference: profile.avatarUrl ?? '',
        avatarUrl,
        avatarText: displayName.slice(0, 1),
      })
    } catch (error) {
      this.setData({ loading: false })
      await wx.showToast({
        title: error instanceof Error ? error.message : '资料加载失败',
        icon: 'none',
      })
    }
  },

  onFieldInput(
    event: WechatMiniprogram.Input<WechatMiniprogram.IAnyObject, { field: EditableField }>,
  ) {
    const field = event.currentTarget.dataset.field
    this.setData({ [field]: event.detail.value })
  },

  onChooseAvatar(event: WechatMiniprogram.CustomEvent<{ avatarUrl: string }>) {
    const avatarUrl = event.detail.avatarUrl
    if (!avatarUrl) return
    this.setData({ avatarUrl, pendingAvatarPath: avatarUrl })
  },

  async handleSave() {
    if (this.data.saving) return
    const phone = this.data.phone.trim()
    const email = this.data.email.trim()
    if (phone && !/^1\d{10}$/.test(phone)) {
      await wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      await wx.showToast({ title: '请输入正确的邮箱', icon: 'none' })
      return
    }

    this.setData({ saving: true })
    let uploadedFileId = ''
    try {
      let avatarReference = this.data.avatarReference
      if (this.data.pendingAvatarPath) {
        const uploaded = await uploadPrivateFileApi(this.data.pendingAvatarPath)
        uploadedFileId = uploaded.id
        avatarReference = createAvatarReference(uploaded.id)
      }

      await updateCurrentUserApi({
        nickname: this.data.nickname.trim(),
        realName: this.data.realName.trim(),
        phone,
        email,
        avatarUrl: avatarReference,
      })

      const previousFileId = getAvatarFileId(this.data.avatarReference)
      if (uploadedFileId && previousFileId && previousFileId !== uploadedFileId) {
        void deletePrivateFileApi(previousFileId).catch(() => undefined)
        removeAvatarFromCache(this.data.avatarReference)
      }
      this.setData({ avatarReference, pendingAvatarPath: '' })
      await wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => void wx.navigateBack(), 500)
    } catch (error) {
      if (uploadedFileId) void deletePrivateFileApi(uploadedFileId).catch(() => undefined)
      await wx.showToast({
        title: error instanceof Error ? error.message : '保存失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saving: false })
    }
  },
})
