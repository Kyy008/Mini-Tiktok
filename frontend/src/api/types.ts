// 前后端数据类型。

// ===== 业务数据 =====

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
  /** 播放地址 */
  playUrl: string
  /** 封面图 */
  coverUrl: string
  author: UserInfo
  likeCount: number
  commentCount: number
  favoriteCount: number
  shareCount: number
  /** 当前登录用户是否已点赞 */
  liked: boolean
  /** 背景音乐文案 */
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
