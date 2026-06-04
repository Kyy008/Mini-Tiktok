import { defineStore } from 'pinia'

import {
  authErrorMessage,
  buildAuthorizationUrl,
  buildLogoutUrl,
  buildRegisterUrl,
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

interface PasswordPayload {
  username?: string
  password?: string
  redirectPath?: string
}

interface CallbackResult {
  user: CurrentUser
  redirectPath: string
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
    async login(options?: { redirectPath?: string }) {
      this.error = null
      const pkce = await createPkceParams()
      savePkce({
        codeVerifier: pkce.codeVerifier,
        state: pkce.state,
        redirectPath: sanitizeRedirectPath(options?.redirectPath),
      })
      window.location.assign(
        buildAuthorizationUrl({
          codeChallenge: pkce.codeChallenge,
          state: pkce.state,
        }),
      )
    },
    async submitLogin(payload: PasswordPayload): Promise<void> {
      this.loading = true
      this.error = null
      try {
        await this.login({ redirectPath: payload.redirectPath })
      } catch (error) {
        this.error = authErrorMessage(error)
        throw new Error(this.error)
      } finally {
        this.loading = false
      }
    },
    async submitRegister(_payload: PasswordPayload): Promise<void> {
      this.register()
    },
    register(): void {
      window.location.assign(buildRegisterUrl())
    },
    async handleCallback(code: string, state: string): Promise<CallbackResult> {
      this.loading = true
      this.error = null
      try {
        const pkce = getPkce()
        if (!pkce) {
          throw new Error('本次登录不是从前端登录按钮发起，或登录状态已过期，请重新登录')
        }
        if (pkce.state !== state) {
          throw new Error('登录状态校验失败，请重新登录')
        }

        const redirectPath = sanitizeRedirectPath(pkce.redirectPath) || '/'
        const token = await exchangeCodeForToken(code, pkce.codeVerifier)
        saveAccessToken(token.access_token)
        this.accessToken = token.access_token

        const user = normalizeCurrentUser(await fetchCurrentUser())
        saveCurrentUser(user)
        this.user = user
        clearPkce()
        return { user, redirectPath }
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
        const user = normalizeCurrentUser(await fetchCurrentUser())
        this.user = user
        saveCurrentUser(user)
      } catch {
        this.logout()
      } finally {
        this.loading = false
      }
    },
    logout(options?: { redirectToAuthServer?: boolean }): void {
      clearAuthStorage()
      this.accessToken = null
      this.user = null
      this.error = null
      if (options?.redirectToAuthServer) {
        window.location.assign(buildLogoutUrl())
      }
    },
  },
})

function normalizeCurrentUser(user: CurrentUser): CurrentUser {
  const numericId = Number(user.userId)
  return {
    ...user,
    id: Number.isFinite(numericId) ? numericId : 0,
    username: user.username || user.userId,
    avatar:
      user.avatar ||
      `https://picsum.photos/seed/${encodeURIComponent(user.userId)}/120/120`,
    signature: user.signature || '这个人很懒，什么都没写~',
  }
}

function sanitizeRedirectPath(path?: string): string | undefined {
  if (!path || !path.startsWith('/') || path.startsWith('//')) {
    return undefined
  }
  if (path.startsWith('/login') || path.startsWith('/register') || path.startsWith('/oauth/callback')) {
    return undefined
  }
  return path
}
