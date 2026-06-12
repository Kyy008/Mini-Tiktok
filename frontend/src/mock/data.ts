// Mock 假数据。后端联调时整体替换 src/mock/api.ts 即可，本文件可删。
import type { CommentItem, UserInfo, VideoItem } from '../api/types'

// 公共测试视频（Google 公开示例桶）。可随时替换为真实地址。
const SAMPLE_VIDEOS = [
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4',
]

export const CURRENT_USER: UserInfo = {
  id: 1,
  username: '我自己',
  avatar: 'https://picsum.photos/seed/me/120/120',
  signature: '这个人很懒，什么都没写~',
}

const AUTHORS: UserInfo[] = [
  { id: 11, username: '旅行日记', avatar: 'https://picsum.photos/seed/a11/120/120' },
  { id: 12, username: '美食研究所', avatar: 'https://picsum.photos/seed/a12/120/120' },
  { id: 13, username: '萌宠星球', avatar: 'https://picsum.photos/seed/a13/120/120' },
  { id: 14, username: '科技前沿', avatar: 'https://picsum.photos/seed/a14/120/120' },
  { id: 15, username: '音乐现场', avatar: 'https://picsum.photos/seed/a15/120/120' },
]

const TITLES = [
  '夏天的风吹过整片麦田 🌾 #风景 #治愈',
  '三分钟教你做出餐厅级的意面 🍝',
  '我家猫主子今天又拆家了……😹',
  '2026 最值得期待的几款新机盘点',
  '现场版太炸了！全场大合唱 🎤',
  '一个人也要好好生活 ☀️',
]

const MUSIC = [
  '@原声 - 旅行日记',
  '轻松的下午 - 纯音乐',
  '可爱的喵 - 萌宠 BGM',
  '科技感电子乐 - DJ Mix',
  '现场版 Live - 音乐现场',
  '夏日清晨 - 民谣',
]

function makeVideo(i: number): VideoItem {
  const author = AUTHORS[i % AUTHORS.length]
  return {
    id: i + 1,
    title: TITLES[i % TITLES.length],
    playUrl: SAMPLE_VIDEOS[i % SAMPLE_VIDEOS.length],
    coverUrl: `https://picsum.photos/seed/cover${i}/375/680`,
    author,
    likeCount: 1200 + i * 137,
    commentCount: 80 + i * 11,
    favoriteCount: 300 + i * 23,
    shareCount: 60 + i * 7,
    liked: false,
    music: MUSIC[i % MUSIC.length],
    createdAt: `2026-05-${String((i % 28) + 1).padStart(2, '0')}`,
  }
}

export const MOCK_FEED: VideoItem[] = Array.from({ length: 12 }, (_, i) => makeVideo(i))

// 我发布的视频
export const MOCK_MY_VIDEOS: VideoItem[] = Array.from({ length: 9 }, (_, i) => ({
  ...makeVideo(i + 100),
  author: CURRENT_USER,
  title: ['我的第一支视频 🎬', '随手拍的日落 🌇', '周末爬山 vlog ⛰️', '深夜放毒 🍜'][i % 4],
}))

export function mockComments(videoId: number): CommentItem[] {
  return Array.from({ length: 8 }, (_, i) => ({
    id: videoId * 100 + i,
    videoId,
    user: AUTHORS[i % AUTHORS.length],
    content: [
      '哈哈哈太好笑了',
      '这是在哪里拍的呀？求地址',
      '前排支持一下博主 👍',
      '已三连，求更新！',
      '太治愈了，单曲循环中',
      '学到了，感谢分享',
      '画质好顶呀',
      '第一次抢到沙发 🛋️',
    ][i],
    createdAt: `${i + 1} 小时前`,
  }))
}
