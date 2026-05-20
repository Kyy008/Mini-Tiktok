<template>
  <div class="callback">
    <template v-if="!errorMessage">
      <div class="spinner" />
      <p>{{ statusMessage }}</p>
    </template>
    <template v-else>
      <p class="error">{{ errorMessage }}</p>
      <button class="retry" type="button" @click="startLogin">重新登录</button>
    </template>
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
  color: rgba(255, 255, 255, 0.75);
  font-size: 14px;
  padding: 0 32px;
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
  color: #ff7a85;
  line-height: 1.5;
}

.retry {
  height: 38px;
  padding: 0 22px;
  border-radius: 19px;
  background: #fe2c55;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
