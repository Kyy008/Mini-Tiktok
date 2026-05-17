# Mini-Tiktok 分工、总揽方案

## 1. 分工原则

本项目按模块主责分工，每个组员负责一个清晰的开发边界，同时承担与上下游模块的联调责任。

组长负责：

- 统一接口和端口约定。
- 检查进度。
- 组织联调。
- 合并代码。
- 汇总文档和答辩材料。
- 做最终验收。

四名组员分别称为：

```text
组员 A：auth-backend 鉴权服务器
组员 B：api-backend 视频基础功能
组员 C：api-backend 推荐、点赞、日志
组员 D：frontend 前端功能
```

统一端口：

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

统一约定：

- 前端通过 OAuth2 Authorization Code + PKCE 登录。
- `auth-backend` 签发 JWT Access Token。
- `api-backend` 通过 `issuer-uri=http://localhost:9000` 校验 token。
- 组员 A 负责登录注册与 OAuth2 对接，包括 `api-backend` 中的登录注册代理接口。
- 视频格式统一为 `MP4`

## 2. 总体分工表

| 成员 | 主责模块 | 主要内容 | 联调对象 |
|---|---|---|---|
| 组员 A | `auth-backend` | 用户注册登录、OAuth2 授权服务器、JWT/JWK、PKCE 联调、`api-backend` 登录注册代理对接 | 组员 B、组员 D |
| 组员 B | `api-backend` 视频基础 | Resource Server 骨架、视频上传、播放、我的视频、删除 | 组员 A、组员 D |
| 组员 C | `api-backend` 互动与日志 | 点赞、访问记录、推荐、请求日志、接口耗时 | 组员 B、组员 D |
| 组员 D | `frontend` | Vue 前端、OAuth2 回调、推荐页、上传页、我的视频页 | 组员 A、组员 B、组员 C |
| 组长 | 项目统筹 | 接口冻结、进度监督、代码合并、最终联调、答辩材料 | 全员 |

## 3. 组员 A：鉴权服务器

### 3.1 负责范围

组员 A 负责 `auth-backend`，实现本地模拟 OAuth2 鉴权服务器。

参考文档：

```text
auth-backend/auth-backend-plan.md
api-backend/api-backend-login-plan.md
```

### 3.2 核心任务

项目初始化：

- 创建 Spring Boot 3 项目。
- 配置端口 `9000`。
- 添加 Spring Security、Spring Authorization Server、MySQL、MyBatis-Plus、Flyway、Validation、Thymeleaf。
- 配置数据库连接。
- 配置 Flyway migration 路径。

用户模块：

- 编写 `V1__create_users.sql`。
- 实现 `users` 表对应实体和 Mapper。
- 实现账密注册。
- 使用 BCrypt 保存密码。
- 实现 `UserDetailsService`。
- 初始化 `demo` 测试用户。

登录注册页面：

- 实现 `GET /login`。
- 实现 `GET /register`。
- 实现 `POST /register`。
- 实现 `POST /api/register`，供 `api-backend` 代理注册时调用。
- 配置 Spring Security 表单登录。
- 配置 logout。

`api-backend` 登录注册代理：

- 实现：

```text
GET  /api/auth/login-url
POST /api/auth/register
```

- `/api/auth/login-url` 生成 OAuth2 Authorization Code + PKCE 登录跳转信息。
- `/api/auth/register` 将注册请求代理到 `auth-backend` 的 `POST /api/register`。
- `api-backend` 不自建用户表，不保存密码，不签发 access token。
- 登录后业务接口仍通过 Bearer JWT 访问。

OAuth2 授权服务器：

- 配置 Authorization Server SecurityFilterChain。
- 配置 issuer：`http://localhost:9000`。
- 配置 RSA key pair。
- 配置 JWKSource。
- 配置 JwtDecoder。
- 暴露标准端点：

