# Mini-Tiktok Docker 部署说明

## 本地生产栈验证

在项目根目录执行：

```bash
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yml config
docker compose --env-file .env.prod -f docker-compose.prod.yml build --progress=plain
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

默认访问地址：

```text
http://localhost:8088
```

查看容器状态：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

查看日志：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f auth-backend api-backend nginx
```

停止服务：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

## 关键验证命令

```bash
curl -i http://localhost:8088/
curl -i http://localhost:8088/api/videos/recommendations
curl -i http://localhost:8088/oauth2/jwks
curl -i http://localhost:8088/.well-known/oauth-authorization-server
curl -i "http://localhost:8088/api/request-logs?limit=5"
```

OAuth 未登录跳转验证：

```bash
curl -i "http://localhost:8088/oauth2/authorize?response_type=code&client_id=tiktok-web&redirect_uri=http%3A%2F%2Flocalhost%3A8088%2Foauth%2Fcallback&scope=video%3Aread%20video%3Awrite%20video%3Alike&state=dev&code_challenge=abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGH&code_challenge_method=S256"
```

正常情况下响应头里的 `Location` 应该是：

```text
http://localhost:8088/login
```

## 上服务器前需要修改

复制 `.env.prod.example` 为服务器上的 `.env.prod`，至少修改：

```env
PUBLIC_ORIGIN=http://你的域名
HTTP_PORT=80
MYSQL_ROOT_PASSWORD=强密码
MYSQL_PASSWORD=强密码
```

如果后续配置 HTTPS，将 `PUBLIC_ORIGIN` 改成：

```env
PUBLIC_ORIGIN=https://你的域名
```

生产环境 MySQL 默认不暴露到公网，只供容器内部访问。视频文件和上传临时文件分别通过 Docker volume 持久化保存。

## GitHub Actions CI/CD

仓库提供两个部署入口：

- `docker-compose.prod.yml`：本地或服务器手动构建镜像并启动。
- `docker-compose.deploy.yml`：CI/CD 使用 GHCR 镜像启动，不在服务器上构建。

服务器固定部署目录：

```text
/home/kyy008/projects/tiktok
```

GitHub Actions 需要配置以下 Repository Secrets：

```text
SERVER_HOST=39.102.59.66
SERVER_USER=kyy008
SERVER_PORT=22
SERVER_SSH_KEY=用于登录服务器的 SSH 私钥
```

工作流使用 GitHub Actions 内置的 `GITHUB_TOKEN` 推送镜像，并在部署步骤中临时登录 GHCR 拉取镜像，不需要额外配置 GHCR PAT。

每次 push 到 `main` 后，工作流会：

```text
构建 frontend/api-backend/auth-backend 镜像
推送到 ghcr.io/kyy008
同步 docker-compose.deploy.yml、Nginx 配置、MySQL 初始化 SQL 到服务器
在服务器执行 docker compose pull && docker compose up -d
```
