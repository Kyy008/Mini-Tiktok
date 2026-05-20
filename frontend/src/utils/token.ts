import type { CurrentUser } from '../api/types'

const ACCESS_TOKEN_KEY = 'mini_tiktok_access_token'
const USER_KEY = 'mini_tiktok_user'
const PKCE_VERIFIER_KEY = 'mini_tiktok_pkce_code_verifier'
const PKCE_STATE_KEY = 'mini_tiktok_pkce_state'

export interface StoredPkce {
  codeVerifier: string
  state: string
}

export function saveAccessToken(accessToken: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function saveCurrentUser(user: CurrentUser): void {
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getCurrentUserFromStorage(): CurrentUser | null {
  const text = sessionStorage.getItem(USER_KEY)
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text) as CurrentUser
  } catch {
    sessionStorage.removeItem(USER_KEY)
    return null
  }
}

export function savePkce(pkce: StoredPkce): void {
  sessionStorage.setItem(PKCE_VERIFIER_KEY, pkce.codeVerifier)
  sessionStorage.setItem(PKCE_STATE_KEY, pkce.state)
}

export function getPkce(): StoredPkce | null {
  const codeVerifier = sessionStorage.getItem(PKCE_VERIFIER_KEY)
  const state = sessionStorage.getItem(PKCE_STATE_KEY)
  if (!codeVerifier || !state) {
    return null
  }
  return { codeVerifier, state }
}

export function clearPkce(): void {
  sessionStorage.removeItem(PKCE_VERIFIER_KEY)
  sessionStorage.removeItem(PKCE_STATE_KEY)
}

export function clearAuthStorage(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  clearPkce()
}
