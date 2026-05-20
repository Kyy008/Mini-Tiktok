<template>
  <div class="phone">
    <div class="screen">
      <div class="screen-body">
        <router-view v-slot="{ Component }">
          <keep-alive include="RecommendView">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
      <TabBar v-if="!route.meta.hideTab" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const authStore = useAuthStore()

// 应用启动时尝试用 sessionStorage 中的 token 恢复登录态
onMounted(() => {
  void authStore.restore()
})
</script>
