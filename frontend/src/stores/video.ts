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
      if (videos.length) {
        feed.value = videos
      }
      return feed.value
    } catch (error) {
      feedErrorMessage.value = getErrorMessage(error)
      return feed.value
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
    } catch {
      // 推荐/点赞接口未合入时保持本地乐观更新，避免影响当前演示 UI。
    }
  }

  async function markViewed(id: number): Promise<void> {
    if (viewedVideoIds.value.has(id)) return
    viewedVideoIds.value.add(id)
    try {
      await markVideoViewedApi(id)
    } catch {
      // 观看记录接口未合入时忽略失败；后端合入后会自动开始上报。
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
