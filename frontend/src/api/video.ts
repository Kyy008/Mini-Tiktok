import { apiHttp } from '../utils/http'
import type { ApiResult, PageResult, VideoItem } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8085'

interface BackendVideoDetail {
  id: number
  title: string
  playUrl: string
  createdAt: string
  uploaderId: string
}

interface BackendUploadVideoResponse {
  id: number
  title: string
  playUrl: string
  createdAt: string
}

interface BackendMyVideosPageResponse {
  records: BackendMyVideoItem[]
  page: number
  size: number
  total: number
}

interface BackendMyVideoItem {
  id: number
  title: string
  playUrl: string
  createdAt: string
}

export async function getVideo(id: number): Promise<VideoItem> {
  const response = await apiHttp.get<ApiResult<BackendVideoDetail>>(`/api/videos/${id}`)
  return toVideoItem(unwrapResult(response.data))
}

export async function uploadVideo(file: File, title: string): Promise<VideoItem> {
  const form = new FormData()
  form.append('file', file)
  form.append('title', title)

  const response = await apiHttp.post<ApiResult<BackendUploadVideoResponse>>('/api/videos', form)
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

function toVideoItem(video: BackendVideoDetail | BackendUploadVideoResponse | BackendMyVideoItem): VideoItem {
  const uploaderId = 'uploaderId' in video ? video.uploaderId : ''
  const authorId = Number(uploaderId)
  return {
    id: Number(video.id),
    title: video.title,
    playUrl: getVideoPlayUrl(video.playUrl),
    coverUrl: `https://picsum.photos/seed/video-${video.id}/375/680`,
    author: {
      id: Number.isFinite(authorId) ? authorId : 0,
      username: uploaderId ? `user_${uploaderId}` : '我',
      avatar: `https://picsum.photos/seed/user-${uploaderId || 'me'}/120/120`,
    },
    likeCount: 0,
    commentCount: 0,
    favoriteCount: 0,
    shareCount: 0,
    liked: false,
    music: '@原声 - Mini Tiktok',
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
