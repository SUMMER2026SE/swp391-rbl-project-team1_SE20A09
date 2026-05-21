# SKILL: Docker Expert — SportVenue Infrastructure

## Infrastructure Overview

```
docker-compose.infra.yml  ← Dev mode: chỉ infra services
docker-compose.yml        ← Full stack: infra + backend + frontend
```

## Key Notes cho dự án này

```yaml
# ⚠️ QUAN TRỌNG: PostgreSQL dùng port 5433 (không phải 5432)
# Lý do: Máy dev có PostgreSQL Windows local đang chiếm port 5432

postgres:
  ports:
    - "5433:5432"              # host:container
  environment:
    POSTGRES_HOST_AUTH_METHOD: trust  # Dev only: no password needed
```

## Common Commands

```powershell
# Khởi động infrastructure
docker compose -f docker-compose.infra.yml up -d

# Dừng và xóa volumes (reset DB)
docker compose -f docker-compose.infra.yml down -v

# Xem logs realtime
docker logs sportvenue-postgres -f
docker logs sportvenue-redis -f

# Connect trực tiếp vào PostgreSQL
docker exec -it sportvenue-postgres psql -U sportvenue_user -d sportvenue

# Kiểm tra health
docker compose -f docker-compose.infra.yml ps
```

## Troubleshooting

```
Port conflict:
  netstat -ano | findstr ":5432"
  → Nếu có 2 process → máy có PostgreSQL local
  → Dùng port 5433 cho Docker

Container không healthy:
  docker logs sportvenue-postgres --tail 50

Volume corrupt:
  docker compose down -v && docker compose up -d
```

## Production Dockerfile (Backend)

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```
