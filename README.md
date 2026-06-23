# VESTA - Fashion E-Commerce Platform

VESTA is a full-stack fashion e-commerce platform built as a graduation thesis project. It combines a modern storefront, an admin back office, online payments, shipping integration, multilingual catalog support, AI-assisted shopping, and virtual try-on.

> Stack: React 18 + Vite, Spring Boot 3.3 + Java 21, MySQL 8, Docker Compose

## Highlights

### Storefront

- Product catalog with categories, brands, variants, sale pricing, coupons, wishlist, reviews, cart, checkout, and order tracking.
- Payment options: COD, VNPay sandbox, and Stripe sandbox.
- GHN shipping integration for shipping fee calculation and waybill-related order flow.
- Account features: register, login, JWT authentication, Google OAuth, password reset, address book, and notifications.
- Cloudflare Turnstile support for login, registration, and password recovery flows.
- Multilingual storefront with English, Vietnamese, and Japanese resources.

### AI Features

- Virtual try-on through fal.ai FASHN.
- Gemini-based shopping assistant with retrieval-augmented generation.
- Product recommendations, including similar products and frequently bought together suggestions.
- DeepL-assisted catalog translation workflow.

### Admin

- Dashboard and statistics.
- Product, variant, image, category, and brand management.
- Order management with status transitions, cancellation, refund-related flow, and timeline.
- User, review, coupon, and notification management.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React 18, Vite 5, React Router, Tailwind CSS, react-i18next, axios |
| Backend | Java 21, Spring Boot 3.3, Spring Security, JWT, Spring Data JPA, Hibernate, Flyway, Lombok |
| Database | MySQL 8 |
| AI and integrations | Gemini, fal.ai, DeepL, GHN, VNPay, Stripe, Cloudinary, Google OAuth, Cloudflare Turnstile, Sentry |
| DevOps | Docker, Docker Compose, Caddy, GitHub Actions |

## Architecture

The application is organized as a client-server system:

- `frontend`: React SPA built by Vite and served by Nginx in Docker.
- `backend`: Spring Boot REST API served under `/api`.
- `mysql`: MySQL 8 database with persistent Docker volume.
- `caddy`: reverse proxy on the production server, responsible for HTTPS and access logging.

For production deployment, Caddy receives public HTTPS traffic and forwards requests to the frontend container. The frontend container serves static assets and proxies API calls to the backend container. MySQL is only exposed inside the Docker network and should not be public.

## Project Structure

```text
.
|-- backend/                  # Spring Boot API
|   `-- src/main/
|       |-- java/com/uniform/store/
|       `-- resources/
|           |-- db/migration/ # Flyway migrations
|           `-- db/seed/      # Seed resources
|-- frontend/                 # React + Vite SPA
|   `-- src/
|       |-- components/
|       |-- context/
|       |-- hooks/
|       |-- lib/
|       |-- locales/
|       |-- pages/
|       `-- services/
|-- docker/mysql-init/        # Optional MySQL init scripts for Docker
|-- .github/workflows/        # GitHub Actions deployment workflow
|-- docker-compose.yml
|-- .env.example
`-- README.md
```

## Prerequisites

- Docker Desktop or Docker Engine with Docker Compose
- JDK 21 and Maven 3.9+
- Node.js 20 LTS and npm
- Git

Default local ports:

| Service | Port |
| --- | --- |
| Frontend dev server | `5173` |
| Frontend Docker/Nginx | `FRONTEND_PORT` |
| Backend | `8080` |
| MySQL host mapping | `3307` |

## Environment Configuration

Copy the sample environment file and fill in local values:

```bash
cp .env.example .env
```

Important groups:

