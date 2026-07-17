# Nova

**Nova** is a premium personal finance platform focused on expense tracking, budgeting, financial insights, and a polished fintech dashboard experience.

The repository has grown through five phases on a clean, production-grade monorepo: the **Phase 1** foundation (backend, frontend, design system, migrations, standards), **Phase 2** authentication and user management, **Phase 3 — Core Finance** (full CRUD for accounts, categories, and transactions plus a live dashboard), **Phase 4A — Budget Foundation** (complete budgets module), **Phase 4B — Budget Intelligence** (progress, remaining, and health analytics), and the current **Phase 4C — Financial Goals**, which adds a first-class goals domain alongside budgets.

> Phase 4C is functional end to end: define long-term savings, debt-payoff, or custom goals; log contributions that maintain a running total; track derived progress, status, and an estimated completion date; and see goals surfaced on the dashboard — all using the same design and data patterns as the rest of Nova.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Quick Start](#quick-start)
  - [1. Configure the database](#1-configure-the-database)
  - [2. Run the backend](#2-run-the-backend)
  - [3. Run the frontend](#3-run-the-frontend)
- [Health & API](#health--api)
- [Finance API (Phase 3)](#finance-api-phase-3)
- [Frontend Routes](#frontend-routes)
- [Testing](#testing)
- [Docker (Optional)](#docker-optional)
- [Roadmap](#roadmap)

---

## Tech Stack

**Backend**
- Java 21, Spring Boot 3
- Spring Web, Spring Security (JWT auth, BCrypt), Spring Data JPA
- PostgreSQL, Flyway
- Bean Validation, Lombok, MapStruct
- OpenAPI / Swagger, Spring Boot Actuator

**Frontend**
- React 18, TypeScript, Vite
- Tailwind CSS (Nova design tokens), shadcn/ui-style components
- React Router, TanStack Query, React Hook Form, Zod
- Recharts, Lucide React, Framer Motion

**Infrastructure**
- GitHub Actions CI
- Environment-variable configuration
- Optional Docker assets (not required for local development)

---

## Prerequisites

You do **not** need Docker to run Nova locally. The only external dependency is a PostgreSQL database, which can be a free hosted instance (Neon, Supabase, or similar).

- **Java 21** (local JDK). Nova targets Java 21 specifically — build and run the
  backend, including tests, with a JDK 21 toolchain. Newer JDKs (25/26) are not
  supported and can cause confusing test failures (for example with Mockito).
- **Node.js 20+** (local)
- **A PostgreSQL database** — a remote instance (Neon / Supabase free tier) is recommended. A local PostgreSQL works too.

---

## Repository Structure

```
nova/
├── backend/                 # Spring Boot API
│   └── src/main/java/com/nova
│       ├── common/         # api, config, exception, security, validation
│       ├── health/         # Health endpoint
│       ├── user/           # User domain (foundation)
│       └── finance/        # account, transaction, category, budget
├── frontend/               # React + TypeScript SPA
│   └── src/
│       ├── components/      # ui primitives, layout, auth, finance forms
│       ├── context/        # ThemeProvider, AuthProvider
│       ├── hooks/          # useAccounts, useCategories, useTransactions, useDashboard, …
│       ├── lib/            # api client, query client, validations, finance helpers
│       ├── pages/          # Dashboard, Accounts, Categories, Transactions, Profile, …
│       └── types/          # shared types
├── docs/                   # Architecture bible, development, database design
├── docker/                 # Optional Dockerfiles
├── .github/workflows/      # CI
├── docker-compose.yml      # Optional, not required for local dev
├── .env.example
└── README.md
```

---

## Quick Start

### 1. Configure the database

Copy the example environment file and set your PostgreSQL connection:

```bash
cp .env.example .env
```

Set `DATABASE_URL` to your PostgreSQL JDBC URL. For hosted providers this typically looks like:

```
DATABASE_URL=jdbc:postgresql://<host>/<db>?sslmode=require
DB_USERNAME=your_user
DB_PASSWORD=your_password
```

> **Note:** Spring Boot reads **OS environment variables**, not the `.env` file
> directly. Export the variables before starting the backend, e.g.
> `source .env`, or set them in your shell/IDE. Sensible defaults in
> `application-local.yml` already point at a local PostgreSQL, so a local database
> needs no configuration. No Docker container is required — point `DATABASE_URL`
> at any reachable PostgreSQL instance.

### 2. Run the backend

From the `backend/` directory:

```bash
# Using Maven
mvn spring-boot:run

# Or build and run the jar
mvn clean package -DskipTests
java -jar target/nova-backend.jar
```

The backend starts on port `8080`. On startup, Flyway applies the schema migrations automatically. The active Spring profile is `local` by default (override with `SPRING_PROFILES_ACTIVE`).

### 3. Run the frontend

From the `frontend/` directory:

```bash
npm install
npm run dev
```

The app is served at `http://localhost:5173` and proxies API calls to the backend origin (configurable via `VITE_API_BASE_URL`).

---

## Health & API

| Endpoint | Description |
| --- | --- |
| `GET /api/health` | Application liveness (returns the standard API envelope) |
| `GET /actuator/health` | Spring Boot Actuator health |
| `GET /swagger-ui.html` | OpenAPI / Swagger UI |
| `GET /v3/api-docs` | OpenAPI specification |

### Authentication

Phase 2 adds stateless JWT authentication. Public endpoints are limited to health,
documentation, and the unauthenticated auth routes; every other route requires a
valid access token in the `Authorization: Bearer <token>` header.

| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create an account, returns tokens + profile |
| `POST` | `/api/auth/login` | Public | Authenticate, returns tokens + profile |
| `POST` | `/api/auth/refresh` | Public* | Rotate a refresh token for a new token pair |
| `POST` | `/api/auth/logout` | Protected | Revoke refresh token(s) |
| `GET` | `/api/users/me` | Protected | Current user profile |
| `PATCH` | `/api/users/me` | Protected | Update profile (name, currency, timezone, avatar) |
| `PATCH` | `/api/users/me/password` | Protected | Change password (revokes other sessions) |

\* The refresh endpoint is public but requires a valid refresh token in the body.

All responses use the standard `ApiResponse` envelope:

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "accessToken": "eyJ…",
    "refreshToken": "eyJ…",
    "tokenType": "Bearer",
    "expiresInSeconds": 900,
    "user": { "id": "…", "email": "you@example.com", "role": "USER", "accountStatus": "ACTIVE" }
  },
  "timestamp": "2026-07-16T…"
}
```

Example health response:

```json
{
  "success": true,
  "message": "OK",
  "data": { "status": "UP", "service": "nova-backend", "timestamp": "2026-07-16T..." },
  "timestamp": "2026-07-16T..."
}
```

---

## Finance API (Phase 3)

All finance endpoints are protected and scoped to the authenticated user — you only
ever see and mutate your own accounts, categories, and transactions. Responses use
the standard `ApiResponse` envelope.

**Accounts** — cash, checking, savings, credit card, and wallet accounts.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/accounts` | List the user's accounts |
| `POST` | `/api/accounts` | Create an account |
| `GET` | `/api/accounts/{id}` | Get one account |
| `PATCH` | `/api/accounts/{id}` | Update an account (including reactivation) |
| `DELETE` | `/api/accounts/{id}` | Deactivate an account (history preserved) |

**Categories** — typed `INCOME` or `EXPENSE`, with sensible system defaults seeded per user.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/categories` | List income and expense categories |
| `POST` | `/api/categories` | Create a category |
| `GET` | `/api/categories/{id}` | Get one category |
| `PATCH` | `/api/categories/{id}` | Update a category |
| `DELETE` | `/api/categories/{id}` | Delete a non-system, unused category |

**Transactions** — `INCOME`, `EXPENSE`, or `TRANSFER`. Amounts are stored positive;
direction comes from the type. Creating, updating, or deleting a transaction keeps
account balances consistent automatically (income adds, expense subtracts, transfer
moves between accounts).

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/transactions` | List transactions; supports `type`, `accountId`, `categoryId`, `from`, `to`, `search` filters |
| `POST` | `/api/transactions` | Record a transaction |
| `GET` | `/api/transactions/{id}` | Get one transaction |
| `PATCH` | `/api/transactions/{id}` | Update a transaction (balances re-synced) |
| `DELETE` | `/api/transactions/{id}` | Delete a transaction (balance effect reversed) |

- `INCOME` / `EXPENSE` require an `accountId` and a matching `categoryId`.
- `TRANSFER` requires a source `accountId` and a `destinationAccountId` (which must differ) and uses no category.

**Budgets** — spending limits (`WEEKLY` / `MONTHLY` / `YEARLY` / `CUSTOM`) scoped to a category or overall spending.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/budgets` | List budgets; optional `?active=true\|false` filter |
| `POST` | `/api/budgets` | Create a budget |
| `GET` | `/api/budgets/{id}` | Get one budget |
| `PATCH` | `/api/budgets/{id}` | Update a budget (including reactivation) |
| `DELETE` | `/api/budgets/{id}` | Deactivate a budget (history preserved) |
| `GET` | `/api/budgets/summary` | Rolled-up budget health + per-budget metrics |
| `GET` | `/api/budgets/{id}/metrics` | Live metrics for one budget |

**Goals** — long-term objectives (`SAVINGS` / `DEBT_PAYOFF` / `CUSTOM`). Each goal maintains a running `currentAmount` updated by logged contributions; progress, status, and an estimated completion date are derived on read.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/goals` | List goals; optional `?active=true\|false` filter |
| `POST` | `/api/goals` | Create a goal (optionally seeded with `currentAmount`) |
| `GET` | `/api/goals/{id}` | Get one goal with its contribution history |
| `PATCH` | `/api/goals/{id}` | Update a goal (including `paused` / reactivation) |
| `DELETE` | `/api/goals/{id}` | Deactivate a goal (history preserved) |
| `POST` | `/api/goals/{id}/contributions` | Log a contribution and update the running total |
| `GET` | `/api/goals/summary` | Rolled-up goal health + per-goal progress |

**Dashboard**

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/dashboard/summary` | Aggregated snapshot: total balance, monthly income/expenses, net cash flow, six-month trend, category breakdown, and recent transactions |

---

## Frontend Routes

All application routes below are protected and render inside the authenticated app
shell; unauthenticated visitors are redirected to `/login`.

| Route | Description |
| --- | --- |
| `/` | Dashboard — live KPIs, cash flow chart, category breakdown, recent activity, budget and goal widgets |
| `/accounts` | Accounts list with create/edit and deactivate/reactivate |
| `/categories` | Income and expense categories with create/edit/delete |
| `/transactions` | Transactions list with type/account/category/date/search filters |
| `/transactions/new` | Create a transaction |
| `/transactions/:id/edit` | Edit an existing transaction |
| `/budgets` | Budgets list with create/edit, active lifecycle, and progress |
| `/goals` | Goals list with create/edit, contributions, and progress |
| `/settings/profile` | Profile and password management |

Public routes: `/login`, `/register`, `/forgot-password`.

---

## Testing

```bash
# Backend: compile + run tests (uses an in-memory H2 database)
cd backend && mvn test

# Frontend: typecheck + lint + production build
cd frontend && npm run typecheck
cd frontend && npm run lint
cd frontend && npm run build
```

---

## Docker (Optional)

Docker is **not required** for local development. Optional assets are provided for deployment convenience:

- `docker-compose.yml` — optional Postgres + services (reference only)
- `docker/backend.Dockerfile`, `docker/frontend.Dockerfile` — optional images

The quick start above does not use Docker. See `docs/DEVELOPMENT.md` for details.

---

## Roadmap

- **Phase 2** — Authentication & User Management (JWT access/refresh tokens, registration, login, profile, password change, role foundation) — *complete*
- **Phase 3** — Core Finance: accounts, categories, and transactions CRUD with automatic balance keeping and a live dashboard summary — *complete*
- **Phase 4A** — Budget Foundation: complete budget CRUD, validation, ownership, and a native Budgets UI — *complete*
- **Phase 4B** — Budget Intelligence: progress, remaining, and health analytics over a reusable calculation engine — *complete*
- **Phase 4C** — Financial Goals: goal domain, contributions, derived progress, dashboard widgets, and a native Goals UI — *complete* (this release)
- **Phase 5** — Financial insights and receipt intelligence

See `docs/NOVA_ARCHITECTURE_BIBLE.md` for the full vision, standards, and conventions.
