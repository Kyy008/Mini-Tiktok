# Mini-Tiktok 主链路修复计划

## Summary

按“主链路优先”修复：登录注册 OAuth 闭环、游客可浏览推荐、API 权限一致性、点赞契约、视频播放体验、上传接口风险。评论、朋友、消息、完整用户资料等占位功能本轮不扩展，只保留为后续任务。

## Key Fixes

1. 修复注册后不能自动完成 OAuth 登录
   - 前端注册入口不再单纯跳 `/register`，而是先创建并保存 PKCE/state，再携带当前授权上下文进入授权服务注册页。
   - `auth-backend` 注册成功后回到登录页时保留原 OAuth saved request；用户继续登录后应正常回调 `/oauth/callback?code=...&state=...`。
   - 验收：从前端点“注册”创建新用户后，继续登录能拿到 access token，并回到原 redirect 页面。

2. 支持游客浏览首页推荐
   - 后端放开只读视频接口：`GET /api/videos/recommendations`、`GET /api/videos/{id}`、`GET /api/videos/{id}/play`，游客返回 `liked=false`。
   - 推荐逻辑支持无登录用户：游客按点赞数和创建时间推荐，不做“已观看过滤”。
   - 前端未登录时不调用需要登录的行为接口：不上报观看记录、不请求或不依赖个人点赞状态；点赞、上传、我的作品仍跳转登录。
   - 验收：无 token 打开 `http://localhost:5173` 可看到推荐视频并播放；点点赞或发布时进入 OAuth 登录。

3. 补齐 API scope 权限规则
   - `/api/my/videos` 明确要求 `SCOPE_video:read`，不再只靠 `authenticated()`。
   - 上传分片、删除作品继续要求 `video:write`；点赞/取消点赞继续要求 `video:like`。
   - 同步更新 mock-auth 安全配置，避免测试环境和真实环境行为不一致。
   - 验收：缺少对应 scope 的 token 访问接口返回 403；测试覆盖 `/api/my/videos` 权限。

4. 统一点赞接口返回契约
   - `POST /api/videos/{id}/likes` 和 `DELETE /api/videos/{id}/likes` 返回 `VideoLikeStatusResponse`，包含 `videoId`、`likeCount`、`liked`。
   - 前端取消“成功后再额外查一次状态”的依赖，直接用接口返回值更新 UI；失败时回滚乐观更新。
   - 验收：点赞/取消点赞后数字立即准确，刷新推荐/详情后状态一致。

5. 修复视频播放 Range 支持
   - `/api/videos/{id}/play` 解析 `Range` 请求，返回 `206 Partial Content`、`Content-Range`、正确的 `Content-Length`。
   - 无 Range 时仍返回完整 MP4；不存在或已删除视频返回 404。
   - 验收：浏览器视频拖动进度条可正常继续加载；curl Range 请求返回 206。

6. 处理旧版整文件上传内存风险
   - 推荐方案：前端继续只使用分片上传，后端将 `POST /api/videos` 标记为兼容接口并限制更小文件，或直接移除/禁用该接口。
   - 若保留接口，改为流式写入并计算 hash，避免 `file.getBytes()` 一次性读 100MB。
   - 验收：分片上传仍正常；旧接口不会因大文件导致明显内存风险。

## Test Plan

- 后端：跑 `auth-backend mvn test`、`api-backend mvn test`，新增游客推荐、scope 权限、点赞返回值、Range 播放测试。
- 前端：跑 `npm run build`，手测游客首页、登录、注册后登录、点赞、上传、我的作品、退出登录。
- 联调：用真实 OAuth2 流程验证 `demo / Demo@123456` 和新注册用户都能拿 token 并访问 API。

## Assumptions

- 首页采用“游客可浏览”策略。
- 本轮只修主链路；评论、朋友、消息、完整用户资料和推荐算法增强不纳入本轮实现。
- 继续保持 OAuth2 Authorization Code + PKCE 架构，不改成 Vue 原生账号密码登录。
