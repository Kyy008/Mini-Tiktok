# Mini-Tiktok 前端方案

## 1. 目标定位

`frontend` 是 Mini-Tiktok 项目的前端应用，负责完成用户侧的功能演示和接口联调。

界面形态：**按抖音 App 1:1 复刻的移动端竖屏 UI**。桌面浏览器中以 375×812 的手机外壳居中呈现；窗口宽度小于 420px 时自动全屏铺满。

前端主要能力：

- 通过 OAuth2 Authorization Code + PKCE 接入 `auth-backend`，登录/注册页面由 `auth-backend` 的 Thymeleaf 模板承担。
- 获取并保存 access token，刷新页面后通过 `sessionStorage` 自动恢复登录态。
- 请求 `api-backend` 时自动携带 Bearer Token；401 时清空登录态。
- 推荐视频流：全屏沉浸式、上下滑动切换（scroll-snap + IntersectionObserver 自动播放/暂停）、点击播放/暂停、双击爱心、右侧悬浮操作栏（关注/点赞/评论/收藏/分享 + 旋转唱片）。
- 评论：底部抽屉 bottom sheet。
- 发布视频（MP4 校验 + 标题）。
- 个人主页：资料卡 + 关注/粉丝/获赞统计 + 作品三宫格 + 全屏播放浮层 + 删除自己的作品。
- 底部 Tab 栏（首页 / 朋友 / + / 消息 / 我），「+」跳转发布页。

登录入口：在「我」页面（未登录时显示「登录 / 注册」按钮），点击后由前端生成 PKCE 参数并跳转授权服务器；注册入口由 `auth-backend` 的登录页提供链接到 `/register`。

> ⚠️ 当前阶段：登录已对接真实 `auth-backend`；推荐流 / 我的视频 / 评论等业务数据走 `src/mock/` 假数据。后端业务接口稳定后，把 `stores/video.ts` 内部实现替换为 axios 调用即可，UI 层不需改动。

本地端口约定：

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

开发环境端口选择：

- `5173`：Vite 前端开发服务器。
- `9000`：OAuth2 授权服务器。
- `8085`：普通业务后端 API。

## 2. 技术栈

### 2.1 基础技术

- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- Element Plus
- Web Crypto API
- 原生 `<video>` 标签

### 2.2 前端职责划分

```text
router/       路由配置
stores/       Pinia 状态管理
api/          后端接口封装
utils/        PKCE、token、格式化工具
views/        页面组件
components/   可复用功能组件
```

实际目录：

```text
frontend/
  src/
    api/
      auth.ts            # OAuth2 token 交换 + /api/me（真接口）
      video.ts           # 视频业务（待真接入，目前 store 直接走 mock）
      types.ts           # 业务类型 UserInfo/VideoItem/CommentItem + OAuth 类型 CurrentUser/...
    components/
      TabBar.vue         # 底部 Tab 栏（首页/朋友/+/消息/我）
      VideoCard.vue      # 全屏视频卡片（含浮层、双击爱心、自动播放）
      ActionRail.vue     # 右侧悬浮操作栏（关注/赞/评论/收藏/分享）
      CommentSheet.vue   # 评论底部抽屉
    composables/
      useDisplayUser.ts  # 把 OAuth CurrentUser 与 mock 头像/签名拼成 UI 所需的 UserInfo
    mock/
      data.ts            # mock 视频 / 评论 / 用户头像数据
    router/
      index.ts
    stores/
      auth.ts            # OAuth2 PKCE 登录态（真接入）
      video.ts           # 推荐流 / 我的视频（mock 实现）
    utils/
      http.ts            # axios 实例 + Bearer 注入 + 401 处理
      pkce.ts            # code_verifier / code_challenge / state 生成
      token.ts           # sessionStorage 存取 token / user / pkce
    views/
      RecommendView.vue  # 抖音式推荐流主页
      OAuthCallbackView.vue
      UploadView.vue
      MyVideosView.vue   # 个人主页 + 作品九宫格 + 全屏播放
    App.vue              # 手机外壳 + 路由出口 + TabBar
    main.ts
    style.css            # 暗色 + 居中手机外壳 + 窄屏全屏
```

> 旧方案中的 `components/PageHeader.vue / VideoPlayer.vue / VideoActions.vue` 已废弃：抖音 UI 不要顶部导航，由 `TabBar.vue` 替代；播放/操作组合到 `VideoCard.vue + ActionRail.vue`。

