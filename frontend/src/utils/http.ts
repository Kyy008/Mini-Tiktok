import axios, { AxiosHeaders } from 'axios'

import type { ApiResult } from '../api/types'
import { clearAuthStorage, getAccessToken } from './token'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8085'

export const apiHttp = axios.create({
  baseURL: API_BASE_URL,
})

apiHttp.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${accessToken}`)
    config.headers = headers
  }
  return config
})

apiHttp.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (!axios.isAxiosError<ApiResult<unknown>>(error)) {
      return Promise.reject(error)
    }

    const status = error.response?.status
    const serverMessage = error.response?.data?.message

    if (status === 401) {
      clearAuthStorage()
      return Promise.reject(new Error(serverMessage || '登录已失效，请重新登录'))
    }

    if (status === 403) {
      return Promise.reject(new Error(serverMessage || '权限不足，无法完成操作'))
    }

    if (serverMessage) {
      return Promise.reject(new Error(serverMessage))
    }

    if (!error.response) {
      return Promise.reject(new Error('网络异常，请检查后端服务'))
    }

    return Promise.reject(new Error(error.message || '操作失败'))
  },
)
