<template>
  <div class="card" @click="togglePlay" @dblclick="onDouble">
    <video
      ref="videoEl"
      class="video"
      :src="playSource"
      :poster="video.coverUrl"
      autoplay
      muted
      loop
      playsinline
      webkit-playsinline
      preload="metadata"
      @loadedmetadata="onLoadedMetadata"
      @durationchange="onLoadedMetadata"
      @timeupdate="onTimeUpdate"
      @progress="onProgress"
      @waiting="onWaiting"
      @playing="onPlaying"
    />

    <transition name="fade">
      <div v-if="!playing" class="play-btn">
        <svg viewBox="0 0 24 24" width="62" height="62" fill="rgba(255,255,255,0.85)">
          <path d="M8 5v14l11-7z" />
        </svg>
      </div>
    </transition>

    <transition name="pop">
      <div v-if="showHeart" class="big-heart" :style="heartStyle">
        <svg viewBox="0 0 24 24" width="90" height="90">
          <path
            d="M12 21s-7.5-4.6-10-9.2C.6 9 1.7 5.6 5 4.7 7.2 4.1 9.4 5 12 7.6 14.6 5 16.8 4.1 19 4.7c3.3.9 4.4 4.3 3 7.1C19.5 16.4 12 21 12 21z"
            fill="#fe2c55"
          />
        </svg>
      </div>
    </transition>

    <div class="bottom-gradient" />

    <div
      class="stream-progress"
      role="slider"
      aria-label="视频播放进度"
      :aria-valuemin="0"
      :aria-valuemax="100"
      :aria-valuenow="Math.round(displayProgressPercent)"
      @click.stop
      @dblclick.stop
      @pointerdown.stop.prevent="startScrub"
      @pointermove.stop.prevent="moveScrub"
      @pointerup.stop.prevent="finishScrub"
      @pointercancel.stop.prevent="cancelScrub"
    >
      <div class="progress-track">
        <div class="buffered-bar" :style="{ width: `${bufferedPercent}%` }" />
        <div class="played-bar" :style="{ width: `${displayProgressPercent}%` }" />
      </div>
      <div
        class="progress-thumb"
        :class="{ show: isScrubbing }"
        :style="{ left: `${displayProgressPercent}%` }"
      />
      <span v-if="isWaiting" class="buffering">缓冲中</span>
    </div>

    <div class="info">
      <div class="author">@{{ video.author.username }}</div>
      <div class="title">{{ video.title }}</div>
      <div class="music">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="#fff">
          <path d="M12 3v10.6A4 4 0 1014 17V7h4V3z" />
        </svg>
        <span class="marquee">{{ video.music }}</span>
      </div>
    </div>

    <ActionRail :video="video" @like="$emit('like')" @comment="$emit('comment')" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { VideoItem } from '../api/types'
import { resolveVideoPlaySource } from '../api/video'
import ActionRail from './ActionRail.vue'

const props = defineProps<{ video: VideoItem; active: boolean; soundEnabled: boolean }>()
const emit = defineEmits<{ like: []; comment: []; 'enable-sound': [] }>()

const videoEl = ref<HTMLVideoElement | null>(null)
const playing = ref(false)
const showHeart = ref(false)
const heartStyle = ref<Record<string, string>>({})
const playSource = ref(props.video.playUrl)
const duration = ref(0)
const currentTime = ref(0)
const bufferedEnd = ref(0)
const isScrubbing = ref(false)
const scrubRatio = ref(0)
const isWaiting = ref(false)

const displayProgressPercent = computed(() => {
  const total = duration.value
  if (!Number.isFinite(total) || total <= 0) return 0
  const displayedTime = isScrubbing.value ? scrubRatio.value * total : currentTime.value
  return clamp((displayedTime / total) * 100, 0, 100)
})

const bufferedPercent = computed(() => {
  const total = duration.value
  if (!Number.isFinite(total) || total <= 0) return 0
  return clamp((bufferedEnd.value / total) * 100, 0, 100)
})

watch(
  () => props.active,
  (a) => {
    void syncPlayback(a)
  },
)

watch(
  () => props.video.playUrl,
  () => {
    resetProgressState()
    void loadPlaySource()
  },
  { immediate: true },
)

watch(
  () => props.soundEnabled,
  (enabled) => {
    const el = videoEl.value
    if (enabled && props.active && el && !el.paused) {
      el.muted = false
    }
  },
)

function togglePlay() {
  const el = videoEl.value
  if (!el) return
  if (!el.paused && el.muted) {
    emit('enable-sound')
    el.muted = false
    playing.value = true
    return
  }
  if (el.paused) {
    emit('enable-sound')
    el.muted = false
    void el.play()
      .then(() => {
        playing.value = true
      })
      .catch(() => {
        playing.value = false
      })
  } else {
    el.pause()
    playing.value = false
  }
}

function onDouble(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  heartStyle.value = {
    left: `${e.clientX - rect.left - 45}px`,
    top: `${e.clientY - rect.top - 45}px`,
  }
  showHeart.value = true
  setTimeout(() => (showHeart.value = false), 700)
  if (!props.video.liked) emit('like')
}

async function loadPlaySource() {
  playSource.value = resolveVideoPlaySource(props.video)
}

async function syncPlayback(active: boolean) {
  const el = videoEl.value
  if (!el) return
  if (!active) {
    el.pause()
    playing.value = false
    isWaiting.value = false
    return
  }

  el.currentTime = 0
  resetProgressState()
  el.muted = !props.soundEnabled
  try {
    await el.play()
    playing.value = true
  } catch {
    if (!el.muted) {
      el.muted = true
      try {
        await el.play()
        playing.value = true
        return
      } catch {
        // 保持下方统一失败状态。
      }
    }
    playing.value = false
  }
}

