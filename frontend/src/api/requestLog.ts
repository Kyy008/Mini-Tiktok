import { apiHttp } from '../utils/http'
import type { ApiResult, RequestLogItem } from './types'

export async function getRequestLogs(limit = 50, afterId = 0): Promise<RequestLogItem[]> {
  const response = await apiHttp.get<ApiResult<RequestLogItem[]>>('/api/request-logs', {
    params: { limit, afterId },
  })
  return unwrapResult(response.data)
}

export async function clearRequestLogs(): Promise<number> {
  const response = await apiHttp.delete<ApiResult<number>>('/api/request-logs')
  return unwrapResult(response.data)
}

function unwrapResult<T>(result: ApiResult<T>): T {
  if (result.code !== 200 || result.data === undefined || result.data === null) {
    throw new Error(result.message || '日志接口未返回数据')
  }
  return result.data
}
