# ============================================================
# Multi-stage build: Vue frontend + Spring Boot backend
# All one-jar deployment modifications are applied at build time
# via sed/rm/heredoc — source code stays untouched.
#
# Usage:
#   docker build -t ruoyi-vue .
#   docker compose up -d          # start MySQL + Redis
#   docker run --network host ruoyi-vue
# ============================================================

# ----------------------------------------------------------
# Stage 1: Build Vue frontend
# ----------------------------------------------------------
FROM node:20-alpine AS frontend-build

WORKDIR /app

COPY ruoyi-ui/package.json ruoyi-ui/package-lock.json ./
RUN npm ci

COPY ruoyi-ui/ ./

# For one-jar deployment, API calls go to the same origin (no prefix)
RUN sed -i "s|VITE_APP_BASE_API = '/prod-api'|VITE_APP_BASE_API = ''|" .env.production

RUN npm run build:prod

# ----------------------------------------------------------
# Stage 2: Build Spring Boot backend
# ----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /build

# Cache Maven dependencies first
COPY pom.xml ./
COPY ruoyi-admin/pom.xml     ruoyi-admin/pom.xml
COPY ruoyi-common/pom.xml    ruoyi-common/pom.xml
COPY ruoyi-framework/pom.xml ruoyi-framework/pom.xml
COPY ruoyi-generator/pom.xml ruoyi-generator/pom.xml
COPY ruoyi-quartz/pom.xml    ruoyi-quartz/pom.xml
COPY ruoyi-system/pom.xml    ruoyi-system/pom.xml

RUN mvn dependency:go-offline -B || true

# Copy full source
COPY . .

# --- Build-time source modifications for one-jar deployment ---

# 1. SecurityConfig: add static resource patterns to permitAll
RUN sed -i '/requestMatchers(HttpMethod\.GET/c\                    .requestMatchers(HttpMethod.GET, "/", "/*.html", "/***.html", "/***.css", "/***.js", "/***.ico", "/***.png", "/***.jpg", "/***.svg", "/***.woff", "/***.woff2", "/***.ttf", "/***.eot", "/assets/**", "/static/**", "/profile/**").permitAll()' \
    ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java

# 2. Remove SysIndexController (its "/" mapping conflicts with index.html)
RUN rm -f ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysIndexController.java

# 3. Fix upload path for Linux container
RUN sed -i 's|profile: D:/ruoyi/uploadPath|profile: /app/uploadPath|' \
    ruoyi-admin/src/main/resources/application.yml

# 4. Create SpaController for Vue Router history mode fallback
RUN cat > ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/SpaController.java << 'JAVAEOF'
package com.ruoyi.web.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA前端路由回退控制器
 * 将前端路由路径转发到index.html，支持Vue Router history模式
 */
@Controller
public class SpaController
{
    @GetMapping(value = {
        "/{path:[^\\.]*}",
        "/{path:[^\\.]*}/{subpath:[^\\.]*}",
        "/{path:[^\\.]*}/{subpath:[^\\.]*}/{subsubpath:[^\\.]*}"
    })
    public String forward()
    {
        return "forward:/index.html";
    }
}
JAVAEOF

# 5. Logback: console-only output for K8s (no file appenders)
RUN cat > ruoyi-admin/src/main/resources/logback.xml << 'LOGEOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="log.pattern" value="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{20} - [%method,%line] - %msg%n" />

    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${log.pattern}</pattern>
        </encoder>
    </appender>

    <logger name="com.ruoyi" level="info" />
    <logger name="org.springframework" level="warn" />

    <root level="info">
        <appender-ref ref="console" />
    </root>
</configuration>
LOGEOF

# Copy frontend build output into Spring Boot static resources
COPY --from=frontend-build /app/dist/ ruoyi-admin/src/main/resources/static/

# Build backend
RUN mvn clean package -Dmaven.test.skip=true -B

# ----------------------------------------------------------
# Stage 3: Runtime
# ----------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

COPY --from=backend-build /build/ruoyi-admin/target/ruoyi-admin.jar app.jar

RUN mkdir -p /app/uploadPath

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx1024m", "-jar", "app.jar"]
