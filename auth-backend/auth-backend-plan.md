# Mini-Tiktok 鉴权服务器方案

## 1. 目标定位

`auth-backend` 是 Mini-Tiktok 项目的 OAuth2 鉴权服务器，负责用户注册、登录、授权、签发 JWT Access Token、暴露 JWK 公钥，以及为普通业务后端 `api-backend` 提供标准化的 token 校验基础。

本模块不是简单自定义 JWT 登录，而是使用成熟安全框架实现 OAuth2 授权能力：

- 使用 Spring Authorization Server 搭建授权服务器。
- 使用 Authorization Code + PKCE 支持 Vue 3 单页应用登录。
- 使用 RSA 私钥签发 JWT Access Token。
- 暴露 JWK Set，让 `api-backend` 作为 Resource Server 校验 token。
- 将用户、OAuth2 client、授权记录持久化到 MySQL。


## 2. 技术栈

### 2.1 基础技术

- Java 17
- Spring Boot 3.5.x
- Spring Security 6
- Spring Authorization Server 1.5.x
- MySQL 8
- MyBatis-Plus
- Flyway
- Lombok
- Spring Validation
- Thymeleaf 或 Spring MVC 模板
- Maven

### 2.2 安全技术

- OAuth2 Authorization Server
- Authorization Code Flow
- PKCE
- JWT Access Token
- RSA key pair
- JWK Set
- BCrypt 密码哈希
- Spring Security Session 登录态

## 3. OAuth2 登录流程

前端 `frontend` 使用 Authorization Code + PKCE：

```text
1. 用户点击登录
2. 前端生成 code_verifier 和 code_challenge
3. 前端跳转到 auth-backend:
   GET http://localhost:9000/oauth2/authorize
4. auth-backend 检查用户是否已登录
5. 未登录则跳转 /login
6. 用户登录成功后，auth-backend 生成 authorization code
7. 浏览器跳回:
   http://localhost:5173/oauth/callback?code=xxx
8. 前端用 code + code_verifier 请求 /oauth2/token
9. auth-backend 校验 PKCE 后签发 access_token
10. 前端调用 api-backend 时携带:
    Authorization: Bearer <access_token>
11. api-backend 通过 auth-backend 的 JWK 公钥校验 token
```

本地端口约定：

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

开发环境端口选择：

- `5173`：Vite 前端开发服务器。
- `9000`：OAuth2 授权服务器，作为 JWT issuer。
- `8085`：普通业务后端。

## 4. OAuth2 Client 设计

系统 v1 只注册一个前端 client。

```text
client_id: tiktok-web
client_authentication_method: none
authorization_grant_type: authorization_code
redirect_uri: http://localhost:5173/oauth/callback
scope:
  - video:read
  - video:write
  - video:like
require_proof_key: true
require_authorization_consent: false
```

说明：

- `client_authentication_method = none` 表示这是 public client。
- `require_proof_key = true` 强制使用 PKCE。
- `require_authorization_consent = false` 表示课程项目中登录后直接授权，不额外展示同意页。
- `video:*` scopes 用于业务后端做接口权限控制。

## 5. 数据库设计

### 5.1 用户表

```sql
users(
  id bigint primary key auto_increment,
  username varchar(64) not null unique,
  password_hash varchar(255) not null,
  enabled boolean not null default true,
  created_at datetime not null,
  updated_at datetime not null
)
```

不区分管理员和普通用户。业务后端的权限控制通过“是否登录”和“资源归属”完成，例如用户只能删除自己发布的视频。

### 5.2 OAuth2 官方表

使用 Flyway 管理数据库版本，迁移文件放在 `src/main/resources/db/migration/`。用户表和 Spring Authorization Server 官方表都通过版本化 SQL 创建，不依赖手工导入数据库。

建议迁移顺序：

```text
V1__create_users.sql
V2__create_oauth2_authorization_server_tables.sql
V3__insert_tiktok_web_client.sql
V4__insert_demo_user.sql
```

Spring Authorization Server 官方表不自行设计字段：

```text
oauth2_registered_client
oauth2_authorization
oauth2_authorization_consent
```

用途：

- `oauth2_registered_client`：保存 `tiktok-web` client 配置。
- `oauth2_authorization`：保存 authorization code、access token 等授权数据。
- `oauth2_authorization_consent`：保存授权同意记录，v1 可保留表但不主动使用。

