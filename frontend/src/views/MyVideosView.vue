<template>
  <div class="profile">
    <div class="scroll">
      <header class="cover">
        <div class="cover-actions">
          <button v-if="isAuthenticated" class="ghost" @click="onLogout">退出登录</button>
          <template v-else>
            <button class="ghost primary" @click="onLogin">登录</button>
            <button class="ghost" @click="onRegister">注册</button>
          </template>
        </div>

        <div class="user">
          <div v-if="!isAuthenticated" class="avatar guest-avatar" aria-label="未登录头像"></div>
          <img v-else class="avatar" :src="displayUser.avatar" alt="" />
          <div class="meta">
            <div class="name">{{ profileName }}</div>
            <div class="uid">{{ profileUid }}</div>
          </div>
        </div>

        <div class="sign">{{ profileSign }}</div>
        <div class="stats">
          <div><b>{{ profileStats.following }}</b><span>关注</span></div>
          <div><b>{{ profileStats.followers }}</b><span>粉丝</span></div>
          <div><b>{{ profileStats.likes }}</b><span>获赞</span></div>
        </div>
      </header>

      <div class="tabs">
        <span class="on">作品 {{ isAuthenticated ? myVideosTotal : 0 }}</span>
        <span>喜欢</span>
        <span>收藏</span>
      </div>

      <div v-if="!isAuthenticated" class="empty action-empty">
        登录后查看和管理你的作品
        <div class="empty-actions">
          <button class="login-inline" type="button" @click="onLogin">登录</button>
          <button class="login-inline secondary" type="button" @click="onRegister">注册</button>
        </div>
      </div>
      <div v-else-if="loading" class="empty">正在加载作品...</div>
      <div v-else-if="errorMessage" class="empty action-empty">
        {{ errorMessage }}
        <button class="login-inline" type="button" @click="loadPage(myVideosPage)">重试</button>
      </div>
      <template v-else>
        <div v-if="myVideos.length" class="grid">
          <div v-for="v in myVideos" :key="v.id" class="cell" @click="play(v)">
            <img :src="v.coverUrl" alt="" />
            <span class="like-tag">♥ {{ v.likeCount }}</span>
          </div>
        </div>

        <div v-if="myVideosTotal > 0" class="pager">
          <label class="page-size">
            <span>每页</span>
            <select v-model.number="selectedPageSize" @change="onPageSizeChange">
              <option :value="5">5</option>
              <option :value="10">10</option>
            </select>
          </label>

          <div class="page-controls">
            <button
              type="button"
              :disabled="myVideosPage <= 1 || loading"
              @click="loadPage(myVideosPage - 1)"
            >
              上一页
            </button>
            <button
              v-for="page in visiblePages"
              :key="page"
              type="button"
              class="page-number"
              :class="{ active: page === myVideosPage }"
              :disabled="loading"
              @click="loadPage(page)"
            >
              {{ page }}
            </button>
            <button
              type="button"
              :disabled="myVideosPage >= totalPages || loading"
              @click="loadPage(myVideosPage + 1)"
            >
              下一页
            </button>
          </div>

          <div class="page-summary">第 {{ myVideosPage }} / {{ totalPages }} 页，共 {{ myVideosTotal }} 项</div>
        </div>

        <div v-if="!myVideos.length" class="empty">还没有发布作品，去发一个吧</div>
      </template>
    </div>

    <transition name="fade">
      <div v-if="current" class="player" @click="closePlayer">
        <video
          class="full"
          :src="currentPlaySource"
          :poster="current.coverUrl"
          autoplay
          loop
          playsinline
        />
        <button class="close" @click.stop="closePlayer">x</button>
        <button class="del" :disabled="deleting" @click.stop="remove">
          {{ deleting ? '删除中...' : '删除作品' }}
        </button>
        <div class="p-title">{{ current.title }}</div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { VideoItem } from '../api/types'
import { resolveVideoPlaySource } from '../api/video'
import { useVideoStore } from '../stores/video'
import { useDisplayUser } from '../composables/useDisplayUser'

const videoStore = useVideoStore()
const router = useRouter()
const { displayUser, isAuthenticated, authStore } = useDisplayUser()
const {
  myVideos,
  myVideosPage,
  myVideosSize,
  myVideosTotal,
  loading,
  errorMessage,
} = storeToRefs(videoStore)

const current = ref<VideoItem | null>(null)
const currentPlaySource = ref('')
const deleting = ref(false)
const selectedPageSize = ref(myVideosSize.value)

const totalPages = computed(() => Math.max(1, Math.ceil(myVideosTotal.value / myVideosSize.value)))
const visiblePages = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const start = Math.max(1, Math.min(myVideosPage.value - 2, total - 4))
  const end = Math.min(total, start + 4)
  for (let page = start; page <= end; page += 1) {
    pages.push(page)
  }
  return pages
})
const profileName = computed(() => (isAuthenticated.value ? displayUser.value.username : '未登录'))
const profileUid = computed(() =>
  isAuthenticated.value ? `抖音号：mini_${displayUser.value.id}` : '登录后显示抖音号',
)
const profileSign = computed(() =>
  isAuthenticated.value ? displayUser.value.signature : '登录后查看和管理你的作品',
)
const profileStats = computed(() => ({
  following: '0',
  followers: '0',
  likes: isAuthenticated.value
    ? myVideos.value.reduce((total, item) => total + item.likeCount, 0).toString()
    : '0',
}))

