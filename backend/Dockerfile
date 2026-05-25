# ── Stage 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper và pom.xml trước (tận dụng Docker layer cache)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix line endings (CRLF to LF) for Windows compatibility and make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Download dependencies trước khi copy source (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Tạo user non-root cho security
RUN addgroup -S sportvenue && adduser -S sportvenue -G sportvenue
USER sportvenue

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
