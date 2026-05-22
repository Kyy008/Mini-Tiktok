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

### 4. 用 `mock-video-read` 访问 `/api/videos/{id}`

```bash
curl.exe -i -H "Authorization: Bearer mock-video-read" http://localhost:8085/api/videos/1
```

预期：`200` 或 `404`

关键点：

- 有数据时返回 `code = 200`
- 视频不存在或已删除时返回 `404`

### 5. 用 `mock-video-read` 播放视频

```bash
curl.exe -i -H "Authorization: Bearer mock-video-read" http://localhost:8085/api/videos/1/play
```

预期：`200` 或 `404`

关键点：

- 成功时 `Content-Type = video/mp4`
- 已删除或缺失文件时返回 `404`

### 6. 用 `mock-video-read` 查看“我的视频”

```bash
curl.exe -i -H "Authorization: Bearer mock-video-read" "http://localhost:8085/api/my/videos?page=1&size=10"
```

预期：`200`

关键返回字段：

- `data.records`
- `data.page`
- `data.size`
- `data.total`

### 7. 用 `mock-video-read` 直接上传视频

```bash
curl.exe -i -X POST "http://localhost:8085/api/videos" ^
  -H "Authorization: Bearer mock-video-read" ^
  -F "title=Demo Video" ^
  -F "file=@D:\test\demo.mp4;type=video/mp4"
```

预期：`403`

### 8. 用 `mock-video-write` 直接上传视频

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

### 9. 用 `mock-video-write` 删除自己上传的视频

```bash
curl.exe -i -X DELETE ^
  -H "Authorization: Bearer mock-video-write" ^
  http://localhost:8085/api/videos/1
```

预期：`200`

删除后可继续验证：

```bash
curl.exe -i -H "Authorization: Bearer mock-video-write" http://localhost:8085/api/videos/1
curl.exe -i -H "Authorization: Bearer mock-video-write" "http://localhost:8085/api/my/videos?page=1&size=10"
```

关键点：

- 详情接口应返回 `404`
- “我的视频”列表中不再出现该视频

### 10. 用 `mock-video-read` 删除视频

```bash
curl.exe -i -X DELETE ^
  -H "Authorization: Bearer mock-video-read" ^
  http://localhost:8085/api/videos/1
```

预期：`403`

### 11. 初始化断点续传会话

```bash
curl.exe -i -X POST "http://localhost:8085/api/video-uploads/init" ^
  -H "Authorization: Bearer mock-video-write" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Demo Video\",\"fileName\":\"demo.mp4\",\"fileSize\":12,\"contentType\":\"video/mp4\",\"chunkSize\":5,\"totalChunks\":3,\"fileHash\":\"hash123\"}"
```

预期：`200`

关键返回字段：

- `data.uploadId`
- `data.nextChunkIndex`
- `data.uploadedBytes`
- `data.totalChunks`
- `data.status`

### 12. 查询断点续传状态

```bash
curl.exe -i -H "Authorization: Bearer mock-video-write" http://localhost:8085/api/video-uploads/upload123
```

预期：`200` 或 `404`

关键点：

- 会话存在时返回当前 `nextChunkIndex` 和 `uploadedBytes`
- 会话不存在时返回 `404`

### 13. 上传单个分片

```bash
curl.exe -i -X PUT "http://localhost:8085/api/video-uploads/upload123/chunks/0" ^
  -H "Authorization: Bearer mock-video-write" ^
  -H "Content-Type: application/octet-stream" ^
  --data-binary "@D:\test\chunk-0.bin"
```

预期：`200`

关键返回字段：

- `data.uploadId`
- `data.nextChunkIndex`
- `data.uploadedBytes`
- `data.completed`

### 14. 完成断点续传上传

```bash
curl.exe -i -X POST "http://localhost:8085/api/video-uploads/upload123/complete" ^
  -H "Authorization: Bearer mock-video-write"
```

预期：`200` 或 `409`

关键点：

- 所有分片已上传时返回最终视频信息
- 状态不完整时返回 `409`

### 15. 用 `mock-video-read` 访问断点续传接口

```bash
curl.exe -i -X POST "http://localhost:8085/api/video-uploads/init" ^
  -H "Authorization: Bearer mock-video-read" ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Demo Video\",\"fileName\":\"demo.mp4\",\"fileSize\":12,\"contentType\":\"video/mp4\",\"chunkSize\":5,\"totalChunks\":3,\"fileHash\":\"hash123\"}"
```

预期：`403`

## 自动测试

验证正式模式回归：

```bash
cd api-backend
mvn -q "-Dtest=UserControllerTest,VideoControllerTest,VideoUploadControllerTest,VideoStorageServiceTest,VideoServiceTest" test
```

验证 `mock-auth` 模式：

```bash
cd api-backend
mvn -q "-Dtest=MockAuthModeUserControllerTest,MockAuthModeVideoControllerTest,MockAuthModeVideoUploadControllerTest" test
```
