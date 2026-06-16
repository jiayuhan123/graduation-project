const BASE_URL = 'http://localhost:10000'

export const request = (options) => {
  return new Promise((resolve, reject) => {
    // 1. 获取本地 token
    const token = uni.getStorageSync('satoken')
    
    // 2. 组装 headers
    const header = {
      ...options.header
    }
    if (token) {
      header['satoken'] = token
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      success: (res) => {
        const { statusCode, data } = res
        // HTTP 状态码 200
        if (statusCode === 200) {
          // 这里 data 就是后端封装的 Result<T> 对象 (code, msg, data)
          if (data.code === 200) {
            resolve(data.data) // 直接返回实际数据
          } else if (data.code === 401) {
            // 未授权，清除 token 并跳转登录页
            uni.removeStorageSync('satoken')
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/login/login' })
            }, 1500)
            reject(new Error(data.msg))
          } else {
            // 业务报错
            uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
            reject(new Error(data.msg))
          }
        } else {
          uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
          reject(new Error('网络异常'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '请求接口失败', icon: 'none' })
        reject(err)
      }
    })
  })
}