```text
GET  /oauth2/authorize
POST /oauth2/token
GET  /oauth2/jwks
GET  /.well-known/oauth-authorization-server
```

OAuth2 client：

- 编写 `V2__create_oauth2_authorization_server_tables.sql`。
- 配置 JDBC RegisteredClientRepository。
- 编写 `V3__insert_tiktok_web_client.sql`。
- 注册 client：

```text
client_id: tiktok-web
redirect_uri: http://localhost:5173/oauth/callback
scope: video:read video:write video:like
require_proof_key: true
```

JWT claims：

- 在 access token 中写入 `sub`。
- 写入 `preferred_username`。
- 保留 `scope`。

### 3.3 联调责任

与组员 D 联调：

- 前端能调用 `GET /api/auth/login-url` 获取 PKCE 登录跳转信息。
- 前端能调用 `POST /api/auth/register` 完成注册。
- 前端点击登录能跳转 `/oauth2/authorize`。
- 登录成功后能回调 `http://localhost:5173/oauth/callback?code=...`。
- 前端能用 code + code_verifier 换取 access token。

与组员 B 联调：

- `api-backend` 能通过 `issuer-uri=http://localhost:9000` 获取 JWK。
- `api-backend` 能验证 `auth-backend` 签发的 JWT。
- token 过期、无 token、scope 不足时表现正确。

### 3.4 交付物

- 可运行的 `auth-backend`。
- Flyway 数据库迁移文件。
- 登录和注册页面。
- JSON 注册接口。
- `api-backend` 登录 URL 生成接口。
- `api-backend` 注册代理接口。
- 可用的 OAuth2 授权流程。
- 可用的 JWK 端点。
- `demo` 测试用户。
- OAuth2 流程说明和 token 示例。

### 3.5 验收标准

- 应用能在 `http://localhost:9000` 启动。
- 空数据库启动后 Flyway 自动建表。
- 用户可以注册和登录。
- `/api/auth/login-url` 无 token 可访问，并返回 PKCE 登录跳转信息。
- `/api/auth/register` 无 token 可访问，并能代理 `auth-backend` 注册。
- 密码入库为 BCrypt hash。
- `/oauth2/jwks` 能返回公钥。
- 前端能拿到 access token。
- `api-backend` 能校验 access token。

## 4. 组员 B：业务后端视频基础

### 4.1 负责范围

组员 B 负责 `api-backend` 的项目骨架、安全骨架、视频上传、视频播放、我的视频分页和删除。

参考文档：

```text
api-backend/api-backend-plan.md
```

### 4.2 核心任务

项目初始化：

- 创建 Spring Boot 3 项目。
- 配置端口 `8085`。
- 添加 Web、Security Resource Server、MyBatis-Plus、MySQL、Flyway、AOP、Validation、Lombok。
- 配置数据库连接。
- 配置 Flyway migration 路径。
- 配置 `issuer-uri=http://localhost:9000`。

数据库基础：

- 编写 `V1__create_video.sql`。
- 实现 `video` 表实体、Mapper 和基础 Service。

安全骨架：

- 配置 OAuth2 Resource Server。
- 配置接口权限规则。
- 实现当前用户工具类，从 JWT 读取：

```text
sub
preferred_username
scope
```

- 实现：

```text
GET /api/me
```

视频上传：

- 实现：

```text
POST /api/videos
```

- 接收 multipart 表单字段：

```text
file
title
```

- 校验文件不能为空。
- 校验文件类型为 MP4。
- 计算 SHA-256。
- 保存文件到：

```text
storage/videos/
```

- 文件名使用：

```text
{file_hash}.mp4
```

- 相同 hash 文件不重复保存。
- 写入 `video` 表。
- `uploader_id` 使用 JWT `sub`。

视频详情与播放：

- 实现：

```text
GET /api/videos/{id}
GET /api/videos/{id}/play
```

- 播放接口返回 `video/mp4`。
- 删除视频返回 404。

