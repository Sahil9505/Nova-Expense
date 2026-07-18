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

---

## Phase 4C — Financial Goals

### D-4C-1 — Goals are a first-class domain mirroring the Budget module

- **Date:** 2026-07-17
- **Decision:** Goals live in `com.nova.finance.goal` with the same layered shape as
  budgets: entity → repository → service → DTOs → MapStruct mapper → controller. The
  model reuses `BaseEntity` (audit columns), the `ApiResponse` envelope, the existing
  exception types (`ConflictException` / `BadRequestException` / `ResourceNotFoundException`),
  and `UserRepository.findPreferredCurrencyById` for the summary currency.
- **Reason:** The Architecture Bible mandates consistency over novelty; budgets are the
  closest solved analog and their patterns are proven. Reusing them keeps the codebase
  uniform and shrinks the surface a future phase must learn.
- **Alternatives considered:**
  1. A generic `objectives` table shared by goals and future modules.
  2. Embedding goals inside the budget module.
- **Why alternatives were rejected:** (1) over-engineering for one domain and would
  force goals into a budget-shaped schema; (2) goals are conceptually distinct from
  budgets (accumulate-toward-a-target vs. spend-against-a-limit) and deserve their own
  bounded context.
- **Impact:** Every goal endpoint returns the standard envelope; the Goals UI reuses
  `Card`/`Dialog`/`Progress`/`Badge`/`StatCard`/`EmptyState`/`ConfirmDialog` exactly as
  budgets do.

### D-4C-2 — Pure, centralized goal math in `GoalCalculator`

- **Date:** 2026-07-17
- **Decision:** All goal progress (percentage complete, remaining, derived status,
  estimated completion) is computed by `GoalCalculator` + `GoalCalculation` — pure,
  persistence-free, reusable value objects in the goal package, parallel to
  `BudgetCalculator` / `BudgetCalculation`.
- **Reason:** The spec requires the same calculations to be reusable by future
  Analytics/AI modules without re-deriving them in the controller or UI. D-4B-1 already
  established this pattern for budgets.
- **Alternatives considered:**
  1. Reuse `BudgetCalculator` for goals.
  2. Compute progress in the controller.
- **Why alternatives were rejected:** (1) budget math is window/spend-based and does not
  model accumulated-progress-toward-a-target; forcing a fit would distort both domains.
  (2) pushes business rules to the web layer and breaks reuse/testability.
- **Impact:** `GoalCalculator` is unit-tested in isolation; the service only orchestrates
  data and calls `evaluate(...)`.

### D-4C-3 — `currentAmount` is a maintained running total, not a sum computed on read

- **Date:** 2026-07-17
- **Decision:** A goal stores `current_amount` and each contribution updates it inside
  the same `@Transactional` method that inserts the `goal_contributions` row. Reads
  never recompute a SUM over history.
- **Reason:** This mirrors how Nova keeps `account.balance` in sync with `transactions`
  — a single, always-consistent source of truth that stays correct under concurrency and
  makes the list/summary endpoints O(1) per goal (no aggregation per read).
- **Alternatives considered:**
  1. Derive `currentAmount` from `SUM(goal_contributions.amount)` on every read.
  2. Store contributions only and compute the total in the controller.
- **Why alternatives were rejected:** (1) a N+1-style aggregate on list/summary reads and
  drift risk if a contribution is ever corrected; (2) pushes math to the web layer.
  Maintaining the total also lets a goal be seeded with an existing balance
  (`createGoal.currentAmount`) for goals already in progress.
- **Impact:** Contributions are immutable history; `current_amount` is clamped to the
  target so a goal can never report more than 100% complete.

### D-4C-4 — One query for the whole goals list and summary (no N+1)

- **Date:** 2026-07-17
- **Decision:** `GoalContributionRepository.aggregateByGoalIds(goalIds)` loads total /
  count / first-date / last-date for every goal in a single grouped query; the service
  pairs the rows with the entities in memory. The list, detail, and summary all share
  this single-load pattern.
