<template>
  <div class="callback">
    <div class="spinner" />
    <p>登录中，请稍候...</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  // Mock：真实流程需用 code 换取 token。此处直接置为已登录后回首页。
  authStore.login()
  setTimeout(() => router.replace('/'), 800)
})
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
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.spinner {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #fe2c55;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
