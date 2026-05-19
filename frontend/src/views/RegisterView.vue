<template>
  <main class="register-page">
    <section class="auth-panel">
      <h1>注册账号</h1>
      <form class="register-form" @submit.prevent="submit">
        <label>
          用户名
          <input
            v-model.trim="form.username"
            autocomplete="username"
            maxlength="32"
            minlength="3"
            required
            type="text"
          >
        </label>
        <label>
          密码
          <input
            v-model="form.password"
            autocomplete="new-password"
            maxlength="64"
            minlength="6"
            required
            type="password"
          >
        </label>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <div class="form-actions">
          <button type="submit" :disabled="submitting">注册</button>
          <button type="button" :disabled="submitting" @click="startLogin">去登录</button>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

import type { RegisterCredentials } from '../api/types'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const form = reactive<RegisterCredentials>({
  username: '',
  password: '',
})
const submitting = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

async function submit(): Promise<void> {
  submitting.value = true
  successMessage.value = ''
  errorMessage.value = ''
  try {
    const user = await authStore.register({
      username: form.username,
      password: form.password,
    })
    successMessage.value = `${user.username} 注册成功，可以登录了`
    form.password = ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败'
  } finally {
    submitting.value = false
  }
}

function startLogin(): void {
  void authStore.login()
}
</script>

<style scoped>
.register-page {
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

.register-form {
  display: grid;
  gap: 18px;
}

label {
  display: grid;
  gap: 8px;
  color: #374151;
  font-size: 14px;
}

input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.form-actions {
  display: flex;
  gap: 12px;
}

button {
  min-width: 80px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #1677ff;
  border-radius: 6px;
  color: #ffffff;
  background: #1677ff;
  cursor: pointer;
}

button[type="button"] {
  color: #1677ff;
  background: #ffffff;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.success-message {
  margin: 0;
  color: #087443;
}

.error-message {
  margin: 0;
  color: #b42318;
}
</style>
