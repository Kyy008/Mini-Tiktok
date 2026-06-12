<template>
  <section class="log-console" aria-label="日志控制台">
    <header class="console-head">
      <div class="console-title">
        <strong>日志控制台</strong>
        <span>{{ logs.length }} 条记录</span>
      </div>
      <div class="console-actions">
        <button type="button" :disabled="loading || clearing" @click="refresh">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
        <button type="button" :disabled="loading || clearing" @click="clear">
          {{ clearing ? '清空中...' : '清空日志' }}
        </button>
        <button type="button" @click="requestLogStore.closeConsole()">关闭</button>
      </div>
    </header>

    <div v-if="errorMessage" class="console-state error">
      {{ errorMessage }}
    </div>
    <div v-else-if="loading && !logs.length" class="console-state">
      正在读取 request_log...
    </div>
    <div v-else-if="!logs.length" class="console-state">
      暂无日志，操作一下首页试试
    </div>

    <div v-else class="log-list">
      <details
        v-for="log in logs"
        :key="log.id"
        class="log-row"
        :class="statusClass(log.statusCode)"
      >
        <summary>
          <span class="time">{{ formatTime(log.createdAt) }}</span>
          <span class="method">{{ log.method }}</span>
          <span class="path">{{ log.path }}</span>
          <span class="status">{{ log.statusCode }}</span>
          <span class="duration">{{ log.durationMs }}ms</span>
        </summary>
        <div class="log-detail">
          <div><b>user</b><code>{{ log.userId || 'ANONYMOUS' }}</code></div>
          <div><b>ip</b><code>{{ log.ip || '-' }}</code></div>
          <div><b>input</b><pre>{{ log.requestBody || '-' }}</pre></div>
          <div><b>output</b><pre>{{ log.responseBody || '-' }}</pre></div>
        </div>
      </details>
    </div>
  </section>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRequestLogStore } from '../stores/requestLog'

const requestLogStore = useRequestLogStore()
const { logs, loading, clearing, errorMessage } = storeToRefs(requestLogStore)

function refresh() {
  void requestLogStore.loadLogs()
}

function clear() {
  void requestLogStore.clearLogs()
}

function formatTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function statusClass(statusCode: number): string {
  if (statusCode >= 500) return 'server-error'
  if (statusCode >= 400) return 'client-error'
  return 'ok'
}
</script>

<style scoped>
.log-console {
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #090b0f;
  border-top: 1px solid rgba(37, 244, 238, 0.28);
  color: #d7ffe9;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.console-head {
  height: 38px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background: #111722;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.console-title {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.console-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6px;
}

.console-head strong {
  color: #25f4ee;
  font-size: 13px;
}

.console-head span {
  color: rgba(215, 255, 233, 0.55);
  font-size: 11px;
}

.console-actions button {
  height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  background: rgba(37, 244, 238, 0.16);
  color: #25f4ee;
  font-size: 11px;
}

.console-actions button:disabled {
  opacity: 0.55;
}

.console-state {
  flex: 1;
  display: grid;
  place-items: center;
  color: rgba(215, 255, 233, 0.55);
  font-size: 12px;
  text-align: center;
  padding: 0 16px;
}

.console-state.error {
  color: #ff8c9f;
}

.log-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 10px 12px;
}

.log-row {
  border-left: 2px solid rgba(215, 255, 233, 0.26);
  padding: 4px 0 4px 8px;
}

.log-row + .log-row {
  margin-top: 4px;
}

.log-row.ok {
  border-left-color: #42d392;
}

.log-row.client-error {
  border-left-color: #ffcf5c;
}

.log-row.server-error {
  border-left-color: #ff5c7a;
}

.log-row summary {
  min-width: 0;
  display: grid;
  grid-template-columns: 58px 46px minmax(0, 1fr) 38px 48px;
  gap: 6px;
  align-items: center;
  cursor: pointer;
  list-style: none;
  font-size: 11px;
}

.log-row summary::-webkit-details-marker {
  display: none;
}

.time {
  color: rgba(215, 255, 233, 0.58);
}

.method {
  color: #25f4ee;
  font-weight: 700;
}

.path {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #fff;
}

.status,
.duration {
  text-align: right;
  color: rgba(215, 255, 233, 0.76);
}

.log-detail {
  display: grid;
  gap: 6px;
  margin-top: 8px;
  padding: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(215, 255, 233, 0.8);
  font-size: 11px;
}

.log-detail div {
  min-width: 0;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
}

.log-detail b {
  color: #25f4ee;
  font-weight: 700;
}

.log-detail code,
.log-detail pre {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  color: rgba(255, 255, 255, 0.86);
  font-family: inherit;
}
</style>
