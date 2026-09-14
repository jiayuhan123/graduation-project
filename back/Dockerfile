FROM eclipse-temurin:21-jdk-jammy AS build

ENV MAVEN_OPTS="-Xms128m -Xmx768m -XX:+UseG1GC"

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY common common
COPY user user
COPY content content
COPY learning learning
COPY teaching teaching
COPY game game
COPY file-service file-service
COPY notification-service notification-service
COPY audit-service audit-service
COPY ai ai
COPY monolith-app monolith-app

RUN chmod +x mvnw \
    && ./mvnw -B -ntp -pl monolith-app -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Shanghai

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/monolith-app/target/monolith-app-0.0.1-SNAPSHOT.jar /app/app.jar

RUN chown app:app /app/app.jar

USER app

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--Xms256m -Xmx640m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError} -jar /app/app.jar"]
