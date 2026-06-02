<template>
  <div class="recommend">
    <header class="top-tabs">
      <span class="t">直播</span>
      <span class="t" :class="{ on: tab === 'follow' }" @click="tab = 'follow'">关注</span>
      <span class="t" :class="{ on: tab === 'recommend' }" @click="tab = 'recommend'">
        推荐
      </span>
      <svg class="search" viewBox="0 0 24 24" width="22" height="22" fill="none">
        <circle cx="11" cy="11" r="7" stroke="#fff" stroke-width="2" />
        <path d="M20 20l-3.5-3.5" stroke="#fff" stroke-width="2" stroke-linecap="round" />
      </svg>
    </header>

    <div ref="scroller" class="feed">
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
import { nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import VideoCard from '../components/VideoCard.vue'
import CommentSheet from '../components/CommentSheet.vue'
import { useVideoStore } from '../stores/video'

defineOptions({ name: 'RecommendView' })

const videoStore = useVideoStore()
const { feed } = storeToRefs(videoStore)

const tab = ref<'follow' | 'recommend'>('recommend')
const scroller = ref<HTMLElement | null>(null)
const activeIndex = ref(0)
const sheetOpen = ref(false)
const sheetVideoId = ref<number | null>(null)

let observer: IntersectionObserver | null = null

function openComments(id: number) {
  sheetVideoId.value = id
  sheetOpen.value = true
}

function onLike(id: number) {
  void videoStore.toggleLike(id)
}

function setupObserver() {
  observer?.disconnect()
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
    ?.querySelectorAll('.slide')
    .forEach((el) => observer!.observe(el))
}

function markActiveViewed() {
  const video = feed.value[activeIndex.value]
  if (video) {
    void videoStore.markViewed(video.id)
  }
}

onMounted(async () => {
  await videoStore.loadRecommendations()
  await nextTick()
  setupObserver()
  markActiveViewed()
})

watch(activeIndex, markActiveViewed)

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

.top-tabs .search {
  margin-left: auto;
}

.feed {
  height: 100%;
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
}

.feed::-webkit-scrollbar {
  display: none;
}

.slide {
  height: 100%;
  scroll-snap-align: start;
  scroll-snap-stop: always;
}
</style>
