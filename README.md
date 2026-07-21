# SF6 Training Hub

[![CI](https://github.com/f1YeeZ/sf6hub/actions/workflows/ci.yml/badge.svg)](https://github.com/f1YeeZ/sf6hub/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个面向《Street Fighter 6》玩家的训练资料与连招社区。项目提供角色与帧数资料、连招投稿和筛选、收藏、通知、用户资料、反馈举报及后台管理等功能。

> 本项目是非官方社区项目，与 CAPCOM 无隶属或授权关系。商标、游戏名称及第三方素材归各自权利人所有，详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 技术栈

- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、ECharts
- 后端：Java 17、Spring Boot 4、MyBatis-Plus、Flyway
- 数据库：PostgreSQL 16
- 对象存储：本地文件系统或阿里云 OSS
- 工程化：npm、Maven、GitHub Actions、Dependabot

## 本地开发

### 环境要求

- JDK 17
- Maven 3.9+
- Node.js 22+
- npm 10+
- PostgreSQL 16

### 1. 安装依赖

```bash
npm ci --prefix hub_demo_frontend
```

### 2. 创建数据库并配置环境

创建名为 `hub_demo` 的 PostgreSQL 数据库，然后复制示例配置：

```powershell
Copy-Item hub_demo_backend/.env.example hub_demo_backend/.env.local
Copy-Item hub_demo_frontend/.env.example hub_demo_frontend/.env.local
```

至少需要在后端 `.env.local` 中填写 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和长度不少于 32 个字符的 `JWT_SECRET`。本地前端可将 `VITE_API_BASE_URL` 设置为 `/api`，由 Vite 代理到后端。

`.env.local` 已被 Git 忽略。不要提交数据库密码、邮件授权码、JWT 密钥或云服务凭证。

### 3. 启动服务

分别在两个终端运行：

```bash
cd hub_demo_backend
mvn spring-boot:run
```

```bash
cd hub_demo_frontend
npm run dev
```

默认地址：

- 前端：`http://localhost:5173`
- 后端 API：`http://localhost:8080/api`
- 健康检查：`http://localhost:8080/api/health`

Flyway 会在后端启动时自动执行数据库迁移。

## 常用命令

| 命令 | 说明 |
| --- | --- |
| `npm test` | 运行前后端测试 |
| `npm run build` | 构建前后端 |
| `npm run verify` | 检查 UTF-8、运行测试并构建 |
| `npm run audit:backend` | 使用 OSV 审计 Maven 依赖 |
| `npm run check:beta-env` | 校验 Beta/生产环境变量 |
| `npm run preflight:beta` | 对已启动的后端执行发布前检查 |

## 项目结构

```text
.
├── .github/             # CI 与依赖更新配置
├── docs/                # 部署和回滚文档
├── hub_demo_backend/    # Spring Boot 后端与接口文档
├── hub_demo_frontend/   # Vue 前端
└── scripts/             # 编码、依赖和发布检查脚本
```

更多文档：

- [后端接口文档](hub_demo_backend/接口文档.md)
- [Beta 部署清单](docs/beta-deploy-checklist.md)
- [OSS 与 CDN 部署](docs/oss-cdn-deployment.md)
- [回滚与恢复](docs/rollback-runbook.md)

## 参与贡献

提交代码前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。安全问题请按 [SECURITY.md](SECURITY.md) 私下报告，不要在公开 Issue 中披露漏洞细节。

## 许可证

项目自有源代码采用 [MIT License](LICENSE) 发布。第三方商标、数据和素材不包含在该许可授权中。
