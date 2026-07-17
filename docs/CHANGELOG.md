# Changelog

All notable changes to Nova are documented in this file. The format is based on
keeping a clear record of Added, Improved, and Fixed work per release.

## [0.6.0] — Phase 4C: Financial Goals

### Added
- **Goals domain (backend).** A first-class `com.nova.finance.goal` module: `Goal` and
  `GoalContribution` entities, repositories, `GoalService`, DTOs, a MapStruct `GoalMapper`,
  and `GoalController` — following the same layered conventions as the budget module.
- **Goal types.** `SAVINGS`, `DEBT_PAYOFF`, and `CUSTOM` (stored as a string enum so new
  types are a code/data change, never a migration).
- **Goal CRUD + lifecycle.** `POST/GET/PATCH/DELETE /api/goals` with an optional
  `?active` filter. Goals soft-delete (deactivate) like budgets/accounts; a separate
  `paused` flag parks a goal without losing it.
- **Goal contributions.** `POST /api/goals/{id}/contributions` logs an immutable
  contribution (amount, optional note, date). Each contribution updates the goal's
  maintained `currentAmount` in the same transaction, clamped to the target — the same
  single-source-of-truth pattern Nova uses for account balances. History is returned by
  `GET /api/goals/{id}` for the progress timeline.
- **Derived progress.** A pure `GoalCalculator` / `GoalCalculation` engine computes
  percentage complete, remaining, derived `GoalStatus` (NOT_STARTED / IN_PROGRESS /
  ACHIEVED / OVERDUE / PAUSED), and a best-effort estimated completion date from
  contribution velocity. Mirrors the reusable Budget Intelligence engine (D-4B-1).
- **Goal endpoints (no breaking changes).** `GET /api/goals/summary` returns the rolled-up
  overview (totals, status counts, overall percent) plus every goal with derived progress
  in one response. A single grouped query aggregates contribution stats for all goals, so
  the list/summary never issue per-goal queries (no N+1).
- **Goal UI.** A new Goals page (`/goals`) with summary `StatCard`s, a responsive card
  grid, skeleton loading, an empty state, a create/edit `GoalFormDialog` (React Hook Form +
  Zod), a `ContributionDialog`, and a `GoalDetailDialog` showing the contribution history
  timeline. Reuses `Card`/`Dialog`/`Progress`/`Badge`/`StatCard`/`ConfirmDialog` — no new
  design primitives.
- **Dashboard goal integration.** `GoalDashboardWidget` composes Goal Summary, Goal
  Progress, Upcoming Deadlines, and Recently Completed from one summary query, dropping
  into the existing dashboard layout beside the budget widgets.
- **Sidebar + version.** Goals nav item added; in-app version marker advanced to
  Phase 4C (v0.6.0).
- **Tests.** `GoalCalculatorTest` (percentage/remaining/status precedence/estimated
  completion) is pure; `GoalApiTest` and `GoalContributionIntegrationTest` cover auth,
  CRUD, validation, duplicate-name conflict, soft-delete, pause, cross-user isolation,
  contribution math (including target clamp), and the summary roll-up.
- **Flyway migration V5.** Adds `goals` and `goal_contributions` tables (with indexes and
  cascade deletes) completing the goals schema; V1/V3/V4 are never edited.

### Improved
- Goals are computed from real contribution data (no placeholders, no mock calculations)
  and never persist a status column — status is always derived on read.
- The existing budgets/accounts/transactions/dashboard APIs and DTOs are unchanged
  (backward compatible); goals are purely additive.

### Fixed
- None in this phase. (See `BUG_TRACKER.md` for carried-forward items.)

## [0.5.0] — Phase 4B: Budget Intelligence

### Added
- **Reusable Budget Intelligence engine (backend).** A centralized, presentation-agnostic
  calculation layer under `com.nova.finance.budget`:
  - `BudgetCalculator` — the single source of budget math (remaining, percentage
    used, and `HEALTHY`/`WARNING`/`EXCEEDED` status from configurable thresholds).
  - `BudgetPeriods` — resolves each budget's spend window (week/month/year use the
    period containing "today"; custom uses its own inclusive range).
  - `BudgetCalculationService` — orchestrates per-budget and rolled-up summaries.
- **Configurable health thresholds.** `nova.budget.warning-threshold` (default `0.80`)
  and `nova.budget.exceeded-threshold` (default `1.00`) via `BudgetProperties`,
  bound with `@EnableConfigurationProperties` like the existing config beans.
- **Budget endpoints (no breaking changes).** `GET /api/budgets/summary` returns
  the rolled-up overview (active budgets, total budgeted, total spent, remaining,
  status counts, and per-budget metrics); `GET /api/budgets/{id}/metrics` returns a
  single budget's live figures. Both follow the standard `ApiResponse` envelope.
- **N+1-free aggregation.** The summary loads all relevant expenses in a single
  query over the union of budget windows, then buckets them in memory per budget —
  no per-budget round-trips.
- **Budget UI.** The Budgets page now shows, per card: amount, spent, remaining,
  percentage used, a live status badge, and an animated accessible `Progress` bar.
  A summary strip (active budgets, total budgeted, total spent, remaining) reuses
  the existing `StatCard` layout.
- **Reusable `Progress` component.** `'@/components/ui/progress'` — animated
  (`transition-[width]`, reduced-motion aware), theme-consistent, `role="progressbar"`
  with value bounds. Built for reuse by future Goals/Analytics/AI modules.
- **Dashboard budget integration.** New Budget Health, Recently Exceeded, and Healthy
  Budgets widgets (`BudgetIntelligenceWidget`) compose from one summary query,
  dropping into the existing dashboard layout without disturbing other widgets.
- **Tests.** `BudgetCalculatorTest` (percentage/remaining/status boundaries, zero and
  over-budget) and `BudgetPeriodsTest` (weekly/monthly/yearly/custom windows)
  are pure unit tests; `BudgetIntelligenceApiTest` covers the new endpoints end-to-end
  (category vs. overall aggregation, income exclusion, the four periods, deactivation
  effects). The shared test harness now reuses seeded categories idempotently.

### Improved
- Budget figures are now computed from real transaction data (no placeholders, no mock
  calculations) and never persisted — they are always derived on read.
- The existing `Budget`/list/get/PATCH/DELETE APIs and DTOs are unchanged (backward
  compatible); intelligence is purely additive.

### Fixed
- A lazy-proxy `LazyInitializationException` when resolving the owner's preferred
  currency for the summary; the currency is now projected inside the service
  transaction via `UserRepository.findPreferredCurrencyById`.

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
