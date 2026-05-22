# 真实 JWT 冒烟验证说明

## 目的

这份说明用于在 `auth-backend` 已可用时，快速验证 `api-backend` 的真实 JWT 主链路。

目标不是覆盖全部业务，而是用最少步骤确认：

- `auth-backend` 的元数据和 JWK 可访问
- `api-backend` 能正确校验真实 access token
- 无 token、scope 不足、有效 token 三种典型情况表现稳定

## 前置条件

- `auth-backend` 已启动在 `http://localhost:9000`
- `api-backend` 已用默认 profile 启动在 `http://localhost:8085`
- 已拿到一个真实 Bearer access token
- 如需验证写接口，token 中应包含 `video:write`
- 如需验证读接口，token 中应包含 `video:read`

示例环境变量：

```bash
set ACCESS_TOKEN=替换成真实token
```

## 第一步：检查授权服务器元数据

验证授权服务器发现文档：

```bash
curl.exe -i http://localhost:9000/.well-known/oauth-authorization-server
```

预期：

- `200`
- 返回 JSON 中包含授权端点、token 端点等元数据

验证 JWK：

```bash
curl.exe -i http://localhost:9000/oauth2/jwks
```

预期：

- `200`
- 返回 JSON Web Key Set

如果这两步失败，优先检查 `auth-backend` 是否正常启动。

## 第二步：验证无 token 场景

```bash
curl.exe -i http://localhost:8085/api/me
```

预期：

- `401`

说明：

- 这一步用于确认资源服务器保护链已生效

## 第三步：验证有效 token 访问 `/api/me`

```bash
curl.exe -i ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  http://localhost:8085/api/me
```

预期：

- `200`
- 返回 `code = 200`
- `data.userId` 对应 token 中的 `sub`
- `data.username` 对应 token 中的 `preferred_username`
- `data.scopes` 与 token 中的 `scope` 一致

如果这里失败：

- 先检查 token 是否过期
- 再检查 `iss` 是否与 `http://localhost:9000` 一致
- 再检查 `api-backend` 是否能访问授权服务器 JWK

## 第四步：验证一个写接口

选择当前最稳定的写接口：

- `POST /api/videos`

```bash
curl.exe -i -X POST "http://localhost:8085/api/videos" ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  -F "title=Real Jwt Demo Video" ^
  -F "file=@D:\test\demo.mp4;type=video/mp4"
```

预期：

- token 含 `video:write` 时返回 `200`
- scope 不足时返回 `403`

成功时关键字段：

- `data.id`
- `data.title`
- `data.playUrl`
- `data.createdAt`

## 第五步：验证一个读接口

可选其一：

- `GET /api/videos/{id}`
- `GET /api/my/videos?page=1&size=10`

推荐先验证“我的视频”：

```bash
curl.exe -i ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  "http://localhost:8085/api/my/videos?page=1&size=10"
```

预期：

- `200`
- 返回当前用户上传的视频分页

如已知视频 ID，也可验证详情：

```bash
curl.exe -i ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  http://localhost:8085/api/videos/1
```

预期：

- token 含 `video:read` 时返回 `200` 或 `404`
- scope 不足时返回 `403`

## 第六步：验证删除接口

若 token 含 `video:write`，可验证删除自己上传的视频：

```bash
curl.exe -i -X DELETE ^
  -H "Authorization: Bearer %ACCESS_TOKEN%" ^
  http://localhost:8085/api/videos/1
```

预期：

- 删除自己上传的视频返回 `200`
- 删除他人视频返回 `403`
- 删除不存在或已删除视频返回 `404`

## 第七步：重点检查项

联调时重点核对以下内容：

- token 中 `sub` 是否映射到后端返回的 `userId`
- token 中 `preferred_username` 是否映射到后端返回的 `username`
- token 中 `scope` 是否正确影响：
  - 无 token -> `401`
  - scope 不足 -> `403`
  - 有效 token -> 成功
- 删除后：
  - `GET /api/videos/{id}` 返回 `404`
  - `GET /api/videos/{id}/play` 返回 `404`
  - `GET /api/my/videos` 中不再出现该视频

## 常见问题

### 1. `/api/me` 卡住或没有响应

优先检查：

- `auth-backend` 是否已启动
- `http://localhost:9000/oauth2/jwks` 是否可访问

当前项目约定：

- 当认证服务不可用时，`api-backend` 应快速返回 `503`

### 2. 明明带了 token 还是 `403`

优先检查：

- token 中是否真的包含目标接口要求的 scope
- 写接口通常需要 `video:write`
- 读接口通常需要 `video:read`

### 3. `/api/videos` 上传返回 `400`

优先检查：

- 标题是否为空
- 文件类型是否为 `video/mp4`
- 文件是否为空文件

## 推荐配合方式

- `auth-backend` 稳定时，优先走本说明做真实 JWT 联调
- `auth-backend` 临时不可用时，回退到 `mock-auth-usage.md` 做本地独立验证
