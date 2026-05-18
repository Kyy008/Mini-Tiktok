import { createRouter, createWebHistory } from 'vue-router'
import RecommendView from '../views/RecommendView.vue'
import OAuthCallbackView from '../views/OAuthCallbackView.vue'
import UploadView from '../views/UploadView.vue'
import MyVideosView from '../views/MyVideosView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'recommend',
      component: RecommendView,
    },
    {
      path: '/oauth/callback',
      name: 'oauth-callback',
      component: OAuthCallbackView,
      meta: { hideTab: true },
    },
    {
      path: '/upload',
      name: 'upload',
      component: UploadView,
    },
    {
      path: '/my/videos',
      name: 'my-videos',
      component: MyVideosView,
    },
  ],
})

export default router
