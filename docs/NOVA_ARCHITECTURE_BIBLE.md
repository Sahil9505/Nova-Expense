# Nova Architecture Bible

The single source of truth for how Nova is designed and built. Every contributor
— and every future phase — must follow the standards in this document.

---

## 1. Product Vision

Nova is a premium, AI-powered personal finance platform. It helps people
understand and control their money through:

- **Expense tracking** — capture and categorize spending with minimal friction.
- **Budgeting** — plan limits and see progress in real time.
- **Financial insights** — trends, forecasts, and anomaly detection.
- **Receipt intelligence** — extract structured data from receipts.
- **A polished dashboard** — a calm, confident fintech experience.

The product feeling we are chasing is **"premium fintech SaaS"**: deep navy
foundations, electric-blue accents, elegant cards, professional typography, and
smooth but restrained motion. No childish design. No default-looking Bootstrap or
Material UI appearance. Mobile-responsive from day one.

---

## 2. Quality Principle: Quality Over Quantity

We would rather ship **fewer things done exceptionally well** than many things
poorly. This principle drives every decision:

- Prefer a small, correct, well-tested surface over a broad, shallow one.
- Delete dead code, unused imports, and placeholder cruft. They are bugs waiting
  to happen.
- Leave no TODOs, fake buttons, unfinished pages, or broken links in a deliverable.
- Consistency beats cleverness. A predictable codebase scales; a clever one rots.

If something is not ready, it is not shipped. Partial features are tracked, not
merged half-finished.

---

## 3. Feature Tier Strategy

Features are classified so we know how much polish they deserve:

| Tier | Meaning | Expectation |
| --- | --- | --- |
| **Core** | Defining product capability | Production-grade, fully tested, accessible |
| **Supporting** | Enables a core feature | Solid, documented, tested where it matters |
| **Experimental** | Exploration / phased | Clearly isolated, behind its phase |

Phase 1 is, by definition, **foundation**: it establishes Core infrastructure
(backend shell, design system, schema) so later tiers can be built safely.

---

## 4. Backend Architecture Rules

- **Layered, package-by-domain.** Root package is `com.nova`. Cross-cutting
  concerns live in `common`; domain logic lives in `user` and `finance.*`.
- **Constructor injection only.** No field injection (`@Autowired` on fields).
  Inject through constructors; let the compiler enforce completeness.
- **No business logic in controllers.** Controllers map HTTP ↔ application
  calls. Services hold logic; repositories hold data access.
- **DTOs where useful.** Never expose entities directly on write paths; map with
  MapStruct.
- **Consistent error handling.** All errors flow through `GlobalExceptionHandler`
  and return the standard `ApiResponse` envelope.
- **Validation-ready.** Use Bean Validation (`jakarta.validation`) on inputs.
- **No dumping grounds.** Utilities belong with the domain that uses them, not in
  a catch-all `util` package.
- **Profiles.** `local`, `dev`, `prod` are first-class. Configuration is
  environment-driven; secrets come from the environment, never the repo.
- **Flyway owns the schema.** Migrations are versioned and reviewed like code.
  Hibernate runs with `ddl-auto=validate` against Flyway-managed schema.

---

## 5. Frontend Architecture Rules

- **TypeScript strictness is non-negotiable.** `strict`, `noUnusedLocals`, and
  `noUnusedParameters` are enabled to keep the codebase clean.
- **Component structure.** Small, focused, reusable primitives in
  `components/ui`; composed features in `pages`; layout in `components/layout`.
- **Design tokens, not magic values.** Colors, spacing, and radii come from the
  Tailwind theme mapped to CSS variables.
- **No giant components.** If a component exceeds a clear responsibility, split
  it.
- **Data access through TanStack Query.** The dashboard currently uses static
  mock data, intentionally structured so a query hook can replace it later.
- **Accessible by default.** Buttons have labels, landmarks are present
  (`header`, `nav`, `main`), charts have text alternatives, and interactive
  controls are keyboard-reachable.
- **Responsive at every breakpoint.** Mobile, tablet, and desktop are designed
  for, not bolted on.
- **No console errors.** Runtime errors are caught by the global error boundary;
  logging is purposeful, not accidental.

---

## 6. API Response Standards

Every response uses a single envelope (`com.nova.common.api.ApiResponse<T>`):

```json
{
  "success": true,
  "message": "OK",
  "data": { },
  "timestamp": "2026-07-16T12:00:00Z"
}
```

Rules:

- `success` — boolean, always present.
- `message` — human-readable summary.
- `data` — payload on success; `null` (omitted) or an `ApiError` object on failure.
- `timestamp` — server time (ISO-8601).
- Validation failures return `success: false` with an `ApiError` containing a
  list of field-level `errors` (`field`, `message`).
