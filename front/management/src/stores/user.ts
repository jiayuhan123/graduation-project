import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/utils/request'
import type { LoginResult, UserInfo } from '@/types/user'

const USER_INFO_KEY = 'userInfo'

/** 用户状态：Token、登录信息、登出 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(loadUserInfo())

  const isLoggedIn = computed(() => Boolean(token.value))

  /** 登录成功后写入状态与本地存储 */
  function setLogin(result: LoginResult) {
    token.value = result.token
    userInfo.value = {
      userId: result.userId,
      username: result.username,
      role: result.role,
    }
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo.value))
  }

  /** 登出：清空状态与本地存储 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  }

  function loadUserInfo(): UserInfo | null {
    const raw = localStorage.getItem(USER_INFO_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as UserInfo
    } catch {
      return null
    }
  }

  return { token, userInfo, isLoggedIn, setLogin, logout }
})
