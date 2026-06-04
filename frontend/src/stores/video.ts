import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { CommentItem, PageResult, VideoItem } from '../api/types'
import {
  deleteVideo as deleteVideoApi,
  getRecommendations as fetchRecommendations,
  getMyVideos as fetchMyVideos,
  likeVideo as likeVideoApi,
  markVideoViewed as markVideoViewedApi,
  unlikeVideo as unlikeVideoApi,
  uploadVideo as uploadVideoApi,
} from '../api/video'
import { mockComments } from '../mock/data'

export const useVideoStore = defineStore('video', () => {
  const feed = ref<VideoItem[]>([])
  const myVideos = ref<VideoItem[]>([])
  const myVideosPage = ref(1)
  const myVideosSize = ref(10)
  const myVideosTotal = ref(0)
  const myVideosHasMore = ref(false)
  const loading = ref(false)
  const feedLoading = ref(false)
  const errorMessage = ref('')
  const feedErrorMessage = ref('')
  const viewedVideoIds = ref<Set<number>>(new Set())

  function findInAll(id: number): VideoItem[] {
    return [...feed.value, ...myVideos.value].filter((v) => v.id === id)
  }

  async function loadRecommendations(size = 10): Promise<VideoItem[]> {
    feedLoading.value = true
    feedErrorMessage.value = ''
    try {
      const videos = await fetchRecommendations(size)
      feed.value = videos
      return videos
    } catch (error) {
      feed.value = []
      feedErrorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      feedLoading.value = false
    }
  }

  async function toggleLike(id: number): Promise<void> {
    const videos = findInAll(id)
    if (!videos.length) return

    const nextLiked = !videos[0].liked
    applyLikeState(videos, nextLiked)

    try {
      const status = nextLiked ? await likeVideoApi(id) : await unlikeVideoApi(id)
      if (status) {
        applyLikeState(videos, status.liked ?? nextLiked, status.likeCount ?? status.count)
      }
    } catch (error) {
      applyLikeState(videos, !nextLiked)
      throw error
    }
  }

  async function markViewed(id: number): Promise<void> {
    if (viewedVideoIds.value.has(id)) return
    try {
      await markVideoViewedApi(id)
      viewedVideoIds.value.add(id)
    } catch (error) {
      throw error
    }
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
      if (page === 1) {
        myVideos.value = []
        myVideosTotal.value = 0
        myVideosHasMore.value = false
      }
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
    feedLoading,
    errorMessage,
    feedErrorMessage,
    toggleLike,
    markViewed,
    loadComments,
    loadRecommendations,
    loadMyVideos,
    publish,
    deleteVideo,
  }
})

function applyLikeState(videos: VideoItem[], liked: boolean, explicitCount?: number): void {
  for (const video of videos) {
    if (typeof explicitCount === 'number') {
      video.likeCount = explicitCount
    } else if (video.liked !== liked) {
      video.likeCount += liked ? 1 : -1
    }
    video.liked = liked
  }
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败'
}
