import axios from 'axios'

import { apiHttp } from '../utils/http'
import type { ApiResult, CurrentUser, OAuthTokenResponse } from './types'

const AUTH_BASE_URL = import.meta.env.VITE_AUTH_BASE_URL ?? 'http://localhost:9000'
const CLIENT_ID = import.meta.env.VITE_CLIENT_ID ?? 'tiktok-web'
const REDIRECT_URI = import.meta.env.VITE_REDIRECT_URI ?? 'http://localhost:5173/oauth/callback'
const SCOPE = import.meta.env.VITE_SCOPE ?? 'video:read video:write video:like'
const AUTH_REQUEST_TIMEOUT_MS = 8000

interface PasswordAuthPayload {
  username: string
  password: string
}

interface CsrfToken {
  name: string
  value: string
}

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

export function buildRegisterUrl(): string {
  return new URL('/register', AUTH_BASE_URL).toString()
}

export function buildLogoutUrl(): string {
  return new URL('/logout', AUTH_BASE_URL).toString()
}

export async function loginWithPassword(payload: PasswordAuthPayload): Promise<void> {
  const csrf = await fetchCsrfToken('/login')
  const response = await submitAuthForm('/login', csrf, payload)
  if (isAuthPage(response, '/login') && response.url.includes('error')) {
    throw new Error('用户名或密码错误')
  }
  if (!response.ok) {
    throw new Error('登录失败，请稍后重试')
  }
}

export async function registerWithPassword(payload: PasswordAuthPayload): Promise<void> {
  const csrf = await fetchCsrfToken('/register')
  const response = await submitAuthForm('/register', csrf, payload)
  if (isAuthPage(response, '/login') && response.url.includes('registered')) {
    return
  }
  if (!response.ok) {
    throw new Error('注册失败，请稍后重试')
  }

  const error = extractPageError(await response.text())
  if (error) {
    throw new Error(error)
  }
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

async function fetchCsrfToken(path: '/login' | '/register'): Promise<CsrfToken> {
  const response = await fetchWithTimeout(new URL(path, AUTH_BASE_URL), {
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error('无法连接鉴权服务')
  }

  const html = await response.text()
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const input = doc.querySelector<HTMLInputElement>('input[type="hidden"][name][value]')
  if (!input?.name || !input.value) {
    throw new Error('鉴权服务缺少 CSRF Token')
  }
  return { name: input.name, value: input.value }
}

async function submitAuthForm(
  path: '/login' | '/register',
  csrf: CsrfToken,
  payload: PasswordAuthPayload,
): Promise<Response> {
  const body = new URLSearchParams({
    username: payload.username,
    password: payload.password,
    [csrf.name]: csrf.value,
  })

  return fetchWithTimeout(new URL(path, AUTH_BASE_URL), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body,
  })
}

function isAuthPage(response: Response, pathname: string): boolean {
  if (!response.url) return false
  const url = new URL(response.url)
  return `${url.origin}${url.pathname}` === new URL(pathname, AUTH_BASE_URL).toString()
}

function extractPageError(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return doc.querySelector('.error')?.textContent?.trim() ?? ''
}

async function fetchWithTimeout(input: URL, init: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), AUTH_REQUEST_TIMEOUT_MS)
  try {
    return await fetch(input, {
      ...init,
      signal: controller.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('无法连接鉴权服务，请确认 auth-backend 已启动')
    }
    throw new Error('无法连接鉴权服务')
  } finally {
    window.clearTimeout(timeout)
  }
}
