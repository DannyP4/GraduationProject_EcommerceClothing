# Vesta — Fashion E-Commerce with AI Virtual Try-On

Vesta is a full-stack fashion e-commerce platform with an integrated **AI virtual try-on** studio,
a **RAG chatbot**, in-house **recommendations**, real-time **GHN shipping**, and multi-language
storefront (English / Vietnamese / Japanese). Built as a graduation thesis project.

> **Stack:** React 18 + Vite · Spring Boot 3.3 (Java 21) · MySQL 8 + Flyway · Docker

---

## Features

**Storefront**
- Catalog with categories, brands, variants (size/color), sale pricing and coupons
- Locale-aware search, product reviews (verified purchase), wishlist
- Cart & checkout — **COD, VNPAY, Stripe**; order lifecycle with status timeline
- **GHN shipping**: real-time fee at checkout, auto-create waybill on ship, auto status sync (→ delivered)
- **AI virtual try-on** (fal.ai FASHN): preview garments on your own photo
- **RAG chatbot** + **recommendations** (similar items + frequently-bought-together), powered by Gemini
- Accounts: register/login, email verification, password reset, Google OAuth, address book, notifications

**Admin**
- Dashboard & statistics, product / variant / image management, categories & brands
- Orders (transition, cancel, refund), users, reviews moderation, coupons
- DeepL-assisted catalog translation tooling

**Platform**
- i18n EN / VI / JA (react-i18next + full DeepL catalog translation)
- Cloudinary image CDN, Sentry monitoring, Cloudflare Turnstile captcha

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 5, React Router, Tailwind CSS, react-i18next, axios |
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA / Hibernate, Flyway, Lombok |
| Database | MySQL 8 (schema via Flyway, `ddl-auto=none`) |
| AI / Integrations | Google Gemini (embeddings, chatbot, recommendations), fal.ai (virtual try-on), GHN (shipping), VNPAY & Stripe (payments), DeepL (translation), Cloudinary (images), Google OAuth, Cloudflare Turnstile, Sentry |
| Tooling | Docker & Docker Compose, Maven |

---

## Prerequisites

- **Docker Desktop** (runs MySQL)
- **JDK 21** + **Maven 3.9+**
- **Node.js 20 LTS** (npm)
- **Git Bash** (Windows) for the `source .env` step below

Free ports: `3307` (MySQL), `8080` (backend), `5173` (frontend dev).

---

## Getting Started (development)

```bash
# 1. Clone
git clone <repo-url> vesta && cd vesta

# 2. Environment — copy the template and fill in values
cp .env.example .env
#    A minimal local run only needs DB_*, JWT_SECRET and VITE_API_BASE_URL.
#    External integrations (VNPAY/Stripe/FAL/Gemini/GHN/Mail/OAuth) are feature-gated
#    and degrade gracefully when their keys are blank.

# 3. Start MySQL (Docker)
docker compose up -d mysql
docker compose ps                 # wait until "healthy" (~60s)

# 4. Backend  (Git Bash — loads every var from .env into the environment)
cd backend
set -a; source ../.env; set +a
export CAPTCHA_ENABLED=false MAIL_ENABLED=false      # smooth local demo
mvn spring-boot:run               # http://localhost:8080/api

# 5. Frontend
cd ../frontend
npm install
npm run dev                       # http://localhost:5173
```

On first start against an empty database the backend **bootstraps itself**:
Flyway builds the schema → `CatalogSeedRunner` loads ~2,000 products from
`backend/src/main/resources/db/seed/catalog_seed.sql` → `AdminBootstrap` creates the admin →
demo orders are seeded. No database dump is required.

**Default admin:** `longpd1911@gmail.com` / `longan47`

> The single root `.env` configures **both** backend and frontend — Vite reads it via `envDir: '../'`.
> There is no separate `frontend/.env`.

### Full Docker stack (alternative)

```bash
docker compose up -d --build      # mysql + backend + frontend
```
Frontend on `http://localhost:5173`, backend on `:8080`. (Slower first build; the dev flow above
is recommended for day-to-day work.)

---

## Testing

```bash
cd backend
set -a; source ../.env; set +a
mvn test                          # runs against a separate `uniform_store_test` DB
```
Integration tests use a dedicated test database and never touch dev data.

---

## Project Structure

```
.
├── backend/                  # Spring Boot API
│   └── src/main/
│       ├── java/com/uniform/store/   # controllers, services, entities, bootstrap, events
│       └── resources/
│           ├── db/migration/         # Flyway migrations (V1…Vn)
│           └── db/seed/catalog_seed.sql   # catalog seed (auto-loaded when empty)
├── frontend/                 # React + Vite SPA
│   └── src/{pages,components,services,locales}
├── docker/mysql-init/        # MySQL container init scripts
├── docker-compose.yml
└── .env.example
```

---

## Configuration

All configuration lives in the root `.env` (see `.env.example` for the full list). Notable groups:

- **Core (required):** `DB_*`, `JWT_SECRET`, `VITE_API_BASE_URL`
- **Payments:** `VNPAY_*`, `STRIPE_*`
- **AI:** `GEMINI_API_KEY` (chatbot/recommendations), `FAL_KEY` (virtual try-on)
- **Shipping:** `GHN_API_KEY`, `GHN_SHOP_ID`, `GHN_FROM_DISTRICT_ID`
- **Media / i18n / auth:** `CLOUDINARY_*`, `DEEPL_API_KEY`, `GOOGLE_OAUTH_*`, `TURNSTILE_*`, `MAIL_*`
- **Feature flags:** `CATALOG_SEED_ENABLED`, `CAPTCHA_ENABLED`, `MAIL_ENABLED`, `GHN_STATUS_SYNC_ENABLED`

---

## Author

**Phạm Đức Long** — **20225737**
HEDSPI, Hanoi University of Science and Technology
✉️ longpd1911@gmail.com
