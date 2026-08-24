# ============================================================
# Dockerfile — Communa Spring Boot Application
# ============================================================
# Multi-stage build:
#   Stage 1 (builder) — Maven compiles and packages the JAR
#   Stage 2 (runtime) — Only the JRE + JAR (tiny final image)
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first (layer cache)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -S communa && adduser -S communa -G communa

WORKDIR /app

# Copy the fat JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create log directory and set ownership
RUN mkdir -p /var/log/communa && chown -R communa:communa /app /var/log/communa

USER communa

# Port the app listens on
EXPOSE 8082

# Health check (Docker monitors this)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/ || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
