# Hub Demo 回滚与恢复 Runbook

更新日期：2026-07-11

使用 OSS/CDN 时，文件同步、URL 前缀切换和回退步骤同时参照 `docs/oss-cdn-deployment.md`；CDN 缓存不能代替源文件备份。

## 适用范围

用于应用发布失败、V20 迁移后异常、前端静态包故障或数据库/上传数据需要恢复的情况。执行人必须记录开始时间、故障原因、当前 release commit、JAR/前端制品校验和、数据库迁移版本和备份标识。

## 1. 立即止损

1. 在 Nginx 启用维护页或限制写流量，停止注册、资料修改、上传、投稿和后台操作。
2. 不要在仍有写请求时恢复数据库或上传目录。
3. 保存后端日志、Nginx 日志、`X-Request-Id`、数据库状态和 `flyway_schema_history`。
4. 确认故障属于前端、应用、配置还是数据库迁移，再选择回滚级别。

## 2. 发布前备份

以下示例适用于自管 PostgreSQL 和本地上传目录；托管数据库应使用等价的时间点快照。备份文件目录必须只允许运维管理员读取。执行前从 JDBC `DB_URL` 确认并单独设置 `DB_HOST`、`DB_PORT` 和 `DB_NAME`；`pg_dump` 不直接使用 JDBC URL。

```bash
export BACKUP_ID="$(date -u +%Y%m%dT%H%M%SZ)"
export PGPASSWORD="$DB_PASSWORD"
pg_dump --host="$DB_HOST" --port="${DB_PORT:-5432}" --username="$DB_USERNAME" \
  --dbname="$DB_NAME" --format=custom --file="/var/backups/hub-demo/db-${BACKUP_ID}.dump"
sudo tar --acls --xattrs -C /var/lib/hub-demo -czf "/var/backups/hub-demo/uploads-${BACKUP_ID}.tar.gz" uploads
sha256sum "/var/backups/hub-demo/db-${BACKUP_ID}.dump" "/var/backups/hub-demo/uploads-${BACKUP_ID}.tar.gz" \
  > "/var/backups/hub-demo/${BACKUP_ID}.sha256"
unset PGPASSWORD
```

执行 `pg_restore --list`、`tar -tzf` 和 `sha256sum -c` 检查备份可读。正式上线前必须至少在隔离环境完整恢复一次，而不只是检查文件存在。

## 3. 仅前端回滚

1. 保持当前后端运行。
2. 将上一个已验证前端制品恢复到临时目录。
3. 原子替换或同步到 `/var/www/hub-demo/`。
4. 执行 `nginx -t` 和 `systemctl reload nginx`。
5. 验证首页、深链接、登录、角色详情、连招分页和后台入口。

## 4. 仅后端应用回滚

1. 确认旧应用是否兼容当前数据库版本。
2. 停止后端：`systemctl stop hub-demo`。
3. 恢复上一个已验证 JAR 和对应环境配置。
4. 若回滚到不理解 `token_version` 的 V20 以前应用，必须同时轮换 `JWT_SECRET`，避免已撤销的旧 JWT 再次生效；这会使全部用户重新登录。
5. 启动后端并检查日志、`/api/health`、Flyway 状态和核心接口。
6. 后端验证通过后再开放流量。

V20 主要是新增列、外键和索引，旧代码通常会忽略新增字段；但必须在 staging 用实际旧 JAR 验证，不能仅凭“加法迁移”假设兼容。

## 5. 数据库与上传恢复

1. 保持应用停止和写流量关闭。
2. 记录当前数据库和上传目录快照，避免覆盖唯一故障证据。
3. 选择同一 `BACKUP_ID` 的 PostgreSQL 与上传备份。优先恢复到新数据库和新上传目录，验证后通过环境变量切换；不要直接覆盖唯一的现有副本。
4. 不手写 V20 down migration，不使用未经分析的 `flyway repair`。
5. 使用具备 `CREATEDB` 权限的受控数据库管理员创建恢复库，并恢复备份：

```bash
export BACKUP_ID=20260711T000000Z
export RESTORE_DB="hub_demo_restore_${BACKUP_ID}"
export PGPASSWORD="$DB_ADMIN_PASSWORD"
createdb --host="$DB_HOST" --port="${DB_PORT:-5432}" --username="$DB_ADMIN_USERNAME" "$RESTORE_DB"
pg_restore --exit-on-error --no-owner --role="$DB_USERNAME" \
  --host="$DB_HOST" --port="${DB_PORT:-5432}" --username="$DB_ADMIN_USERNAME" \
  --dbname="$RESTORE_DB" "/var/backups/hub-demo/db-${BACKUP_ID}.dump"
unset PGPASSWORD

sudo install -d -o hub-demo -g hub-demo -m 0750 "/var/lib/hub-demo/uploads-${BACKUP_ID}"
sudo tar --acls --xattrs -C "/var/lib/hub-demo/uploads-${BACKUP_ID}" \
  --strip-components=1 -xzf "/var/backups/hub-demo/uploads-${BACKUP_ID}.tar.gz"
```

6. 在 `/etc/hub-demo/backend.env` 中将 `DB_URL` 改为实际值，例如 `jdbc:postgresql://db-host:5432/hub_demo_restore_20260711T000000Z`，将 `UPLOAD_DIR` 指向恢复目录；保留原数据库和原上传目录直到恢复验证与观察窗口结束。
7. 针对恢复库运行：

```bash
export FLYWAY_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${RESTORE_DB}"
export FLYWAY_USER="$DB_USERNAME"
export FLYWAY_PASSWORD="$DB_PASSWORD"
mvn -f hub_demo_backend/pom.xml flyway:info
mvn -f hub_demo_backend/pom.xml flyway:validate
```

8. 确认 `flyway_schema_history` 与将要启动的 JAR 匹配。
9. 启动后端，执行部署后预检，再验证上传文件 URL 与数据库引用一致。

托管 PostgreSQL 或对象存储使用平台等价的“恢复到新实例/新前缀并切换”流程。不要把含凭据的命令或备份提交到仓库。

## 6. 恢复验证

- `/api/health` 返回应用和数据库 `UP`。
- 安全响应头、CSRF、登录 Cookie 和 SSE 正常。
- 注册邮件、登录/退出、密码重置、资料修改正常。
- 角色、帧数据、连招分页和筛选选项正常。
- 上传、投稿、审核、通知、举报、反馈和审计日志正常。
- 观察至少一个稳定窗口内的 5xx/429、连接池、JVM、磁盘和邮件错误。

## 7. 重新开放流量

1. 由第二位负责人复核版本、迁移、备份和验证结果。
2. 逐步恢复流量或关闭维护页。
3. 持续监控并记录恢复时间。
4. 完成事故复盘，补充触发条件、检测缺口和预防措施。