我的视频：

- 实现：

```text
GET /api/my/videos?page=1&size=10
```

- 只返回当前用户上传的视频。
- 排除 `deleted = true`。
- 按创建时间倒序。

删除视频：

- 实现：

```text
DELETE /api/videos/{id}
```

- 删除前校验 `video.uploader_id == JWT sub`。
- 使用软删除，将 `deleted` 置为 `true`。

### 4.3 联调责任

与组员 A 联调：

- 验证 `GET /api/me` 能识别 token。
- 验证无 token 返回 401。
- 验证 scope 不足返回 403。

与组员 D 联调：

- 前端能上传 MP4。
- 前端能播放视频。
- 前端能分页查看我的视频。
- 前端能删除自己的视频。

与组员 C 联调：

- 提供稳定的 `video` 表结构。
- 确保推荐、点赞、访问记录能正确关联视频 ID。
- 删除视频后，C 的推荐和点赞逻辑能正确排除已删除视频。

### 4.4 交付物

- 可运行的 `api-backend` 基础项目。
- `video` 表迁移文件。
- Resource Server 安全配置。
- `/api/me` 接口。
- 视频上传接口。
- 视频播放接口。
- 视频详情接口。
- 我的视频分页接口。
- 删除视频接口。

### 4.5 验收标准

- 应用能在 `http://localhost:8085` 启动。
- 有效 token 可以访问 `/api/me`。
- 上传 MP4 成功。
- 非 MP4 上传失败。
- 相同文件只保存一份实体文件。
- 视频能通过 `<video>` 播放。
- 用户只能看到自己的视频。
- 用户不能删除他人的视频。
- 删除后视频不再出现在我的视频列表。

## 5. 组员 C：推荐、点赞和日志

### 5.1 负责范围

组员 C 负责 `api-backend` 的互动功能、推荐逻辑、访问记录和请求日志。

参考文档：

```text
api-backend/api-backend-plan.md
```

### 5.2 核心任务

数据库迁移：

- 编写 `V2__create_video_like.sql`。
- 编写 `V3__create_video_view.sql`。
- 编写 `V4__create_request_log.sql`。

点赞模块：

- 实现：

```text
POST   /api/videos/{id}/likes
DELETE /api/videos/{id}/likes
GET    /api/videos/{id}/likes
```

- `video_like` 使用唯一索引：

```text
unique(user_id, video_id)
```

- 点赞时插入 `video_like`。
- 重复点赞不重复插入。
- 取消点赞时删除 `video_like`。
- 查询点赞状态时统计 `video_like`。
- 返回当前用户是否已点赞。

访问记录：

- 实现：

```text
POST /api/videos/{id}/views
```

- 当前用户访问视频后写入 `video_view`。
- 重复访问同一视频不重复插入。
- 删除的视频返回 404。

推荐接口：

- 实现：

```text
GET /api/videos/recommendations?size=10
```

- 排除当前用户已经访问过的视频。
- 排除 `deleted = true` 的视频。
- 按点赞数降序、创建时间降序排序。
- 返回当前用户是否已点赞。
- 没有可推荐视频时返回空列表。

日志与耗时监控：

- 使用 Spring AOP 拦截 `/api/**`。
- 记录：

```text
user_id
method
path
request_body
response_body
status_code
duration_ms
ip
created_at
```

- 写入 Logback 文件日志。
- 写入 `request_log` 表。
- 上传接口只记录文件摘要。
- 播放接口只记录视频 ID、响应状态和耗时。
- 异常请求也记录状态码和耗时。

### 5.3 联调责任

与组员 B 联调：

- 确认 `video` 表字段稳定。
- 点赞、访问记录、推荐都能正确关联视频。
- 删除视频后，推荐接口和点赞接口正确处理。

与组员 D 联调：

- 前端能展示推荐列表。
- 前端切换视频时能写访问记录。
- 前端能点赞和取消点赞。
- 前端能展示点赞数和点赞状态。