- **Reason:** The spec forbids N+1 and asks to reuse existing aggregation patterns. A
  single grouped query — like `BudgetCalculationService`'s union-window expense load —
  scales to any number of goals with zero extra round-trips.
- **Alternatives considered:**
  1. One aggregate query per goal.
  2. Load every contribution row and group in memory.
- **Why alternatives were rejected:** (1) classic N+1; (2) loads far more rows than
  needed (full history) when only stats are required for the list/summary.
- **Impact:** `GoalResponse` carries a fully-derived `GoalProgress` (including estimated
  completion) everywhere, so the UI never issues follow-up calls.

### D-4C-5 — Status is mostly derived; `PAUSED` is the only manual input

- **Date:** 2026-07-17
- **Decision:** `GoalStatus` (NOT_STARTED / IN_PROGRESS / ACHIEVED / OVERDUE / PAUSED) is
  derived from progress and `target_date`, except `PAUSED`, which the API sets via
  `paused`. Precedence: ACHIEVED > PAUSED > OVERDUE > IN_PROGRESS > NOT_STARTED.
- **Reason:** Deriving status removes a class of "stale status" bugs and matches the
  spec's "derived where appropriate". A manual pause is genuinely user intent and cannot
  be inferred. `ACHIEVED` takes top precedence so a completed goal is never mislabeled.
- **Alternatives considered:**
  1. Persist a mutable `status` column updated on every write.
  2. Derive everything including pause from data.
- **Why alternatives were rejected:** (1) drifts from reality and needs careful
  invalidation; (2) a pause is a preference, not derivable from amounts/dates.
- **Impact:** No `status` column exists on the entity; the API never accepts it. The
  frontend drives pause/resume through `PATCH { paused }`.

### D-4C-6 — Intelligence is additive; the existing API is untouched

- **Date:** 2026-07-17
- **Decision:** New endpoints (`POST/GET/PATCH/DELETE /api/goals`,
  `POST /api/goals/{id}/contributions`, `GET /api/goals/summary`) are added alongside the
  existing finance API; no prior endpoint or DTO is modified.
- **Reason:** Rule 6 (backward compatibility) and the Architecture Bible's one-envelope
  convention. Extending rather than changing keeps every prior client working.
- **Alternatives considered:**
  1. Fold goals into the budget endpoints.
  2. Version the API.
- **Why alternatives were rejected:** (1) goals are not budgets; (2) premature for an
  additive feature.
- **Impact:** Frontend fetches `/summary` once for both the Goals strip and the dashboard
  widgets; the bare list remains available for forms/lookups.

### D-4C-7 — `goal_type` is a string enum for forward-compatible expansion

- **Date:** 2026-07-17
- **Decision:** `goals.goal_type` is `VARCHAR(32)` storing SAVINGS / DEBT_PAYOFF / CUSTOM
  via `Goal.Type` (`EnumType.STRING`), exactly like `budgets.period`.
- **Reason:** The spec requires SAVINGS / DEBT_PAYOFF / CUSTOM now but "allow future
  expansion without schema rewrites". Storing the enum name (not an ordinal) means a new
  type is a data/code change, not a migration.
- **Alternatives considered:**
  1. A join-table of goal types.
  2. A Postgres `ENUM` type.
- **Why alternatives were rejected:** (1) premature complexity for three fixed values;
  (2) altering a native enum later requires a migration and locks the column's domain.
- **Impact:** V5 migration never edits V1/V3/V4; the type list can grow by adding an enum
  constant.

### D-4C-8 — Dashboard integration reuses the `BudgetIntelligenceWidget` pattern

- **Date:** 2026-07-17
- **Decision:** The dashboard gains a `GoalDashboardWidget` (filterable: Goal Progress /
  Upcoming Deadlines / Recently Completed) composed from one `GET /api/goals/summary`
  query, dropped into the existing dashboard layout beside the budget widgets.
- **Reason:** Reuses the proven widget + single-summary-query pattern (D-4B-4) so the
  dashboard stays consistent and issues no extra per-goal requests.
