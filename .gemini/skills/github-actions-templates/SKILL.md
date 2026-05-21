# SKILL: GitHub Actions Templates

## Backend CI (đã có: .github/workflows/backend.yml)

Workflow mẫu cho Spring Boot:

```yaml
name: Backend CI

on:
  push:
    branches: [main, develop]
    paths: ['backend/**']
  pull_request:
    branches: [main, develop]
    paths: ['backend/**']

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: sportvenue_test
          POSTGRES_USER: sportvenue_user
          POSTGRES_PASSWORD: testpassword
          POSTGRES_HOST_AUTH_METHOD: trust
        ports: ['5432:5432']
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and Test
        working-directory: backend
        run: mvn clean verify -Dspring.profiles.active=test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/sportvenue_test
          SPRING_DATASOURCE_USERNAME: sportvenue_user
          SPRING_DATASOURCE_PASSWORD: testpassword
```

## Frontend CI

```yaml
name: Frontend CI

on:
  push:
    branches: [main, develop]
    paths: ['frontend/**']
  pull_request:
    branches: [main, develop]
    paths: ['frontend/**']

jobs:
  lint-and-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Type check
        working-directory: frontend
        run: npx tsc --noEmit

      - name: Lint
        working-directory: frontend
        run: npm run lint

      - name: Build
        working-directory: frontend
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: http://localhost:8080
```

## Secrets Setup (GitHub → Settings → Secrets)

```
Repository secrets cần thiết:
  POSTGRES_PASSWORD       ← production DB password
  JWT_SECRET              ← production JWT signing key
  MINIO_ROOT_PASSWORD     ← MinIO admin password

Optional (khi có deployment):
  VERCEL_TOKEN            ← Vercel deployment
  RAILWAY_TOKEN           ← Railway backend deployment
```

## PR Checks Template

```yaml
# .github/pull_request_template.md
## 📋 Mô tả thay đổi
<!-- Tóm tắt những gì đã thay đổi và tại sao -->

## 🔗 Liên quan đến
Closes #

## ✅ Checklist
- [ ] Code đã được test locally
- [ ] Không có lỗi TypeScript (`tsc --noEmit`)
- [ ] Không có lỗi ESLint
- [ ] Backend build thành công (`mvn clean package`)
- [ ] Không commit file `.env`

## 📸 Screenshots (nếu có UI change)
```
