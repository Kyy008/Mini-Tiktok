# api-backend 登录注册代理对接方案

## 1. 目标定位

`api-backend` 在原有 Resource Server 职责外，增加一层前端友好的登录注册代理能力：

- 主责人为组员 A，因为该能力属于登录注册与 OAuth2 对接边界。
- 实现落点涉及 `api-backend` 和 `auth-backend`，组员 B 只需保证业务接口继续按 JWT `sub` 工作。
- 不自建用户表。
- 不保存密码。
- 不签发 access token。
- 用户、密码和 OAuth2 token 仍由 `auth-backend` 负责。
- 业务接口仍使用 `Authorization: Bearer <access_token>` 访问。

## 2. 端口与配置

```text
frontend:     http://localhost:5173
auth-backend: http://localhost:9000
api-backend:  http://localhost:8085
```

`api-backend` 需要配置：

```yaml
auth-backend:
  base-url: http://localhost:9000

oauth2:
  client-id: tiktok-web
  redirect-uri: http://localhost:5173/oauth/callback
  scopes:
    - video:read
    - video:write
    - video:like
```

## 3. 接口契约

### 3.1 获取登录跳转信息

```text
GET /api/auth/login-url
```

权限：

```text
permitAll
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "authorizationUrl": "http://localhost:9000/oauth2/authorize?...",
    "codeVerifier": "...",
    "state": "...",
    "clientId": "tiktok-web",
    "redirectUri": "http://localhost:5173/oauth/callback",
    "scope": "video:read video:write video:like"
  }
}
```

说明：

- `api-backend` 生成 `code_verifier`、`state` 和 S256 `code_challenge`。
- 前端保存 `codeVerifier` 和 `state` 后跳转 `authorizationUrl`。
- 后续 authorization code 换 token 仍按 OAuth2 Authorization Code + PKCE 流程执行。

### 3.2 注册代理

```text
POST /api/auth/register
Content-Type: application/json
```

请求：

```json
{
  "username": "demo2",
  "password": "Demo@123456"
}
```

权限：

```text
permitAll
```

行为：

- `api-backend` 校验请求格式。
- `api-backend` 调用 `auth-backend` 的 `POST /api/register`。
- 注册成功后返回 `auth-backend` 的用户基础信息。
- 用户名重复、参数错误、`auth-backend` 不可用时返回统一 `Result.failure`。

### 3.3 当前用户

```text
GET /api/me
Authorization: Bearer <access_token>
```

返回：

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

说明：

- `userId` 来自 JWT `sub`。
- `username` 来自 JWT `preferred_username`。
- `uploader_id` 仍使用 JWT `sub`，不因登录注册代理而变化。

## 4. 安全规则

```text
OPTIONS /**              permitAll
GET /health              permitAll
GET /api/auth/login-url  permitAll
POST /api/auth/register  permitAll
其他接口                 authenticated
```

`api-backend` 继续作为 OAuth2 Resource Server，通过 `issuer-uri` 和 JWK 校验 `auth-backend` 签发的 JWT。

## 5. 测试计划

- `GET /api/auth/login-url` 无 token 可访问。
- 登录 URL 包含 `client_id`、`redirect_uri`、`scope`、`state`、`code_challenge`、`code_challenge_method=S256`。
- `POST /api/auth/register` 无 token 可访问，并能代理 `auth-backend` 响应。
- `GET /api/me` 无 token 返回 401。
- `GET /api/me` 携带测试 JWT 时返回 `sub`、`preferred_username` 和 `scope`。
- `auth-backend` 的 `POST /api/register` 支持 JSON 注册、重复用户名错误和参数校验错误。