- **Alternatives considered:**
  1. A bespoke goals dashboard section with its own endpoints.
  2. Embedding goals inside the budget widgets.
- **Why alternatives were rejected:** (1) duplicates the widget pattern; (2) conflates
  two distinct domains. The widget composes client-side from the shared summary, keeping
  the API surface minimal.
- **Impact:** Four requested dashboard slices (Goal Summary, Goal Progress, Upcoming
  Deadlines, Recently Completed) are covered by the summary header + three filtered
  widget instances.

---

## Phase 4D — Premium UI & Design System Refinement

### D-4D-1 — Atmospheric background is pure CSS, fixed, and theme-token-driven

- **Date:** 2026-07-18
- **Decision:** The application background is a single fixed, `position: fixed; z-index: -1`
  layer (`components/layout/Atmosphere.tsx` + the `.nova-bg` styles in `index.css`)
  rendered once at the app root. It has four non-interactive layers — a deep navy base
  radial gradient, three large radial-gradient glow blobs (blue/purple/cyan) that drift
  on 24–30s transforms, and a vignette. All colors come from CSS variables
  (`--atm-base-*`, `--glow-*`, `--glow-opacity`, `--vignette`) defined per theme.
- **Reason:** The spec requires a premium, cloudy-cosmos atmosphere with zero image or
  video assets and no readability cost. A fixed CSS layer (not a per-component
  background) guarantees one consistent backdrop across every route, is cheap to paint
  (gradient falloff, no `filter: blur` on animated elements), and is trivially
  theme-aware via variables.
- **Alternatives considered:**
  1. Per-page `background` utility classes (the old `app-surface-grid`).
  2. Animated `filter: blur()` glows.
  3. A `<canvas>`/WebGL starfield.
- **Why alternatives were rejected:** (1) scatters the background across components and
  can't sit behind a transparent shell uniformly; (2) animating a blurred element
  re-rasterizes the filter every frame — a real cost on the dashboard's many surfaces;
  (3) heavy, off-brief (gaming aesthetic), and a maintenance/perf risk. The radial
  gradient soft falloff reads as "blurred" for free.
- **Impact:** `body` is transparent with an `html` fallback color so there is never a
  white flash; the app containers (AppLayout, AuthLayout, ScreenLoader) are transparent;
  reduced-motion users get a static backdrop.

### D-4D-2 — One reusable glass surface system, tokenized

- **Date:** 2026-07-18
- **Decision:** `Card`/`StatCard`/`ChartCard` use `.glass`; `Dialog`/`Toast` use
  `.glass-strong`; the `Sidebar` and auth brand panel use `.glass`. The look
  (fill alpha, blur radius, border color/alpha, layered shadow) is defined once as
  `--glass-*` / `--glass-strong-*` variables and mirrored into Tailwind
  (`boxShadow.glass`, `boxShadow['glass-strong']`, `backdropBlur.glass`, glow shadows,
  `transitionTimingFunction.premium`). Future phases consume these tokens.
- **Reason:** The spec asks for a single premium glass language and for tokens future
  phases must reuse. Centralizing into two classes + a small set of variables keeps
  every surface consistent and lets a single change restyle the whole app.
- **Alternatives considered:**
  1. Tweak each component's Tailwind classes independently.
  2. A `glass` utility only, re-applied ad hoc.
- **Why alternatives were rejected:** (1) drift and inconsistency (exactly what the spec
  warns against); (2) no token story and duplicate blur/shadow literals everywhere.
- **Impact:** Surfaces that already compose `Card`/`Dialog`/`Toast` (dashboard, budgets,
  goals, transactions, accounts, categories, profile, all form dialogs) inherit the glass
  treatment automatically with no per-page edits.

### D-4D-3 — Charts keep Recharts; only tooltip/legend/grid are refined

- **Date:** 2026-07-18
- **Decision:** No new charting library is introduced. A reusable `ChartTooltip`
  (glass-styled) and `ChartLegend` (compact inline) in `components/ui/chart-tooltip.tsx`
  replace Recharts' flat default tooltip via the existing `<Tooltip content=…>` prop.
  Dashboard charts also get a softer grid, a dashed hover cursor, glowing active dots,
  and an inline legend — all standard Recharts props.
