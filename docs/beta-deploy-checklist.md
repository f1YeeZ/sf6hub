# Beta Deploy Checklist

更新日期：2026-07-11

## 1. Release Source

- [ ] All intended source, migrations, tests, and lockfiles are committed.
- [ ] `git status --short` is empty before packaging.
- [ ] The release commit/tag and artifact checksum are recorded.
- [ ] `npm ci --prefix hub_demo_frontend` is used for reproducible dependency installation.
- [ ] `npm run verify:beta` passes.

Build toolchain: JDK 17, Maven 3.9+, Node.js 22 and npm compatible with the committed lockfile.

```bash
npm ci --prefix hub_demo_frontend
npm run verify:beta
sha256sum hub_demo_backend/target/hub-demo-0.0.1-SNAPSHOT.jar
```

## 2. Required Environment

Backend:

```env
DB_URL=jdbc:postgresql://host:5432/hub_demo
DB_USERNAME=hub_demo
DB_PASSWORD=replace-with-a-strong-password
APP_ENV=beta
JWT_SECRET=replace-with-at-least-32-random-characters
QQ_MAIL_USERNAME=your-mailbox@example.com
QQ_MAIL_AUTH_CODE=mail-provider-app-password
QQ_MAIL_FROM=your-mailbox@example.com
PUBLIC_BASE_URL=https://example.com/api
CORS_ALLOWED_ORIGIN_PATTERNS=https://example.com,https://www.example.com
TRUSTED_PROXY_ADDRESSES=127.0.0.1
STORAGE_PROVIDER=local
UPLOAD_DIR=/var/lib/hub-demo/uploads
UPLOAD_USER_QUOTA_BYTES=1073741824
UPLOAD_ORPHAN_RETENTION_HOURS=24
UPLOAD_CLEANUP_CRON=0 30 4 * * *
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
SQL_INIT_MODE=never
OFFICIAL_FRAME_SYNC_ENABLED=false
```

OSS/CDN production alternative (keep `local` for local development):

```env
STORAGE_PROVIDER=oss
OSS_ENDPOINT=https://oss-cn-shanghai.aliyuncs.com
OSS_BUCKET=your-hub-media
OSS_ACCESS_KEY_ID=inject-from-secret-manager
OSS_ACCESS_KEY_SECRET=inject-from-secret-manager
OSS_SECURITY_TOKEN=
OSS_PUBLIC_BASE_URL=https://cdn.example.com
```

Frontend:

```env
VITE_API_BASE_URL=https://example.com/api
```

- [ ] Run `npm run check:beta-env` in the release environment.
- [ ] For `STORAGE_PROVIDER=local`, keep `UPLOAD_DIR` outside the checkout on persistent storage. For `oss`, complete `docs/oss-cdn-deployment.md` and verify the CDN/RAM configuration.
- [ ] `TRUSTED_PROXY_ADDRESSES` only contains the real reverse proxy addresses/CIDRs; for same-host Nginx use `127.0.0.1` or the actual loopback address.
- [ ] Confirm the upload quota, orphan retention, cleanup schedule, storage monitoring, backup and restore policy.
- [ ] Do not enable official synchronization until the external-data review is complete.

## 3. Database

- [ ] Back up the database before deployment and verify the backup can be restored.
- [ ] From the repository root, export `FLYWAY_URL=$DB_URL`, `FLYWAY_USER=$DB_USERNAME`, `FLYWAY_PASSWORD=$DB_PASSWORD`. Do not print these values in CI logs.
- [ ] Run `mvn -f hub_demo_backend/pom.xml flyway:info`; confirm the Pending migrations match the target environment and all applied checksums are intact. The current code migration chain ends at V26. Flyway 11 strict `validate` fails while a resolved migration is Pending, so the release gate is `migrate` followed by `validate`.
- [ ] Run `mvn -f hub_demo_backend/pom.xml flyway:migrate` and then `mvn -f hub_demo_backend/pom.xml flyway:validate`; application startup should subsequently find no pending migration.
- [ ] Never edit an applied migration; add a new numbered migration instead.
- [ ] Do not use `flyway repair` until the checksum difference and target history are understood and approved.
- [ ] Before migration, record every expected pending migration and back up the database.
- [ ] After startup, verify `flyway_schema_history` contains successful versions 1 through 26.
- [ ] Verify V20 backfilled `user_id` for known likes, favorites and notifications, and added `token_version` plus the expected indexes.
- [ ] Verify V23 removed retired follow/comment/reply notifications, dropped `user_follows` and the two follow-count columns, and retained only `combo_review`, `combo_like`, `combo_favorite`, `feedback`, and `system` notification types.
- [ ] Verify V26 leaves at most one `combo_followups` row per `followup_combo_id`, adds `uk_combo_followups_followup`, and backfills every historical follow-up combo to exactly one supported pressure type.
- [ ] Only after the external-data/IP review is approved, run one complete official frame synchronization and spot-check classic/modern inputs, OD double-punch/double-kick inputs, `4溜め6/2溜め8` yellow charge arrows, rotations, `+`, OR, and arrow icons against Capcom. Otherwise keep synchronization disabled.
- [ ] Verify Ingrid shows both control types after synchronization. The 2026-07-10 upstream snapshot contained 75 classic and 74 modern rows; treat counts as a diagnostic snapshot, not a permanent assertion.
- [ ] Verify Zangief shows both control types, includes 360/720 circle icons, and contains no charge inputs or Ingrid-only moves. The 2026-07-10 upstream snapshot contained 72 classic and 66 modern rows.
- [ ] Confirm a full synchronization removed obsolete non-manual rows while preserving every `manual_override=true` row.
- [ ] Confirm administrator-corrected frame rows still have their manual move notation after the synchronization.

