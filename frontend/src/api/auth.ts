import axios from 'axios'

import { apiHttp } from '../utils/http'
import type {
  ApiResult,
  CurrentUser,
  OAuthTokenResponse,
  RegisterCredentials,
  UserProfile,
} from './types'

const AUTH_BASE_URL = import.meta.env.VITE_AUTH_BASE_URL ?? 'http://localhost:9000'
const CLIENT_ID = import.meta.env.VITE_CLIENT_ID ?? 'tiktok-web'
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI ?? 'http://localhost:5173/oauth/callback'
const SCOPE = import.meta.env.VITE_SCOPE ?? 'video:read video:write video:like'

export function buildAuthorizationUrl(params: { codeChallenge: string; state: string }): string {
  const url = new URL('/oauth2/authorize', AUTH_BASE_URL)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('client_id', CLIENT_ID)
  url.searchParams.set('redirect_uri', REDIRECT_URI)
  url.searchParams.set('scope', SCOPE)
  url.searchParams.set('state', params.state)
  url.searchParams.set('code_challenge', params.codeChallenge)
  url.searchParams.set('code_challenge_method', 'S256')
  return url.toString()
}

export async function registerUser(credentials: RegisterCredentials): Promise<UserProfile> {
  const response = await axios.post<ApiResult<UserProfile>>(`${AUTH_BASE_URL}/api/register`, credentials)
  return unwrapResult(response.data)
}

export async function exchangeCodeForToken(
  code: string,
  codeVerifier: string,
): Promise<OAuthTokenResponse> {
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    code,
    code_verifier: codeVerifier,
  })
  const response = await axios.post<OAuthTokenResponse>(`${AUTH_BASE_URL}/oauth2/token`, body, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })
  return response.data
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  const response = await apiHttp.get<ApiResult<CurrentUser>>('/api/me')
  return unwrapResult(response.data)
}

export function authErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiResult<unknown>>(error)) {
    return error.response?.data?.message ?? error.response?.statusText ?? error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return '操作失败'
}

function unwrapResult<T>(result: ApiResult<T>): T {
  if (result.code !== 200 || !result.data) {
    throw new Error(result.message || '操作失败')
  }
  return result.data
}
