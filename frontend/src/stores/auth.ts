// 登录状态管理。当前为 Mock 实现：默认已登录，便于演示。
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '../api/types'
import { CURRENT_USER } from '../mock/data'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(CURRENT_USER)
  const accessToken = ref<string | null>('mock-token')

  function login() {
    // Mock：直接置为已登录。真实流程走 OAuth2 PKCE 跳转。
    user.value = CURRENT_USER
    accessToken.value = 'mock-token'
  }

  function logout() {
    user.value = null
    accessToken.value = null
  }

  return { user, accessToken, login, logout }
})
