<template>
  <PageHeader />
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

import PageHeader from './components/PageHeader.vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()

onMounted(() => {
  void authStore.restore()
})
</script>