## 6. JWT Token 设计

`auth-backend` 使用 RSA 私钥签发 JWT Access Token，`api-backend` 使用 JWK 公钥验证。

Access Token 有效期：

```text
2 hours
```

JWT claims 至少包含：

```json
{
  "iss": "http://localhost:9000",
  "sub": "1",
  "preferred_username": "demo",
  "scope": "video:read video:write video:like",
  "exp": 1710000000,
  "iat": 1709992800
}
```

字段说明：

- `iss`：签发者，必须等于 `http://localhost:9000`。
- `sub`：用户 ID，业务后端以此识别当前用户。
- `preferred_username`：用户名，用于前端展示或日志记录。
- `scope`：OAuth2 权限范围，业务接口按 scope 控制。
- `exp`：过期时间。
- `iat`：签发时间。

## 7. 需要实现的接口

### 7.1 自定义页面与用户接口

```text
GET  /login
POST /login
GET  /register
POST /register
POST /logout
GET  /users/me
```

接口说明：

- `GET /login`：展示登录页。
- `POST /login`：由 Spring Security `formLogin()` 的 `UsernamePasswordAuthenticationFilter` 处理，不在 Controller 中手写；它调用 `DatabaseUserDetailsService` 从 `users` 表按用户名查找用户，并用 `BCryptPasswordEncoder` 校验密码。
- `GET /register`：展示注册页。
- `POST /register`：创建用户，密码使用 BCrypt 加密。
- `POST /logout`：注销当前 auth server session。
- `GET /users/me`：返回当前已登录用户基础信息，可选，用于调试和前端展示。

### 7.2 Spring Authorization Server 标准端点

这些端点由 Spring Authorization Server 提供，不手写业务逻辑：

```text
GET  /oauth2/authorize
POST /oauth2/token
GET  /oauth2/jwks
GET  /.well-known/oauth-authorization-server
```

端点说明：

- `/oauth2/authorize`：授权端点，生成 authorization code。
- `/oauth2/token`：token 端点，用 code + code_verifier 换 access token。
- `/oauth2/jwks`：暴露 JWK 公钥集合。
- `/.well-known/oauth-authorization-server`：OAuth2 授权服务器元数据。

## 8. 权限与安全策略

### 8.1 页面权限

```text
/login                 permitAll
/register              permitAll
/assets/**             permitAll
/oauth2/**             交给 Authorization Server filter chain
/.well-known/**         permitAll
/users/me              authenticated
```

### 8.2 密码策略

注册校验：

- 用户名不能为空，建议 3 到 32 位。
- 密码不能为空，建议至少 6 位。
- 用户名唯一。

存储策略：

- 不保存明文密码。
- 使用 BCrypt 保存 `password_hash`。

### 8.3 默认用户

开发环境启动时初始化：

```text
demo / Demo@123456
```

如果数据库中已存在同名用户，不重复创建。

## 9. 开发顺序

### 阶段 1：项目初始化

目标：让 `auth-backend` 能启动。

任务：

1. 创建 Spring Boot 3 项目。
2. 配置端口 `9000`。
3. 添加 MySQL、MyBatis-Plus、Flyway、Spring Security、Authorization Server、Validation、Thymeleaf 依赖。
4. 配置数据库连接。
5. 配置 Flyway migration 路径：`classpath:db/migration`。
6. 建立基础包结构：

```text
config
controller
entity
mapper
service
dto
security
```

验收：

- 应用启动成功。
- 能连接 MySQL。
- Flyway 启动时能自动执行数据库迁移。
- 测试环境通过 `JdbcTemplate` 执行连接探测，并验证 `UserService.findByUsername`、`JdbcRegisteredClientRepository.findByClientId` 查询可用。

### 阶段 2：用户模块

目标：完成本地账号系统。

任务：

1. 编写 `V1__create_users.sql` 创建 `users` 表。
2. 实现 User 实体。
3. 实现 UserMapper。
4. 实现用户注册服务。
5. 实现根据用户名加载用户的 UserDetailsService。
6. 使用 BCryptPasswordEncoder。
7. 初始化 `demo` 测试用户。

验收：

