// 视频状态管理。当前为 Mock 实现。
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { CommentItem, VideoItem } from '../api/types'
import { MOCK_FEED, MOCK_MY_VIDEOS, mockComments } from '../mock/data'

function clone<T>(v: T): T {
  return JSON.parse(JSON.stringify(v))
}

export const useVideoStore = defineStore('video', () => {
  const feed = ref<VideoItem[]>(clone(MOCK_FEED))
  const myVideos = ref<VideoItem[]>(clone(MOCK_MY_VIDEOS))

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

  function publish(title: string, file: File): VideoItem {
    const url = URL.createObjectURL(file)
    const item: VideoItem = {
      id: Date.now(),
      title: title || '未命名作品',
      playUrl: url,
      coverUrl: `https://picsum.photos/seed/up${Date.now()}/375/680`,
      author: clone(MOCK_MY_VIDEOS[0].author),
      likeCount: 0,
      commentCount: 0,
      favoriteCount: 0,
      shareCount: 0,
      liked: false,
      music: '@原声 - 我自己',
      createdAt: new Date().toISOString().slice(0, 10),
    }
    myVideos.value.unshift(item)
    feed.value.unshift(clone(item))
    return item
  }

  function deleteVideo(id: number) {
    myVideos.value = myVideos.value.filter((v) => v.id !== id)
    feed.value = feed.value.filter((v) => v.id !== id)
  }

  return { feed, myVideos, toggleLike, loadComments, publish, deleteVideo }
})
