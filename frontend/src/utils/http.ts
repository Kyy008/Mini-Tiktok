import axios, { AxiosHeaders } from 'axios'

import { getAccessToken } from './token'

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
