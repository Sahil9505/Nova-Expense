# Bug Tracker

Tracks known issues and their resolution. Open bugs block releases; fixed bugs are
moved to **Fixed Bugs** with the version they were resolved in.

## Open Bugs

None known.

## Fixed Bugs

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
