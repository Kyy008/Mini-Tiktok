import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { CommentItem, PageResult, UploadProgress, VideoItem } from '../api/types'
import {
  createVideoComment as createVideoCommentApi,
  getVideoComments as fetchVideoComments,
} from '../api/comment'
import {
  clearVideoViewHistory as clearVideoViewHistoryApi,
  deleteVideo as deleteVideoApi,
  getRecommendations as fetchRecommendations,
  getMyVideos as fetchMyVideos,
  getVideo as fetchVideo,
  getVideoLikeStatus,
  likeVideo as likeVideoApi,
  markVideoViewed as markVideoViewedApi,
  unlikeVideo as unlikeVideoApi,
  uploadVideo as uploadVideoApi,
} from '../api/video'
import { getAccessToken } from '../utils/token'

export const useVideoStore = defineStore('video', () => {
  const feed = ref<VideoItem[]>([])
  const myVideos = ref<VideoItem[]>([])
  const myVideosPage = ref(1)
  const myVideosSize = ref(3)
  const myVideosTotal = ref(0)
  const myVideosHasMore = ref(false)
  const loading = ref(false)
  const feedLoading = ref(false)
  const errorMessage = ref('')
  const feedErrorMessage = ref('')
  const uploadProgress = ref<UploadProgress | null>(null)
  const viewedVideoIds = ref<Set<number>>(new Set())
  const recommendationsStale = ref(false)

  interface LoadMyVideosOptions {
    preserveOnError?: boolean
  }

  function findInAll(id: number): VideoItem[] {
    return [...feed.value, ...myVideos.value].filter((v) => v.id === id)
  }

  function replaceVideo(video: VideoItem): void {
    replaceInList(feed, video)
    replaceInList(myVideos, video)
  }

  function setCommentCount(id: number, count: number): void {
    setCommentCountInList(feed, id, count)
    setCommentCountInList(myVideos, id, count)
  }

  async function loadRecommendations(size = 10): Promise<VideoItem[]> {
    feedLoading.value = true
    feedErrorMessage.value = ''
    try {
      const videos = await fetchRecommendations(size)
      feed.value = videos
      recommendationsStale.value = false
      return videos
    } catch (error) {
      feed.value = []
      feedErrorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      feedLoading.value = false
    }
  }

  async function loadVideoDetail(id: number): Promise<VideoItem> {
    const detail = await fetchVideo(id)
    if (!getAccessToken()) {
      replaceVideo(detail)
      return detail
    }
    const likeStatus = await getVideoLikeStatus(id)
    if (likeStatus) {
      detail.liked = likeStatus.liked ?? detail.liked
      detail.likeCount = likeStatus.likeCount ?? likeStatus.count ?? detail.likeCount
    }
    replaceVideo(detail)
    return detail
  }

  async function refreshLikeStatus(id: number): Promise<void> {
    if (!getAccessToken()) return
    const status = await getVideoLikeStatus(id)
    if (!status) return
    applyLikeState(findInAll(id), status.liked ?? false, status.likeCount ?? status.count)
  }

  async function toggleLike(id: number): Promise<void> {
    const videos = findInAll(id)
    if (!videos.length) return

    const nextLiked = !videos[0].liked
    applyLikeState(videos, nextLiked)

    try {
      const status = await (nextLiked ? likeVideoApi(id) : unlikeVideoApi(id))
      if (status) {
        applyLikeState(findInAll(id), status.liked ?? nextLiked, status.likeCount ?? status.count)
      }
    } catch (error) {
      applyLikeState(videos, !nextLiked)
      throw error
    }
  }

  async function markViewed(id: number): Promise<void> {
    if (!getAccessToken()) return
    if (viewedVideoIds.value.has(id)) return
    try {
      await markVideoViewedApi(id)
      viewedVideoIds.value.add(id)
    } catch (error) {
      throw error
    }
  }

  async function clearViewHistory(): Promise<void> {
    await clearVideoViewHistoryApi()
    viewedVideoIds.value.clear()
  }

  async function loadComments(id: number): Promise<CommentItem[]> {
    const comments = await fetchVideoComments(id)
    setCommentCount(id, comments.length)
    return comments
  }

  async function createComment(id: number, content: string): Promise<CommentItem[]> {
    await createVideoCommentApi(id, content)
    return loadComments(id)
  }

  async function loadMyVideos(
    page = 1,
    size = myVideosSize.value,
    options: LoadMyVideosOptions = {},
  ): Promise<PageResult<VideoItem>> {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await fetchMyVideos(page, size)
      myVideos.value = result.list
      myVideosPage.value = result.page
      myVideosSize.value = result.size
      myVideosTotal.value = result.total
      myVideosHasMore.value = result.hasMore
      return result
    } catch (error) {
      if (!options.preserveOnError) {
        if (page === 1) {
          myVideos.value = []
          myVideosTotal.value = 0
          myVideosHasMore.value = false
        }
        errorMessage.value = getErrorMessage(error)
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  async function publish(title: string, file: File): Promise<VideoItem> {
    loading.value = true
    errorMessage.value = ''
    uploadProgress.value = null
    try {
      const item = await uploadVideoApi(file, title, (progress) => {
        uploadProgress.value = progress
      })
      invalidateRecommendations()
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

  function clearUploadProgress(): void {
    uploadProgress.value = null
  }

  async function deleteVideo(id: number): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    try {
      await deleteVideoApi(id)
      myVideos.value = myVideos.value.filter((v) => v.id !== id)
      feed.value = feed.value.filter((v) => v.id !== id)
      invalidateRecommendations()
      myVideosTotal.value = Math.max(0, myVideosTotal.value - 1)
    } catch (error) {
      errorMessage.value = getErrorMessage(error)
      throw error
    } finally {
      loading.value = false
    }
  }

  function invalidateRecommendations(): void {
    recommendationsStale.value = true
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
    uploadProgress,
    recommendationsStale,
    clearUploadProgress,
    invalidateRecommendations,
    toggleLike,
    markViewed,
    clearViewHistory,
    loadComments,
    createComment,
    loadRecommendations,
    loadVideoDetail,
    refreshLikeStatus,
    loadMyVideos,
    publish,
    deleteVideo,
  }
})

function replaceInList(list: { value: VideoItem[] }, video: VideoItem): void {
  const index = list.value.findIndex((item) => item.id === video.id)
  if (index !== -1) {
    list.value[index] = video
  }
}

function setCommentCountInList(list: { value: VideoItem[] }, id: number, count: number): void {
  for (const video of list.value) {
    if (video.id === id) {
      video.commentCount = count
    }
  }
}

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
