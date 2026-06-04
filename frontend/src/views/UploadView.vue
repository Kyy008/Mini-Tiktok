<template>
  <div class="upload">
    <header class="bar">
      <button class="back" type="button" @click="$router.back()">‹</button>
      <span class="t">发布作品</span>
      <span style="width: 24px" />
    </header>

    <div class="body">
      <label class="picker" :class="{ filled: !!file }">
        <input
          ref="fileInput"
          type="file"
          accept="video/mp4"
          hidden
          @change="onPick"
        />
        <template v-if="!file">
          <div class="plus">+</div>
          <div class="hint">选择 MP4 视频</div>
        </template>
        <video v-else class="preview" :src="previewUrl" muted playsinline />
      </label>

      <div class="field">
        <textarea
          v-model="title"
          maxlength="55"
          rows="3"
          placeholder="添加作品描述..."
        />
        <span class="count">{{ title.length }}/55</span>
      </div>

      <div v-if="uploadProgress" class="progress">
        <div class="progress-top">
          <span>{{ progressLabel }}</span>
          <b>{{ uploadProgress.percent }}%</b>
        </div>
        <div class="progress-track">
          <div class="progress-bar" :style="{ width: `${uploadProgress.percent}%` }" />
        </div>
        <div class="progress-meta">
          分片 {{ uploadProgress.currentChunk }}/{{ uploadProgress.totalChunks }}
        </div>
      </div>

      <button class="submit" :disabled="!canSubmit" @click="submit">
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useVideoStore } from '../stores/video'

const router = useRouter()
const videoStore = useVideoStore()
const { uploadProgress } = storeToRefs(videoStore)

const file = ref<File | null>(null)
const previewUrl = ref('')
const title = ref('')
const submitting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const canSubmit = computed(
  () => !!file.value && file.value.size > 0 && title.value.trim().length > 0 && !submitting.value,
)
const progressLabel = computed(() =>
  uploadProgress.value?.status === 'COMPLETED' ? '合并完成' : '正在上传',
)

function onPick(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  if (f.type !== 'video/mp4') {
    ElMessage.error('仅支持 MP4 格式视频')
    resetFileInput()
    return
  }
  if (f.size <= 0) {
    ElMessage.error('视频文件不能为空')
    resetFileInput()
    return
  }
  revokePreviewUrl()
  file.value = f
  previewUrl.value = URL.createObjectURL(f)
}

async function submit() {
  if (!canSubmit.value || !file.value) return
  submitting.value = true
  try {
    await videoStore.publish(title.value.trim(), file.value)
    ElMessage.success('发布成功')
    resetForm()
    router.push('/my/videos')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  revokePreviewUrl()
  file.value = null
  title.value = ''
  resetFileInput()
}

function resetFileInput() {
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function revokePreviewUrl() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

onBeforeUnmount(revokePreviewUrl)
</script>

<style scoped>
.upload {
  position: absolute;
  inset: 0;
  background: #161616;
  display: flex;
  flex-direction: column;
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.bar .back {
  font-size: 26px;
  line-height: 1;
  color: #fff;
}

.bar .t {
  font-size: 17px;
  font-weight: 600;
}

.body {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.picker {
  width: 130px;
  height: 180px;
  border-radius: 10px;
  background: #242424;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  overflow: hidden;
}

.picker .plus {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.5);
}

.picker .hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.field {
  position: relative;
  background: #242424;
  border-radius: 10px;
  padding: 12px;
}

.field textarea {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #fff;
  font-size: 15px;
  resize: none;
  font-family: inherit;
}

.field .count {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: #242424;
}

.progress-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
}

.progress-track {
  height: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: inherit;
  background: #fe2c55;
  transition: width 0.18s ease;
}

.progress-meta {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

.submit {
  margin-top: auto;
  height: 46px;
  border-radius: 23px;
  background: #fe2c55;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.submit:disabled {
  background: #3a2a30;
  color: rgba(255, 255, 255, 0.4);
}
</style>
