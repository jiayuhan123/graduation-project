# 4C / 4G 单机服务器部署

当前默认架构是一个模块化单体后端加独立 FastAPI Agent 服务。服务器运行 PostgreSQL、Redis、RabbitMQ、Prometheus、Grafana、Jenkins 和 `agent-service`；Java 业务只运行 `back/monolith-app`，不再运行 Gateway、Nacos 或八个独立 Java 进程。

## 资源预算

| 组件 | 内存上限 | 说明 |
| --- | ---: | --- |
| PostgreSQL + pgvector | 256 MB | 单体数据库与 Agent 向量索引 |
| Redis | 128 MB | Sa-Token 会话与业务缓存 |
| RabbitMQ | 320 MB | 领域事件、重试和死信 |
| Prometheus | 192 MB | 单体指标采集 |
| Grafana | 160 MB | 指标展示 |
| Jenkins 控制器 | 640 MB | CI/CD 控制器 |
| FastAPI Agent | 512 MB | 模型编排、RAG、记忆与指标 |
| Compose 服务合计 | 2,208 MB | 不含 Java 单体进程 |

单体 Java 进程在当前低流量阶段建议使用 `-Xms256m -Xmx640m`，数据库连接池默认最大 5。相比原微服务方案，默认运行链路减少了 Nacos、Gateway 和八个独立 JVM，适合当前 4 GB 服务器。

## 基础设施

以下命令假设仓库位于 `/opt/primary-math-platform`，服务器根目录 `.env` 已通过安全渠道配置：

```bash
cd /opt/primary-math-platform
chmod 600 .env
sudo bash deploy/server/init-data-dirs.sh
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml config --quiet
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d --build
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml ps
```

开发机统一连接这台服务器的 PostgreSQL、Redis 和 RabbitMQ，不在开发机启动本地基础设施。若需要从公网开放中间件端口，应通过云防火墙和 UFW 只放行固定开发 IP；数据库和中间件密码不得使用示例值。

首次创建 `monolith_db` 后，在 `back` 目录执行单库脚本：

```bash
psql -v ON_ERROR_STOP=1 -d monolith_db -f database/init-schema.sql
psql -v ON_ERROR_STOP=1 -d monolith_db -f database/seed-data.sql
```

当前服务器数据库已在 2026-09-11 完成八库数据迁入，47 张表共 788 行；不要在已有数据的服务器上重复执行迁移或初始化脚本。

PostgreSQL 容器使用与 PostgreSQL 18 兼容的固定版 pgvector 镜像。已有数据库升级时只重建容器，不得删除 `./data/postgres`；FastAPI 首次就绪前会幂等创建 `vector` 扩展和 `ai_*` 表，并使用迁移校验和阻止历史脚本漂移。

教学 Agent 启用前还必须配置 `AI_MODEL_NAME`、`AI_EMBEDDING_MODEL`、模型 API Key、桥接令牌和至少 32 字符的工具签名密钥：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d postgres redis agent-service
curl -fsS http://127.0.0.1:8100/ready
curl -fsS http://127.0.0.1:8100/metrics >/dev/null
```

## 构建并启动单体

服务器需要 JDK 21。先完成全量验证和打包：

```bash
cd /opt/primary-math-platform/back
./mvnw clean verify
```

前台启动用于首次联调：

```bash
java -Xms256m -Xmx640m -jar monolith-app/target/monolith-app-0.0.1-SNAPSHOT.jar
```

应用默认读取仓库根目录 `.env`、启用 `server` profile，并在 `10000` 端口提供业务 API、Swagger 和健康检查。正式常驻时应交给 systemd 或受控容器管理，不要依赖交互式终端。

```bash
curl -fsS http://127.0.0.1:10000/actuator/health
curl -fsS http://127.0.0.1:10000/actuator/prometheus >/dev/null
```

## 数据目录

| 服务 | 宿主机目录 | 容器内目录 |
| --- | --- | --- |
| PostgreSQL | `./data/postgres` | `/var/lib/postgresql` |
| Redis | `./data/redis` | `/data` |
| RabbitMQ | `./data/rabbitmq` | `/var/lib/rabbitmq` |
| Prometheus | `./data/prometheus` | `/prometheus` |
| Grafana | `./data/grafana` | `/var/lib/grafana` |
| Jenkins | `./data/jenkins` | `/var/jenkins_home` |

`data/` 已被 Git 忽略。首次启动前必须运行 `init-data-dirs.sh`；已有服务器不要替换或手工修改 PostgreSQL 数据目录。

## Caddy 与端口

业务流量统一反向代理到单体 `10000` 端口。如果 Caddy 运行在容器中，应让它访问宿主机地址：

```caddyfile
api.example.com {
    reverse_proxy host.docker.internal:10000
}
```

微信小程序正式环境必须使用已备案并加入微信合法域名列表的 HTTPS 域名，不能继续使用裸 IP 和 HTTP。

## Jenkins 与日常操作

Jenkins Web 默认只绑定服务器本机 `127.0.0.1:8080`。服务器需要在 `.env` 中提供 `DOCKER_GID` 和独立管理员密码。日常更新基础设施不要附加 `-v`：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d --build
docker stats --no-stream
free -h
df -h
```

至少备份 `./data/postgres`、`./data/redis` 和 `./data/rabbitmq`；备份不得只保留在同一台服务器上。
