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
