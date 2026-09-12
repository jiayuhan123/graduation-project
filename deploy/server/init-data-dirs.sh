#!/usr/bin/env bash
set -euo pipefail

# 在 Ubuntu 服务器上、仓库根目录中执行：
#   sudo bash deploy/server/init-data-dirs.sh
#
# 这些目录对应 docker-compose.yaml 中的 ./data 绑定挂载。
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
data_dir="${project_dir}/data"

install -d -m 0750 "${data_dir}"
install -d -m 0700 "${data_dir}/postgres"
install -d -m 0750 "${data_dir}/redis"
install -d -m 0750 "${data_dir}/rabbitmq"
install -d -m 0750 "${data_dir}/prometheus"
install -d -m 0750 "${data_dir}/grafana"
install -d -m 0750 "${data_dir}/jenkins"

# 固定镜像版本使用的运行 UID/GID，避免绑定挂载目录因宿主机 root 所有而不可写。
chown -R 999:999 "${data_dir}/postgres" "${data_dir}/redis" "${data_dir}/rabbitmq"
chown -R 1000:1000 "${data_dir}/jenkins"
chown -R 65534:65534 "${data_dir}/prometheus"
chown -R 472:0 "${data_dir}/grafana"

printf '数据目录已初始化：%s\n' "${data_dir}"
