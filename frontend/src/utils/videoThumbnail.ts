const DEFAULT_MAX_WIDTH = 360
const DEFAULT_MAX_HEIGHT = 640

export async function captureVideoFirstFrameDataUrl(
  src: string,
  options: { maxWidth?: number; maxHeight?: number } = {},
): Promise<string> {
  if (typeof document === 'undefined') {
    throw new Error('当前环境不支持生成视频缩略图')
  }

  const video = document.createElement('video')
  video.crossOrigin = 'anonymous'
  video.preload = 'auto'
  video.muted = true
  video.playsInline = true
  video.src = src

  await waitForFirstFrame(video)

  const videoWidth = video.videoWidth
  const videoHeight = video.videoHeight
  if (!videoWidth || !videoHeight) {
    throw new Error('无法读取视频尺寸')
  }

  const { width, height } = fitWithinBounds(
    videoWidth,
    videoHeight,
    options.maxWidth ?? DEFAULT_MAX_WIDTH,
    options.maxHeight ?? DEFAULT_MAX_HEIGHT,
  )

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height

  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('无法创建缩略图画布')
  }

  context.drawImage(video, 0, 0, width, height)
  return canvas.toDataURL('image/jpeg', 0.82)
}

function waitForFirstFrame(video: HTMLVideoElement): Promise<void> {
  return new Promise((resolve, reject) => {
    if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
      resolve()
      return
    }

    const onLoadedData = () => {
      cleanup()
      resolve()
    }
    const onError = () => {
      cleanup()
      reject(new Error('视频首帧加载失败'))
    }
    const cleanup = () => {
      video.removeEventListener('loadeddata', onLoadedData)
      video.removeEventListener('error', onError)
    }

    video.addEventListener('loadeddata', onLoadedData, { once: true })
    video.addEventListener('error', onError, { once: true })
    video.load()
  })
}

function fitWithinBounds(
  width: number,
  height: number,
  maxWidth: number,
  maxHeight: number,
): { width: number; height: number } {
  const scale = Math.min(maxWidth / width, maxHeight / height, 1)
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  }
}
