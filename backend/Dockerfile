# ── Stage 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

<<<<<<< HEAD
=======
<<<<<<< HEAD
# Fix Windows line endings (CRLF → LF) using sed, then make executable
=======
# Fix line endings (CRLF to LF) for Windows compatibility and make executable
>>>>>>> af0f783c01c48b5748e64259381d94628f8aaf9e
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Download dependencies trước khi copy source (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code và build
>>>>>>> main
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S sportvenue && adduser -S sportvenue -G sportvenue \
    && mkdir -p /app/uploads/avatars \
    && chown -R sportvenue:sportvenue /app/uploads
USER sportvenue

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