- **Reason:** The spec explicitly forbids a new charting library and any chart redesign;
  it asks only for tooltip/grid/legend/hover polish. Reusing `content` keeps the data and
  layout untouched while lifting the visual quality.
- **Alternatives considered:**
  1. Swap to a different chart library for "better" tooltips.
  2. Hand-roll an SVG chart.
- **Why alternatives were rejected:** Both violate the no-new-library and no-redesign
  constraints and would ripple through the dashboard's data binding.
- **Impact:** Charts remain Recharts-rendered and data-driven; only presentation improved.


---

## D-5-1 — Analytics is a reusable financial-intelligence domain

- **Date:** 2026-07-18
- **Decision:** All financial aggregation for Phase 5+ lives in a single
  `com.nova.finance.analytics` module (`AnalyticsService`) that every consumer
  (Analytics page, dashboard widgets, report exports, and future AI/OCR modules) calls
  instead of re-deriving data. Transaction-based sections are computed from **one**
  in-memory load of the filtered rows (`TransactionRepository.loadAnalyticsRows`) and then
  bucketed for every view (spending overview, cash-flow trend, category breakdown) — no
  per-section query, no N+1, no duplicate read within a request.
- **Reason:** The spec's Golden Rule requires Analytics to be a foundation, not a screen;
  duplicating aggregation in each surface would drift and waste queries.
- **Alternatives considered:**
  1. Put aggregation in the controller(s) per endpoint.
  2. Compute each chart's data independently in the frontend.
- **Why alternatives were rejected:** Controllers would hold business logic (Rule/architecture
  anti-pattern — "Avoid putting analytics logic inside controllers"); client-side recompute
  would re-fetch raw transactions and contradict the "all analytics from real transaction
  data, computed server-side" requirement.
- **Impact:** Future modules reuse `AnalyticsService` (or its pure reused engines) directly.

---

## D-5-2 — Budget & goal analytics reflect the *current* period, ignoring the date filter

- **Date:** 2026-07-18
- **Decision:** The `/api/analytics/budgets` and `/api/analytics/goals` endpoints always
  return each budget's / goal's **current** period health, even when a date filter
  (period/account/category) is applied to the transaction-based endpoints. The date filter
  continues to scope spending overview, cash flow, and category analysis.
- **Reason:** A budget's health in March is meaningless when the user is inspecting June;
  goals have intrinsic target dates independent of the inspection window. This matches the
  existing Phase 4B/4C semantics (budgets measure "this week/month/year").
- **Alternatives considered:**
  1. Slice budget/goal health to the supplied date window.
  2. Drop the date filter from those endpoints entirely.
- **Why alternatives were rejected:** (1) would produce nonsensical partial-period health;
  (2) is effectively what we do, but keeping the endpoints explicit documents intent and
  lets future UI pass a window without surprising results.
- **Impact:** Frontend dashboard widgets call these endpoints without a date filter; the
  Analytics page's budget/goal cards always show live standing.

---

## D-5-3 — Period presets resolve to UTC-bounded half-open windows

- **Date:** 2026-07-18
- **Decision:** The controller maps `period=weekly|monthly|yearly` to a `[from, to)`
  UTC window (Monday-start week / first-of-month / first-of-year) and passes it as the
  `AnalyticsFilter`. `custom` expects explicit `from`/`to`. The window is half-open
  (inclusive start, exclusive end) to avoid double-counting day boundaries, consistent with
  `BudgetPeriods` and the dashboard aggregation.
- **Reason:** Keeps the client simple (a single `period` string) while preserving Nova's
  date-handling invariants; the service defaults the window to the trailing 12 months when
  none is supplied.
- **Alternatives considered:** Send raw `from`/`to` from the UI for every preset.
- **Why alternatives were rejected:** More wire noise and client date math; the controller
  already owns filter resolution for other modules.
- **Impact:** One resolution path, reused by every analytics endpoint.

