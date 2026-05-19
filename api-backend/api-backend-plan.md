# Mini-Tiktok 普通业务后台方案

## 1. 目标定位

`api-backend` 是 Mini-Tiktok 项目的普通业务后台，负责视频上传、视频播放、推荐视频、点赞、我的视频管理、访问记录、请求日志和接口耗时记录。

本模块不处理账密登录，也不签发 token。它作为 OAuth2 Resource Server，校验 `auth-backend` 签发的 Bearer JWT，并从 JWT 中读取当前用户信息。

核心目标：

- 完成作业要求中的全部业务功能。
- 使用 OAuth2 Resource Server 校验 access token。
- 使用 JWT `sub` 识别当前用户。
- 使用“是否登录 + scope + 资源归属”完成权限控制。
- 使用 Flyway 管理业务数据库表。
- 使用 AOP + Logback 记录输入、输出、状态码和接口耗时。

## 2. 技术栈

### 2.1 基础技术

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Security 6
- OAuth2 Resource Server
- MySQL 8
- MyBatis-Plus
- Flyway
- Lombok
- Spring Validation
- Spring AOP
- Logback
- Maven

### 2.2 安全技术

- Bearer Token 认证
- JWT 验签
- OAuth2 scope 权限控制
- 资源归属校验

关键配置：

```yaml
server:
  port: 8085

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

说明：

- `auth-backend` 使用私钥签发 JWT。
- `api-backend` 通过 `issuer-uri` 发现授权服务器元数据和 JWK 公钥。
- `api-backend` 只验证 token，不保存用户密码，不生成登录 URL，也不代理注册。
- 当前用户 ID 从 JWT `sub` 读取。
- 开发环境使用 `http://localhost:8085` 提供业务接口。

### 2.3 视频存储

视频使用本地文件系统存储：

```text
storage/videos/
```

文件保存规则：

- 上传时计算文件 SHA-256。
- 文件名使用 `SHA-256 hash + 原扩展名`。
- 相同 hash 的文件只保存一份。
- 数据库保存文件 hash，文件路径由 hash 和固定目录推导。
- 视频播放接口根据数据库记录读取本地文件。

## 3. 数据库设计

使用 Flyway 管理数据库版本，迁移文件放在：

```text
api-backend/src/main/resources/db/migration/
```

建议迁移顺序：

```text
V1__create_video.sql
V2__create_video_like.sql
V3__create_video_view.sql
V4__create_request_log.sql
```

### 3.1 video 表

```sql
video(
  id bigint primary key auto_increment,
  title varchar(128) not null,
  file_hash varchar(128) not null,
  uploader_id varchar(64) not null,
  deleted boolean not null default false,
  created_at datetime not null,
  index idx_uploader_id(uploader_id),
  index idx_deleted_created(deleted, created_at)
)
```

说明：

- `uploader_id` 对应 JWT `sub`。
- 使用软删除，删除视频时只将 `deleted` 置为 `true`。
- `file_hash` 用于视频文件去重。
- 视频文件路径按 `storage/videos/{file_hash}.mp4` 推导。

### 3.2 video_like 表

```sql
video_like(
  id bigint primary key auto_increment,
  user_id varchar(64) not null,
  video_id bigint not null,
  created_at datetime not null,
  unique uk_user_video(user_id, video_id),
  index idx_video_id(video_id)
)
```

说明：

- 记录用户点赞关系。
- `unique(user_id, video_id)` 防止重复点赞。
- 点赞数通过统计 `video_like` 得到，避免维护冗余计数字段。

### 3.3 video_view 表

```sql
video_view(
  id bigint primary key auto_increment,
  user_id varchar(64) not null,
  video_id bigint not null,
  viewed_at datetime not null,
  unique uk_user_video(user_id, video_id),
  index idx_user_id(user_id),
  index idx_video_id(video_id)
)
```

说明：

- 记录用户访问过的视频。
- 推荐接口根据当前用户的 `video_view` 过滤已访问视频。
- 重复访问同一个视频时不重复插入。

### 3.4 request_log 表

```sql
request_log(
  id bigint primary key auto_increment,
  user_id varchar(64),
  method varchar(16) not null,
  path varchar(512) not null,
  request_body text,
  response_body text,
  status_code int not null,
  duration_ms bigint not null,
  ip varchar(64),
  created_at datetime not null,
  index idx_user_id(user_id),
  index idx_path(path),
  index idx_created_at(created_at)
)
```

说明：

