# SKILL: PowerShell Windows — SportVenue Scripts

## Dev Environment Scripts

### Khởi động toàn bộ stack

```powershell
# scripts/dev-start.ps1
Write-Host "🚀 Starting SportVenue Dev Environment..." -ForegroundColor Green

# 1. Start infrastructure
Write-Host "📦 Starting Docker infrastructure..." -ForegroundColor Cyan
docker compose -f docker-compose.infra.yml up -d

# 2. Wait for PostgreSQL to be healthy
Write-Host "⏳ Waiting for PostgreSQL (port 5433)..." -ForegroundColor Yellow
$maxRetries = 30
$retries = 0
do {
    $healthy = docker inspect sportvenue-postgres --format='{{.State.Health.Status}}' 2>$null
    if ($healthy -eq "healthy") { break }
    Start-Sleep -Seconds 2
    $retries++
} while ($retries -lt $maxRetries)

if ($retries -ge $maxRetries) {
    Write-Host "❌ PostgreSQL failed to start!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Infrastructure ready!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  Terminal 2: cd backend && .\mvnw.cmd spring-boot:run `"-Dspring-boot.run.profiles=dev`"" -ForegroundColor Gray
Write-Host "  Terminal 3: cd frontend && npm run dev" -ForegroundColor Gray
```

### Dừng tất cả services

```powershell
# scripts/dev-stop.ps1
Write-Host "🛑 Stopping SportVenue..." -ForegroundColor Yellow
docker compose -f docker-compose.infra.yml down
Write-Host "✅ Done" -ForegroundColor Green
```

### Reset database hoàn toàn

```powershell
# scripts/db-reset.ps1
Write-Host "⚠️  This will DELETE ALL DATA!" -ForegroundColor Red
$confirm = Read-Host "Type 'yes' to confirm"
if ($confirm -ne "yes") { exit 0 }

docker compose -f docker-compose.infra.yml down -v
docker compose -f docker-compose.infra.yml up -d
Write-Host "✅ Database reset complete" -ForegroundColor Green
```

## Useful PowerShell Commands

```powershell
# Kiểm tra port đang bị chiếm
netstat -ano | findstr ":5432"   # Check 5432 (local PG)
netstat -ano | findstr ":5433"   # Check 5433 (Docker PG)
netstat -ano | findstr ":8080"   # Check backend
netstat -ano | findstr ":3000"   # Check frontend

# Xem process đang dùng port
Get-Process -Id (netstat -ano | findstr ":5432" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)

# Kill process theo port
$pid = (netstat -ano | findstr ":8080" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
Stop-Process -Id $pid -Force

# Test API endpoint
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/hello" | ConvertTo-Json

# Test với auth header
$headers = @{ Authorization = "Bearer your-token-here" }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/bookings/my" -Headers $headers

# Watch Docker logs
docker logs sportvenue-postgres -f
docker logs sportvenue-redis -f
```

## Maven Commands

```powershell
# Clean build
.\mvnw.cmd clean package -DskipTests

# Run với profile cụ thể
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Run tests
.\mvnw.cmd test

# Dependency tree
.\mvnw.cmd dependency:tree

# Check outdated dependencies
.\mvnw.cmd versions:display-dependency-updates
```

## NPM Commands

```powershell
# Dev server
npm run dev

# Type check (không emit files)
npx tsc --noEmit

# Lint
npm run lint

# Fix lint tự động
npm run lint -- --fix

# Production build
npm run build

# Analyze bundle size
npx @next/bundle-analyzer
```
