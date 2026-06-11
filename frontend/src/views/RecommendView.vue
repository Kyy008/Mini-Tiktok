<template>
  <div class="recommend">
    <header class="top-tabs">
      <span class="t on">推荐</span>
      <button class="clear-history" type="button" :disabled="clearingHistory" @click="clearHistory">
        {{ clearingHistory ? '清除中...' : '清除推荐历史' }}
      </button>
    </header>

    <div v-if="feedLoading" class="state">正在加载推荐...</div>
    <div v-else-if="feedErrorMessage" class="state action-state">
      {{ feedErrorMessage }}
      <button type="button" @click="reload">重试</button>
    </div>
    <div v-else-if="!feed.length" class="state">暂无推荐视频</div>

    <div
      v-else
      ref="scroller"
      class="feed"
      @wheel="onWheel"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
    >
      <div
        v-for="(v, i) in feed"
        :key="v.id"
        class="slide"
        :data-index="i"
      >
        <VideoCard
          :video="v"
          :active="i === activeIndex"
          @like="onLike(v.id)"
          @comment="openComments(v.id)"
        />
      </div>
    </div>

    <CommentSheet
      :visible="sheetOpen"
      :video-id="sheetVideoId"
      @close="sheetOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VideoCard from '../components/VideoCard.vue'
import CommentSheet from '../components/CommentSheet.vue'
import { useVideoStore } from '../stores/video'
import { useAuthStore } from '../stores/auth'

defineOptions({ name: 'RecommendView' })

const videoStore = useVideoStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { feed, feedLoading, feedErrorMessage } = storeToRefs(videoStore)
const { isAuthenticated } = storeToRefs(authStore)

const scroller = ref<HTMLElement | null>(null)
const activeIndex = ref(0)
const sheetOpen = ref(false)
const sheetVideoId = ref<number | null>(null)
const clearingHistory = ref(false)

let observer: IntersectionObserver | null = null
let touchStartY = 0
let wheelLock = false

function openComments(id: number) {
  sheetVideoId.value = id
  sheetOpen.value = true
}

async function onLike(id: number) {
  if (!isAuthenticated.value) {
    await router.push({
      path: '/login',
      query: { redirect: route.fullPath },
    })
    return
  }
  try {
    await videoStore.toggleLike(id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '点赞失败')
  }
}

async function reload() {
  try {
    await videoStore.loadRecommendations()
    await nextTick()
    activeIndex.value = 0
    setupObserver()
    await syncActiveVideo()
  } catch {
    // 错误文案已经写入 feedErrorMessage。
  }
}

async function clearHistory() {
  if (!isAuthenticated.value) {
    await router.push({
      path: '/login',
      query: { redirect: route.fullPath },
    })
    return
  }
  if (clearingHistory.value) return
  clearingHistory.value = true
  try {
    await videoStore.clearViewHistory()
    await reload()
    ElMessage.success('推荐历史已清除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清除推荐历史失败')
  } finally {
    clearingHistory.value = false
  }
}

function setupObserver() {
  observer?.disconnect()
  if (!scroller.value) return
  observer = new IntersectionObserver(
    (entries) => {
      for (const e of entries) {
        if (e.isIntersecting && e.intersectionRatio >= 0.6) {
          activeIndex.value = Number((e.target as HTMLElement).dataset.index)
        }
      }
    },
    { root: scroller.value, threshold: [0.6] },
  )
  scroller.value
    .querySelectorAll('.slide')
    .forEach((el) => observer!.observe(el))
}

async function syncActiveVideo() {
  const video = feed.value[activeIndex.value]
  if (!video) return
  try {
    await videoStore.loadVideoDetail(video.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载视频详情失败')
  }
  try {
    await videoStore.markViewed(video.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '访问记录上报失败')
  }
}

function goToIndex(index: number) {
  const next = Math.max(0, Math.min(feed.value.length - 1, index))
  if (next === activeIndex.value || !scroller.value) return
  activeIndex.value = next
  scroller.value.scrollTo({
    top: next * scroller.value.clientHeight,
    behavior: 'smooth',
  })
}

function onWheel(event: WheelEvent) {
  if (Math.abs(event.deltaY) < 20 || wheelLock) return
  wheelLock = true
  goToIndex(activeIndex.value + (event.deltaY > 0 ? 1 : -1))
  window.setTimeout(() => {
    wheelLock = false
  }, 420)
}

function onTouchStart(event: TouchEvent) {
  touchStartY = event.changedTouches[0]?.clientY ?? 0
}

function onTouchEnd(event: TouchEvent) {
  const endY = event.changedTouches[0]?.clientY ?? touchStartY
  const delta = touchStartY - endY
  if (Math.abs(delta) < 40) return
  goToIndex(activeIndex.value + (delta > 0 ? 1 : -1))
}

onMounted(reload)

watch(activeIndex, () => {
  void syncActiveVideo()
})

watch(
  () => feed.value.length,
  async () => {
    await nextTick()
    setupObserver()
  },
)

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.recommend {
  position: absolute;
  inset: 0;
  background: #000;
}

.top-tabs {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px 16px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.4), transparent);
}

.top-tabs .t {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
}

.top-tabs .t.on {
  color: #fff;
  font-weight: 700;
}

.clear-history {
  margin-left: auto;
  padding: 5px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  white-space: nowrap;
}

.clear-history:disabled {
  opacity: 0.55;
}

.feed {
  height: 100%;
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
  overscroll-behavior-y: contain;
}

.feed::-webkit-scrollbar {
  display: none;
}

.slide {
  height: 100%;
  scroll-snap-align: start;
  scroll-snap-stop: always;
}

.state {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 28px;
  color: rgba(255, 255, 255, 0.55);
  text-align: center;
  font-size: 14px;
}

.action-state {
  flex-direction: column;
  gap: 14px;
}

.action-state button {
  height: 34px;
  padding: 0 18px;
  border-radius: 17px;
  background: #fe2c55;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
</style>