- 用于作业要求中的日志和集成监控能力。
- 记录用户请求的输入、输出、状态码、耗时和 IP。
- 上传视频和播放视频接口不记录文件二进制内容，只记录摘要信息。

## 4. 接口设计

统一响应格式建议：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

异常响应示例：

```json
{
  "code": 403,
  "message": "forbidden"
}
```

### 4.1 当前用户接口

```text
GET /api/me
```

权限：

```text
authenticated
```

返回示例：

```json
{
  "userId": "1",
  "username": "demo",
  "scopes": ["video:read", "video:write", "video:like"]
}
```

用途：

- 验证 `api-backend` 与 `auth-backend` 已联通。
- 前端可用它确认当前 token 对应的用户信息。

### 4.2 推荐视频接口

```text
GET /api/videos/recommendations?size=10
```

权限：

```text
SCOPE_video:read
```

行为：

- 返回当前用户未访问过的视频。
- 排除 `deleted = true` 的视频。
- 按点赞数降序、创建时间降序排序。
- `size` 默认 10，建议最大 50。

返回示例：

```json
[
  {
    "id": 1,
    "title": "demo video",
    "likeCount": 10,
    "liked": false,
    "playUrl": "/api/videos/1/play",
    "createdAt": "2026-05-16T10:00:00"
  }
]
```

### 4.3 视频详情接口

```text
GET /api/videos/{id}
```

权限：

```text
SCOPE_video:read
```

行为：

- 返回视频基础信息。
- 返回当前用户是否已点赞。
- 删除的视频返回 404。

### 4.4 视频播放接口

```text
GET /api/videos/{id}/play
```

权限：

```text
SCOPE_video:read
```

行为：

- 读取本地视频文件并返回视频流。
- 响应 `Content-Type` 使用 `video/mp4`。
- 支持浏览器 `<video>` 标签播放。
- 删除的视频返回 404。

### 4.5 记录访问接口

```text
POST /api/videos/{id}/views
```

权限：

```text
SCOPE_video:read
```

行为：

- 当前用户访问视频后写入 `video_view`。
- 如果记录已存在，不重复插入，仍返回成功。
- 删除的视频返回 404。

### 4.6 发布视频接口

```text
POST /api/videos
Content-Type: multipart/form-data
```

权限：

```text
SCOPE_video:write
```

表单字段：

```text
file          视频文件
title         视频标题
```

行为：

- 校验文件不能为空。
- 校验视频类型，优先支持 `video/mp4`。
- 计算 SHA-256。
- 相同 hash 文件只保存一份。
- 创建 `video` 记录，`uploader_id = JWT sub`。

### 4.7 我的视频接口

```text
GET /api/my/videos?page=1&size=10
```

权限：

```text
authenticated
```

行为：

- 只查询当前用户上传的视频。
- 排除 `deleted = true`。
- 分页返回。
- 按 `created_at desc` 排序。

### 4.8 删除视频接口

```text
DELETE /api/videos/{id}
```

权限：

```text
SCOPE_video:write
```

行为：

- 只允许删除当前用户自己上传的视频。
- 如果 `video.uploader_id != JWT sub`，返回 403。
- 使用软删除，将 `deleted` 置为 `true`。
- 删除后不再出现在推荐和我的视频列表。

### 4.9 点赞接口

```text
POST /api/videos/{id}/likes
```

权限：

```text
SCOPE_video:like
```

行为：

- 当前用户给视频点赞。
- `video_like` 唯一索引防重复。
- 首次点赞插入 `video_like`。
- 重复点赞不重复插入。
- 删除的视频返回 404。

### 4.10 取消点赞接口

```text
DELETE /api/videos/{id}/likes
```

权限：

```text
SCOPE_video:like
```

行为：

- 当前用户取消点赞。
- 已点赞时删除 `video_like`。
- 未点赞时保持稳定，返回成功。

### 4.11 查询点赞状态接口

```text
GET /api/videos/{id}/likes
```

权限：

```text
SCOPE_video:read
```

返回示例：

```json
{
  "videoId": 1,
  "likeCount": 10,
  "liked": true
}
```

## 5. 权限规则

Spring Security 统一校验 Bearer Token。

```text
GET    /api/me                         authenticated
GET    /api/videos/**                  SCOPE_video:read
POST   /api/videos                     SCOPE_video:write
DELETE /api/videos/{id}                SCOPE_video:write + 本人视频
POST   /api/videos/{id}/views          SCOPE_video:read
POST   /api/videos/{id}/likes          SCOPE_video:like
DELETE /api/videos/{id}/likes          SCOPE_video:like
GET    /api/my/videos                  authenticated
```

