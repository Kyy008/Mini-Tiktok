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

onMounted(() => {
  void authStore.restore()
})
</script>
