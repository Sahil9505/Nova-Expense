# Changelog

All notable changes to Nova are documented in this file. The format is based on
keeping a clear record of Added, Improved, and Fixed work per release.

## [0.4.0] — Phase 4A: Budget Foundation

### Added
- **Budget module (backend).** Full CRUD for budgets via `POST/GET/PATCH/DELETE
  /api/budgets`, plus an optional `?active=true|false` list filter. Includes
  entity, repository, service, DTOs, MapStruct mapper, and controller following the
  existing finance-domain conventions.
- **Budget model.** Budgets support a name, optional description, amount, period
  (`WEEKLY` / `MONTHLY` / `YEARLY` / `CUSTOM`), an optional category scope
  (nullable for overall budgets), active status, and a start/end date range.
- **Budget validation.** Server-side rules: amount must be positive, required
  fields present, `CUSTOM` periods require both start and end dates with
  `endDate >= startDate`, duplicate budget names per user are rejected, and a
  supplied `categoryId` must belong to the caller.
- **Budget soft-delete.** Deletion flips `is_active = false` rather than removing
  the row, preserving financial history; reactivation is available through `PATCH`.
- **Budget UI.** A new Budgets page (`/budgets`) with a responsive card grid,
  skeleton loading states, an empty state, a create/edit `BudgetFormDialog`
  (React Hook Form + Zod), and a destructive-action confirm dialog. Integrated into
  the sidebar navigation.
- **Flyway migration V4.** Adds `description` and `is_active` to the `budgets`
  table and an `idx_budgets_active` index, completing the Phase 1 schema stub.
- **Tests.** `BudgetApiTest` covers auth, CRUD, validation, duplicate-name
  conflict, category ownership, soft-delete, active filtering, and cross-user
  isolation (12 cases). Budget query/command helpers added to the shared test
  harness.

### Improved
- README phase reference and in-app version marker advanced to Phase 4A (v0.4.0).

### Fixed
- The `Budget.Period` enum stub (DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY) did not match
  the Phase 4A specification; it is now `WEEKLY/MONTHLY/YEARLY/CUSTOM`.
- The `budgets` table was missing `description` and `is_active` columns; these are
  now added via migration V4 so the entity and schema agree under `ddl-auto=validate`.

## [0.3.0] — Phase 3: Core Finance
- Accounts, categories, and transactions CRUD with automatic balance keeping.
- Live dashboard summary.

## [0.2.0] — Phase 2: Authentication & User Management
- JWT access/refresh tokens, registration, login, profile, password change.

## [0.1.0] — Phase 1: Foundation
- Backend shell, design system, schema, health endpoint, and documentation.