- Core: `SPRING_PROFILES_ACTIVE`, `DB_*`, `SERVER_PORT`, `BACKEND_PORT`, `JWT_SECRET`
- Frontend: `FRONTEND_PORT`, `VITE_API_BASE_URL`, `VITE_TURNSTILE_SITE_KEY`
- URL and CORS: `APP_BASE_URL`, `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS`
- Payments: `VNPAY_*`, `STRIPE_*`
- AI: `GEMINI_API_KEY`, `FAL_KEY`
- Shipping: `GHN_API_KEY`, `GHN_SHOP_ID`, `GHN_FROM_DISTRICT_ID`
- Media and auth: `CLOUDINARY_*`, `GOOGLE_OAUTH_*`, `DEEPL_API_KEY`, `TURNSTILE_SECRET_KEY`, `MAIL_*`
- Feature flags: `CAPTCHA_ENABLED`, `MAIL_ENABLED`, `GHN_STATUS_SYNC_ENABLED`, `TRYON_ENABLED`

Notes:

- Never commit `.env`, API keys, private keys, or production database dumps.
- `VITE_*` variables are baked into the frontend bundle at build time. Rebuild the frontend after changing them.
- `VITE_TURNSTILE_SITE_KEY` is public and belongs to the frontend. `TURNSTILE_SECRET_KEY` must stay server-side.

## Local Development

Start MySQL:

```bash
docker compose up -d mysql
docker compose ps
```

Run the backend:

```bash
cd backend
set -a; source ../.env; set +a
mvn spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`, and the backend API runs at `http://localhost:8080/api`.

For a smoother local demo, external verification can be disabled in `.env`:

```env
CAPTCHA_ENABLED=false
MAIL_ENABLED=false
```

## Docker Workflow

Build and run the full stack:

```bash
docker compose up -d --build
```

Rebuild only the backend:

```bash
docker compose up -d --build backend
```

Rebuild only the frontend:

```bash
docker compose up -d --build frontend
```

Recreate backend after changing backend-only environment variables:

```bash
docker compose up -d --force-recreate backend
```

View logs:

```bash
docker compose logs -f --tail=100 backend
docker compose logs -f --tail=100 frontend
docker compose logs -f --tail=100 mysql
```

## Testing

Run backend tests:

```bash
cd backend
set -a; source ../.env; set +a
mvn test
```

The automated test suite covers service-level business logic and integration paths for core flows such as authentication, orders, pricing, coupons, payments, and external-provider boundaries.

## Deployment

The project supports deployment to an Ubuntu-based virtual machine using Docker Compose and Caddy.

Production-like deployment includes:

- Docker Compose services: `mysql`, `backend`, `frontend`
- Caddy reverse proxy for HTTPS and access logs
- Environment variables stored in `.env` on the server
- GitHub Actions workflow for automated deployment

The GitHub Actions workflow is defined in:

```text
.github/workflows/deploy-ec2.yml
```

Required GitHub Actions secrets:

```text
EC2_HOST
EC2_USER
EC2_SSH_KEY
```

Deployment flow:

```text
git push origin main
-> GitHub Actions starts
-> SSH into the server
-> git pull --ff-only origin main
-> docker compose up -d --build backend frontend
-> docker compose ps
```

The public website URL is intentionally omitted from this README.

## Operations

Useful production commands:

```bash
cd ~/GraduationProject_EcommerceClothing
docker compose ps
docker compose logs -f --tail=100 backend
docker compose logs -f --tail=100 frontend
```

Create a database backup:

```bash
mkdir -p ~/backups
docker compose exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > ~/backups/vesta_$(date +%Y%m%d_%H%M%S).sql
```

Avoid deleting Docker volumes unless a database reset is intentional:

```bash
# Dangerous: removes the MySQL data volume
docker compose down -v
```

## Security Notes

- Keep production secrets only in `.env` on the server or in GitHub Actions secrets.
- Do not expose MySQL to the public Internet in production.
- Keep SSH restricted where possible. If GitHub-hosted runners need SSH access, use a dedicated deploy key and monitor access.
- Rotate leaked or accidentally shared API keys immediately.
- Frontend variables starting with `VITE_` are visible in the browser and must not contain secrets.

## Author

Pham Duc Long - 20225737  
HEDSPI, Hanoi University of Science and Technology
