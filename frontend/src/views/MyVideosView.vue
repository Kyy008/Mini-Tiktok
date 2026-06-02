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
          <img class="avatar" :src="displayUser.avatar" alt="" />
          <div class="meta">
            <div class="name">{{ displayUser.username }}</div>
            <div class="uid">抖音号：mini_{{ displayUser.id }}</div>
          </div>
        </div>
        <div class="sign">{{ displayUser.signature }}</div>
        <div class="stats">
          <div><b>56</b><span>关注</span></div>
          <div><b>1.2w</b><span>粉丝</span></div>
          <div><b>3.4w</b><span>获赞</span></div>
        </div>
      </header>

      <div class="tabs">
        <span class="on">作品 {{ isAuthenticated ? myVideos.length : 0 }}</span>
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
        <button class="login-inline" type="button" @click="loadVideos">重试</button>
      </div>
      <template v-else>
        <div v-if="myVideos.length" class="grid">
          <div v-for="v in myVideos" :key="v.id" class="cell" @click="play(v)">
            <img :src="v.coverUrl" alt="" />
            <span class="like-tag">♥ {{ v.likeCount }}</span>
          </div>
        </div>
        <button
          v-if="myVideosHasMore"
          class="load-more"
          type="button"
          @click="loadMore"
        >
          加载更多
        </button>
        <div v-if="!myVideos.length" class="empty">还没有发布作品，去发一个吧~</div>
      </template>
    </div>

    <transition name="fade">
      <div v-if="current" class="player" @click="current = null">
        <video
          class="full"
          :src="current.playUrl"
          :poster="current.coverUrl"
          autoplay
          loop
          playsinline
        />
        <button class="close" @click.stop="current = null">×</button>
        <button class="del" :disabled="deleting" @click.stop="remove">
          {{ deleting ? '删除中...' : '删除作品' }}
        </button>
        <div class="p-title">{{ current.title }}</div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { VideoItem } from '../api/types'
import { useVideoStore } from '../stores/video'
import { useDisplayUser } from '../composables/useDisplayUser'

const videoStore = useVideoStore()
const { displayUser, isAuthenticated, authStore } = useDisplayUser()
const { myVideos, myVideosHasMore, myVideosPage, myVideosSize, loading, errorMessage } =
  storeToRefs(videoStore)

const current = ref<VideoItem | null>(null)
const deleting = ref(false)

function play(v: VideoItem) {
  current.value = v
}

async function loadVideos() {
  if (!isAuthenticated.value) return
  try {
    await videoStore.loadMyVideos(1, myVideosSize.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载作品失败')
  }
}

async function loadMore() {
  if (!isAuthenticated.value || loading.value) return
  try {
    await videoStore.loadMyVideos(myVideosPage.value + 1, myVideosSize.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载更多失败')
  }
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
    current.value = null
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
  void authStore.login()
}

function onRegister() {
  authStore.register()
}

onMounted(() => {
  void loadVideos()
})

watch(isAuthenticated, (value) => {
  if (value) {
    void loadVideos()
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

.login-inline,
.load-more {
  height: 34px;
  padding: 0 18px;
  border-radius: 17px;
  background: #fe2c55;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.load-more {
  display: block;
  margin: 18px auto 32px;
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
</style>
