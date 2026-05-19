# Mini-Tiktok 前端方案

## 1. 目标定位

`frontend` 是 Mini-Tiktok 项目的前端应用，负责完成用户侧的功能演示和接口联调。

前端主要能力：

- 通过 OAuth2 Authorization Code + PKCE 接入 `auth-backend`。
- 获取并保存 access token。
- 请求 `api-backend` 时自动携带 Bearer Token。
- 展示推荐视频并支持播放。
- 支持上一个视频、下一个视频。
- 支持视频点赞和取消点赞。
- 支持发布视频。
- 支持分页查看我的视频。
- 支持删除自己发布的视频。

登录页面由 `auth-backend` 托管。前端提供登录入口，负责生成 PKCE 参数、跳转授权服务器、处理 OAuth2 回调和维护登录状态；注册采用 SPA 表单直连 `auth-backend` 的 `POST /api/register`。

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

建议目录：

```text
frontend/
  src/
    api/
      auth.ts
      video.ts
    components/
      VideoPlayer.vue
      VideoActions.vue
      PageHeader.vue
    router/
      index.ts
    stores/
      auth.ts
      video.ts
    utils/
      http.ts
      pkce.ts
      token.ts
    views/
      RecommendView.vue
      OAuthCallbackView.vue
      UploadView.vue
      MyVideosView.vue
    App.vue
    main.ts
```

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
RecommendView.vue
```

功能：

- 展示当前登录状态。
- 提供登录和退出入口。
- 请求推荐视频列表。
- 播放当前推荐视频。
- 显示视频标题、点赞数、点赞状态。
- 支持上一个视频。
- 支持下一个视频。
- 支持点赞。
- 支持取消点赞。
- 切换视频时记录访问。
- 无推荐视频时展示空状态。

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

- 第一次进入页面时加载推荐列表。
- 当前视频变化后调用 `POST /api/videos/{id}/views`。
- 下一个按钮将 `currentIndex + 1`。
- 到达列表末尾后重新拉取推荐列表。
- 上一个按钮将 `currentIndex - 1`。
- 当前视频点赞后刷新点赞状态或更新本地展示。

### 5.2 SPA 注册页

```text
/register
```

页面组件：

```text
RegisterView.vue
```

功能：

- 输入用户名和密码。
- 直接调用 `auth-backend` JSON 注册接口。
- 注册成功后提示用户登录。
- 展示用户名重复、参数错误等注册失败信息。

调用接口：

```text
POST /api/register      auth-backend
```

### 5.3 OAuth2 回调页

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

### 5.4 发布视频页

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

### 5.5 我的视频页

```text
/my/videos
```

页面组件：

```text
MyVideosView.vue
```

功能：

- 分页查看当前用户上传的视频。
- 展示视频标题、点赞数、创建时间。
- 支持播放我的视频。
- 支持删除自己的视频。
- 删除成功后刷新当前页。
- 空列表时展示空状态。

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
register(credentials)
handleCallback(code, state)
loadMe()
logout()
restore()
setToken(token)
clear()
```

职责：

- 生成 OAuth2 授权跳转地址。
- 调用 `auth-backend` 完成 SPA 注册。
- 处理回调换 token。
- 保存和恢复登录状态。
- 调用 `/api/me`。
- 退出时清理本地状态。

### 6.2 videoStore

字段：

```text
recommendations
currentIndex
myVideos
myVideosPage
myVideosSize
```

方法：

```text
loadRecommendations()
nextVideo()
previousVideo()
markViewed(videoId)
likeVideo(videoId)
unlikeVideo(videoId)
loadMyVideos()
deleteVideo(videoId)
```

职责：

- 管理推荐列表。
- 管理当前视频。
- 管理点赞状态。
- 管理我的视频分页。

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
registerUser(credentials)
getCurrentUser()
```

说明：

- `exchangeCodeForToken` 请求 `auth-backend` 的 `/oauth2/token`。
- `registerUser` 请求 `auth-backend` 的 `/api/register`。
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

### 8.1 PageHeader

功能：

- 展示应用名称。
- 展示当前用户。
- 提供登录按钮。
- 提供退出按钮。
- 提供导航入口：推荐、发布、我的视频。

### 8.2 VideoPlayer

功能：

- 接收视频对象。
- 使用 `<video controls>` 播放视频。
- 根据视频 ID 拼接播放地址。
- 视频为空时展示占位状态。

### 8.3 VideoActions

功能：

- 展示点赞数。
- 展示点赞/取消点赞按钮。
- 展示上一个/下一个按钮。
- 触发父组件传入的操作函数。

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
2. 实现 `authStore.register()`，直接调用 `auth-backend /api/register`。
3. 实现 `/register` SPA 注册页。
4. 实现 `/oauth/callback` 页面。
5. 实现 token 换取请求。
6. 保存 access token。
7. 调用 `/api/me` 获取当前用户。
8. 处理回调失败状态。

验收：

- 点击登录能跳转授权服务器。
- SPA 注册页能直连 `auth-backend` 完成注册。
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

### 阶段 5：推荐视频页

目标：完成推荐主页核心流程。

任务：

1. 请求推荐视频列表。
2. 展示当前视频信息。
3. 使用 `<video>` 播放。
4. 实现上一个/下一个。
5. 当前视频变化时写访问记录。
6. 处理空推荐列表。

验收：

- 推荐视频能显示。
- 视频能播放。
- 上一个/下一个可用。
- 切换视频后会调用访问记录接口。

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

### 阶段 8：我的视频页

目标：完成用户视频管理。

任务：

1. 请求我的视频分页列表。
2. 展示标题、点赞数、创建时间。
3. 支持播放我的视频。
4. 实现删除视频。
5. 删除成功后刷新当前页。

验收：

- 能看到当前用户上传的视频。
- 分页可用。
- 删除后列表刷新。

### 阶段 9：完整联调

目标：完成作业演示闭环。

任务：

1. 登录。
2. 查看推荐。
3. 播放视频。
4. 切换上一个/下一个。
5. 点赞和取消点赞。
6. 上传视频。
7. 查看我的视频。
8. 删除视频。
9. 刷新推荐列表确认删除和访问过滤生效。

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

- 能显示推荐视频列表。
- 当前视频能播放。
- 下一个切换正确。
- 上一个切换正确。
- 切换视频会写访问记录。
- 已访问视频刷新后不再推荐。
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

- 能完成 OAuth2 PKCE 登录流程。
- 能保存 access token 并自动携带 Bearer Token。
- 能展示当前登录用户。
- 能展示推荐视频并播放。
- 能完成上一个/下一个切换。
- 能记录视频访问。
- 能点赞和取消点赞。
- 能上传 MP4 视频。
- 能分页查看我的视频。
- 能删除自己发布的视频。
- 能处理 401、403 和普通接口错误。
