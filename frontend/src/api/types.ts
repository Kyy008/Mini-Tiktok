export interface ApiResult<T> {
  code: number
  message: string
  data?: T | null
}

export interface UserInfo {
  id: number
  username: string
  avatar: string
  signature?: string
}

export interface CurrentUser extends UserInfo {
  userId: string
  scopes: string[]
}

export interface OAuthTokenResponse {
  access_token: string
  token_type: string
  expires_in?: number
  scope?: string
}

export interface VideoItem {
  id: number
  title: string
  playUrl: string
  coverUrl: string
  author: UserInfo
  likeCount: number
  commentCount: number
  favoriteCount: number
  shareCount: number
  liked: boolean
  music: string
  createdAt: string
}

export interface CommentItem {
  id: number
  user: UserInfo
  content: string
  likeCount: number
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
  hasMore: boolean
}

export interface UploadProgress {
  uploadId: string
  uploadedBytes: number
  fileSize: number
  currentChunk: number
  totalChunks: number
  percent: number
  status: string
}

export interface RequestLogItem {
  id: number
  userId: string | null
  method: string
  path: string
  requestBody: string | null
  responseBody: string | null
  statusCode: number
  durationMs: number
  ip: string | null
  createdAt: string
}
