import { defineStore } from 'pinia'
import { ref } from 'vue'

import { clearRequestLogs, getRequestLogs } from '../api/requestLog'
import type { RequestLogItem } from '../api/types'

export const useRequestLogStore = defineStore('requestLog', () => {
  const consoleOpen = ref(false)
  const logs = ref<RequestLogItem[]>([])
  const loading = ref(false)
  const clearing = ref(false)
  const errorMessage = ref('')

  async function openConsole(): Promise<void> {
    consoleOpen.value = true
    try {
      await loadLogs()
    } catch {
      // 让控制台组件展示错误态即可。
    }
  }

  function closeConsole(): void {
    consoleOpen.value = false
  }

  async function toggleConsole(): Promise<void> {
    if (consoleOpen.value) {
      closeConsole()
      return
    }
    await openConsole()
  }

  async function loadLogs(limit = 50): Promise<RequestLogItem[]> {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await getRequestLogs(limit)
      logs.value = result
      return result
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '日志加载失败'
      throw error
    } finally {
      loading.value = false
    }
  }

  async function clearLogs(): Promise<void> {
    clearing.value = true
    errorMessage.value = ''
    try {
      await clearRequestLogs()
      logs.value = []
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '日志清空失败'
      throw error
    } finally {
      clearing.value = false
    }
  }

  return {
    consoleOpen,
    logs,
    loading,
    clearing,
    errorMessage,
    openConsole,
    closeConsole,
    toggleConsole,
    loadLogs,
    clearLogs,
  }
})
