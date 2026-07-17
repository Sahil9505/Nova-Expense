# Decision Log

Architectural decisions for Nova. Every non-trivial choice is recorded here with its
context, the options considered, and the impact, so future phases can build on a clear
rationale.

---

## D-4A-1 — Budget list returns a plain `List`, not Spring pagination

- **Date:** 2026-07-17
- **Decision:** `GET /api/budgets` returns `List<BudgetResponse>` (with an optional
  `?active` filter), matching every other finance endpoint.
- **Reason:** Accounts, categories, transactions, and the dashboard all return plain
  `List` payloads inside the `ApiResponse` envelope. The spec asks for a "Paginated
  List" but supplies no pagination parameters.
- **Alternatives considered:**
  1. Spring Data `Page<BudgetResponse>` with `page`/`size`/`sort` query params, used
     only for budgets.
  2. A custom page envelope.
- **Why alternatives were rejected:** Both would introduce a second response shape
  unique to the budget module, breaking the "one envelope, one list convention"
  principle of the Architecture Bible and forcing the frontend to special-case budget
  responses. Pagination can be added uniformly across all finance modules later if
  real scale demands it.
- **Impact:** Frontend consumes budgets with the same `useQuery<List<Budget>>` pattern
  as other domains. No pagination metadata on the client yet.

---

## D-4A-2 — `Budget.Period` aligned to `WEEKLY / MONTHLY / YEARLY / CUSTOM`

- **Date:** 2026-07-17
- **Decision:** Use exactly the four periods from the Phase 4A spec. `CUSTOM`
  requires both `startDate` and `endDate`; other periods require only `startDate`.
- **Reason:** The spec is explicit about the supported periods, and the prior stub
  enum (`DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY`) was never exposed publicly.
- **Alternatives considered:**
  1. Keep the stub enum and map spec periods onto it.
  2. Add `CUSTOM` alongside the stub's five values.
- **Why alternatives were rejected:** Keeping an unused/divergent enum would create a
  contract that doesn't match the deliverable and confuse later phases (4B analytics).
- **Impact:** Clean, spec-matching enum; `CUSTOM` drives a conditional end-date field
  in both the API validation and the UI form.

---

## D-4A-3 — Budgets use soft-delete (`is_active`) like Accounts

- **Date:** 2026-07-17
- **Decision:** `DELETE /api/budgets/{id}` sets `is_active = false`; a `PATCH` can
  reactivate. History is preserved.
- **Reason:** Budgets are financial records; the spec calls for an active status and
  a sensible lifecycle. Accounts already use this pattern.
- **Alternatives considered:**
  1. Hard-delete (like Categories).
  2. Hard-delete with a separate audit table.
- **Why alternatives were rejected:** Hard-delete loses the record and the
  active/inactive lifecycle the spec requires; an audit table is premature complexity
  for this phase.
- **Impact:** List filtering by `active` is supported; the UI shows Active/Inactive
  badges and offers deactivate/reactivate actions.

---

## D-4A-4 — Migration V4 extends the Phase 1 `budgets` table

- **Date:** 2026-07-17
- **Decision:** Add `description`, `is_active`, and `idx_budgets_active` via a new
  `V4__budgets_description_and_active.sql`; V1/V3 are never edited.
- **Reason:** Flyway owns the schema and `ddl-auto=validate` checks the entity against
  it. The Phase 1 table was incomplete for Phase 4A.
- **Alternatives considered:**
  1. Edit the V1 migration to add the columns.
  2. Recreate the table in V4.
- **Why alternatives were rejected:** Editing an applied migration breaks
  reproducibility and the "migrations are reviewed like code" rule; recreating the
  table would drop data and duplicate DDL.
- **Impact:** Schema now matches the entity; `ddl-auto=validate` passes with no
  warnings.

---

## D-4A-5 — Duplicate budget name guard is per-user, case-insensitive

- **Date:** 2026-07-17
- **Decision:** A user may not have two budgets with the same name (trimmed,
  case-insensitive), regardless of active state; conflicts return `409 CONFLICT`.
- **Reason:** Prevents ambiguous budgets in the UI and matches the account/category
  duplicate-name guards.
- **Alternatives considered:**
  1. Allow duplicates.
  2. Scope uniqueness to active budgets only.
- **Why alternatives were rejected:** Duplicates harm UX; scoping to active-only would
  allow reviving a deactivated name and re-creating confusion.
- **Impact:** `existsByUserIdAndNameIgnoreCase(AndIdNot)` repository methods enforce
  this on create and update.

---

## D-4A-6 — Category ownership is enforced on budget creation

- **Date:** 2026-07-17
- **Decision:** If a `categoryId` is supplied when creating/updating a budget, it must
  belong to the caller; otherwise the API returns `404 RESOURCE_NOT_FOUND`.
- **Reason:** Consistency with transaction/category ownership checks; prevents a user
  from attaching another user's category.
- **Alternatives considered:**
  1. Return `400 BAD_REQUEST` for a foreign category.
  2. Silently ignore a foreign category.
- **Why alternatives were rejected:** `404` matches the existing ownership-check
  contract (treats it as "not your resource"); silently ignoring would mask a bug.
- **Impact:** The UI only offers the caller's own categories in the budget form, and
  the API defends the rule regardless.

---

## Cross-cutting decisions

- **No new exception types.** Budget errors reuse `ConflictException`,
  `BadRequestException`, and `ResourceNotFoundException`; all flow through the existing
  `GlobalExceptionHandler` into the standard `ApiResponse`/`ApiError` envelope.
