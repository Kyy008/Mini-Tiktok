<template>
  <div class="phone">
    <div class="screen" :class="{ 'console-open': showGlobalConsole }">
      <div class="app-stage">
        <div class="screen-body">
          <router-view v-slot="{ Component }">
            <keep-alive include="RecommendView">
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </div>
        <TabBar v-if="!route.meta.hideTab" />
      </div>
      <RequestLogConsole v-if="showGlobalConsole" class="global-log-console" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import RequestLogConsole from './components/RequestLogConsole.vue'
import TabBar from './components/TabBar.vue'
import { useAuthStore } from './stores/auth'
import { useRequestLogStore } from './stores/requestLog'

const route = useRoute()
const authStore = useAuthStore()
const requestLogStore = useRequestLogStore()
const showGlobalConsole = computed(() => requestLogStore.consoleOpen && !route.meta.hideTab)

// 应用启动时尝试用 sessionStorage 中的 token 恢复登录态
onMounted(() => {
  void authStore.restore()
})
</script>

<style scoped>
.app-stage {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #000;
  transform-origin: top center;
  transition:
    flex-basis 0.28s ease,
    transform 0.28s ease,
    border-radius 0.28s ease;
}

.console-open .app-stage {
  flex: 0 0 75%;
  transform: translateY(-8px) scale(0.94);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 18px 32px rgba(0, 0, 0, 0.38);
}

.global-log-console {
  flex: 0 0 25%;
  min-height: 0;
}
</style>
