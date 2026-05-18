<template>
  <div class="rail">
    <div class="avatar-wrap">
      <img class="avatar" :src="video.author.avatar" alt="" />
      <button class="follow" @click.stop="followed = !followed">
        <svg v-if="!followed" viewBox="0 0 24 24" width="14" height="14">
          <path d="M12 5v14M5 12h14" stroke="#fff" stroke-width="3" stroke-linecap="round" />
        </svg>
        <span v-else class="ok">✓</span>
      </button>
    </div>

    <button class="item" @click.stop="$emit('like')">
      <svg viewBox="0 0 24 24" width="34" height="34">
        <path
          d="M12 21s-7.5-4.6-10-9.2C.6 9 1.7 5.6 5 4.7 7.2 4.1 9.4 5 12 7.6 14.6 5 16.8 4.1 19 4.7c3.3.9 4.4 4.3 3 7.1C19.5 16.4 12 21 12 21z"
          :fill="video.liked ? '#fe2c55' : 'rgba(255,255,255,0.95)'"
        />
      </svg>
      <span>{{ fmt(video.likeCount) }}</span>
    </button>

    <button class="item" @click.stop="$emit('comment')">
      <svg viewBox="0 0 24 24" width="34" height="34" fill="rgba(255,255,255,0.95)">
        <path d="M12 3C6.5 3 2 6.7 2 11.2c0 2.5 1.4 4.7 3.6 6.2L5 21l4.2-2.1c.9.2 1.8.3 2.8.3 5.5 0 10-3.7 10-8.2S17.5 3 12 3z" />
      </svg>
      <span>{{ fmt(video.commentCount) }}</span>
    </button>

    <button class="item" @click.stop="favorited = !favorited">
      <svg viewBox="0 0 24 24" width="34" height="34">
        <path
          d="M12 2l2.9 6.3 6.8.8-5 4.7 1.3 6.7L12 17.8 5.9 20.5 7.2 13.8l-5-4.7 6.8-.8z"
          :fill="favorited ? '#ffd400' : 'rgba(255,255,255,0.95)'"
        />
      </svg>
      <span>{{ fmt(video.favoriteCount + (favorited ? 1 : 0)) }}</span>
    </button>

    <button class="item" @click.stop>
      <svg viewBox="0 0 24 24" width="34" height="34" fill="rgba(255,255,255,0.95)">
        <path d="M14 9V5l7 7-7 7v-4.1C9 11.8 6 13 3 18c0-7 4-11 11-11z" />
      </svg>
      <span>{{ fmt(video.shareCount) }}</span>
    </button>

    <div class="disc">
      <img :src="video.author.avatar" alt="" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { VideoItem } from '../api/types'

defineProps<{ video: VideoItem }>()
defineEmits<{ like: []; comment: [] }>()

const followed = ref(false)
const favorited = ref(false)

function fmt(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return String(n)
}
</script>

<style scoped>
.rail {
  position: absolute;
  right: 8px;
  bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.avatar-wrap {
  position: relative;
  margin-bottom: 6px;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid #fff;
  object-fit: cover;
}

.follow {
  position: absolute;
  left: 50%;
  bottom: -10px;
  transform: translateX(-50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fe2c55;
  display: flex;
  align-items: center;
  justify-content: center;
}

.follow .ok {
  font-size: 12px;
  color: #fff;
}

.item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.item span {
  font-size: 12px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

.disc {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #1f1f1f;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  animation: spin 6s linear infinite;
}

.disc img {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
