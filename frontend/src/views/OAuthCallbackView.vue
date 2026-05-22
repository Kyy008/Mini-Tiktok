<template>
  <div class="callback">
    <div v-if="!errorMessage" class="spinner" />
    <p :class="{ error: errorMessage }">{{ errorMessage || statusMessage }}</p>
    <button v-if="errorMessage" type="button" @click="startLogin">重新登录</button>
  </div>
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
.callback {
  position: absolute;
  inset: 0;
  background: #000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 24px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  text-align: center;
}

.spinner {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #fe2c55;
  animation: spin 0.8s linear infinite;
}

.error {
  color: #ff8fa3;
}

button {
  height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  color: #ffffff;
  background: #fe2c55;
  cursor: pointer;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