资源归属规则：

- 删除视频时，`video.uploader_id` 必须等于 JWT `sub`。
- 查询我的视频时，只查 JWT `sub` 对应的视频。
- 推荐过滤时，只过滤当前 JWT `sub` 的访问记录。
- 点赞记录使用 JWT `sub`，不信任前端传入用户 ID。

## 6. 日志与监控设计

使用 Spring AOP 拦截 `/api/**` 业务接口，记录每次请求：

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

特殊处理：

- 上传视频接口不记录文件二进制内容，只记录文件名、大小、MIME 类型。
- 视频播放接口不记录视频二进制内容，只记录视频 ID、响应状态和耗时。
- 请求异常时也记录状态码和耗时。
- 密码不经过 `api-backend`，因此日志中不涉及密码脱敏。

日志输出：

- 写入 Logback 文件日志。
- 同步写入 `request_log` 表，方便展示作业要求中的输入、输出、耗时。

## 7. 开发顺序

### 阶段 1：项目初始化

目标：让 `api-backend` 能启动，并具备 Resource Server 基础配置。

任务：

1. 创建 Spring Boot 3 项目。
2. 配置端口 `8085`。
3. 添加 Web、Security Resource Server、MyBatis-Plus、MySQL、Flyway、AOP、Validation、Lombok 依赖。
4. 配置 `issuer-uri=http://localhost:9000`。
5. 配置数据库连接。
6. 配置 Flyway migration 路径：`classpath:db/migration`。
7. 建立基础包结构：

```text
config
controller
entity
mapper
service
dto
security
logging
storage
```

验收：

- 应用启动成功。
- 能连接 MySQL。
- 空数据库启动后 Flyway 自动建表。

### 阶段 2：数据库迁移

目标：完成业务表结构。

任务：

1. 编写 `V1__create_video.sql`。
2. 编写 `V2__create_video_like.sql`。
3. 编写 `V3__create_video_view.sql`。
4. 编写 `V4__create_request_log.sql`。

验收：

- 表结构创建成功。
- 唯一索引和查询索引存在。
- 重复启动应用不会重复执行已完成迁移。

### 阶段 3：安全骨架

目标：打通 `auth-backend` 和 `api-backend`。

任务：

1. 配置 OAuth2 Resource Server。
2. 配置接口权限规则。
3. 实现当前用户工具类，从 JWT 读取 `sub`、`preferred_username`、`scope`。
4. 实现 `GET /api/me`。

验收：

- 无 token 访问 `/api/me` 返回 401。
- 有效 token 访问 `/api/me` 成功。
- scope 不足访问对应接口返回 403。

### 阶段 4：视频上传与存储

目标：完成发布视频功能。

任务：

1. 实现 multipart 上传。
2. 校验文件类型和标题。
3. 计算 SHA-256。
4. 保存文件到 `storage/videos/`。
5. 相同 hash 文件不重复保存。
6. 写入 `video` 表。

验收：

- 上传 MP4 成功。
- 非 MP4 上传失败。
- 相同文件重复上传只保存一份实体文件。
- 视频记录中的 `uploader_id` 等于 JWT `sub`。

### 阶段 5：视频详情与播放

目标：让前端可以展示和播放视频。

任务：

1. 实现 `GET /api/videos/{id}`。
2. 实现 `GET /api/videos/{id}/play`。
3. 删除视频返回 404。
4. 播放接口返回正确 MIME 类型。

验收：

- `<video>` 标签可以播放视频。
- 删除视频不能播放。

### 阶段 6：我的视频管理

目标：完成发布者管理功能。

任务：

1. 实现 `GET /api/my/videos` 分页。
2. 实现 `DELETE /api/videos/{id}` 软删除。
3. 删除时校验 `uploader_id == JWT sub`。

验收：

- 只能看到自己的视频。
- 用户不能删除他人视频。
- 删除后不再显示在我的视频和推荐列表。

### 阶段 7：点赞模块

目标：完成点赞、防重复点赞和点赞状态查询。

任务：

1. 实现 `POST /api/videos/{id}/likes`。
2. 实现 `DELETE /api/videos/{id}/likes`。
3. 实现 `GET /api/videos/{id}/likes`。
4. 使用唯一索引防重复点赞。
5. 查询点赞状态时统计 `video_like`。

验收：

- 点赞成功后点赞数统计增加。
- 重复点赞不重复插入。
- 取消点赞后点赞数统计减少。

### 阶段 8：访问记录与推荐

目标：完成推荐主页核心功能。

任务：

