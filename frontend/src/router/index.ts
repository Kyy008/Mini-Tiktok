import { createRouter, createWebHistory } from 'vue-router'
import RecommendView from '../views/RecommendView.vue'
import OAuthCallbackView from '../views/OAuthCallbackView.vue'
import UploadView from '../views/UploadView.vue'
import MyVideosView from '../views/MyVideosView.vue'
import AuthLoginView from '../views/AuthLoginView.vue'
import AuthRegisterView from '../views/AuthRegisterView.vue'
import { getAccessToken } from '../utils/token'

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
      path: '/login',
      name: 'login',
      component: AuthLoginView,
      meta: { hideTab: true },
    },
    {
      path: '/register',
      name: 'register',
      component: AuthRegisterView,
      meta: { hideTab: true },
    },
    {
      path: '/upload',
      name: 'upload',
      component: UploadView,
      meta: { requiresAuth: true },
    },
    {
      path: '/my/videos',
      name: 'my-videos',
      component: MyVideosView,
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getAccessToken()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
})

export default router
