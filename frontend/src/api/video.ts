import { apiHttp } from '../utils/http'
import type { ApiResult, PageResult, UploadProgress, VideoItem } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8085'
const DEFAULT_CHUNK_SIZE = 1024 * 1024

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
  videoId?: number
  liked?: boolean
  likeCount?: number
  count?: number
}

interface InitVideoUploadRequest {
  title: string
  fileName: string
  fileSize: number
  contentType: string
  chunkSize: number
  totalChunks: number
  fileHash: string
}

interface VideoUploadSessionResponse {
  uploadId: string
  nextChunkIndex: number
  uploadedBytes: number
  fileSize: number
  chunkSize: number
  totalChunks: number
  status: string
}

interface UploadChunkResponse {
  uploadId: string
  nextChunkIndex: number
  uploadedBytes: number
  completed: boolean
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

export async function uploadVideo(
  file: File,
  title: string,
  onProgress?: (progress: UploadProgress) => void,
): Promise<VideoItem> {
  return uploadVideoInChunks(file, title, onProgress)
}

export async function uploadVideoInChunks(
  file: File,
  title: string,
  onProgress?: (progress: UploadProgress) => void,
): Promise<VideoItem> {
  const chunkSize = DEFAULT_CHUNK_SIZE
  const totalChunks = Math.ceil(file.size / chunkSize)
  const fileHash = await sha256Hex(file)
  const session = await initVideoUpload({
    title,
    fileName: file.name || 'video.mp4',
    fileSize: file.size,
    contentType: file.type || 'video/mp4',
    chunkSize,
    totalChunks,
    fileHash,
  })
  const status = await getVideoUploadStatus(session.uploadId)
  emitUploadProgress(status, onProgress)

  let nextChunkIndex = status.nextChunkIndex
  while (nextChunkIndex < status.totalChunks) {
    const start = nextChunkIndex * status.chunkSize
    const end = Math.min(start + status.chunkSize, file.size)
    const chunkResult = await uploadVideoChunk(
      status.uploadId,
      nextChunkIndex,
      file.slice(start, end),
    )
    nextChunkIndex = chunkResult.nextChunkIndex
    emitUploadProgress(
      {
        ...status,
        nextChunkIndex,
        uploadedBytes: chunkResult.uploadedBytes,
      },
      onProgress,
    )
  }

  const completed = await completeVideoUpload(status.uploadId)
  emitUploadProgress(
    {
      ...status,
      nextChunkIndex: status.totalChunks,
      uploadedBytes: status.fileSize,
      status: 'COMPLETED',
    },
    onProgress,
  )
  return completed
}

export async function initVideoUpload(
  payload: InitVideoUploadRequest,
): Promise<VideoUploadSessionResponse> {
  const response = await apiHttp.post<ApiResult<VideoUploadSessionResponse>>(
    '/api/video-uploads/init',
    payload,
  )
  return unwrapResult(response.data)
}

export async function getVideoUploadStatus(uploadId: string): Promise<VideoUploadSessionResponse> {
  const response = await apiHttp.get<ApiResult<VideoUploadSessionResponse>>(
    `/api/video-uploads/${uploadId}`,
  )
  return unwrapResult(response.data)
}

export async function uploadVideoChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob,
): Promise<UploadChunkResponse> {
  const response = await apiHttp.put<ApiResult<UploadChunkResponse>>(
    `/api/video-uploads/${uploadId}/chunks/${chunkIndex}`,
    chunk,
    {
      headers: {
        'Content-Type': 'application/octet-stream',
      },
    },
  )
  return unwrapResult(response.data)
}

export async function completeVideoUpload(uploadId: string): Promise<VideoItem> {
  const response = await apiHttp.post<ApiResult<BackendVideoPayload>>(
    `/api/video-uploads/${uploadId}/complete`,
  )
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
  const response = await apiHttp.delete<ApiResult<BackendLikeStatus | null>>(
    `/api/videos/${id}/likes`,
  )
  ensureSuccess(response.data)
  return response.data.data ?? null
}

export async function getVideoLikeStatus(id: number): Promise<BackendLikeStatus | null> {
  const response = await apiHttp.get<ApiResult<BackendLikeStatus | null>>(`/api/videos/${id}/likes`)
  ensureSuccess(response.data)
  return response.data.data ?? null
}

export function getVideoPlayUrl(path: string): string {
  if (/^https?:\/\//.test(path) || path.startsWith('blob:')) {
    return path
  }
  return new URL(path, API_BASE_URL).toString()
}

export function resolveVideoPlaySource(video: Pick<VideoItem, 'playUrl'>): string {
  return getVideoPlayUrl(video.playUrl)
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

function emitUploadProgress(
  session: VideoUploadSessionResponse,
  onProgress?: (progress: UploadProgress) => void,
): void {
  onProgress?.({
    uploadId: session.uploadId,
    uploadedBytes: session.uploadedBytes,
    fileSize: session.fileSize,
    currentChunk: session.nextChunkIndex,
    totalChunks: session.totalChunks,
    percent: session.fileSize > 0 ? Math.round((session.uploadedBytes / session.fileSize) * 100) : 0,
    status: session.status,
  })
}

async function sha256Hex(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buffer)
  return [...new Uint8Array(digest)]
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
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