## 3. 环境配置

前端使用 Vite 环境变量配置后端地址和 OAuth2 client 信息。

```text
VITE_AUTH_BASE_URL=http://localhost:9000
VITE_API_BASE_URL=http://localhost:8085
VITE_CLIENT_ID=tiktok-web
VITE_REDIRECT_URI=http://localhost:5173/oauth/callback
VITE_SCOPE=video:read video:write video:like
```

OAuth2 固定参数：

```text
client_id: tiktok-web
redirect_uri: http://localhost:5173/oauth/callback
scope: video:read video:write video:like
response_type: code
code_challenge_method: S256
```

## 4. OAuth2 前端流程

前端使用 Authorization Code + PKCE。

### 4.1 登录流程

1. 用户点击登录按钮。
2. 前端生成随机 `code_verifier`。
3. 前端通过 Web Crypto API 计算 `code_challenge`。
4. 前端将 `code_verifier` 保存到 `sessionStorage`。
5. 前端跳转到 `auth-backend`：

```text
GET http://localhost:9000/oauth2/authorize
```

授权请求参数：

```text
response_type=code
client_id=tiktok-web
redirect_uri=http://localhost:5173/oauth/callback
scope=video:read video:write video:like
code_challenge=<code_challenge>
code_challenge_method=S256
state=<random_state>
```

6. 用户在 `auth-backend` 完成登录。
7. 授权服务器回调：

```text
http://localhost:5173/oauth/callback?code=xxx&state=xxx
```

### 4.2 回调处理流程

1. `OAuthCallbackView` 读取 URL 中的 `code` 和 `state`。
2. 校验 `state` 与 `sessionStorage` 中保存的值一致。
3. 读取 `code_verifier`。
4. 请求 `auth-backend` 的 token 端点：

```text
POST http://localhost:9000/oauth2/token
Content-Type: application/x-www-form-urlencoded
```

请求参数：

```text
grant_type=authorization_code
client_id=tiktok-web
redirect_uri=http://localhost:5173/oauth/callback
code=<authorization_code>
code_verifier=<code_verifier>
```

5. 保存返回的 `access_token`。
6. 清理 `code_verifier` 和 `state`。
7. 调用 `GET /api/me` 获取当前用户信息。
8. 跳转推荐视频页 `/`。

### 4.3 token 存储

前端将登录状态保存到 `sessionStorage`：

```text
mini_tiktok_access_token
mini_tiktok_user
```

页面刷新时：

- 从 `sessionStorage` 恢复 access token。
- 调用 `/api/me` 重新确认用户信息。

退出时：

- 清空 `sessionStorage` 中的 token 和用户信息。
- 跳转到首页。

## 5. 路由设计

### 5.1 推荐视频页

```text
/
```

页面组件：

```text
RecommendView.vue + VideoCard.vue + ActionRail.vue + CommentSheet.vue
```

功能（抖音风格沉浸式）：

- 顶部："直播 / 关注 / 推荐" 标签 + 右上角搜索图标（仅 UI）。
- 主体：竖向滚动容器 + `scroll-snap-type: y mandatory`；每张视频卡占满手机屏（去 TabBar 后的可视区）。
- 自动播放：IntersectionObserver 监听当前可见卡片（threshold 0.6），活跃的视频 `play()` 并 `currentTime=0`，其余 `pause()`。
- 单击：切换播放/暂停（中间出现大三角图标）。
- 双击：弹出大爱心，触发点赞（已赞则不再增加）。
- 右侧操作栏：头像 + 关注红 + 号、点赞数、评论数、收藏星、分享、底部旋转唱片。
- 底部信息：`@作者`、标题、🎵 音乐文字 marquee 滚动。
- 评论：点击评论图标弹出底部抽屉（CommentSheet）。
- 登录入口：在底部 Tab 栏的「我」页面提供登录按钮，不在推荐页打断浏览体验。

> 没有"上一个/下一个"按钮——以滑动手势/鼠标滚轮代替。

调用接口：

```text
GET  /api/me
GET  /api/videos/recommendations?size=10
GET  /api/videos/{id}/play
POST /api/videos/{id}/views
POST /api/videos/{id}/likes
DELETE /api/videos/{id}/likes
GET  /api/videos/{id}/likes
```

页面状态：

```text
recommendations       推荐视频列表
currentIndex          当前播放下标
currentVideo          当前视频
loading               加载状态
errorMessage          错误信息
emptyMessage          空状态信息
```

