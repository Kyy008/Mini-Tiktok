// 把 OAuth2 真用户（仅含 userId/username/scopes）和 mock 的头像/签名
// 合成抖音 UI 期望的 UserInfo。真接口提供完整资料后，把 mock fallback 去掉即可。
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../stores/auth'
import type { UserInfo } from '../api/types'
import { CURRENT_USER } from '../mock/data'

export function useDisplayUser() {
  const authStore = useAuthStore()
  const { user, accessToken } = storeToRefs(authStore)

  const displayUser = computed<UserInfo>(() => {
    if (!user.value) return CURRENT_USER
    return {
      id: Number(user.value.userId) || CURRENT_USER.id,
      username: user.value.username || CURRENT_USER.username,
      avatar: CURRENT_USER.avatar,
      signature: CURRENT_USER.signature,
    }
  })

  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value))

  return { displayUser, isAuthenticated, authStore }
}
