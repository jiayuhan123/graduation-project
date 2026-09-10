const TAB_PAGES = [
  '/pages/index/index',
  '/pages/learning/index',
  '/pages/classroom/index',
  '/pages/profile/index',
]

export function navigateToPage(url: string) {
  if (TAB_PAGES.includes(url)) void wx.switchTab({ url })
  else void wx.navigateTo({ url })
}

export function syncTabBar(role: string) {
  const labels =
    role === 'teacher'
      ? ['首页', '教学', '班级', '我的']
      : role === 'admin'
        ? ['首页', '管理', '权限', '我的']
        : ['首页', '学习', '班级', '我的']
  labels.forEach((text, index) => {
    void wx.setTabBarItem({ index, text })
  })
}
