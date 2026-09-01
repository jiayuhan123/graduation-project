# Jenkins 控制器使用 JDK 21，并包含 Git、SSH 与 Docker CLI/Compose 插件。
# Docker daemon 不在本镜像中运行，而是通过受控的宿主机 socket 访问。
FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl git gnupg lsb-release openssh-client \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

USER jenkins

# 预装最小的 CI 插件集；首次打开仍保留官方初始化向导与管理员创建流程。
RUN jenkins-plugin-cli --plugins "workflow-aggregator git docker-workflow credentials-binding pipeline-stage-view configuration-as-code"