- Errors use stable `ErrorCode` values (e.g., `RESOURCE_NOT_FOUND`,
  `VALIDATION_ERROR`, `CONFLICT`) rather than free-text.

---

## 7. Database Standards

- **PostgreSQL** is the system of record.
- **UUID primary keys** for all entities.
- **snake_case** for table and column names.
- **Audit columns** `created_at` / `updated_at` on every table (via
  `BaseEntity`).
- **Foreign keys** are explicit with sensible `ON DELETE` behavior.
- **Indexes** on foreign keys and common query columns.
- **Flyway migrations** are the only way schema changes ship. Keep Phase 1 schema
  clean and avoid premature complexity.

---

## 8. UI Design System Rules

- Deep navy background (`#0F172A`) with elevated surfaces (`#1E293B`).
- Electric-blue primary (`#3B82F6`) with secondary (`#60A5FA`) and accent cyan
  (`#38BDF8`).
- Success / warning / danger: `#10B981` / `#F59E0B` / `#EF4444`.
- Text `#F8FAFC`, muted `#94A3B8`.
- Dark mode is the default; light mode is fully supported via a `class` toggle.
- Motion is smooth but restrained — short, ease-out transitions only.
- Components are built shadcn/ui-style: primitive, composable, theme-aware.

### Color palette

| Token | Hex | RGB (for CSS vars) |
| --- | --- | --- |
| Background (dark) | `#0F172A` | `15 23 42` |
| Surface (dark) | `#1E293B` | `30 41 59` |
| Primary | `#3B82F6` | `59 130 246` |
| Secondary | `#60A5FA` | `96 165 250` |
| Accent | `#38BDF8` | `56 189 248` |
| Success | `#10B981` | `16 185 129` |
| Warning | `#F59E0B` | `245 158 11` |
| Danger | `#EF4444` | `239 68 68` |
| Text | `#F8FAFC` | `248 250 252` |
| Muted | `#94A3B8` | `148 163 184` |

---

## 9. Naming Conventions

**Backend (Java)**
- Packages: lowercase, reverse-domain (`com.nova.finance.account`).
- Classes: `PascalCase`; suffixes signal role — `Controller`, `Service`,
  `Repository`, `Entity`, `Dto`, `Config`, `Exception`.
- Methods: `camelCase`, verb-led (`findByEmail`, `createUser`).
- Constants: `UPPER_SNAKE_CASE`.

**Frontend (TypeScript/React)**
- Files: `kebab-case.ts(x)` for components and hooks.
- Components: `PascalCase` exports.
- Types/interfaces: `PascalCase`.
- Variables/functions: `camelCase`.
- CSS classes: Tailwind utilities; custom component classes in `@layer`.

**Database**
- Tables: `snake_case`, plural where natural (`app_users`, `transactions`).
- Columns: `snake_case`.
- Migrations: `V{n}__{description}.sql`.

---

## 10. Folder Structure

```
backend/src/main/java/com/nova/
  NovaApplication
  common/
    api/         # ApiResponse envelope
    config/      # CORS, OpenAPI, config properties
    exception/   # GlobalExceptionHandler, ErrorCode, domain exceptions
    security/    # SecurityConfig
    validation/  # ValidationError model
  health/        # Health endpoint
  user/          # User domain
  finance/
    account/ transaction/ category/ budget/

frontend/src/
  components/ui/      # Button, Card, Input, Badge, StatCard, ChartCard, ...
  components/layout/  # AppLayout, Sidebar, Topbar, ThemeToggle
  components/health/  # HealthStatusWidget
  context/  hooks/  lib/  pages/  routes/  types/  data/
```

---

## 11. No AI Watermark Rule

Nova is a product, not a demonstration. **Under no circumstances** may the project
contain references that it was generated or assisted by an AI tool. This includes:

- Attribution statements that imply machine authorship in the README, docs,
  UI, metadata, or package descriptions.
- Comments or code that attribute authorship to a tool or service.
- Watermarks, banners, or hidden markers implying machine authorship.

Write documentation and UI copy as a professional engineering team would. The work
speaks for itself.

---

## 12. Future Phase Roadmap

| Phase | Scope |
| --- | --- |
| **1 (current)** | Foundation: backend shell, design system, schema, health, docs |
| **2** | Authentication & User Management (JWT, accounts, profiles) |
| **3** | Transactions, categories, budgets — full CRUD |
| **4** | Analytics & financial insights |
| **5** | Receipt intelligence |

Each phase extends this foundation without rewrites. The architecture above is
designed so Phase 2+ slots in cleanly.
