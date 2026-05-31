import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { CommentItem, PageResult, VideoItem } from '../api/types'
import {
  deleteVideo as deleteVideoApi,
  getMyVideos as fetchMyVideos,
  uploadVideo as uploadVideoApi,
} from '../api/video'
import { MOCK_FEED, MOCK_MY_VIDEOS, mockComments } from '../mock/data'

function clone<T>(v: T): T {
  return JSON.parse(JSON.stringify(v))
}

export const useVideoStore = defineStore('video', () => {
  const feed = ref<VideoItem[]>(clone(MOCK_FEED))
  const myVideos = ref<VideoItem[]>(clone(MOCK_MY_VIDEOS))
  const myVideosPage = ref(1)
  const myVideosSize = ref(10)
  const myVideosTotal = ref(myVideos.value.length)
  const myVideosHasMore = ref(false)
  const loading = ref(false)
  const errorMessage = ref('')

  function findInAll(id: number): VideoItem[] {
    return [...feed.value, ...myVideos.value].filter((v) => v.id === id)
  }

  function toggleLike(id: number) {
    findInAll(id).forEach((v) => {
      v.liked = !v.liked
      v.likeCount += v.liked ? 1 : -1
    })
  }

  function loadComments(id: number): CommentItem[] {
    return mockComments(id)
  }

  async function loadMyVideos(page = 1, size = myVideosSize.value): Promise<PageResult<VideoItem>> {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await fetchMyVideos(page, size)
      myVideos.value = page === 1 ? result.list : [...myVideos.value, ...result.list]
      myVideosPage.value = result.page
      myVideosSize.value = result.size
      myVideosTotal.value = result.total
      myVideosHasMore.value = result.hasMore
      return result
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function publish(title: string, file: File): Promise<VideoItem> {
    loading.value = true
    errorMessage.value = ''
    try {
      const item = await uploadVideoApi(file, title)
      myVideos.value.unshift(item)
      myVideosTotal.value += 1
      return item
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function deleteVideo(id: number): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    try {
      await deleteVideoApi(id)
      myVideos.value = myVideos.value.filter((v) => v.id !== id)
      feed.value = feed.value.filter((v) => v.id !== id)
      myVideosTotal.value = Math.max(0, myVideosTotal.value - 1)
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    feed,
    myVideos,
    myVideosPage,
    myVideosSize,
    myVideosTotal,
    myVideosHasMore,
    loading,
    errorMessage,
    toggleLike,
    loadComments,
    loadMyVideos,
    publish,
    deleteVideo,
  }
})

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败'
}
