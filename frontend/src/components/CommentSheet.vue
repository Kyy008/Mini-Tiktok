<template>
  <transition name="sheet">
    <div v-if="visible" class="mask" @click.self="$emit('close')">
      <div class="sheet">
        <div class="sheet-head">
          <span>{{ comments.length }} 条评论</span>
          <button class="close" @click="$emit('close')">✕</button>
        </div>

        <ul class="list">
          <li v-if="loading" class="state">评论加载中...</li>
          <li v-else-if="errorMessage" class="state error">{{ errorMessage }}</li>
          <li v-else-if="comments.length === 0" class="state">还没有评论，来发第一条吧</li>
          <li v-for="c in comments" :key="c.id" class="comment">
            <div class="avatar">{{ getAvatarText(c.user.username) }}</div>
            <div class="body">
              <div class="name">{{ c.user.username }}</div>
              <div class="text">{{ c.content }}</div>
              <div class="meta">{{ formatTime(c.createdAt) }}</div>
            </div>
          </li>
        </ul>

        <div class="input-bar">
          <input
            v-model="draft"
            :disabled="sending"
            placeholder="善语结善缘，恶言伤人心"
            @keyup.enter="send"
          />
          <button :disabled="sending || !draft.trim()" @click="send">
            {{ sending ? '发送中' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CommentItem } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { useVideoStore } from '../stores/video'

const props = defineProps<{ visible: boolean; videoId: number | null }>()
defineEmits<{ close: [] }>()

const authStore = useAuthStore()
const videoStore = useVideoStore()
const comments = ref<CommentItem[]>([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const errorMessage = ref('')

watch(
  () => [props.visible, props.videoId],
  async () => {
    if (props.visible && props.videoId != null) {
      await refreshComments()
    }
  },
  { immediate: true },
)

async function refreshComments() {
  if (props.videoId == null) return
  loading.value = true
  errorMessage.value = ''
  try {
    comments.value = await videoStore.loadComments(props.videoId)
  } catch (error) {
    comments.value = []
    errorMessage.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text || props.videoId == null || sending.value) return
  if (!authStore.isAuthenticated) {
    errorMessage.value = '请先登录后再发表评论'
    await authStore.login({ redirectPath: window.location.pathname })
    return
  }

  sending.value = true
  errorMessage.value = ''
  try {
    comments.value = await videoStore.createComment(props.videoId, text)
    draft.value = ''
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    sending.value = false
  }
}

function getAvatarText(username: string): string {
  const normalized = username.trim()
  if (!normalized) return '匿'
  return normalized.slice(0, 1).toUpperCase()
}

function formatTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败'
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
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2f80ed, #27ae60);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
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

.state {
  padding: 28px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
}

.state.error {
  color: #ff7b9a;
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