function onLoadedMetadata() {
  const el = videoEl.value
  if (!el) return
  duration.value = normalizeMediaTime(el.duration)
  currentTime.value = normalizeMediaTime(el.currentTime)
  updateBufferedProgress()
}

function onTimeUpdate() {
  const el = videoEl.value
  if (!el) return
  if (!isScrubbing.value) {
    currentTime.value = normalizeMediaTime(el.currentTime)
  }
  updateBufferedProgress()
}

function onProgress() {
  updateBufferedProgress()
}

function onWaiting() {
  if (props.active) {
    isWaiting.value = true
  }
}

function onPlaying() {
  playing.value = true
  isWaiting.value = false
  onTimeUpdate()
}

function startScrub(event: PointerEvent) {
  if (!canSeek()) return
  const target = event.currentTarget as HTMLElement
  isScrubbing.value = true
  target.setPointerCapture?.(event.pointerId)
  updateScrubRatio(event, target)
}

function moveScrub(event: PointerEvent) {
  if (!isScrubbing.value) return
  updateScrubRatio(event, event.currentTarget as HTMLElement)
}

function finishScrub(event: PointerEvent) {
  if (!isScrubbing.value) return
  const target = event.currentTarget as HTMLElement
  updateScrubRatio(event, target)
  seekToRatio(scrubRatio.value)
  target.releasePointerCapture?.(event.pointerId)
  isScrubbing.value = false
}

function cancelScrub(event: PointerEvent) {
  if (!isScrubbing.value) return
  const target = event.currentTarget as HTMLElement
  target.releasePointerCapture?.(event.pointerId)
  isScrubbing.value = false
  const total = duration.value
  scrubRatio.value = total > 0 ? clamp(currentTime.value / total, 0, 1) : 0
}

function canSeek() {
  return Number.isFinite(duration.value) && duration.value > 0
}

function updateScrubRatio(event: PointerEvent, target: HTMLElement) {
  const rect = target.getBoundingClientRect()
  if (rect.width <= 0) return
  scrubRatio.value = clamp((event.clientX - rect.left) / rect.width, 0, 1)
}

function seekToRatio(ratio: number) {
  const el = videoEl.value
  const total = duration.value
  if (!el || !Number.isFinite(total) || total <= 0) return
  const nextTime = clamp(ratio, 0, 1) * total
  el.currentTime = nextTime
  currentTime.value = nextTime
  if (props.active && el.paused) {
    void el.play()
      .then(() => {
        playing.value = true
      })
      .catch(() => {
        playing.value = false
      })
  }
}

function updateBufferedProgress() {
  const el = videoEl.value
  if (!el || !Number.isFinite(el.duration) || el.duration <= 0) {
    bufferedEnd.value = 0
    return
  }

  let latestBufferedEnd = 0
  for (let index = 0; index < el.buffered.length; index += 1) {
    latestBufferedEnd = Math.max(latestBufferedEnd, el.buffered.end(index))
  }
  bufferedEnd.value = clamp(latestBufferedEnd, 0, el.duration)
}

function resetProgressState() {
  duration.value = 0
  currentTime.value = 0
  bufferedEnd.value = 0
  isScrubbing.value = false
  scrubRatio.value = 0
  isWaiting.value = false
}

function normalizeMediaTime(value: number) {
  return Number.isFinite(value) && value > 0 ? value : 0
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

onMounted(() => {
  if (props.active) {
    void syncPlayback(true)
  }
})
</script>

<style scoped>
.card {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  overflow: hidden;
}

.video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.play-btn,
.big-heart {
  position: absolute;
  pointer-events: none;
}

.play-btn {
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-gradient {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 220px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.55), transparent);
  pointer-events: none;
}

.stream-progress {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3;
  height: 20px;
  cursor: pointer;
  touch-action: none;
}

.progress-track {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: rgba(255, 255, 255, 0.22);
  overflow: hidden;
}

.buffered-bar,
.played-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  transition: width 0.12s linear;
}

.buffered-bar {
  background: rgba(255, 255, 255, 0.38);
}

.played-bar {
  background: #fff;
}

.progress-thumb {
  position: absolute;
  bottom: -3px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.35);
  opacity: 0;
  transform: translateX(-50%) scale(0.8);
  transition:
    opacity 0.16s,
    transform 0.16s;
}

.progress-thumb.show,
.stream-progress:hover .progress-thumb {
  opacity: 1;
  transform: translateX(-50%) scale(1);
}

.stream-progress:hover .progress-track,
.stream-progress:active .progress-track {
  height: 4px;
}

.buffering {
  position: absolute;
  right: 10px;
  bottom: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.45);
  color: rgba(255, 255, 255, 0.82);
  font-size: 11px;
  line-height: 1.2;
  pointer-events: none;
}

.info {
  position: absolute;
  left: 12px;
  bottom: 22px;
  z-index: 2;
  width: 250px;
}

.author {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.title {
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 10px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.music {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  max-width: 200px;
  overflow: hidden;
  white-space: nowrap;
}

.marquee {
  display: inline-block;
  animation: scroll-text 8s linear infinite;
}

@keyframes scroll-text {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-60%);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.pop-enter-active {
  transition:
    transform 0.25s cubic-bezier(0.2, 1.4, 0.4, 1),
    opacity 0.25s;
}
.pop-leave-active {
  transition: opacity 0.4s;
}
.pop-enter-from {
  transform: scale(0.3);
  opacity: 0;
}
.pop-leave-to {
  opacity: 0;
}
</style>
