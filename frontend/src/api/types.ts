export interface ApiResult<T> {
  code: number
  message: string
  data?: T | null
}

export interface RegisterCredentials {
  username: string
  password: string
}

export interface UserProfile {
  id: number
  username: string
}

export interface CurrentUser {
  userId: string
  username: string | null
  scopes: string[]
}

export interface OAuthTokenResponse {
  access_token: string
  token_type: string
  expires_in?: number
  scope?: string
}
