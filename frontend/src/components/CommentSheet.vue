<template>
  <transition name="sheet">
    <div v-if="visible" class="mask" @click.self="$emit('close')">
      <div class="sheet">
        <div class="sheet-head">
          <span>{{ comments.length }} 条评论</span>
          <button class="close" @click="$emit('close')">✕</button>
        </div>

        <ul class="list">
          <li v-for="c in comments" :key="c.id" class="comment">
            <img class="avatar" :src="c.user.avatar" alt="" />
            <div class="body">
              <div class="name">{{ c.user.username }}</div>
              <div class="text">{{ c.content }}</div>
              <div class="meta">{{ c.createdAt }}</div>
            </div>
            <div class="like">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="rgba(255,255,255,0.5)">
                <path d="M12 21s-7.5-4.6-10-9.2C.6 9 1.7 5.6 5 4.7 7.2 4.1 9.4 5 12 7.6 14.6 5 16.8 4.1 19 4.7c3.3.9 4.4 4.3 3 7.1C19.5 16.4 12 21 12 21z" />
              </svg>
              <span>{{ c.likeCount }}</span>
            </div>
          </li>
        </ul>

        <div class="input-bar">
          <input v-model="draft" placeholder="善语结善缘，恶言伤人心" @keyup.enter="send" />
          <button :disabled="!draft.trim()" @click="send">发送</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CommentItem } from '../api/types'
import { useVideoStore } from '../stores/video'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{ visible: boolean; videoId: number | null }>()
defineEmits<{ close: [] }>()

const videoStore = useVideoStore()
const authStore = useAuthStore()
const comments = ref<CommentItem[]>([])
const draft = ref('')

watch(
  () => [props.visible, props.videoId],
  () => {
    if (props.visible && props.videoId != null) {
      comments.value = videoStore.loadComments(props.videoId)
    }
  },
)

function send() {
  const text = draft.value.trim()
  if (!text || !authStore.user) return
  comments.value.unshift({
    id: Date.now(),
    user: authStore.user,
    content: text,
    likeCount: 0,
    createdAt: '刚刚',
  })
  draft.value = ''
}
</script>

<style scoped>
.mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 20;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  height: 62%;
  background: #1c1c1e;
  border-radius: 14px 14px 0 0;
  display: flex;
  flex-direction: column;
}

.sheet-head {
  position: relative;
  text-align: center;
  padding: 14px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.close {
  position: absolute;
  right: 16px;
  top: 12px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
}

.list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 14px;
}

.comment {
  display: flex;
  gap: 10px;
  padding: 10px 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.body {
  flex: 1;
}

.name {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 3px;
}

.text {
  font-size: 14px;
  color: #fff;
  line-height: 1.4;
}

.meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 4px;
}

.like {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.input-bar {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.input-bar input {
  flex: 1;
  height: 38px;
  border-radius: 19px;
  border: none;
  background: #2c2c2e;
  color: #fff;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.input-bar button {
  color: #fe2c55;
  font-size: 14px;
  font-weight: 600;
  padding: 0 8px;
}

.input-bar button:disabled {
  color: rgba(255, 255, 255, 0.3);
}

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.25s;
}
.sheet-enter-active .sheet,
.sheet-leave-active .sheet {
  transition: transform 0.25s ease;
}
.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}
.sheet-enter-from .sheet,
.sheet-leave-to .sheet {
  transform: translateY(100%);
}
</style>