- 注册用户成功。
- 密码入库为 BCrypt hash。
- 用户名重复会报错。
- 禁用用户不能登录。
- `DatabaseUserDetailsService` 能从 `users` 表查到用户并交给 `POST /login` 表单登录流程验证。

### 阶段 3：登录与注册页面

目标：让用户能通过浏览器完成登录和注册。

任务：

1. 实现 `GET /login` 页面。
2. 实现 `GET /register` 页面。
3. 实现 `POST /register`。
4. 配置 Spring Security formLogin。
5. 配置 logout。

验收：

- 访问 `/login` 可以看到登录页。
- 登录成功后进入原本请求的 OAuth2 授权流程。
- 注册成功后可以登录。
- `/logout` 后 session 失效。

### 阶段 4：Authorization Server 基础配置

目标：让 OAuth2 标准端点可用。

任务：

1. 配置 Authorization Server SecurityFilterChain。
2. 配置默认授权服务器端点。
3. 配置 issuer：`http://localhost:9000`。
4. 配置 RSA key pair。
5. 配置 JWKSource。
6. 配置 JwtDecoder。

验收：

- `GET /.well-known/oauth-authorization-server` 可访问。
- `GET /oauth2/jwks` 可访问并返回公钥。

### 阶段 5：注册 OAuth2 Client

目标：让 Vue 前端作为 public client 接入。

任务：

1. 编写 `V2__create_oauth2_authorization_server_tables.sql` 创建官方 OAuth2 表。
2. 配置 JDBC RegisteredClientRepository。
3. 编写 `V3__insert_tiktok_web_client.sql` 初始化 `tiktok-web` client。
4. 配置 redirect uri。
5. 配置 scopes。
6. 配置 `ClientAuthenticationMethod.NONE`。
7. 配置 `requireProofKey(true)`。

验收：

- 数据库中存在 `tiktok-web` client。
- 重启应用不会重复插入 client 数据。
- 访问 `/oauth2/authorize` 时 client 校验通过。
- 未带 PKCE 时授权失败。

### 阶段 6：JWT Claims 定制

目标：让业务后端能拿到用户 ID 和用户名。

任务：

1. 实现 OAuth2TokenCustomizer。
2. 在 access token 中写入 `sub`。
3. 写入 `preferred_username`。
4. 保留 `scope`。

验收：

- 解码 JWT 后能看到 `sub`、`preferred_username`、`scope`。
- `api-backend` 可以用 `sub` 关联业务用户。

### 阶段 7：PKCE 完整联调

目标：前端能拿到 access token。

任务：

1. 使用浏览器访问授权地址。
2. 登录用户。
3. 确认回调到 `http://localhost:5173/oauth/callback?code=...`。
4. 使用 code + code_verifier 调 `/oauth2/token`。
5. 获得 access token。

验收：

- 正确 code_verifier 能换取 token。
- 错误 code_verifier 换 token 失败。
- 错误 redirect_uri 失败。
- 重复使用 authorization code 失败。

### 阶段 8：与 api-backend 联调

目标：普通后端能验证鉴权服务器签发的 token。

任务：

1. `api-backend` 配置 issuer-uri：

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

2. 使用 access token 请求 `api-backend` 的测试接口。
3. 验证 token 过期、scope 不足、无 token 的表现。

验收：

- 携带有效 token 能访问受保护接口。
- 无 token 返回 401。
- scope 不足返回 403。
- 过期 token 返回 401。

### 阶段 9：文档与答辩材料

目标：让 OAuth2 加分点讲得清楚。

任务：

1. 绘制 OAuth2 登录流程图。
2. 记录 client 配置。
3. 记录 token 示例。
4. 记录 JWK 验签机制。
5. 对比学长项目：自定义鉴权服务 vs 标准 OAuth2 授权服务器。

验收：

- 文档能解释为什么这是成熟安全方案。
- 文档能解释 Authorization Server 和 Resource Server 的分工。

## 10. 测试计划

### 10.1 用户测试

