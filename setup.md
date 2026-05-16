# Mini-Tiktok 前端准备工作

本文档记录组长在前端开发开始前需要完成的准备工作。目标是搭好 `frontend` 的工程骨架，让组员 D 可以直接进入页面功能开发和接口联调。

组长只负责工程初始化、依赖安装、环境变量、基础目录、空路由和说明文档。具体 OAuth2 登录、PKCE、token 管理、接口调用和页面业务由组员 D 实现。

## 1. 初始化 Vue 工程

进入项目根目录：

```bash
cd Mini-Tiktok
```

由于 `frontend/` 目录中已经有计划文档，可以先用临时目录创建 Vite 项目，再把生成的工程文件合并到 `frontend/`：

```bash
npm create vite@latest frontend-tmp -- --template vue-ts
cp -R frontend-tmp/. frontend/
rm -rf frontend-tmp
```

进入前端目录：

```bash
cd frontend
```

安装依赖：

```bash
npm install
npm install vue-router@4 pinia axios element-plus
```

## 2. 配置环境变量

在 `frontend/` 下创建 `.env.example`：

```env
VITE_AUTH_BASE_URL=http://localhost:9000
VITE_API_BASE_URL=http://localhost:8085
VITE_CLIENT_ID=tiktok-web
VITE_REDIRECT_URI=http://localhost:5173/oauth/callback
VITE_SCOPE=video:read video:write video:like
```

在 `frontend/` 下创建 `.env`，内容与 `.env.example` 保持一致。

统一端口：

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

## 3. 建立目录结构

在 `frontend/src/` 下建立基础目录：

```text
src/
  api/
  components/
  router/
  stores/
  utils/
  views/
```

创建以下空文件，用于固定代码位置：

```text
src/api/auth.ts
src/api/types.ts
src/api/video.ts
src/components/PageHeader.vue
src/router/index.ts
src/stores/auth.ts
src/utils/http.ts
src/utils/pkce.ts
src/utils/token.ts
src/views/RecommendView.vue
src/views/OAuthCallbackView.vue
src/views/UploadView.vue
src/views/MyVideosView.vue
```

这些文件中只写简单注释或占位内容，具体逻辑由组员 D 补充。

## 4. 接入基础插件

在 `src/main.ts` 中接入：

```text
Vue Router
Pinia
Element Plus
```

只需要保证项目能正常启动，插件能正常挂载。

## 5. 配置基础路由

在 `src/router/index.ts` 中配置四个页面路由：

```text
/               推荐视频页
/oauth/callback OAuth2 回调页
/upload         发布视频页
/my/videos      我的视频页
```

页面组件对应关系：

```text
/               -> RecommendView.vue
/oauth/callback -> OAuthCallbackView.vue
/upload         -> UploadView.vue
/my/videos      -> MyVideosView.vue
```

每个页面只放置简单标题，确保路由可以打开。

## 6. 放置接口和工具文件占位

在 `src/api/` 下准备：

```text
auth.ts   鉴权相关接口位置
video.ts  视频业务接口位置
types.ts  前后端数据类型位置
```

在 `src/utils/` 下准备：

```text
http.ts   Axios 请求实例位置
pkce.ts   PKCE 工具位置
token.ts  token 存取工具位置
```

在 `src/stores/` 下准备：

```text
auth.ts   登录状态管理位置
```

这些文件只负责占位和写明用途，不提前实现业务逻辑。

## 7. 准备页面占位

四个页面先放置标题即可：

```text
RecommendView.vue       推荐视频
OAuthCallbackView.vue   登录回调处理
UploadView.vue          发布视频
MyVideosView.vue        我的视频
```

页面中不编写接口请求、token 处理、上传、点赞、删除等逻辑。

## 8. 准备开发说明

在 `frontend/README.md` 中写清楚：

```text
1. 安装依赖
   npm install

2. 启动开发服务器
   npm run dev

3. 前端访问地址
   http://localhost:5173

4. 后端地址
   auth-backend: http://localhost:9000
   api-backend:  http://localhost:8085

5. 环境变量
   参考 .env.example
```

在 `frontend/TODO.md` 中写清楚组员 D 的任务：

```text
1. 实现首页登录入口和退出入口。
2. 实现 OAuth2 Authorization Code + PKCE 登录跳转。
3. 实现 /oauth/callback 换取 access token。
4. 实现 token 保存、恢复和清理。
5. 实现 Axios 自动携带 Bearer Token。
6. 实现推荐视频列表、播放、上一个、下一个。
7. 实现访问记录上报。
8. 实现点赞和取消点赞。
9. 实现发布视频。
10. 实现我的视频分页。
11. 实现删除我的视频。
12. 与 auth-backend、api-backend 完成联调。
```

## 9. 验收前端准备工作

执行：

```bash
npm run dev
```

确认 Vite 启动地址为：

```text
http://localhost:5173
```

逐个访问：

```text
http://localhost:5173/
http://localhost:5173/oauth/callback
http://localhost:5173/upload
http://localhost:5173/my/videos
```

准备工作完成标准：

- 前端项目可以正常启动。
- Vue Router 可以正常切换四个页面。
- Pinia 已经接入。
- Element Plus 已经接入。
- 环境变量文件已经准备好。
- `api/`、`utils/`、`stores/`、`views/` 目录和占位文件已经准备好。
- `README.md` 和 `TODO.md` 已经写清楚启动方式和开发任务。