切换规则：

- 第一次进入页面时加载推荐列表（当前从 mock 加载）。
- 当前视频变化（IntersectionObserver 检测）后调用 `POST /api/videos/{id}/views`（联调后启用）。
- 用户滑动到列表末尾后重新拉取推荐列表并 append。
- 当前视频点赞后乐观更新本地状态，请求失败再回滚。

### 5.2 OAuth2 回调页

```text
/oauth/callback
```

页面组件：

```text
OAuthCallbackView.vue
```

功能：

- 读取授权服务器回调参数。
- 校验 `state`。
- 使用 authorization code 换 access token。
- 保存 token。
- 加载当前用户信息。
- 跳转推荐视频页。
- 展示回调处理中的加载状态。
- 展示回调失败信息。

调用接口：

```text
POST /oauth2/token       auth-backend
GET  /api/me             api-backend
```

### 5.3 发布视频页

```text
/upload
```

页面组件：

```text
UploadView.vue
```

功能：

- 选择视频文件。
- 输入视频标题。
- 校验标题不为空。
- 校验文件为 MP4。
- 提交 multipart 表单。
- 上传成功后清空表单。
- 上传成功后可跳转我的视频页。
- 上传失败时展示错误信息。

调用接口：

```text
POST /api/videos
```

表单字段：

```text
file
title
```

### 5.4 我的视频页

```text
/my/videos
```

页面组件：

```text
MyVideosView.vue
```

功能（抖音风格个人主页）：

- 顶部封面区：渐变背景 + 头像 + 昵称 + 抖音号 + 签名 + 关注/粉丝/获赞 三段统计。
- 顶部右上角：已登录显示"退出登录"；未登录显示"登录 / 注册"（点击触发 `authStore.login()` 走 PKCE）。
- 标签：作品 / 喜欢 / 收藏（当前仅作品可点）。
- 作品三宫格：每个 cell 9:14 比例 + 封面 + 左下角点赞数。
- 点击作品：弹出全屏播放浮层（自动播放 + 循环）+ 左上返回 + 右上删除（带二次确认）。
- 头像/签名当前来自 `useDisplayUser`：用 OAuth 用户名 + mock 头像；后端补完用户资料后切到真字段。

调用接口：

```text
GET    /api/my/videos?page=1&size=10
GET    /api/videos/{id}/play
DELETE /api/videos/{id}
```

分页状态：

```text
page
size
videos
loading
hasMore
```

## 6. 状态管理

### 6.1 authStore

字段：

```text
accessToken
userId
username
scopes
isAuthenticated
```

方法：

```text
login()
handleCallback(code, state)
loadMe()
logout()
restore()
setToken(token)
clear()
```

职责：

- 生成 OAuth2 授权跳转地址。
- 处理回调换 token。
- 保存和恢复登录状态。
- 调用 `/api/me`。
- 退出时清理本地状态。

### 6.2 videoStore（当前 mock 实现）

字段：

```text
feed       推荐列表
myVideos   我的作品
```

方法：

```text
toggleLike(id)
loadComments(id)
publish(title, file)
deleteVideo(id)
```

职责：

- 推荐流 / 我的作品的本地状态管理。
- 当前从 `src/mock/data.ts` 加载初始数据；接真后端时把方法体替换为 axios 调用，UI 不动。
- 点赞采用乐观更新（同一 video 在 feed 和 myVideos 两个列表里的实例同步翻转）。
- `publish` 使用 `URL.createObjectURL(file)` 让用户立即在「我」页看到刚发的本地视频，便于演示。

## 7. 接口封装

### 7.1 Axios 实例

`utils/http.ts` 负责创建 Axios 实例：

```text
baseURL = VITE_API_BASE_URL
```

请求拦截：

- 从 `authStore` 或 `sessionStorage` 获取 access token。
- 自动添加请求头：

```text
Authorization: Bearer <access_token>
```

响应拦截：

- 401：清空登录状态，提示重新登录。
- 403：提示权限不足。
- 其他错误：展示后端 `message`。

### 7.2 Auth API

`api/auth.ts`

```text
exchangeCodeForToken(code, codeVerifier)
getCurrentUser()
```

说明：

- `exchangeCodeForToken` 请求 `auth-backend` 的 `/oauth2/token`。
- `getCurrentUser` 请求 `api-backend` 的 `/api/me`。

### 7.3 Video API

`api/video.ts`