- 空数据库启动后 Flyway 自动创建用户表和 OAuth2 表。
- 重复启动应用不会重复执行已完成迁移。
- 数据库连接探测成功。
- `UserService.findByUsername` 能从 `users` 表查到用户。
- `JdbcRegisteredClientRepository.findByClientId("tiktok-web")` 能查到 OAuth2 client。
- 注册成功。
- `POST /register` 表单注册成功后密码入库为 BCrypt hash，不保存明文。
- 用户名重复注册失败。
- 密码过短注册失败。
- `demo / Demo@123456` 可以通过表单登录验证。
- `POST /login` 经 Spring Security 表单登录处理后登录成功。
- 密码错误登录失败。
- 禁用用户登录失败。
- 注销后访问需登录页面会重新跳转登录。

### 10.2 OAuth2 测试

- 未登录访问 `/oauth2/authorize` 会跳转 `/login`。
- 登录后能获得 authorization code。
- 正确 PKCE 能通过真实 `/oauth2/authorize` + `/oauth2/token` 链路换 access token。
- 错误 PKCE 不能换 token。
- 错误 redirect uri 不能授权。
- 错误 client id 不能授权。
- authorization code 只能使用一次。
- JDBC `RegisteredClientRepository` 中的 `tiktok-web` client 支持 `authorization_code`、`ClientAuthenticationMethod.NONE` 和 `requireProofKey(true)`。

### 10.3 Token 测试

- access token 是 JWT。
- JWT 签名可通过 `/oauth2/jwks` 验证。
- JWT issuer 正确。
- JWT 过期时间正确。
- JWT 包含 scope。
- JWT 包含用户 ID 和用户名。
- 解码真实签发的 access token 后，能看到 `sub`、`preferred_username`、`scope`。

### 10.4 与 api-backend 联调测试

- `api-backend` 能通过 issuer-uri 发现 JWK。
- 有效 token 能访问业务接口。
- 无 token 返回 401。
- 过期 token 返回 401。
- scope 不足返回 403。

## 11. 关键风险与处理策略

### 11.1 PKCE 调试复杂

风险：前端生成的 `code_verifier` 和 `code_challenge` 不匹配会导致 token 交换失败。

处理：

- 前端统一封装 PKCE 工具。
- 调试时打印 `code_verifier`，但不要在正式日志中输出。
- 使用 S256 方法，不使用 plain。

### 11.2 redirect_uri 必须完全一致

风险：`http://localhost:5173/oauth/callback` 和 `http://127.0.0.1:5173/oauth/callback` 会被视为不同地址。

处理：

- 开发期统一使用 `localhost`。
- 文档、前端配置、数据库 client 配置保持一致。

### 11.3 JWK/RSA 密钥重启变化

风险：如果每次启动都生成新 RSA key，旧 token 会全部失效。

处理：

- 开发期可以接受重启后 token 失效。
- 答辩前固定一份 RSA key pair。
- 后续可改为从 JKS 或 PEM 文件加载密钥。

### 11.4 scope 和资源归属边界混乱

风险：只检查 scope，不检查资源归属，可能导致用户删除或管理他人的视频。

处理：

- scope 控制资源操作能力，如 `video:read`、`video:write`、`video:like`。
- 资源归属由 `api-backend` 根据 JWT `sub` 和业务表 `uploader_id` 校验。
- 删除视频、我的视频列表、已访问记录、点赞记录都以当前用户 `sub` 为准。

### 11.5 JDBC 授权记录反序列化

风险：Spring Authorization Server 的 `JdbcOAuth2AuthorizationService` 会把登录 principal 存入 `oauth2_authorization.attributes`。如果使用自定义 `AuthUserPrincipal`，换 token 时反序列化不在 Spring Security Jackson allowlist 内会失败。

处理：

- `AuthUserPrincipal` 显式添加 Jackson 类型信息和创建器。
- 忽略 `authorities`、账号未过期等可计算字段，只持久化 `id`、`username`、`password`、`enabled`。
- 使用真实 Authorization Code + PKCE 测试覆盖 `/oauth2/authorize`、JDBC authorization 保存、`/oauth2/token` 换 JWT。

## 12. 完成标准

`auth-backend` 完成后应该达到：

- 支持用户注册、登录、注销。
- 支持 OAuth2 Authorization Code + PKCE。
- 能签发 JWT Access Token。
- 能暴露 JWK 公钥集合。
- JWT 中包含用户 ID、用户名、scope。
- `api-backend` 能通过 `issuer-uri` 验证 token。
- client 配置、OAuth2 流程、token 示例都有文档可用于答辩。
