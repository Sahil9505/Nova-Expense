# Nova

**Nova** is a premium, AI-powered personal finance platform focused on expense tracking, budgeting, financial insights, receipt intelligence, and a polished fintech dashboard experience.

This repository is the **Phase 1 foundation**: a clean, production-grade monorepo that future phases will build on. It establishes the backend (Spring Boot), the frontend (React + TypeScript), shared design system, database migrations, and the architectural standards that govern everything that follows.

> Phase 1 intentionally ships a foundation only. Full CRUD, authentication, and analytics arrive in later phases.

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
- [Testing](#testing)
- [Docker (Optional)](#docker-optional)
- [Roadmap](#roadmap)

---

## Tech Stack

**Backend**
- Java 21, Spring Boot 3
- Spring Web, Spring Security (foundation), Spring Data JPA
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
│       ├── components/      # ui primitives, layout, health
│       ├── context/        # ThemeProvider
│       ├── hooks/          # useHealth, etc.
│       ├── lib/            # api client, query client, utils
│       ├── pages/          # Dashboard, NotFound
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

- **Phase 2** — Authentication & User Management (JWT, accounts, profiles)
- **Phase 3** — Transactions, categories, and budgeting CRUD
- **Phase 4** — Analytics and financial insights
- **Phase 5** — Receipt intelligence

See `docs/NOVA_ARCHITECTURE_BIBLE.md` for the full vision, standards, and conventions.
