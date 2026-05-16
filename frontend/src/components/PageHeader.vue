<template>
  <header class="page-header">
    <router-link class="brand" to="/">Mini-Tiktok</router-link>
    <nav class="nav-links">
      <router-link to="/">推荐视频</router-link>
      <router-link to="/upload">发布视频</router-link>
      <router-link to="/my/videos">我的视频</router-link>
    </nav>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
}

.brand {
  font-size: 18px;
  font-weight: 700;
}

.nav-links {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.router-link-active {
  color: #1677ff;
}
</style>
