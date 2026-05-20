<template>
  <div class="profile">
    <div class="scroll">
      <header class="cover">
        <div class="cover-actions">
          <button v-if="isAuthenticated" class="ghost" @click="onLogout">退出登录</button>
          <button v-else class="ghost primary" @click="onLogin">登录 / 注册</button>
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
        <span class="on">作品 {{ myVideos.length }}</span>
        <span>喜欢</span>
        <span>收藏</span>
      </div>

      <div v-if="myVideos.length" class="grid">
        <div
          v-for="v in myVideos"
          :key="v.id"
          class="cell"
          @click="play(v)"
        >
          <img :src="v.coverUrl" alt="" />
          <span class="like-tag">
            ♥ {{ v.likeCount }}
          </span>
        </div>
      </div>
      <div v-else class="empty">还没有发布作品，去发一个吧~</div>
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
        <button class="close" @click.stop="current = null">‹</button>
        <button class="del" @click.stop="remove">删除作品</button>
        <div class="p-title">{{ current.title }}</div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { VideoItem } from '../api/types'
import { useVideoStore } from '../stores/video'
import { useDisplayUser } from '../composables/useDisplayUser'

const router = useRouter()
const videoStore = useVideoStore()
const { displayUser, isAuthenticated, authStore } = useDisplayUser()
const { myVideos } = storeToRefs(videoStore)

const current = ref<VideoItem | null>(null)

function play(v: VideoItem) {
  current.value = v
}

async function remove() {
  if (!current.value) return
  try {
    await ElMessageBox.confirm('确定删除该作品？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    videoStore.deleteVideo(current.value.id)
    current.value = null
    ElMessage.success('已删除')
  } catch {
    /* 取消 */
  }
}

function onLogout() {
  authStore.logout()
  ElMessage('已退出登录')
  router.push('/')
}

function onLogin() {
  // 跳到 auth-backend 走 OAuth2 PKCE
  void authStore.login()
}
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
