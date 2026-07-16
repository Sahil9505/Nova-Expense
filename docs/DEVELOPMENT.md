# Development Guide

How to run, test, and develop Nova locally. **Docker is not required.**

---

## Prerequisites

- **Java 21** — Nova targets Java 21 specifically. Build and run tests with a
  JDK 21 toolchain. Running on newer JDKs (25/26) is not supported and may cause
  confusing test failures (for example with Mockito).
- Node.js 20+
- A reachable PostgreSQL database (Neon / Supabase free tier recommended)

---

## Environment Configuration

Copy `.env.example` to `.env` and fill in the values. All variables are optional
and have local defaults.

| Variable | Used by | Default | Purpose |
| --- | --- | --- | --- |
| `DATABASE_URL` | Backend | `jdbc:postgresql://localhost:5432/nova` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Backend | `nova` | DB user |
| `DB_PASSWORD` | Backend | `nova` | DB password |
| `SPRING_PROFILES_ACTIVE` | Backend | `local` | Active Spring profile |
| `NOVA_CORS_ALLOWED_ORIGINS` | Backend | `http://localhost:5173` | CORS allowlist |
| `VITE_API_BASE_URL` | Frontend | `http://localhost:8080` | Backend API origin |

**Loading the variables:**

- **Backend (Spring Boot)** reads **OS environment variables**, not the `.env`
  file. Export them before running, e.g. `source .env` (bash) or set them in your
  IDE run configuration. A local PostgreSQL needs no overrides thanks to the
  defaults in `application-local.yml`.
- **Frontend (Vite)** reads `.env` automatically from the `frontend/` directory
  (see `frontend/.env.example`). If omitted, it defaults to
  `http://localhost:8080`.

---

## Backend

```bash
cd backend

# Run with live reload
mvn spring-boot:run

# Or build and run the artifact
mvn clean package -DskipTests
java -jar target/nova-backend.jar
```

- Default port: **8080**
- Health: `GET /api/health`
- Docs: `http://localhost:8080/swagger-ui.html`
- Actuator: `http://localhost:8080/actuator/health`

### Profiles

| Profile | Datasource | Notes |
| --- | --- | --- |
| `local` | `DATABASE_URL` or localhost Postgres | Default for development |
| `dev` | `DATABASE_URL` (required) | Shared environment |
| `prod` | `DATABASE_URL` (required) | Hardened actuator exposure |

Spring Boot reads `DATABASE_URL` directly, so a hosted Postgres (Neon/Supabase)
works without any local database or container.

### Testing

```bash
mvn test
```

Tests boot the Spring context against an in-memory H2 database and disable Flyway,
so they run fully offline. The health endpoint is covered by a context test.

---

## Frontend

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

### Scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Start Vite dev server |
| `npm run build` | Typecheck (`tsc --noEmit`) then production build |
| `npm run preview` | Preview the production build |
| `npm run lint` | ESLint |
| `npm run typecheck` | TypeScript typecheck only |

### Mock Data

The dashboard uses static sample data from `src/data/mock.ts`. This is
intentional: the data layer is structured so a TanStack Query hook can replace the
mock imports in Phase 2 without touching the UI.

---

## Docker (Optional)

Nova does **not** require Docker. Optional assets exist for deployment only:

- `docker-compose.yml` — reference compose (Postgres + services)
- `docker/backend.Dockerfile`, `docker/frontend.Dockerfile` — optional images

To use them (only if you want a containerized Postgres locally):

```bash
docker compose up -d postgres
# then point DATABASE_URL at the container
```

The standard quick start does not use Docker at all.

---

## Conventions

See `docs/NOVA_ARCHITECTURE_BIBLE.md` for the full standards: API envelope,
database rules, naming conventions, and the design system.
