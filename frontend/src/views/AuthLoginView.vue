<template>
  <div class="auth-page">
    <header class="top">
      <button class="back" type="button" aria-label="返回" @click="goBack">‹</button>
      <span>Mini-Tiktok</span>
    </header>

    <main class="content">
      <p class="eyebrow">账号登录</p>
      <h1>欢迎回来</h1>
      <p v-if="registered" class="notice">注册成功，请登录</p>
      <p v-if="errorMessage" class="notice error">{{ errorMessage }}</p>

      <form class="form" @submit.prevent="submit">
        <label for="username">用户名</label>
        <input
          id="username"
          v-model.trim="username"
          autocomplete="username"
          maxlength="32"
          placeholder="请输入用户名"
          required
          type="text"
        />

        <label for="password">密码</label>
        <input
          id="password"
          v-model="password"
          autocomplete="current-password"
          placeholder="请输入密码"
          required
          type="password"
        />

        <button class="primary" type="submit" :disabled="!canSubmit || loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="switch">
        <span>还没有账号？</span>
        <button type="button" @click="router.push('/register')">去注册</button>
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

const username = ref(String(route.query.username ?? ''))
const password = ref('')
const errorMessage = ref('')

const registered = computed(() => route.query.registered === '1')
const canSubmit = computed(() => username.value.length > 0 && password.value.length > 0)

async function submit() {
  if (!canSubmit.value || loading.value) return
  errorMessage.value = ''
  try {
    await authStore.submitLogin({
      username: username.value,
      password: password.value,
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/')
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

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

label {
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  font-weight: 700;
}

input {
  width: 100%;
  height: 48px;
  margin-bottom: 8px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  outline: none;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  font: inherit;
}

input::placeholder {
  color: rgba(255, 255, 255, 0.36);
}

input:focus {
  border-color: rgba(37, 244, 238, 0.7);
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