async function play(v: VideoItem) {
  current.value = v
  currentPlaySource.value = resolveVideoPlaySource(v)
  try {
    const detail = await videoStore.loadVideoDetail(v.id)
    if (current.value?.id === v.id) {
      current.value = detail
      currentPlaySource.value = resolveVideoPlaySource(detail)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载视频详情失败')
  }
}

function closePlayer() {
  current.value = null
  currentPlaySource.value = ''
}

async function loadPage(page: number) {
  if (!isAuthenticated.value || loading.value) return
  const nextPage = Math.max(1, Math.min(page, totalPages.value))
  try {
    await videoStore.loadMyVideos(nextPage, selectedPageSize.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载作品失败')
  }
}

function onPageSizeChange() {
  void loadPage(1)
}

async function remove() {
  if (!current.value || deleting.value) return
  try {
    await ElMessageBox.confirm('确定删除该作品？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    deleting.value = true
    await videoStore.deleteVideo(current.value.id)
    closePlayer()
    const nextTotalPages = Math.max(1, Math.ceil(myVideosTotal.value / selectedPageSize.value))
    const nextPage = Math.min(myVideosPage.value, nextTotalPages)
    try {
      await videoStore.loadMyVideos(nextPage, selectedPageSize.value, {
        preserveOnError: true,
      })
    } catch {
      // 删除已成功，分页刷新失败时保留当前删除结果，避免把成功操作显示成失败。
    }
    ElMessage.success('已删除')
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  } finally {
    deleting.value = false
  }
}

function onLogout() {
  authStore.logout({ redirectToAuthServer: true })
  ElMessage('已退出登录')
}

function onLogin() {
  router.push('/login')
}

function onRegister() {
  router.push('/register')
}

onMounted(() => {
  void loadPage(1)
})

watch(isAuthenticated, (value) => {
  if (value) {
    void loadPage(1)
  }
})
</script>

<style scoped>
.profile {
  position: absolute;
  inset: 0;
  background: #161616;
}

.scroll {
  height: 100%;
  overflow-y: auto;
}

.cover {
  padding: 16px;
  background: linear-gradient(160deg, #2a2730, #161616);
}

.cover-actions {
  display: flex;
  justify-content: flex-end;
}

.ghost {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 14px;
  padding: 5px 12px;
}

.ghost.primary {
  color: #fff;
  background: #fe2c55;
  border-color: #fe2c55;
}

.user {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 14px 0 10px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.guest-avatar {
  background: #050505;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.name {
  font-size: 20px;
  font-weight: 700;
}

.uid {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.sign {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 14px;
}

.stats {
  display: flex;
  gap: 26px;
}

.stats div {
  display: flex;
  flex-direction: column;
}

.stats b {
  font-size: 17px;
}

.stats span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

.tabs {
  display: flex;
  gap: 28px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.tabs .on {
  color: #fff;
  font-weight: 600;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2px;
}

.cell {
  position: relative;
  aspect-ratio: 9 / 14;
  background: #222;
}

.cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.like-tag {
  position: absolute;
  left: 6px;
  bottom: 6px;
  font-size: 12px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
}

.pager {
  margin: 16px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(55, 56, 50, 0.92);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
}

.page-size {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.page-size select {
  width: 58px;
  height: 34px;
  border: none;
  border-radius: 6px;
  background: #20211d;
  color: #fff;
  padding: 0 8px;
  font-weight: 700;
}

.page-controls {
  display: flex;
  justify-content: center;
  gap: 7px;
}

.page-controls button {
  min-width: 38px;
  height: 34px;
  border-radius: 6px;
  background: #141a12;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 0 12px;
}

.page-controls button:disabled {
  color: rgba(255, 255, 255, 0.28);
}

.page-number.active {
  background: #2d384a;
  box-shadow: inset 0 0 0 1px rgba(196, 210, 255, 0.9);
}

.page-summary {
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.empty {
  text-align: center;
  padding: 60px 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}

.action-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.login-inline {
  height: 34px;
  padding: 0 18px;
  border-radius: 17px;
  background: #fe2c55;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.login-inline.secondary {
  background: #333;
}

.empty-actions {
  display: flex;
  gap: 10px;
}

.player {
  position: absolute;
  inset: 0;
  background: #000;
  z-index: 30;
}

.full {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.close {
  position: absolute;
  top: 14px;
  left: 14px;
  font-size: 28px;
  color: #fff;
}

.del {
  position: absolute;
  top: 16px;
  right: 16px;
  font-size: 14px;
  color: #fff;
  background: rgba(254, 44, 85, 0.85);
  padding: 7px 14px;
  border-radius: 16px;
}

.del:disabled {
  opacity: 0.6;
}

.p-title {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 24px;
  font-size: 14px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 420px) {
  .pager {
    grid-template-columns: 1fr;
  }

  .page-controls {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .page-summary {
    text-align: right;
  }
}
</style>