与组长联调：

- 提供日志样例。
- 提供接口耗时样例。
- 协助准备答辩中的日志和监控说明。

### 5.4 交付物

- `video_like` 表迁移文件。
- `video_view` 表迁移文件。
- `request_log` 表迁移文件。
- 点赞接口。
- 取消点赞接口。
- 点赞状态接口。
- 访问记录接口。
- 推荐接口。
- AOP 请求日志。

### 5.5 验收标准

- 点赞成功后点赞数统计增加。
- 重复点赞不会重复插入记录。
- 取消点赞后点赞数统计减少。
- 访问过的视频不再推荐。
- 不同用户的访问记录互不影响。
- 推荐结果按点赞数降序。
- 普通接口能记录输入、输出、耗时、状态码、IP、用户 ID。
- 上传和播放接口不记录二进制内容。

## 6. 组员 D：前端

### 6.1 负责范围

组员 D 负责 `frontend`，实现用户侧页面、OAuth2 前端流程和业务接口联调。

参考文档：

```text
frontend/frontend-plan.md
```

### 6.2 核心任务

项目初始化：

- 创建 Vue 3 + Vite + TypeScript 项目。
- 安装 Vue Router、Pinia、Axios、Element Plus。
- 配置环境变量：

```text
VITE_AUTH_BASE_URL=http://localhost:9000
VITE_API_BASE_URL=http://localhost:8085
VITE_CLIENT_ID=tiktok-web
VITE_REDIRECT_URI=http://localhost:5173/oauth/callback
VITE_SCOPE=video:read video:write video:like
```

- 配置路由。
- 配置 Pinia。
- 配置 Axios 实例。

OAuth2 前端流程：

- 实现 PKCE 工具。
- 生成 `code_verifier`。
- 生成 S256 `code_challenge`。
- 生成 `state`。
- 保存 `code_verifier` 和 `state` 到 `sessionStorage`。
- 实现登录跳转。
- 实现 `/oauth/callback` 页面。
- 用 authorization code 换 access token。
- 保存 access token。
- 调用 `/api/me` 获取当前用户。

路由页面：

```text
/                  推荐视频页
/oauth/callback    OAuth2 回调页
/upload            发布视频页
/my/videos         我的视频页
```

推荐视频页：

- 请求推荐视频列表。
- 使用 `<video>` 播放当前视频。
- 展示标题、点赞数、点赞状态。
- 实现上一个和下一个。
- 当前视频变化时调用访问记录接口。
- 实现点赞和取消点赞。
- 处理无推荐视频状态。

发布视频页：

- 输入标题。
- 选择 MP4 文件。
- 校验标题不为空。
- 校验文件类型为 MP4。
- 使用 `FormData` 上传。
- 上传成功后清空表单或跳转我的视频页。

我的视频页：

- 请求我的视频分页列表。
- 展示标题、点赞数、创建时间。
- 支持播放我的视频。
- 支持删除视频。
- 删除成功后刷新当前页。

请求封装：

- Axios 自动添加：

```text
Authorization: Bearer <access_token>
```

- 处理 401。
- 处理 403。
- 展示普通接口错误。

### 6.3 联调责任

与组员 A 联调：

- 点击登录能跳转授权服务器。
- 授权成功后能回到 `/oauth/callback`。
- 前端能换取 access token。

与组员 B 联调：

- 上传视频。
- 播放视频。
- 查看我的视频。
- 删除自己的视频。

与组员 C 联调：

- 推荐列表。
- 访问记录。
- 点赞。
- 取消点赞。
- 点赞状态展示。

### 6.4 交付物

- 可运行的 Vue 前端项目。
- OAuth2 登录入口。
- OAuth2 回调页。
- 推荐视频页。
- 发布视频页。
- 我的视频页。
- Axios 请求封装。
- Pinia 状态管理。

