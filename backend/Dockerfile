# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first (take advantage of Docker layer cache)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix Windows line endings (CRLF to LF) using sed, then make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Download dependencies before copying source (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S sportvenue && adduser -S sportvenue -G sportvenue
USER sportvenue

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
