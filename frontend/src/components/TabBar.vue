<template>
  <nav class="tab-bar">
    <button
      v-for="t in tabs"
      :key="t.key"
      class="tab"
      :class="{ active: isActive(t) }"
      @click="onTap(t)"
    >
      <span v-if="t.key === 'plus'" class="plus">
        <svg viewBox="0 0 24 24" width="22" height="22">
          <path d="M12 5v14M5 12h14" stroke="#000" stroke-width="3" stroke-linecap="round" />
        </svg>
      </span>
      <span v-else class="label">{{ t.label }}</span>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

interface Tab {
  key: string
  label: string
  to?: string
}

const tabs: Tab[] = [
  { key: 'home', label: '首页', to: '/' },
  { key: 'friends', label: '朋友' },
  { key: 'plus', label: '' },
  { key: 'msg', label: '消息' },
  { key: 'me', label: '我', to: '/my/videos' },
]

const route = useRoute()
const router = useRouter()

function isActive(t: Tab) {
  if (t.key === 'plus') return route.path === '/upload'
  return t.to !== undefined && route.path === t.to
}

function onTap(t: Tab) {
  if (t.key === 'plus') return router.push('/upload')
  if (t.to) return router.push(t.to)
  ElMessage({ message: '该功能敬请期待', grouping: true })
}
</script>

<style scoped>
.tab-bar {
  display: flex;
  align-items: center;
  height: 52px;
  background: #000;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.tab {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.tab.active .label {
  color: #fff;
  font-weight: 700;
}

.plus {
  width: 44px;
  height: 30px;
  border-radius: 9px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 4px 0 0 -1px #25f4ee, -4px 0 0 -1px #fe2c55;
}
</style>
