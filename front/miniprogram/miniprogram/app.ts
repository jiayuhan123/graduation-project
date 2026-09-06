import { setUnauthorizedHandler } from './services/request'

setUnauthorizedHandler(() => {
  void wx.reLaunch({ url: '/pages/login/index' })
})

App({})
