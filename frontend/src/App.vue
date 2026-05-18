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
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'

const route = useRoute()
</script>
