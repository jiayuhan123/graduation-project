# 4C / 4G 单机服务器部署

本目录是仓库根目录 `docker-compose.yaml` 的服务器覆盖配置。它默认部署 PostgreSQL、Redis、Nacos、RabbitMQ、Prometheus、Grafana 和 Jenkins。

## 资源预算

| 组件 | CPU 上限 | 内存上限 | 说明 |
| --- | ---: | ---: | --- |
| PostgreSQL | 不限制 | 256 MB | 业务数据库 |
| Redis | 不限制 | 128 MB | 64 MB 缓存上限 |
| Nacos | 不限制 | 512 MB | 单机注册/配置中心 |
| RabbitMQ | 不限制 | 320 MB | 含管理插件 |
| Prometheus | 不限制 | 192 MB | 指标采集 |
| Grafana | 不限制 | 160 MB | 仪表盘 |
| Jenkins 控制器 | 不限制 | 640 MB | CI/CD 控制器 |
| 全部 Compose 服务合计 | 不限制 | 2,208 MB | 默认启动集 |

Docker 与 Caddy、Ubuntu 本身还会消耗约 500–800 MB。全部服务默认启动后，4G 主机仍保留约 600–900 MB 作为系统与突发缓冲；Jenkins 构建产生的 Maven、Node 或 Docker 子容器不受 Jenkins 控制器 640 MB 限制，构建高峰仍可能触发内存不足。后续若部署完整 Java 微服务集，需要重新评估内存或升级到 8 GB。

如后续在同一台机器部署 Java 服务，可按下表作为低流量起点；全部 Java 微服务同时运行时仍应优先升级内存：

| Java 服务 | 建议 `-Xms / -Xmx` | 数据库连接池上限 |
| --- | --- | ---: |
| Gateway | `128m / 256m` | 不适用 |
| user-service | `128m / 256m` | 5 |
| file-service | `96m / 192m` | 4 |
| notification-service | `96m / 160m` | 3 |
| audit-service | `96m / 160m` | 3 |

启动每个服务时显式注入相应 JVM 参数。`user-service` 可设置 `DB_POOL_MAX_SIZE=5`、`DB_POOL_MIN_IDLE=1`；其余服务可设置 Spring 标准环境变量 `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` 和 `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE`。这些数值适用于单机低并发，不是高可用生产容量方案。

## 部署基础设施

以下命令假设仓库位于 `/opt/primary-math-platform`。如使用其他目录，保持 Compose 文件和 `deploy/` 目录的相对路径不变。

```bash
cd /opt/primary-math-platform
chmod 600 .env
sudo bash deploy/server/init-data-dirs.sh
```

仓库根目录已经包含实际运行用的 `.env`（被 Git 忽略，必须通过安全渠道单独同步到服务器）。

先校验、构建 Jenkins 并启动全部服务：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml config --quiet
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d --build
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml ps
```

## 数据目录

所有持久化数据均以相对绑定挂载保存在仓库根目录的 `./data/` 中：

| 服务 | 宿主机目录 | 容器内目录 |
| --- | --- | --- |
| PostgreSQL | `./data/postgres` | `/var/lib/postgresql` |
| Redis | `./data/redis` | `/data` |
| Nacos | `./data/nacos/data`、`./data/nacos/logs` | `/home/nacos/data`、`/home/nacos/logs` |
| RabbitMQ | `./data/rabbitmq` | `/var/lib/rabbitmq` |
| Prometheus | `./data/prometheus` | `/prometheus` |
| Grafana | `./data/grafana` | `/var/lib/grafana` |
| Jenkins | `./data/jenkins` | `/var/jenkins_home` |

`data/` 已被 Git 忽略，不能提交，也不要手工修改其中的数据库文件。首次启动前必须执行 `sudo bash deploy/server/init-data-dirs.sh`，以创建目录并配置各镜像所需的写入权限。

若已经用旧配置启动过项目，旧的 Docker 命名卷不会自动迁移，且会继续保留。此时不要直接用新配置启动空目录；先备份或迁移旧卷数据后再切换。

初次启动后确认健康状态：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml logs --tail=100 postgres redis nacos rabbitmq
curl -fsS http://127.0.0.1:8848/nacos/actuator/health
```

Nacos 首次启用鉴权后，需要在仅本机可访问的控制台中初始化 `nacos` 管理员密码；设置为根目录 `.env` 中的 `NACOS_PASSWORD`。后续 Java 服务使用的 `NACOS_USERNAME`、`NACOS_PASSWORD` 必须与 Nacos 中已创建的账号一致。

## Caddy 与端口

当前 Compose 的数据库、中间件和监控端口都绑定到 `127.0.0.1`，不会占用 Caddy 已使用的 `80/443`，也不应直接暴露到公网。基础设施启动时无需修改 Caddy。

当以后部署 Gateway 时，推荐把 Gateway 容器和 Caddy 放到一个共享 Docker 网络中，并让 Caddy 反代容器内部地址 `gateway:10000`。不要在 Caddy 容器里写 `reverse_proxy 127.0.0.1:10000`：该地址指向 Caddy 容器自身，而不是 Ubuntu 宿主机。

如果 Java 服务暂时必须以宿主机进程运行，Caddy 容器需在创建时加入 `--add-host host.docker.internal:host-gateway`，并反代 `host.docker.internal:10000`；同时用 UFW 限制 Gateway 端口，避免它直接暴露公网。容器化 Gateway 是更安全、也更易维护的方案。

## Jenkins CI

Jenkins 与其余服务一起启动。控制器镜像基于官方 `jenkins/jenkins:lts-jdk21`，并构建时安装 Docker CLI、Docker Compose 插件、Git、SSH 和最小 Pipeline 插件集。

先在服务器获取 Docker socket 的 GID，并将数字填入 `.env` 的 `DOCKER_GID`：

```bash
stat -c '%g' /var/run/docker.sock
```

如果需要单独重建 Jenkins：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml build jenkins
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d jenkins
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml logs --tail=100 jenkins
```

Jenkins Web 端口只绑定到服务器本机 `127.0.0.1:8080`。可通过 SSH 隧道访问，或将现有 Caddy 加入项目网络后以 HTTPS 反代：

```bash
ssh -L 8080:127.0.0.1:8080 root@YOUR_SERVER_IP
docker network connect primary-math-platform_local-net caddy
```

SSH 隧道建立后，在本机浏览器打开 `http://127.0.0.1:8080`。若使用 Caddy，请为独立域名添加如下站点块并重载其现有配置：

```caddyfile
jenkins.example.com {
    reverse_proxy jenkins:8080
}
```

Jenkins 通过 Configuration as Code 自动创建 `.env` 中的管理员账号；不显示首次安装向导。不要开启匿名访问。Jenkins 通过 Docker socket 可以创建拥有宿主机权限的容器；因此只能授予可信管理员使用，不能执行来自不可信 Fork、PR 或用户提交的 Pipeline。

## 监控与日常操作

查看实际资源占用：

```bash
docker stats --no-stream
free -h
df -h
```

日常更新只拉取和重建基础设施，不要带 `-v`，否则会删除数据卷：

```bash
docker compose --env-file .env -f docker-compose.yaml -f docker-compose.server.yaml up -d --build
```

至少备份 `./data/postgres` 与 `./data/nacos`；生产密码、Nacos 配置和 PostgreSQL 备份不要只保留在同一台服务器上。
