# VESTA Setup Guide

This document explains how to run the VESTA project locally from source code. The recommended setup for grading or quick evaluation is Docker Compose, because it starts MySQL, Redis, the Spring Boot backend, and the React frontend in one consistent environment.

## 1. Prerequisites

Install the following tools:

- Git
- Docker Desktop or Docker Engine with Docker Compose
- JDK 21
- Maven 3.9+
- Node.js 20 LTS and npm

If you only want to run the project quickly with Docker Compose, Docker is the most important requirement. JDK, Maven, and Node.js are needed when running backend/frontend separately in development mode.

Default local ports:

| Component | Port |
| --- | --- |
| Frontend Docker container | `5173` or `FRONTEND_PORT` |
| Frontend Vite dev server | `5173` |
| Backend API | `8080` |
| MySQL host mapping | `3307` |
| Redis | `6379` |

## 2. Get The Source Code

```bash
git clone https://github.com/DannyP4/GraduationProject_EcommerceClothing.git
cd GraduationProject_EcommerceClothing
```

If the project is submitted as a compressed source package, extract it and open a terminal at the project root. The root directory should contain:

```text
backend/
frontend/
docker-compose.yml
.env.example
README.md
SETUP.md
```

## 3. Configure Environment Variables

Create `.env` from the sample file:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Open `.env` and fill in at least the following values:

```env
SPRING_PROFILES_ACTIVE=docker

DB_HOST=localhost
DB_PORT=3307
DB_NAME=uniform_store
DB_USERNAME=uniform
DB_PASSWORD=uniform_pass
DB_ROOT_PASSWORD=rootpassword

SERVER_PORT=8080
BACKEND_PORT=8080

SPRING_CACHE_TYPE=redis
REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=please_change_this_to_a_random_secret_with_at_least_32_characters
JWT_ACCESS_TOKEN_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000

CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8088

FRONTEND_PORT=5173
VITE_API_BASE_URL=/api

APP_BASE_URL=http://localhost:8080/api
FRONTEND_BASE_URL=http://localhost:5173

CAPTCHA_ENABLED=false
MAIL_ENABLED=false
TRYON_ENABLED=false
GHN_ENABLED=false
GHN_STATUS_SYNC_ENABLED=false
DEEPL_ENABLED=false
AI_EMBEDDINGS_BACKFILL_ON_STARTUP=false
```

Notes:

- `JWT_SECRET` must not be empty and should contain at least 32 characters.
- Do not commit `.env`.
- API keys for Gemini, fal.ai, Stripe, VNPay, Cloudinary, Gmail, DeepL, and GHN are optional for a basic local demo. If these keys are unavailable, disable related integrations with `*_ENABLED=false`.
- Variables starting with `VITE_` are baked into the frontend bundle at build time. Rebuild the frontend after changing them.

Generate a random `JWT_SECRET` with PowerShell:

```powershell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

Or with OpenSSL:

```bash
openssl rand -base64 64
```

## 4. Quick Run With Docker Compose

From the project root, run:

```bash
docker compose up -d --build
```

Check container status:

```bash
docker compose ps
```

When the containers are running:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

View logs if needed:

```bash
docker compose logs -f --tail=100 backend
docker compose logs -f --tail=100 frontend
docker compose logs -f --tail=100 mysql
docker compose logs -f --tail=100 redis
```

Stop the application:

```bash
docker compose down
```

Do not run the following command unless you intentionally want to delete the MySQL data volume:

```bash
docker compose down -v
```

## 5. Initial Data

When the backend starts with an empty database, it automatically:

1. Runs Flyway migrations to create the database schema.
2. Creates the default admin account.
3. Loads sample catalog data from `backend/src/main/resources/db/seed/catalog_seed.sql` if the product catalog is empty.
4. Creates demo data for orders, reviews, recommendations, and related workflows.

Default demo admin account:

```text
Email: longpd1911@gmail.com
Password: longan47
```

This account is intended for demo and grading purposes. Change it before any real deployment.

If the submission includes a separate SQL dump under `docker/mysql-init/`, MySQL will execute those scripts only on the first creation of the MySQL Docker volume. If a previous MySQL volume already exists, init scripts will not run again.

## 6. Development Mode

Use this mode when modifying backend or frontend code directly.

### 6.1. Start MySQL And Redis With Docker

```bash
docker compose up -d mysql redis
docker compose ps
```

Wait until `vesta_mysql` and `vesta_redis` become `healthy`.

### 6.2. Run Backend

On Git Bash, Linux, or macOS:

```bash
cd backend
set -a
source ../.env
set +a
mvn spring-boot:run
```

On Windows PowerShell, it is often easier to run the backend through Docker:

```powershell
docker compose up -d --build backend
```

Backend API:

```text
http://localhost:8080/api
```

### 6.3. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

If frontend requests go to the wrong backend URL, check:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

When the frontend is served by Docker/Nginx, use:

```env
VITE_API_BASE_URL=/api
```

## 7. Run Tests

Start MySQL and Redis first:

```bash
docker compose up -d mysql redis
```

Run all backend tests:

```bash
cd backend
set -a
source ../.env
set +a
mvn test
```

On Windows PowerShell, if loading `.env` with `source` is inconvenient, run tests from the IDE and configure environment variables in the IDE run configuration.

## 8. Common Commands

Rebuild backend:

```bash
docker compose up -d --build backend
```

Rebuild frontend:

```bash
docker compose up -d --build frontend
```

Recreate backend after changing backend-only environment variables:

```bash
docker compose up -d --force-recreate backend
```

Clean unused Docker resources:

```bash
docker system prune -f
```

Backup database:

```bash
mkdir -p backups
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backups/vesta_backup.sql
```

## 9. Troubleshooting

### Port Already In Use

If `5173`, `8080`, or `3307` is already used, change these values in `.env`:

```env
FRONTEND_PORT=8088
BACKEND_PORT=8081
DB_PORT=3308
```

Then run:

```bash
docker compose up -d --build
```

### Backend Fails Because Of JWT

Check `JWT_SECRET`. It must not be empty and should contain at least 32 characters.

### Login Shows `Captcha verification required`

For local demo without Turnstile keys, use:

```env
CAPTCHA_ENABLED=false
VITE_TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=
```

Then rebuild frontend and recreate backend:

```bash
docker compose up -d --build frontend
docker compose up -d --force-recreate backend
```

### Captcha Does Not Appear After Setting Turnstile Site Key

`VITE_TURNSTILE_SITE_KEY` is a build-time variable. Rebuild the frontend after changing it:

```bash
docker compose build --no-cache frontend
docker compose up -d frontend
```

### MySQL Init Scripts Do Not Run Again

Docker runs scripts in `docker/mysql-init/` only when the MySQL volume is created for the first time. To reinitialize from scratch, remove the volume. This deletes the current database:

```bash
docker compose down -v
docker compose up -d --build
```

## 10. Security Notes

- Do not submit `.env` with real API keys if the source code is public.
- Do not submit SSH private keys, `.pem` files, GitHub tokens, or database dumps containing sensitive information.
- Variables starting with `VITE_` are visible in the frontend bundle and must not contain secrets.
- If any key has been exposed publicly, revoke it and create a new one.

## 11. Additional Data File

The embedding/data SQL file `01-uniform_store.sql` (about 30 MB) is not included directly in the source repository. It will be submitted separately in the `additional documents/data` section (in qldt) below.
