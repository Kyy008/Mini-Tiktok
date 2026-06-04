<template>
  <div class="card" @click="togglePlay" @dblclick="onDouble">
    <video
      ref="videoEl"
      class="video"
      :src="playSource"
      :poster="video.coverUrl"
      loop
      playsinline
      webkit-playsinline
      preload="metadata"
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
import { ref, watch } from 'vue'
import type { VideoItem } from '../api/types'
import { resolveVideoPlaySource } from '../api/video'
import ActionRail from './ActionRail.vue'

const props = defineProps<{ video: VideoItem; active: boolean }>()
const emit = defineEmits<{ like: []; comment: [] }>()

const videoEl = ref<HTMLVideoElement | null>(null)
const playing = ref(false)
const showHeart = ref(false)
const heartStyle = ref<Record<string, string>>({})
const playSource = ref(props.video.playUrl)

watch(
  () => props.active,
  (a) => {
    const el = videoEl.value
    if (!el) return
    if (a) {
      el.currentTime = 0
      el.play().then(() => (playing.value = true)).catch(() => (playing.value = false))
    } else {
      el.pause()
      playing.value = false
    }
  },
)

watch(
  () => props.video.playUrl,
  () => {
    void loadPlaySource()
  },
  { immediate: true },
)

function togglePlay() {
  const el = videoEl.value
  if (!el) return
  if (el.paused) {
    el.play()
    playing.value = true
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
  object-fit: cover;
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

.info {
  position: absolute;
  left: 12px;
  bottom: 22px;
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