```text
getRecommendations(size)
getVideo(id)
getVideoPlayUrl(id)
markVideoViewed(id)
uploadVideo(file, title)
getMyVideos(page, size)
deleteVideo(id)
likeVideo(id)
unlikeVideo(id)
getVideoLikeStatus(id)
```

## 8. 组件设计

### 8.1 TabBar

底部 Tab 栏（首页 / 朋友 / + / 消息 / 我）。中间「+」用抖音双色阴影 + 黑色加号；点击跳到 `/upload`。朋友/消息为占位（ElMessage 提示"敬请期待"）。当前路由自动高亮。

### 8.2 VideoCard

接收 `video` 和 `active` 两个 prop。内部包含原生 `<video>`（无 `controls`，`loop` + `playsinline`）+ 中央播放图标 +  底部信息浮层 + 双击爱心动画 + `ActionRail`。监听 `active` 变化执行 `play/pause`。

### 8.3 ActionRail

抖音风格右侧悬浮操作栏。展示头像 + 关注红 + 号、心形点赞（已赞变粉红）、评论气泡、星形收藏、分享箭头、底部旋转唱片。数字 >=1w 自动转换为"1.2w"。

### 8.4 CommentSheet

底部抽屉。`visible` 控制显隐，`videoId` 决定加载哪条评论。包含计数 header、评论列表、底部输入框。发送时取 `useDisplayUser().displayUser.value` 作为评论作者。

### 8.5 useDisplayUser（composable）

把 `authStore.user`（`CurrentUser { userId, username, scopes }`）和 mock `CURRENT_USER` 的头像/签名合成抖音 UI 需要的 `UserInfo`。在 `MyVideosView` 和 `CommentSheet` 中复用。未登录时直接返回 mock，便于演示。后端补完用户头像/签名字段后，删除 fallback、直接用真接口数据即可。

## 9. 开发顺序

### 阶段 1：项目初始化

目标：创建前端基础工程。

任务：

1. 创建 Vue 3 + Vite + TypeScript 项目。
2. 安装 Vue Router、Pinia、Axios、Element Plus。
3. 配置环境变量。
4. 配置基础路由。
5. 配置 Pinia。
6. 配置 Axios 实例。

验收：

- 应用能在 `http://localhost:5173` 启动。
- 五个路由页面能正常打开。

### 阶段 2：OAuth2 PKCE 工具

目标：完成授权跳转所需工具。

任务：

1. 实现随机字符串生成。
2. 实现 `code_verifier` 生成。
3. 实现 S256 `code_challenge` 生成。
4. 实现 `state` 生成。
5. 将 `code_verifier` 和 `state` 保存到 `sessionStorage`。

验收：

- 能生成授权 URL。
- 授权 URL 包含 `client_id`、`redirect_uri`、`scope`、`code_challenge`、`state`。

### 阶段 3：登录与回调

目标：打通前端到 `auth-backend` 的 OAuth2 流程。

任务：

1. 实现 `authStore.login()`。
2. 实现注册入口跳转到 `auth-backend /register`。
3. 实现 `/oauth/callback` 页面。
4. 实现 token 换取请求。
5. 保存 access token。
6. 调用 `/api/me` 获取当前用户。
7. 处理回调失败状态。

验收：

- 点击登录能跳转授权服务器。
- 点击注册能跳转到 `auth-backend` 注册页面。
- 授权成功后能回到前端。
- 前端能保存 access token。
- 前端能显示当前用户名。

### 阶段 4：请求封装与权限状态

目标：所有业务请求自动携带 token。

任务：

1. Axios 请求拦截器添加 Authorization。
2. 响应拦截器处理 401 和 403。
3. 页面刷新时恢复 token。
4. token 失效时清空状态。

验收：

- 已登录状态下业务接口请求带 Bearer Token。
- 无效 token 时前端进入未登录状态。

### 阶段 5：推荐视频页（沉浸式）

目标：完成推荐主页核心流程。

任务：

1. 请求推荐视频列表（当前 mock，联调时改为 axios）。
2. 用 `scroll-snap-type: y mandatory` 实现整屏对齐的竖向滑动。
3. 用 IntersectionObserver 检测活跃视频，调用 `play()` / 其余 `pause()`。
4. 点击单击切播放/暂停；双击触发点赞 + 大爱心动画。
5. 当前视频变化时写访问记录（接真后端时启用）。
6. 处理空推荐列表（loading skeleton + 空状态文案）。

