import { apiHttp } from '../utils/http'
import type { ApiResult, CommentItem } from './types'

interface BackendCommentPayload {
  id: number
  videoId: number
  userId: string
  username: string
  content: string
  createdAt: string
}

export async function getVideoComments(videoId: number): Promise<CommentItem[]> {
  const response = await apiHttp.get<ApiResult<BackendCommentPayload[]>>(
    `/api/videos/${videoId}/comments`,
  )
  const data = unwrapResult(response.data)
  return data.map(toCommentItem)
}

export async function createVideoComment(videoId: number, content: string): Promise<CommentItem> {
  const response = await apiHttp.post<ApiResult<BackendCommentPayload>>(
    `/api/videos/${videoId}/comments`,
    { content },
  )
  return toCommentItem(unwrapResult(response.data))
}

function toCommentItem(comment: BackendCommentPayload): CommentItem {
  const numericUserId = Number(comment.userId)
  return {
    id: Number(comment.id),
    videoId: Number(comment.videoId),
    user: {
      id: Number.isFinite(numericUserId) ? numericUserId : 0,
      username: comment.username || comment.userId || '匿名用户',
      avatar: '',
    },
    content: comment.content,
    createdAt: comment.createdAt,
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
