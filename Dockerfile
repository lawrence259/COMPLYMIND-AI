# ─────────────────────────────────────────────────────────────
# COMPLYMIND AI — Multi-stage Dockerfile
# Stage 1: Build with Maven
# Stage 2: Runtime with lean JRE 17
# ─────────────────────────────────────────────────────────────

# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Cache Maven dependencies first (faster rebuilds)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q

# Copy source and build (skip tests — tests run in CI pipeline separately)
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
LABEL org.opencontainers.image.title="COMPLYMIND AI" \
      org.opencontainers.image.description="AICTE Approval Workflow Automation & Compliance Intelligence" \
      org.opencontainers.image.version="1.0.0"

# Non-root user for security
RUN addgroup -S complymind && adduser -S complymind -G complymind
USER complymind

WORKDIR /app

# Persistent volumes: evidence uploads + H2 data (override with postgres profile in prod)
VOLUME ["/app/uploads/evidence", "/app/data"]

# Copy built JAR
COPY --from=builder /workspace/target/complymind-ai-1.0.0-SNAPSHOT.jar app.jar

# Expose HTTP port
EXPOSE 8080

# Health check — Spring Boot Actuator not yet included; fall back to login endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/login || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