### 6.5 验收标准

- 应用能在 `http://localhost:5173` 启动。
- 能完成 OAuth2 PKCE 登录流程。
- 登录后能显示当前用户。
- 能展示推荐视频并播放。
- 能上一个和下一个切换。
- 能记录视频访问。
- 能点赞和取消点赞。
- 能上传 MP4 视频。
- 能分页查看我的视频。
- 能删除自己发布的视频。
- 能处理 401、403 和普通接口错误。

## 7. 组长工作

### 7.1 统筹任务

组长负责全局协调，不承担单一大模块开发。

主要工作：

- 确认三端端口和接口路径。
- 维护接口约定。
- 组织每日进度同步。
- 检查每个组员的可运行成果。
- 组织联调。
- 处理模块之间的接口冲突。
- 合并代码。
- 汇总文档。
- 准备答辩材料。

### 7.2 接口冻结清单

组长需要维护以下接口清单：

```text
GET    /api/me
GET    /api/videos/recommendations?size=10
GET    /api/videos/{id}
GET    /api/videos/{id}/play
POST   /api/videos/{id}/views
POST   /api/videos
GET    /api/my/videos?page=1&size=10
DELETE /api/videos/{id}
POST   /api/videos/{id}/likes
DELETE /api/videos/{id}/likes
GET    /api/videos/{id}/likes
```

OAuth2 端点：

```text
GET  /oauth2/authorize
POST /oauth2/token
GET  /oauth2/jwks
GET  /.well-known/oauth-authorization-server
```

### 7.3 每日检查点

建议每天检查：

- 每个人是否有可运行进展。
- 是否有接口字段变更。
- 是否影响其他组员。
- 是否通过当前阶段验收。
- 是否需要组长协调联调。

建议使用表格追踪：

```text
任务 | 负责人 | 状态 | 阻塞点 | 联调对象 | 验收结果
```

## 8. 推荐开发节奏

### 第 1 阶段：项目骨架

目标：三个项目都能启动。

```text
A：auth-backend 启动，用户表迁移可执行。
B：api-backend 启动，video 表迁移可执行。
C：准备 video_like、video_view、request_log 表迁移。
D：frontend 启动，四个路由页面可打开。
```

### 第 2 阶段：鉴权链路

目标：登录和 token 验证打通。

```text
A + D：OAuth2 PKCE 登录打通。
A + B：api-backend 能验证 JWT。
B：/api/me 可用。
D：前端能显示当前用户。
```

### 第 3 阶段：视频基础功能

目标：上传、播放、我的视频、删除可用。

```text
B：完成视频上传、播放、我的视频、删除。
D：接入上传页和我的视频页。
C：准备推荐和点赞依赖的视频数据查询。
```

### 第 4 阶段：推荐和互动

目标：推荐、访问记录、点赞和日志可用。

```text
C：完成推荐、访问记录、点赞、日志。
D：接入推荐页、点赞按钮、上一个、下一个。
B：配合处理视频删除后推荐和播放逻辑。
```

### 第 5 阶段：完整联调和答辩

目标：完成演示闭环。

完整流程：

```text
登录
上传视频
查看推荐
播放视频
点赞
下一个视频
查看我的视频
删除视频
刷新推荐
展示日志和耗时
```

## 9. 贡献说明建议

最后答辩或文档里可以这样描述贡献：

```text
组员 A：负责 OAuth2 鉴权服务器、用户注册登录、JWT 签发与 JWK 验签。
组员 B：负责业务后端安全骨架、视频上传存储、播放、我的视频和删除权限。
组员 C：负责推荐逻辑、访问记录、点赞防重复、请求日志和接口耗时监控。
组员 D：负责 Vue 前端、OAuth2 前端流程、推荐页、上传页和我的视频页。
组长：负责系统架构设计、任务拆分、接口协调、联调验收和答辩材料整合。
```
