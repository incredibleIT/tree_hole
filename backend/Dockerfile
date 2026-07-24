# ============================================================
# RKOS 后端服务 - 多阶段 Docker 构建
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建（跳过测试，测试在本地/CI 执行）
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 安装 curl（用于 healthcheck，需要 root 权限）
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 jar 包
COPY --from=build /app/target/*.jar app.jar

# 创建非 root 用户（Docker 安全最佳实践）
RUN addgroup --system app && adduser --system --ingroup app app \
    && chown -R app:app /app

# 切换到非 root 用户
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