---

## D-5-4 — Reports export via POST with a JSON filter body

- **Date:** 2026-07-18
- **Decision:** `POST /api/analytics/reports/export` accepts `{ format: CSV|PDF, from, to,
  accountId, categoryId }` and streams the file (CSV or PDF) with a `Content-Disposition`
  attachment. Reports are generated from the same `AnalyticsOverviewResponse` the page shows,
  so exports always match what the user sees. The applied filter is embedded in the document
  header.
- **Reason:** The filter can be large (custom ranges, many dimensions); a POST body avoids
  URL-length limits and keeps CSV and PDF behind one consistent method. Streaming bytes (not
  the JSON envelope) lets the browser save directly.
- **Alternatives considered:**
  1. `GET /reports/export?...` per format.
  2. Generate reports client-side from UI state.
- **Why alternatives were rejected:** (1) URL length limits and two near-identical routes;
  (2) violates "generate reports from the Analytics domain rather than UI state" and would
  duplicate aggregation on the client.
- **Impact:** `ReportExportService` (opencsv + OpenPDF) is the single report generator.

---

## D-5-5 — Export dependencies: opencsv + OpenPDF

- **Date:** 2026-07-18
- **Decision:** Added `com.opencsv:opencsv:5.9` (CSV) and `com.github.librepdf:openpdf:2.0.3`
  (PDF) to the backend. No charting library was added — the frontend continues to use Recharts.
- **Reason:** Both are pure-Java, actively maintained, and Java 21-clean. OpenPDF is
  LGPL/MPL (no AGPL/license surprises) and renders documents without external fonts or
  binaries. The project had no CSV/PDF library before Phase 5.
- **Alternatives considered:**
  1. Apache POI for both.
  2. iText (AGPL) for PDF.
  3. A SaaS/headless-chrome PDF renderer.
- **Why alternatives were rejected:** POI is heavier than needed for CSV/PDF; iText's AGPL
  license is restrictive for a production product; a renderer adds an external runtime
  dependency. OpenPDF covers the requirement cleanly.
- **Impact:** Two new test-scoped-friendly dependencies; `ReportExportServiceTest` verifies
  both outputs.

---

## D-5-6 — Additive `/api/analytics` endpoints; no breaking changes

- **Date:** 2026-07-18
- **Decision:** All analytics endpoints are new (`/api/analytics/overview`, `/spending`,
  `/cash-flow`, `/categories`, `/budgets`, `/goals`, `/reports/export`) and return the
  standard `ApiResponse` envelope. No existing endpoint, DTO, or migration was changed.
- **Reason:** Backward compatibility (Rule 6) and zero regression for Phases 1–4D.
- **Alternatives considered:** Extend existing controllers with analytic params.
- **Why alternatives were rejected:** Would blur module boundaries and risk regressions in
  stable endpoints.
- **Impact:** Existing dashboard/budgets/goals/transactions APIs are untouched and still pass.

---

## D-6-1 — Smart Receipt Capture is a modular pipeline, not "OCR"

- **Date:** 2026-07-18
- **Decision:** Receipt capture is modeled as an isolated, stage-by-stage pipeline —
  `Upload → Validation → Storage → OCR Extraction → Normalization (parse) → Confidence
  Scoring → Transaction Draft → User Review → Transaction Creation`. Each stage is a
  separate, replaceable collaborator (`ReceiptValidationService`, `ReceiptStorage`,
  `OcrProvider`, `ReceiptParsingService`, `ReceiptConfidenceService`, `ReceiptService`).
- **Reason:** The Phase 6 mandate is explicit — "Receipt Capture is NOT OCR." OCR is one
  implementation detail of one stage. Encoding the whole flow as OCR would block future
  AI extractors (ML layout models, cloud document APIs) from dropping in without
  rewrites.
- **Alternatives considered:**
  1. A single `ReceiptOcrService` that takes a file and returns a transaction.
  2. An "OCR microservice" that owns storage and parsing too.