For a new installation, register the designated administrator and then promote that verified account through a restricted database/operations session:

```sql
UPDATE users
SET role = 'admin'
WHERE email = 'verified-admin@example.com';
```

Confirm exactly one intended row changed. There is no fixed-ID administrator and no automatic first-user promotion.

## 4. Backend and Frontend Deployment

Backend artifact: `hub_demo_backend/target/hub-demo-0.0.1-SNAPSHOT.jar`.

One-time server bootstrap (Debian/Ubuntu-style paths):

```bash
sudo groupadd --system hub-demo
sudo useradd --system --gid hub-demo --home /opt/hub-demo --shell /usr/sbin/nologin hub-demo
sudo install -d -o root -g hub-demo -m 0750 /opt/hub-demo /etc/hub-demo
sudo install -d -o hub-demo -g hub-demo -m 0750 /var/lib/hub-demo/uploads
sudo install -d -o www-data -g www-data -m 0755 /var/www/hub-demo
sudo install -d -o root -g root -m 0700 /var/backups/hub-demo
```

Create `/etc/hub-demo/backend.env` from the variables in section 2, then run `sudo chown root:hub-demo` and `sudo chmod 0640` on it. Create the following unit as `/etc/systemd/system/hub-demo.service`.

Recommended systemd layout:

```ini
[Unit]
Description=Hub Demo Backend
After=network-online.target postgresql.service

[Service]
User=hub-demo
Group=hub-demo
WorkingDirectory=/opt/hub-demo
EnvironmentFile=/etc/hub-demo/backend.env
ExecStart=/usr/bin/java -jar /opt/hub-demo/hub-demo.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Deployment steps:

```bash
sudo install -o root -g hub-demo -m 0640 hub_demo_backend/target/hub-demo-0.0.1-SNAPSHOT.jar /opt/hub-demo/hub-demo.jar
sudo install -d -o hub-demo -g hub-demo -m 0750 /var/lib/hub-demo/uploads
sudo systemctl daemon-reload
sudo systemctl enable hub-demo
sudo systemctl restart hub-demo
curl --fail https://example.com/api/health

npm ci --prefix hub_demo_frontend
npm --prefix hub_demo_frontend run build
sudo rsync -a --delete hub_demo_frontend/dist/ /var/www/hub-demo/
sudo nginx -t
sudo systemctl reload nginx
```

- [ ] `/etc/hub-demo/backend.env` is readable only by root/service administrators and contains all variables from section 2.
- [ ] In local mode the service user can read/write `UPLOAD_DIR`; in OSS mode the RAM identity is limited to the configured Bucket. The service cannot modify the application artifact or environment file.
- [ ] Deploy and verify the backend before replacing frontend files.
- [ ] Record the deployed backend JAR checksum, frontend build identifier and release commit/tag.

## 5. Reverse Proxy and Frontend

Example same-origin Nginx layout:

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    # Provision a valid certificate before enabling this server block.
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    root /var/www/hub-demo;
    index index.html;
    client_max_body_size 100m;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
    # 使用 CDN 时把真实域名加入 img-src/media-src；完整示例见 docs/oss-cdn-deployment.md。
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://cdn.example.com; media-src 'self' https://cdn.example.com; connect-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'" always;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        # 覆盖客户端自带 X-Forwarded-For，避免伪造来源绕过限流。
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Request-Id $request_id;
    }

    location ~ ^/api/(events|admin/events)$ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 35m;
        add_header X-Accel-Buffering no;
    }
}
```

Save the server block as `/etc/nginx/sites-available/hub-demo`, replace the domain and certificate paths, then enable it:

```bash
sudo ln -s /etc/nginx/sites-available/hub-demo /etc/nginx/sites-enabled/hub-demo
sudo nginx -t
sudo systemctl reload nginx
```

The example assumes a certificate has already been issued, for example by the platform load balancer or Certbot. Do not expose the HTTP application publicly before HTTPS and redirect policy are configured.

- [ ] Deep links such as `/admin` and `/characters/1` return `index.html`.
- [ ] The application port is reachable only from the reverse proxy/private network.
- [ ] 后端 `TRUSTED_PROXY_ADDRESSES` 与真实代理来源一致；遗漏时所有用户会共享代理 IP 的限流额度。
- [ ] CDN/tunnel layers do not buffer SSE responses.

## 6. Runtime Preflight

Run:

```powershell
npm run preflight:beta -- https://example.com/api
```

Required `OK` checks:

- health and database
- security headers
- character catalog
- announcements
- public realtime SSE
- visit analytics with CSRF

Then manually verify registration mail, password reset, upload, combo publishing, moderation, notifications, and admin operations.

部署顺序要求：先迁移数据库并部署后端，确认 `/characters/{id}/combo-filter-options` 和 `/characters/{id}/combo-parent-options` 等新接口可用，再发布前端静态包。

## 7. Operations and Rollback

- [ ] Monitor `/api/health`, HTTP 5xx/429 rates, database pool usage, disk usage, mail failures, JVM memory, and SSE connection counts.
- [ ] Centralize logs and retain `X-Request-Id` for incident correlation.
- [ ] Back up database and upload storage on the same recovery schedule.
- [ ] Keep the previous application artifact and frontend bundle available.
- [ ] Document whether each migration is backward-compatible before deployment.
- [ ] 当前版本按单实例运行；在引入 Redis、对象存储和跨实例 SSE 广播前，不启用多实例负载均衡。
- [ ] If rollback requires a database restore, stop writes first and follow `docs/rollback-runbook.md`.
- [ ] Confirm applicable privacy, content, filing and third-party asset requirements before public launch; review `../THIRD_PARTY_NOTICES.md`.
