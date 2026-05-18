<template>
  <div class="upload">
    <header class="bar">
      <button class="back" @click="$router.back()">‹</button>
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

      <button class="submit" :disabled="!canSubmit" @click="submit">
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useVideoStore } from '../stores/video'

const router = useRouter()
const videoStore = useVideoStore()

const file = ref<File | null>(null)
const previewUrl = ref('')
const title = ref('')
const submitting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const canSubmit = computed(
  () => !!file.value && title.value.trim().length > 0 && !submitting.value,
)

function onPick(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  if (f.type !== 'video/mp4') {
    ElMessage.error('仅支持 MP4 格式视频')
    return
  }
  file.value = f
  previewUrl.value = URL.createObjectURL(f)
}

function submit() {
  if (!canSubmit.value || !file.value) return
  submitting.value = true
  setTimeout(() => {
    videoStore.publish(title.value.trim(), file.value!)
    submitting.value = false
    ElMessage.success('发布成功')
    router.push('/my/videos')
  }, 600)
}
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
