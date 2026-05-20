<template>
  <main class="callback-page">
    <section class="auth-panel">
      <h1>登录回调处理</h1>
      <p v-if="!errorMessage">{{ statusMessage }}</p>
      <p v-else class="error-message">{{ errorMessage }}</p>
      <button v-if="errorMessage" type="button" @click="startLogin">重新登录</button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const statusMessage = ref('正在完成登录...')
const errorMessage = ref('')

onMounted(async () => {
  try {
    const providerError = firstQueryValue(route.query.error)
    if (providerError) {
      throw new Error(providerError)
    }

    const code = firstQueryValue(route.query.code)
    const state = firstQueryValue(route.query.state)
    if (!code || !state) {
      throw new Error('缺少授权回调参数')
    }

    await authStore.handleCallback(code, state)
    statusMessage.value = '登录成功，正在返回首页...'
    await router.replace('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  }
})

function firstQueryValue(value: unknown): string {
  if (Array.isArray(value)) {
    return String(value[0] ?? '')
  }
  return typeof value === 'string' ? value : ''
}

function startLogin(): void {
  void authStore.login()
}
</script>

<style scoped>
.callback-page {
  display: grid;
  min-height: calc(100vh - 120px);
  place-items: center;
}

.auth-panel {
  width: min(420px, 100%);
  padding: 28px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.error-message {
  color: #b42318;
}

button {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #1677ff;
  border-radius: 6px;
  color: #ffffff;
  background: #1677ff;
  cursor: pointer;
}
</style>
