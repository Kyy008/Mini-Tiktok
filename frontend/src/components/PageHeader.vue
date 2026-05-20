<template>
  <header class="page-header">
    <router-link class="brand" to="/">Mini-Tiktok</router-link>
    <nav class="nav-links">
      <router-link to="/">推荐视频</router-link>
      <router-link to="/upload">发布视频</router-link>
      <router-link to="/my/videos">我的视频</router-link>
    </nav>
    <div class="auth-actions">
      <span v-if="isAuthenticated" class="username">{{ user?.username || user?.userId }}</span>
      <button v-if="isAuthenticated" type="button" @click="logout">退出</button>
      <template v-else>
        <a :href="registerUrl">注册</a>
        <button type="button" :disabled="loading" @click="login">登录</button>
      </template>
    </div>
  </header>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'

import { buildLogoutUrl, buildRegisterUrl } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const { isAuthenticated, loading, user } = storeToRefs(authStore)
const registerUrl = buildRegisterUrl()

function login(): void {
  void authStore.login()
}

function logout(): void {
  authStore.logout()
  window.location.assign(buildLogoutUrl())
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.brand {
  font-size: 18px;
  font-weight: 700;
}

.nav-links {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.auth-actions button {
  min-width: 56px;
  height: 32px;
  border: 1px solid #1677ff;
  border-radius: 6px;
  color: #ffffff;
  background: #1677ff;
  cursor: pointer;
}

.auth-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.username {
  max-width: 160px;
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.router-link-active {
  color: #1677ff;
}
</style>
