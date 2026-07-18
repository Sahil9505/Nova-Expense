# Bug Tracker

Tracks known issues and their resolution. Open bugs block releases; fixed bugs are
moved to **Fixed Bugs** with the version they were resolved in.

## Open Bugs

None known.

## Fixed Bugs

### B-6-1 — Receipt upload must never persist a row with a null `storage_key`
- **Severity:** High (design safeguard)
- **Symptom:** N/A — caught by code review, not observed in production.
- **Root cause:** The `receipts.storage_key` column is `NOT NULL`, so persisting a row
  before a storage key exists would violate the constraint and surface as a 500.
- **Fix:** `ReceiptService.upload` stores the image **first** (obtaining the key), then
  inserts the `Receipt` exactly once with `storageKey` already set — a single INSERT that
  can never violate the constraint. Any failure while reading the uploaded bytes is wrapped
  in `RECEIPT_STORAGE_FAILED` rather than leaking a generic 500.
- **Resolved in:** v0.8.0

### B-4A-1 — Budget `Period` enum did not match the Phase 4A spec
- **Severity:** Low
- **Symptom:** The `Budget.Period` stub exposed `DAILY / WEEKLY / MONTHLY /
  QUARTERLY / YEARLY`, but the Phase 4A deliverable requires `WEEKLY / MONTHLY /
  YEARLY / CUSTOM`.
- **Root cause:** The entity stub was created before the budget requirements were
  finalized and never reconciled.
- **Fix:** Changed the enum to `WEEKLY / MONTHLY / YEARLY / CUSTOM`. Because the
  entity was not yet exposed via any API or migration, the change carried no
  migration or contract risk.
- **Resolved in:** v0.4.0

### B-4A-2 — `budgets` table missing `description` and `is_active`
- **Severity:** Medium
- **Symptom:** The V1 `budgets` table lacked `description` and `is_active`, so the
  intended budget model (optional description, active lifecycle) could not be
  persisted; Hibernate `ddl-auto=validate` would reject the entity.
- **Root cause:** The Phase 1 schema only sketched the budgets table; the columns
  were deferred to Phase 4 but never added.
- **Fix:** Added migration `V4__budgets_description_and_active.sql` (never editing
  V1/V3) introducing both columns and an `idx_budgets_active` index.
- **Resolved in:** v0.4.0

### B-4A-3 — Test database shared stale budgets across finance tests
- **Severity:** Low (test-only)
- **Symptom:** Budget tests that registered two users, or relied on an empty budget
  list, collided with budgets committed by prior test methods (the in-memory H2
  database is shared for the whole run and `ddl-auto=create-drop` only drops at the
  end).
- **Root cause:** `register` and `create*` helpers commit their own transactions;
  budgets persisted between test methods.
- **Fix:** The shared finance test harness now clears budgets in `@BeforeEach`,
  isolating budget tests without touching other suites.
- **Resolved in:** v0.4.0

### B-4B-1 — `LazyInitializationException` resolving currency for budget summary
- **Severity:** High (blocked the new `GET /api/budgets/summary` endpoint)
- **Symptom:** The summary endpoint returned `500` because the controller read
  `user.getPreferredCurrency()` from a `User` proxy obtained via
  `userRepository.getReferenceById(...)` *after* the service's read-only
  transaction had closed.
- **Root cause:** `getReferenceById` returns an uninitialized lazy proxy; the
  currency field was touched outside the persistence context.
- **Fix:** The currency is now projected inside the service transaction via
  `UserRepository.findPreferredCurrencyById(...)` (a targeted `@Query`), and the
  controller passes a placeholder default to the service. No lazy access escapes the
  transaction.
- **Resolved in:** v0.5.0

### B-4B-2 — Test harness collided with seeded categories
- **Severity:** Low (test-only)
- **Symptom:** `BudgetIntelligenceApiTest` got `409 CONFLICT` when the
  `createCategory` helper posted a duplicate of a category seeded on registration
  (e.g. "Food").
- **Root cause:** The helper assumed a unique name and always `POST`ed, ignoring
  the seeded system/user categories already present.
- **Fix:** `AbstractFinanceApiTest.createCategory` is now idempotent — it returns an
  existing same-name (case-insensitive, same type) category before attempting a
  create. No production code changed.
- **Resolved in:** v0.5.0

### B-4C-1 — Goal tests needed to clear both goals and contributions
- **Severity:** Low (test-only)
- **Symptom:** The shared in-memory H2 database is reused across the whole test run
  (`ddl-auto=create-drop` only drops at the end), so goal + contribution rows committed
  by one test method would leak into the next, producing duplicate-name conflicts and
  inflated summary counts.
- **Root cause:** `register`/`create*` helpers commit their own transactions; the budget
  harness only cleared `budgets`, leaving the new `goal_contributions`/`goals` tables
  populated between test methods.
- **Fix:** `GoalApiTest` and `GoalContributionIntegrationTest` extend the finance harness
  and add a `@BeforeEach` that deletes contributions then goals before each method,
  isolating goal suites without touching other finance tables. No production code changed.
- **Resolved in:** v0.6.0
