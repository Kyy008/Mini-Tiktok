# mock-auth 本地使用说明

## 用途

`mock-auth` 是 `api-backend` 的本地开发专用模式。

启用后：

- 不依赖 `auth-backend`
- 不访问 `issuer-uri` 或 `jwk-set-uri`
- 仍然走 Bearer token + Spring Security + `JwtAuthenticationToken`
- 可以直接用固定 token 手工调用当前已实现的受保护接口

默认模式不变，只有显式启用 `mock-auth` profile 才会进入本地 mock 鉴权。

## 启动方式

```bash
cd api-backend
mvn spring-boot:run "-Dspring-boot.run.profiles=mock-auth"
```

## 可用 token

- `mock-video-write`
- `mock-video-read`
- `mock-video-like`
- `mock-all`

## token 对应用户

- `mock-video-write`
  - `sub = local-uploader`
  - `preferred_username = local_uploader`
  - `scope = video:write`
- `mock-video-read`
  - `sub = local-reader`
  - `preferred_username = local_reader`
  - `scope = video:read`
- `mock-video-like`
  - `sub = local-liker`
  - `preferred_username = local_liker`
  - `scope = video:like`
- `mock-all`
  - `sub = local-admin`
  - `preferred_username = local_admin`
  - `scope = video:read video:write video:like`

## 手工验证

### 1. 健康检查

```bash
curl.exe -i http://localhost:8085/health
```

预期：`200`

### 2. 未带 token 访问 `/api/me`

```bash
curl.exe -i http://localhost:8085/api/me
```

预期：`401`

### 3. 用 `mock-all` 访问 `/api/me`

```bash
curl.exe -i -H "Authorization: Bearer mock-all" http://localhost:8085/api/me
```

预期：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "local-admin",
    "username": "local_admin",
    "scopes": [
      "video:read",
      "video:write",
      "video:like"
    ]
  }
}
```

### 4. 用 `mock-video-read` 访问 `/api/me`

```bash
curl.exe -i -H "Authorization: Bearer mock-video-read" http://localhost:8085/api/me
```

预期：`200`

### 5. 用 `mock-video-read` 上传视频

```bash
curl.exe -i -X POST "http://localhost:8085/api/videos" ^
  -H "Authorization: Bearer mock-video-read" ^
  -F "title=Demo Video" ^
  -F "file=@D:\test\demo.mp4;type=video/mp4"
```

预期：`403`

### 6. 用 `mock-video-write` 上传视频

```bash
curl.exe -i -X POST "http://localhost:8085/api/videos" ^
  -H "Authorization: Bearer mock-video-write" ^
  -F "title=Demo Video" ^
  -F "file=@D:\test\demo.mp4;type=video/mp4"
```

预期：`200`

返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Demo Video",
    "playUrl": "/api/videos/1/play",
    "createdAt": "2026-05-20T12:00:00"
  }
}
```

## 自动测试

验证正式模式回归：

```bash
cd api-backend
mvn -q "-Dtest=UserControllerTest,VideoControllerTest,VideoStorageServiceTest" test
```

验证 mock-auth 模式：

```bash
cd api-backend
mvn -q "-Dtest=MockAuthModeUserControllerTest,MockAuthModeVideoControllerTest" test
```
