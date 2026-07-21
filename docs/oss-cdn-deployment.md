# 阿里云 OSS 与 CDN 部署指南

更新日期：2026-07-15

## 当前实现边界

项目使用统一 `ObjectStorage` 接口，默认 `local`，生产可切换为 `oss`。当前上传仍经过后端 `/api/files`：后端继续负责登录校验、限流、文件大小、扩展名、MIME 和文件头检查，再把文件写入本地目录或 OSS。因此本地开发不需要 OSS 账号，前端也不需要 AccessKey。

对象键保持稳定：

```text
avatar/{userId}/{uuid}.{ext}
combo-video/{userId}/{uuid}.{ext}
```

数据库当前保存完整公开 URL。不要把带过期时间的签名 URL 写入数据库。若以后增加私有审核区或前端直传，应先增加独立资源表并只保存 object key。

## 本地开发

保持以下配置即可：

```properties
STORAGE_PROVIDER=local
UPLOAD_DIR=uploads
PUBLIC_BASE_URL=http://localhost:8080/api
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://127.0.0.1:5173
```

不配置任何 `OSS_*` 变量也能启动、上传和运行测试。本地 URL 仍为 `/api/uploads/...`。

## OSS、CDN 和 RAM 准备

1. 创建与应用同地域的 OSS Bucket，名称使用小写字母、数字和连字符。
2. Bucket 建议保持私有，通过 CDN 回源鉴权读取；不要开放匿名写入。
3. 为 CDN 配置 HTTPS、自定义域名、Range 回源和 206 缓存，确保视频可拖动播放。
4. CDN 应尊重源站的 `Cache-Control: public, max-age=31536000, immutable`。文件名使用 UUID，替换文件时生成新对象，不覆盖旧对象。
5. 创建专用 RAM 用户，不使用主账号 AccessKey。最小权限应限定到目标 Bucket，并只包含应用需要的 `PutObject`、`GetObject`、`DeleteObject` 和 `ListObjects`。
6. OSS/CDN 响应应保留正确的 `Content-Type`、`Content-Length`、`ETag`、`Accept-Ranges` 和 `Content-Range`。

当前是后端代理上传，浏览器不会直接向 OSS 执行 PUT，因此 OSS CORS 只需允许站点对公开图片和视频的 GET/HEAD。以后改为前端直传时，再增加 PUT/POST、`x-oss-*` 请求头和 ETag 暴露，并改用短期 STS。

## 生产环境变量

```properties
STORAGE_PROVIDER=oss
OSS_ENDPOINT=https://oss-cn-shanghai.aliyuncs.com
OSS_BUCKET=your-hub-media
OSS_ACCESS_KEY_ID=由密钥管理系统注入
OSS_ACCESS_KEY_SECRET=由密钥管理系统注入
OSS_SECURITY_TOKEN=
OSS_PUBLIC_BASE_URL=https://cdn.example.com

UPLOAD_USER_QUOTA_BYTES=1073741824
UPLOAD_ORPHAN_RETENTION_HOURS=24
UPLOAD_CLEANUP_CRON=0 30 4 * * *
```

AccessKey 只允许放在服务端环境变量或密钥管理系统中，不得写入 Git、前端 `.env`、构建产物或日志。使用临时 STS 时同时填写 `OSS_SECURITY_TOKEN`，并确保凭证在应用运行期间有效。

执行：

```powershell
npm run check:beta-env
npm run verify:beta
```

缺失 OSS 配置、非法 Bucket 名或生产环境 HTTP 地址会被预检/启动校验拒绝。

## 现有本地文件迁移

切换前进入维护窗口并同时备份数据库与 `UPLOAD_DIR`。

1. 使用 `ossutil` 保留目录结构复制文件：

```bash
ossutil cp -r /var/lib/hub-demo/uploads/ oss://your-hub-media/ --update
ossutil ls oss://your-hub-media/avatar/ --recursive
ossutil ls oss://your-hub-media/combo-video/ --recursive
```

2. 抽查文件大小、Content-Type 和视频 Range 请求。
3. 在事务中把旧公开地址前缀改成 CDN 前缀。以下示例中的旧、新域名必须替换为真实值：

```sql
BEGIN;

UPDATE combos
SET video_url = regexp_replace(video_url,
    '^https://api\.example\.com/api/uploads/',
    'https://cdn.example.com/')
WHERE video_url LIKE 'https://api.example.com/api/uploads/%';

UPDATE users
SET avatar = regexp_replace(avatar,
    '^https://api\.example\.com/api/uploads/',
    'https://cdn.example.com/')
WHERE avatar LIKE 'https://api.example.com/api/uploads/%';

UPDATE characters
SET avatar = regexp_replace(avatar,
    '^https://api\.example\.com/api/uploads/',
    'https://cdn.example.com/')
WHERE avatar LIKE 'https://api.example.com/api/uploads/%';

COMMIT;
```

4. 设置 `STORAGE_PROVIDER=oss` 并重启后端。
5. 验证头像、角色头像、视频播放、上传配额、投稿归属校验和孤儿清理日志。
6. 保留原本地目录和数据库备份至回滚观察期结束。

若历史数据库保存的是相对 `/api/uploads/...` 地址，需要使用对应的 `replace` 规则单独迁移，不要直接套用上述绝对 URL SQL。

## CDN 与安全头

前端 Nginx 的 CSP 至少需要把 CDN 加入图片和媒体来源：

```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://cdn.example.com; media-src 'self' https://cdn.example.com; connect-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'" always;
```

如前端、API、CDN 域名不同，分别配置 HTTPS。防盗链可以减少普通外链，但不能替代权限控制。当前公开 CDN URL 适合已公开资源；如果需要保证待审核视频不可通过 URL 访问，应另行实现私有审核前缀、短期签名 URL 和审核通过后的对象发布流程。

## 监控与回滚

至少监控 OSS 存储量、请求次数、上传失败、CDN 命中率、回源流量、4xx/5xx、视频 Range 命中和费用告警。

回滚到本地存储时：

1. 停止写入。
2. 把观察期内新 OSS 对象同步回原 `UPLOAD_DIR`。
3. 将数据库中的 CDN URL 前缀反向恢复为 `/api/uploads/` 地址。
4. 设置 `STORAGE_PROVIDER=local`、恢复 `UPLOAD_DIR` 后重启。
5. 完成上传、投稿、头像和视频回归验证后再恢复写入。
