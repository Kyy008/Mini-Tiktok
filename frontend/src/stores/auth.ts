import { defineStore } from 'pinia'

import {
  authErrorMessage,
  buildAuthorizationUrl,
  exchangeCodeForToken,
  fetchCurrentUser,
} from '../api/auth'
import type { CurrentUser } from '../api/types'
import { createPkceParams } from '../utils/pkce'
import {
  clearAuthStorage,
  clearPkce,
  getAccessToken,
  getCurrentUserFromStorage,
  getPkce,
  saveAccessToken,
  saveCurrentUser,
  savePkce,
} from '../utils/token'

interface AuthState {
  accessToken: string | null
  user: CurrentUser | null
  loading: boolean
  error: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: getAccessToken(),
    user: getCurrentUserFromStorage(),
    loading: false,
    error: null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.user),
  },
  actions: {
    async login() {
      this.error = null
      const pkce = await createPkceParams()
      savePkce({
        codeVerifier: pkce.codeVerifier,
        state: pkce.state,
      })
      window.location.assign(
        buildAuthorizationUrl({
          codeChallenge: pkce.codeChallenge,
          state: pkce.state,
        }),
      )
    },
    async handleCallback(code: string, state: string): Promise<CurrentUser> {
      this.loading = true
      this.error = null
      try {
        const pkce = getPkce()
        if (!pkce) {
          throw new Error('登录状态已过期，请重新发起登录')
        }
        if (pkce.state !== state) {
          throw new Error('登录状态校验失败，请重新登录')
        }

        const token = await exchangeCodeForToken(code, pkce.codeVerifier)
        saveAccessToken(token.access_token)
        this.accessToken = token.access_token

        const user = await fetchCurrentUser()
        saveCurrentUser(user)
        this.user = user
        clearPkce()
        return user
      } catch (error) {
        clearAuthStorage()
        this.accessToken = null
        this.user = null
        this.error = authErrorMessage(error)
        throw new Error(this.error)
      } finally {
        this.loading = false
      }
    },
    async restore(): Promise<void> {
      const accessToken = getAccessToken()
      if (!accessToken) {
        return
      }

      this.loading = true
      this.accessToken = accessToken
      try {
        const user = await fetchCurrentUser()
        this.user = user
        saveCurrentUser(user)
      } catch {
        this.logout()
      } finally {
        this.loading = false
      }
    },
    logout(): void {
      clearAuthStorage()
      this.accessToken = null
      this.user = null
      this.error = null
    },
  },
})