验收：

- 抖音风格界面渲染正确（手机外壳 / 顶部 tab / 操作栏 / 信息浮层）。
- 滑动切换流畅，每次只停一张视频。
- 活跃视频自动播放，其余暂停。
- 单击/双击/评论按钮交互正确。

### 阶段 6：点赞功能

目标：完成视频点赞交互。

任务：

1. 展示点赞数。
2. 展示当前用户是否已点赞。
3. 实现点赞。
4. 实现取消点赞。
5. 点赞后刷新当前视频状态。

验收：

- 点赞按钮状态正确。
- 点赞后点赞数变化。
- 取消点赞后点赞数变化。

### 阶段 7：发布视频页

目标：完成视频上传。

任务：

1. 实现标题输入。
2. 实现 MP4 文件选择。
3. 前端校验标题和文件类型。
4. 使用 `FormData` 上传。
5. 上传成功后清空表单或跳转我的视频页。

验收：

- MP4 文件上传成功。
- 非 MP4 文件被拦截。
- 标题为空时不能提交。

### 阶段 8：个人主页（我）

目标：完成抖音风格的个人页与作品管理。

任务：

1. 头部资料卡（头像 / 昵称 / 抖音号 / 签名 / 关注/粉丝/获赞）。
2. 作品三宫格（9:14 比例 + 封面 + 点赞数）。
3. 点击作品弹出全屏播放浮层。
4. 删除自己的作品（二次确认）。
5. 未登录时显示"登录 / 注册"按钮，已登录显示"退出登录"。

验收：

- 个人页样式接近抖音风格。
- 已登录显示真实 username。
- 上传后能立即在作品宫格看到新作品（mock 阶段用 blob URL）。
- 删除后列表刷新，且不再出现在推荐流。

### 阶段 9：完整联调

目标：完成作业演示闭环。

任务：

1. 点击「我」页的"登录 / 注册"，跳到 `auth-backend` 登录页。
2. 登录回到前端，自动回首页，能看到 `username`。
3. 滑动浏览推荐流，单击/双击/点赞/评论可用。
4. 上传视频，新作品出现在「我」页。
5. 删除作品，列表刷新。
6. 刷新整页，登录态保持（sessionStorage）；token 失效后回到未登录态。

验收：

- 前端可以演示所有作业功能点。
- 接口错误能展示提示。
- 页面刷新后登录状态能恢复。

## 10. 测试计划

### 10.1 认证测试

- 点击登录能跳转 `auth-backend`。
- 回调页能用 code 换 access token。
- `/api/me` 能返回当前用户。
- 页面刷新后能恢复登录状态。
- token 失效后接口返回 401，前端清空登录状态。

### 10.2 推荐测试

- 能显示推荐视频列表（抖音式全屏卡片）。
- 当前活跃视频自动播放，其余暂停。
- 上下滑动一次切换一张视频（scroll-snap 不会停留半屏）。
- 单击切播放/暂停；双击触发点赞动画。
- 切换视频会写访问记录（联调阶段）。
- 已访问视频刷新后不再推荐（联调阶段）。
- 无推荐视频时展示空状态。

### 10.3 点赞测试

- 未点赞时按钮显示点赞。
- 点赞后点赞数变化。
- 点赞后按钮显示取消点赞。
- 取消点赞后状态恢复。
- 重复点击不会导致前端状态错乱。

### 10.4 上传测试

- MP4 文件上传成功。
- 非 MP4 文件前端拦截。
- 标题为空时禁止提交。
- 上传成功后表单重置或跳转我的视频页。

### 10.5 我的视频测试

- 我的页面能分页展示当前用户视频。
- 视频播放可用。
- 删除自己视频成功。
- 删除后视频从列表消失。
- 删除后视频不再出现在推荐页。

## 11. 完成标准

前端完成后应该达到：

- UI 整体接近抖音 App 用户端体验（手机外壳 + TabBar + 沉浸式视频流 + 个人主页）。
- 能完成 OAuth2 PKCE 登录流程。
- 能保存 access token 并自动携带 Bearer Token。
- 能展示当前登录用户名。
- 能沉浸式浏览推荐视频（滑动切换 + 自动播放）。
- 能记录视频访问（联调阶段）。
- 能点赞和取消点赞（含双击大爱心）。
- 能上传 MP4 视频。
- 能查看我的作品（三宫格）。
- 能删除自己发布的视频。
- 能处理 401、403 和普通接口错误。
