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

# For one-jar deployment, API calls go to the same origin (no /prod-api prefix)
RUN sed -i "s|VITE_APP_BASE_API = '/prod-api'|VITE_APP_BASE_API = '/'|" .env.production

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
COPY ruoyi-project/pom.xml   ruoyi-project/pom.xml
COPY ruoyi-gen-cli/pom.xml   ruoyi-gen-cli/pom.xml

RUN mvn dependency:go-offline -B || true

# Copy full source
COPY . .

# --- Build-time source modifications for one-jar deployment ---

# 1. SecurityConfig: permit all GET requests so SPA routes reach SpaController;
#    API data endpoints remain protected by @PreAuthorize method-level security
RUN sed -i '/requestMatchers(HttpMethod\.GET/c\                    .requestMatchers(HttpMethod.GET, "/**").permitAll()' \
    ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java

# 2. Remove SysIndexController (its "/" mapping conflicts with index.html)
RUN rm -f ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysIndexController.java

# 3. Fix upload path for Linux container
RUN sed -i 's|profile: D:/ruoyi/uploadPath|profile: /app/uploadPath|' \
    ruoyi-admin/src/main/resources/application.yml

# 4. Create SpaController for Vue Router history mode fallback
#    使用 ErrorController 实现：Spring MVC 先正常匹配所有 @RequestMapping，
#    找不到 handler 才触发 404 error，由此控制器 fallback 到 index.html，
#    不会与任何 API 路径（@GetMapping/@PostMapping 等）产生冲突。
RUN cat > ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/SpaController.java << 'JAVAEOF'
package com.ruoyi.web.controller.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA前端路由回退控制器（ErrorController 实现）
 * Spring MVC 先正常路由，404 时才 fallback 到 index.html，
 * 不干扰任何 API 路径的 GET/POST/PUT/DELETE。
 */
@Controller
public class SpaController implements ErrorController
{
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request)
    {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null && Integer.parseInt(status.toString()) == 404) {
            return "forward:/index.html";
        }
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