- **MapStruct for mapping.** `BudgetMapper` projects the entity to `BudgetResponse`
  and embeds a minimal `CategoryRef`, mirroring the account/category mapper style.
- **Frontend parity.** The Budgets page and `BudgetFormDialog` reuse the existing
  `Dialog`, `Select`, `TextField`, `Button`, `Card`, `Badge`, `EmptyState`,
  `ConfirmDialog`, skeleton loaders, and toast system — no new design primitives.

---

## D-4B-1 — Budget intelligence is a centralized, reusable engine

- **Date:** 2026-07-17
- **Decision:** All budget math lives in `com.nova.finance.budget`
  (`BudgetCalculator`, `BudgetPeriods`, `BudgetCalculation`, `BudgetStatus`,
  `BudgetCalculationService`) — separate from controllers, services, and UI.
- **Reason:** The spec requires the same calculations to be reusable by future
  Analytics, Financial Goals, and AI modules. Scattering math across the
  `BudgetController` or the React cards would force every future module to
  re-derive it (and likely diverge).
- **Alternatives considered:**
  1. Compute spent/remaining/status inside `BudgetController` per request.
  2. Compute them in the frontend from raw transactions.
- **Why alternatives were rejected:** (1) duplicates logic with the dashboard and
  blocks reuse; (2) pushes business rules and DB aggregation to the client,
  breaks offline-correctness, and cannot enforce the single-query performance
  rule. The engine is the single source of truth and is pure/testable.
- **Impact:** Frontend and future modules consume `BudgetCalculation`/`BudgetMetrics`
  directly; the engine has no web or persistence dependency.

---

## D-4B-2 — Recurring budgets measure the *current* calendar window

- **Date:** 2026-07-17
- **Decision:** `WEEKLY`/`MONTHLY`/`YEARLY` budgets are evaluated against the
  calendar period containing "today" (week starts Monday, ISO-8601); only `CUSTOM`
  budgets use their own explicit `[startDate, endDate]` range.
- **Reason:** A "July groceries" budget created in January should still report July's
  spend on the live dashboard, not a stale January window. Recurring limits are
  inherently about the current period.
- **Alternatives considered:**
  1. Anchor every recurring budget to its `startDate` anniversary.
  2. Treat recurring budgets as infinite (sum all history).
- **Why alternatives were rejected:** (1) surprises users (a budget "expires" until its
  anniversary) and complicates the UI; (2) defeats the purpose of a period limit.
- **Impact:** `BudgetPeriods.resolve(budget, referenceDate)` is the single window
  resolver; tests pin it for all four periods and boundary inclusion.

---

## D-4B-3 — One query for the whole summary (no N+1)

- **Date:** 2026-07-17
- **Decision:** `BudgetCalculationService.summary` loads expense rows for the *union*
  of all budgets' windows in a single `TransactionRepository.findExpenseRows` call,
  then buckets them in memory per budget (category-scoped vs. overall).
- **Reason:** A budget per budget would issue one query each (classic N+1) as the
  spec explicitly forbids. A single in-memory pass mirrors the dashboard's existing
  aggregation style.
- **Alternatives considered:**
  1. One `SUM` query per budget.
  2. A native SQL window/union query returning all budgets at once.
- **Why alternatives were rejected:** (1) N+1 at scale; (2) a large, hard-to-test
  SQL that still needs in-memory pairing with entities. The single-load + in-memory
  bucket is simple, testable, and adds zero round-trips.
- **Impact:** Cost is one expense query regardless of budget count; values are computed
  once per request.

---

## D-4B-4 — Intelligence is additive; the existing Budget API is untouched

- **Date:** 2026-07-17
- **Decision:** New endpoints (`/api/budgets/summary`,
  `/api/budgets/{id}/metrics`) and new DTOs (`BudgetMetrics`,
  `BudgetMetricsResponse`, `BudgetSummaryResponse`) are added; the existing
  `BudgetResponse` and CRUD endpoints are not modified.
- **Reason:** Rule 6 (preserve backward compatibility) and the Architecture Bible's
  "one envelope" convention. Extending rather than changing keeps every prior
  client working.
- **Alternatives considered:**
  1. Embed metrics inside `BudgetResponse`.
  2. Version the budget API (`/api/v2/budgets`).
- **Why alternatives were rejected:** (1) changes a stable DTO and bloats every
  list payload; (2) premature for an additive feature.
- **Impact:** Frontend fetches `/summary` once for both the Budgets strip and the
  Dashboard widgets; the bare list remains available for forms/lookups.

---

## D-4B-5 — Configurable, defaulted health thresholds

- **Date:** 2026-07-17
- **Decision:** `HEALTHY` < 80%, `WARNING` 80–99%, `EXCEEDED` ≥ 100%
  via `BudgetProperties` (`nova.budget.warning-threshold` / `exceeded-threshold`,
  as fractions). Sensible defaults apply if unconfigured.
- **Reason:** The spec asks for tunable boundaries; a config bean lets ops adjust
  without a code change, and defaults keep the module zero-config.
- **Alternatives considered:**
  1. Hard-coded constants in `BudgetCalculator`.
  2. Per-budget threshold fields on the entity.
- **Why alternatives were rejected:** (1) not tunable; (2) user-facing complexity the
  spec does not ask for. A deploy-time config bean is the right layer.
- **Impact:** `BudgetCalculator.evaluate(amount, spent, thresholds)` is the only
  classifier; unit tests cover both defaults and custom thresholds.