- **Why alternatives were rejected:** Both collapse extraction, parsing, and scoring into
  one unit, so replacing the engine means rewriting the flow. Keeping stages isolated means
  a future provider only implements `OcrProvider.extractText(image) → text` (or replaces
  `ReceiptParsingService`) and the rest of the system is unchanged.
- **Impact:** Future AI features extend the same pipeline. `ReceiptService` is the only
  orchestrator and reuses the existing `TransactionService` to create the transaction, so
  balance/ownership rules stay consistent everywhere.

---

## D-6-2 — OCR behind a provider interface, selected by name

- **Date:** 2026-07-18
- **Decision:** `OcrProvider` exposes `name()`, `isAvailable()`, and
  `extractText(image, contentType) → OcrResult(text, provider)`. `TesseractOcrProvider`
  (CLI-based, real Tesseract OCR) is the shipped implementation. `ReceiptConfig` selects
  the active provider by name from `nova.receipt.ocr.provider`.
- **Reason:** A future cloud or ML provider is a drop-in: implement the interface and point
  configuration at it. `isAvailable()` lets the pipeline degrade gracefully (mark the
  receipt `FAILED`, fall back to manual entry) instead of throwing when the engine is
  missing.
- **Alternatives considered:**
  1. Bake Tesseract calls directly into `ReceiptService`.
  2. A hardcoded provider chain.
- **Why alternatives were rejected:** Tight coupling would make every new provider a fork of
  the service; a hardcoded chain is unconfigurable. Name-based selection mirrors the
  storage-selection decision (D-6-3).
- **Impact:** Adding Google Vision, AWS Textract, or a local ML model means a new
  `@Component` + a config value, no changes to the pipeline or API.

---

## D-6-3 — Storage behind a minimal interface, swappable backend

- **Date:** 2026-07-18
- **Decision:** `ReceiptStorage` exposes `store(userId, bytes, contentType, filename) → key`,
  `load(key, contentType) → StoredFile`, `delete(key)`. `LocalReceiptStorage` (per-user
  directory under `nova.receipt.storage.local.path`) ships today. `ReceiptConfig` selects
  the backend by name from `nova.receipt.storage.backend`.
- **Reason:** Receipts are stored separately from transactions (separate table, separate
  lifecycle). The contract is intentionally tiny so Cloudinary, AWS S3, or MinIO can be
  added as another `@Component` without touching the service, controller, or pipeline.
- **Alternatives considered:**
  1. Store image bytes as a column on the `receipts` table (BLOB).
  2. Couple storage to the transaction attachment system.
- **Why alternatives were rejected:** A BLOB column bloats the row and the JSON envelope;
  coupling to transactions violates "store uploads separately from transactions" and the
  separation of concerns. The interface keeps the business logic storage-agnostic.
- **Impact:** Local disk is sufficient for single-node deployments; switching to S3/MinIO is
  a configuration change, not a code change.

---

## D-6-4 — Confidence scoring is deterministic and isolated from extraction

- **Date:** 2026-07-18
- **Decision:** Every extracted field is wrapped in `ReceiptField<T>(value, confidence)` with
  a 0–100 `confidence` and a `lowConfidence` flag (threshold 60). `ReceiptConfidenceService`
  assigns scores from cheap, explainable signals (present + well-formed, label-backed,
  amounts reconcile) after parsing. The frontend highlights `lowConfidence` fields.
- **Reason:** The user must stay in control ("review extracted values before saving"). A
  transparent score tells them which values to double-check without pretending to be a model.
  Nothing is invented — a missing field stays `null` and scores 0.
- **Alternatives considered:**
  1. A learned confidence model.
  2. No confidence at all (trust the parse).
- **Why alternatives were rejected:** A model adds a training/serving dependency the phase
  explicitly excludes ("No fake OCR… No placeholder parsing"); blind trust violates the
  user-in-control rule and turns parser gaps into silent bad transactions.
- **Impact:** The review screen renders `ConfidenceField`/`ConfidenceIndicator` for every
  detected value; a future ML scorer can replace `ReceiptConfidenceService.populate` without
  changing the field shape or UI.
