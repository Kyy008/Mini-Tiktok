# Mini-Tiktok 登录注册流程说明

## 1. 参与模块

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

- `frontend`：负责提供注册入口、生成 PKCE 参数、保存临时登录状态、处理 OAuth2 回调和保存 access token。
- `auth-backend`：负责用户注册登录、OAuth2 授权、签发 JWT access token、暴露 JWK 公钥。
- `api-backend`：只作为 Resource Server 校验 Bearer JWT，不参与登录注册中转。

边界：

- 用户账号、密码和 token 签发都归 `auth-backend`。
- 注册页面、登录页面和 token 签发都由 `auth-backend` 负责。
- `api-backend` 不保存密码，不签发 token，不生成 PKCE 参数，不代理注册。
- 业务数据中的 `uploader_id` 使用 JWT `sub`。

## 2. 注册流程

```text
1. 用户在 frontend 点击注册
2. 浏览器跳转到:
   GET http://localhost:9000/register
3. auth-backend 返回注册 HTML 页面
4. 用户在 auth-backend 注册页面输入 username/password
5. 浏览器提交表单:
   POST http://localhost:9000/register
6. auth-backend 校验用户名、密码和 CSRF token
7. auth-backend 使用 BCrypt 保存 password_hash
8. 注册成功后重定向到:
   GET http://localhost:9000/login?registered
```

请求示例：

```http
POST /register HTTP/1.1
Host: localhost:9000
Content-Type: application/x-www-form-urlencoded

username=demo2&password=Demo@123456&_csrf=...
```

异常情况：

- 用户名长度不合法或密码过短：`auth-backend` 重新渲染注册页并展示字段错误。
- 用户名重复：`auth-backend` 重新渲染注册页并展示错误。
- `auth-backend` 不可用：浏览器无法打开注册页。

## 3. 登录流程

登录使用 OAuth2 Authorization Code + PKCE。

```text
1. 用户点击 frontend 登录按钮
2. frontend 生成:
   code_verifier
   code_challenge = BASE64URL(SHA256(code_verifier))
   state
3. frontend 将 code_verifier 和 state 保存到 sessionStorage
4. frontend 跳转 auth-backend:
   GET http://localhost:9000/oauth2/authorize
5. auth-backend 检查用户是否已登录
6. 未登录时展示 auth-backend 的 /login 页面
7. 用户提交账号密码
8. 浏览器提交 `POST /login`
9. Spring Security 表单登录 filter 调用 `DatabaseUserDetailsService` 查询 `users`
10. BCrypt 校验密码成功后建立 auth-backend 登录 session
11. auth-backend 继续原授权请求并生成 authorization code
12. 浏览器回跳 frontend:
   http://localhost:5173/oauth/callback?code=xxx&state=xxx
```

说明：`POST /login` 不是业务 Controller 方法，而是 Spring Security `formLogin()` 自动注册的登录处理端点。项目里的 `AuthPageController` 只负责 `GET /login` 返回 HTML 页面。

授权 URL 示例：

```text
http://localhost:9000/oauth2/authorize
  ?response_type=code
  &client_id=tiktok-web
  &redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fcallback
  &scope=video%3Aread%20video%3Awrite%20video%3Alike
  &state=...
  &code_challenge=...
  &code_challenge_method=S256
```

## 4. 回调换 Token

```text
1. frontend 进入 /oauth/callback
2. frontend 校验 URL 中的 state 是否等于 sessionStorage 中保存的 state
3. frontend 读取 sessionStorage 中的 code_verifier
4. frontend 调用 auth-backend:
   POST http://localhost:9000/oauth2/token
5. auth-backend 校验 authorization code、redirect_uri 和 PKCE code_verifier
6. auth-backend 签发 JWT access token
7. frontend 保存 access_token
8. frontend 清理 sessionStorage 中的 code_verifier 和 state
9. frontend 调用 api-backend 的 /api/me 获取当前用户
```

换 token 请求示例：

```http
POST /oauth2/token HTTP/1.1
Host: localhost:9000
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
client_id=tiktok-web&
redirect_uri=http://localhost:5173/oauth/callback&
code=xxx&
code_verifier=xxx
```

JWT access token 至少包含：

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

## 5. 访问业务接口

登录成功后，frontend 调用 `api-backend` 业务接口时携带 Bearer token：

```http
GET /api/me HTTP/1.1
Host: localhost:8085
Authorization: Bearer <access_token>
```

`api-backend` 的处理逻辑：

```text
1. 从 Authorization header 读取 Bearer token
2. 根据 issuer-uri 发现 auth-backend 元数据
3. 通过 /oauth2/jwks 获取 JWK 公钥
4. 校验 JWT 签名、issuer、过期时间
5. 从 JWT 读取 sub、preferred_username、scope
6. 将 scope 转成 SCOPE_video:read 等 Spring Security 权限
7. 业务接口使用 sub 作为当前用户 ID
```

`GET /api/me` 成功响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "1",
    "username": "demo",
    "scopes": ["video:read", "video:write", "video:like"]
  }
}
```

## 6. 权限和失败场景

- 无 token 访问受保护接口：`401 Unauthorized`。
- token 过期或签名错误：`401 Unauthorized`。
- scope 不足：`403 Forbidden`。
- `redirect_uri` 与 client 注册值不一致：授权失败。
- `state` 与 sessionStorage 中保存的值不一致：前端拒绝换 token。
- `code_verifier` 与 `code_challenge` 不匹配：换 token 失败。
- authorization code 重复使用：换 token 失败。

## 7. Review 覆盖点

- 浏览器访问 `GET /register` 获取 auth-backend HTML 页面，再提交 `POST /register` 表单。
- PKCE 参数由 frontend 生成并写入 `sessionStorage`；`api-backend` 不生成 `code_verifier`、`code_challenge` 或 `state`。
- frontend 换到 access token 后立即请求 `api-backend` 的 `GET /api/me`，确认当前登录用户并保存用户信息。
- auth-backend 使用 users 实体、UserMapper、BCrypt、表单登录页和注册页完成本地账号能力，`POST /login` 由 Spring Security filter 处理，`demo / Demo@123456` 可用于验证。
- 测试覆盖数据库连接探测、`UserService.findByUsername` 用户查找、`JdbcRegisteredClientRepository.findByClientId("tiktok-web")` client 查找。
- auth-backend 使用 Spring Authorization Server、JDBC client、Authorization Code + PKCE、JWT、JWK 和 issuer `http://localhost:9000` 签发并暴露可验签 token。

## 8. 流程图

```mermaid
sequenceDiagram
    participant F as frontend
    participant AUTH as auth-backend
    participant API as api-backend

    F->>AUTH: 跳转 /register
    AUTH-->>F: 返回注册 HTML
    F->>AUTH: POST /register 表单
    AUTH-->>F: 重定向 /login?registered
    F->>F: 生成 code_verifier/code_challenge/state
    F->>F: 保存 code_verifier/state
    F->>AUTH: 跳转 /oauth2/authorize
    AUTH->>AUTH: 检查登录态
    AUTH-->>F: 未登录则展示 /login
    F->>AUTH: 提交账号密码
    AUTH-->>F: 回调 /oauth/callback?code&state
    F->>F: 校验 state
    F->>AUTH: POST /oauth2/token + code_verifier
    AUTH-->>F: access_token
    F->>API: Authorization: Bearer access_token
    API->>AUTH: 通过 JWK 验证 JWT
    API-->>F: 业务响应
```
