<template>
  <div class="auth-page">
    <header class="top">
      <button class="back" type="button" aria-label="返回" @click="goBack">‹</button>
      <span>Mini-Tiktok</span>
    </header>

    <main class="content">
      <p class="eyebrow">注册账号</p>
      <h1>创建你的账号</h1>
      <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

      <button class="primary" type="button" :disabled="loading" @click="submit">
        {{ loading ? '跳转中...' : '前往授权服务注册' }}
      </button>

      <div class="switch">
        <span>已有账号？</span>
        <button type="button" @click="goLogin">去登录</button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { loading } = storeToRefs(authStore)

const errorMessage = ref('')

const redirectPath = computed(() => firstQueryValue(route.query.redirect))

async function submit() {
  if (loading.value) return
  errorMessage.value = ''
  try {
    await authStore.submitRegister({})
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败'
  }
}

function goLogin() {
  router.push({
    path: '/login',
    query: redirectPath.value ? { redirect: redirectPath.value } : undefined,
  })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/')
}

function firstQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return String(value[0] ?? '')
  }
  return typeof value === 'string' ? value : ''
}
</script>

<style scoped>
.auth-page {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 80% 4%, rgba(37, 244, 238, 0.28), transparent 26%),
    radial-gradient(circle at 10% 92%, rgba(254, 44, 85, 0.26), transparent 28%),
    #111;
  color: #fff;
}

.top {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 700;
}

.back {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  font-size: 28px;
  line-height: 1;
}

.content {
  padding: 42px 28px 0;
}

.eyebrow {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 13px;
  font-weight: 700;
}

h1 {
  margin: 0 0 24px;
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: 0;
}

.notice {
  margin: 0 0 14px;
  padding: 11px 12px;
  border-radius: 8px;
  background: rgba(37, 244, 238, 0.14);
  color: #aafaf7;
  font-size: 13px;
}

.notice.error {
  background: rgba(254, 44, 85, 0.13);
  color: #ffb3c0;
}

.primary {
  width: 100%;
  height: 48px;
  margin-top: 8px;
  border-radius: 24px;
  background: #fe2c55;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.primary:disabled {
  background: #3a2a30;
  color: rgba(255, 255, 255, 0.42);
}

.switch {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 22px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.switch button {
  color: #25f4ee;
  font-weight: 700;
}
</style>
