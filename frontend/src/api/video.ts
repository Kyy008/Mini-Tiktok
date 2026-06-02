import { apiHttp } from '../utils/http'
import type { ApiResult, PageResult, VideoItem } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8085'

interface BackendVideoPayload {
  id: number
  title: string
  playUrl: string
  coverUrl?: string
  createdAt: string
  uploaderId?: string
  author?: {
    id?: number
    userId?: string
    username?: string
    avatar?: string
    signature?: string
  }
  likeCount?: number
  commentCount?: number
  favoriteCount?: number
  shareCount?: number
  liked?: boolean
  music?: string
}

interface BackendMyVideosPageResponse {
  records: BackendVideoPayload[]
  page: number
  size: number
  total: number
}

interface BackendLikeStatus {
  liked?: boolean
  likeCount?: number
  count?: number
}

export async function getVideo(id: number): Promise<VideoItem> {
  const response = await apiHttp.get<ApiResult<BackendVideoPayload>>(`/api/videos/${id}`)
  return toVideoItem(unwrapResult(response.data))
}

export async function getRecommendations(size = 10): Promise<VideoItem[]> {
  const response = await apiHttp.get<
    ApiResult<BackendVideoPayload[] | { records?: BackendVideoPayload[]; list?: BackendVideoPayload[] }>
  >('/api/videos/recommendations', {
    params: { size },
  })
  const data = unwrapResult(response.data)
  const records = Array.isArray(data) ? data : data.records ?? data.list ?? []
  return records.map(toVideoItem)
}

export async function uploadVideo(file: File, title: string): Promise<VideoItem> {
  const form = new FormData()
  form.append('file', file)
  form.append('title', title)

  const response = await apiHttp.post<ApiResult<BackendVideoPayload>>('/api/videos', form)
  return toVideoItem(unwrapResult(response.data))
}

export async function getMyVideos(page = 1, size = 10): Promise<PageResult<VideoItem>> {
  const response = await apiHttp.get<ApiResult<BackendMyVideosPageResponse>>('/api/my/videos', {
    params: { page, size },
  })
  const data = unwrapResult(response.data)
  return {
    list: data.records.map(toVideoItem),
    page: data.page,
    size: data.size,
    total: data.total,
    hasMore: data.page * data.size < data.total,
  }
}

export async function deleteVideo(id: number): Promise<void> {
  const response = await apiHttp.delete<ApiResult<void>>(`/api/videos/${id}`)
  ensureSuccess(response.data)
}

export async function markVideoViewed(id: number): Promise<void> {
  const response = await apiHttp.post<ApiResult<void>>(`/api/videos/${id}/views`)
  ensureSuccess(response.data)
}

export async function likeVideo(id: number): Promise<BackendLikeStatus | null> {
  const response = await apiHttp.post<ApiResult<BackendLikeStatus | null>>(`/api/videos/${id}/likes`)
  ensureSuccess(response.data)
  return response.data.data ?? null
}

export async function unlikeVideo(id: number): Promise<BackendLikeStatus | null> {
  const response = await apiHttp.delete<ApiResult<BackendLikeStatus | null>>(`/api/videos/${id}/likes`)
  ensureSuccess(response.data)
  return response.data.data ?? null
}

export async function getVideoLikeStatus(id: number): Promise<BackendLikeStatus | null> {
  const response = await apiHttp.get<ApiResult<BackendLikeStatus | null>>(`/api/videos/${id}/likes`)
  ensureSuccess(response.data)
  return response.data.data ?? null
}

export function getVideoPlayUrl(path: string): string {
  if (/^https?:\/\//.test(path)) {
    return path
  }
  return new URL(path, API_BASE_URL).toString()
}

export async function getVideoPlayObjectUrl(id: number): Promise<string> {
  const response = await apiHttp.get<Blob>(`/api/videos/${id}/play`, {
    responseType: 'blob',
  })
  return URL.createObjectURL(response.data)
}

export function isApiVideoPlayUrl(path: string): boolean {
  try {
    const url = new URL(path, API_BASE_URL)
    const apiUrl = new URL(API_BASE_URL)
    return url.origin === apiUrl.origin && /^\/api\/videos\/\d+\/play$/.test(url.pathname)
  } catch {
    return false
  }
}

export async function resolveVideoPlaySource(video: Pick<VideoItem, 'id' | 'playUrl'>): Promise<string> {
  if (!isApiVideoPlayUrl(video.playUrl)) {
    return video.playUrl
  }
  return getVideoPlayObjectUrl(video.id)
}

function toVideoItem(video: BackendVideoPayload): VideoItem {
  const uploaderId = video.uploaderId || ''
  const resolvedAuthorId = Number(video.author?.id ?? video.author?.userId ?? uploaderId)
  return {
    id: Number(video.id),
    title: video.title,
    playUrl: getVideoPlayUrl(video.playUrl),
    coverUrl: video.coverUrl || `https://picsum.photos/seed/video-${video.id}/375/680`,
    author: {
      id: Number.isFinite(resolvedAuthorId) ? resolvedAuthorId : 0,
      username: video.author?.username || (uploaderId ? `user_${uploaderId}` : '我'),
      avatar: video.author?.avatar || `https://picsum.photos/seed/user-${uploaderId || 'me'}/120/120`,
      signature: video.author?.signature,
    },
    likeCount: video.likeCount ?? 0,
    commentCount: video.commentCount ?? 0,
    favoriteCount: video.favoriteCount ?? 0,
    shareCount: video.shareCount ?? 0,
    liked: video.liked ?? false,
    music: video.music || '@原声 - Mini Tiktok',
    createdAt: video.createdAt,
  }
}

function unwrapResult<T>(result: ApiResult<T>): T {
  ensureSuccess(result)
  if (result.data == null) {
    throw new Error(result.message || '接口未返回数据')
  }
  return result.data
}

function ensureSuccess(result: ApiResult<unknown>): void {
  if (result.code !== 200) {
    throw new Error(result.message || '操作失败')
  }
}