1. 实现 `POST /api/videos/{id}/views`。
2. 实现 `GET /api/videos/recommendations`。
3. 推荐时排除当前用户已访问视频。
4. 推荐排序使用点赞数降序、创建时间降序。

验收：

- 推荐按点赞数降序。
- 访问过的视频不再推荐。
- 不同用户访问记录互不影响。
- 没有可推荐视频时返回空列表。

### 阶段 9：日志与耗时监控

目标：满足日志和集成监控评分点。

任务：

1. 实现 AOP 请求日志。
2. 记录输入、输出、状态码、耗时、IP、用户 ID。
3. 写入 Logback 文件日志。
4. 写入 `request_log` 表。
5. 对上传和播放接口做摘要记录。

验收：

- 普通接口能记录完整输入输出。
- 上传接口不记录文件二进制。
- 播放接口不记录视频二进制。
- 异常请求也记录耗时和状态码。

### 阶段 10：联调与文档

目标：完成整体演示闭环。

任务：

1. 联调 `frontend` 登录后携带 access token 调接口。
2. 联调推荐、播放、点赞、我的视频、删除。
3. 整理接口文档。
4. 整理数据库 ER 图。
5. 整理安全校验、日志样例、视频存储方案说明。

验收：

- 前端可完整演示作业要求。
- 文档能说明所有评分点如何实现。

## 8. 测试计划

### 8.1 安全测试

- 无 token 访问受保护接口返回 401。
- token 过期返回 401。
- scope 不足返回 403。
- 用户不能删除他人视频。
- 前端传入用户 ID 不会影响后端归属判断。

### 8.2 视频测试

- 上传 MP4 成功。
- 非 MP4 上传失败。
- 空文件上传失败。
- 相同文件重复上传只保存一份实体文件。
- 视频详情接口可用。
- 视频播放接口可用。
- 删除后视频不再出现在推荐和我的视频列表。

### 8.3 推荐测试

- 推荐按点赞数降序排序。
- 访问过的视频不再推荐。
- 没有可推荐视频时返回空列表。
- 不同用户的访问记录互不影响。

### 8.4 点赞测试

- 点赞成功后点赞数统计增加。
- 重复点赞不会重复插入记录。
- 取消点赞后点赞数统计减少。
- 未点赞时取消点赞保持稳定。

### 8.5 日志测试

- 每个普通业务接口记录输入、输出、耗时、状态码、IP、用户 ID。
- 上传接口不记录文件二进制内容。
- 播放接口不记录视频二进制内容。
- 异常请求也记录状态码和耗时。

## 9. 关键风险与处理策略

### 9.1 视频文件过大

风险：上传大文件导致请求耗时长或超过限制。

处理：

- 配置 Spring multipart 最大文件大小。
- 作业演示阶段使用较小 MP4 文件。
- 日志只记录文件摘要。

### 9.2 重复点赞并发

风险：用户快速重复点击点赞，导致重复点赞记录。

处理：

- 使用 `video_like` 唯一索引保证不会重复插入。
- 点赞和取消点赞操作放在事务中。

### 9.3 推荐结果重复

风险：用户已看过的视频仍然出现在推荐列表。

处理：

- 推荐查询必须关联 `video_view` 排除当前用户已访问视频。
- `video_view` 使用唯一索引避免重复访问记录。

### 9.4 删除权限绕过

风险：用户直接调用删除接口删除他人视频。

处理：

- 删除接口不信任前端传入用户 ID。
- 后端从 JWT `sub` 获取当前用户。
- 删除前检查 `video.uploader_id == sub`。

### 9.5 日志过大

风险：请求日志记录视频文件或视频流，导致日志膨胀。

处理：

- 上传接口只记录文件名、大小、MIME 类型。
- 播放接口只记录视频 ID、响应状态和耗时。
- 响应体过长时截断。

## 10. 完成标准

`api-backend` 完成后应该达到：

- 能作为 OAuth2 Resource Server 校验 `auth-backend` 签发的 JWT。
- 能从 JWT `sub` 获取当前用户 ID。
- 支持视频上传、本地存储、SHA-256 去重。
- 支持视频播放。
- 支持按点赞数推荐，并过滤已访问视频。
- 支持上一个/下一个所需的推荐列表数据。
- 支持点赞、防重复点赞、取消点赞和点赞状态查询。
- 支持我的视频分页查询。
- 支持用户删除自己发布的视频。
- 支持 AOP 日志，记录输入、输出、状态码、耗时、IP、用户 ID。
- 数据库结构由 Flyway 迁移文件维护。
