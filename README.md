# Mini-Tiktok

Mini-Tiktok 是一个迷你短视频系统，支持登录注册、推荐视频流、视频上传、点赞评论、我的作品管理和请求日志展示。

## 在线体验

[https://tiktok.kyy008.me](https://tiktok.kyy008.me)

## 主要功能

- 浏览推荐视频，支持上下切换、播放和暂停
- OAuth2 登录注册
- 发布 MP4 视频
- 点赞、取消点赞和发表评论
- 查看、播放和删除自己的作品
- 查看接口请求日志和耗时

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus
- 后端：Spring Boot 3、Spring Security、Spring Authorization Server、MyBatis-Plus、Flyway
- 数据库：MySQL 8
- 部署：Docker Compose、Nginx

## 快速启动

请先安装 Docker 和 Docker Compose。

```bash
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

启动完成后打开：

[http://localhost:8088](http://localhost:8088)

停止服务：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

## 如何使用

1. 打开首页后可以直接浏览推荐视频。
2. 点击登录或需要登录的功能，进入授权登录流程。
3. 登录后可以点赞、评论、上传视频。
4. 点击底部“我的”查看自己发布的视频。
5. 点击首页“日志控制台”可以查看接口请求记录。

## 项目结构

```text
auth-backend/   OAuth2 鉴权服务
api-backend/    视频、互动、推荐和日志业务服务
frontend/       Vue 前端应用
docker/         Nginx 和数据库初始化配置
docs/           项目文档
```

## 文档

- [需求文档](docs/Mini-tiktok需求文档.pdf)
- [技术文档](docs/Mini-tiktok技术文档.pdf)
- [分工说明](分工.md)
