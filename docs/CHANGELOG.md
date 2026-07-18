# Changelog

All notable changes to Nova are documented in this file. The format is based on
keeping a clear record of Added, Improved, and Fixed work per release.

## [0.8.0] — Phase 6: Smart Receipt Capture

Adds an intelligent document-processing pipeline that turns a photo or upload of a receipt
into a confidence-scored transaction draft the user reviews before saving. The domain is
fully isolated (upload, validation, storage, OCR extraction, normalization, confidence
scoring, draft, review, transaction creation) and reuses the existing transaction-creation
flow — so balances and ownership rules are unchanged. No existing endpoint, DTO, or
migration was modified.

### Added
- **Receipt domain (backend).** A dedicated `com.nova.finance.receipt` package with
  `ReceiptService` (orchestrator), `ReceiptController`, `ReceiptMapper` (MapStruct),
  `ReceiptValidationService`, `ReceiptOcrService`, `ReceiptParsingService`,
  `ReceiptConfidenceService`, `ReceiptProperties`, and the
  `Receipt` entity + `V7__receipts.sql` migration (stored separately from transactions).
- **OCR abstraction.** `OcrProvider` interface with a shipped `TesseractOcrProvider`
  (real Tesseract via CLI). `isAvailable()` lets the pipeline degrade to manual entry when
  the engine is absent; selection is by name via `nova.receipt.ocr.provider`.
- **Storage abstraction.** `ReceiptStorage` interface with a shipped `LocalReceiptStorage`
  (per-user directory). Selection is by name via `nova.receipt.storage.backend`, so
  Cloudinary / AWS S3 / MinIO can be added as another bean with no pipeline change.
- **Confidence scoring.** Every extracted field is `ReceiptField<T>(value, confidence)` with
  a 0–100 score and a `lowConfidence` flag (threshold 60). The review UI highlights
  low-confidence values; missing fields stay `null` (never invented).
- **Receipt API (`/api/receipts`).** `POST` upload (multipart, 10 MB, PNG/JPEG/JPG/WEBP),
  `GET /recent`, `GET /{id}`, `GET /{id}/image` (raw bytes), `POST /{id}/process`
  (OCR → parse → score), `GET /{id}/draft` (editable transaction draft), and
  `POST /{id}/finalize` (reuses `TransactionService` — the only path that creates a
  transaction).
- **Frontend Receipts feature.** Types + `receiptsApi` (incl. a `receiptImageUrl` blob
  helper) + `useReceipts` hooks; `ReceiptUploadDialog` (drag/drop + client-side
  type/size validation), `ConfidenceIndicator`/`ConfidenceField`, `ReceiptPreview`, and
  `ReceiptDraftForm`; the `/receipts` list page and `/receipts/:id` review page; and a
  lightweight `RecentReceiptsWidget` on the dashboard.
- **Error handling.** Dedicated `ErrorCode`s (`RECEIPT_UNSUPPORTED_TYPE`,
  `RECEIPT_FILE_TOO_LARGE`, `RECEIPT_INVALID_IMAGE`, `RECEIPT_OCR_UNAVAILABLE`,
  `RECEIPT_PROCESSING_FAILED`, `RECEIPT_STORAGE_FAILED`) with friendly messages, plus
  graceful OCR-failure → `FAILED` status so users can still enter the transaction manually.

### Improved
- Sidebar gains a Receipts entry; in-app version marker advanced to Phase 6 (v0.8.0).

### Fixed
- `Receipt.upload` no longer persists a row before the storage key exists (which violated
  the `storage_key NOT NULL` constraint under Flyway `create-drop`); the file is stored
  first, then the receipt is inserted once with its key. Upload no longer declares a
  checked `IOException` (wrapped in `RECEIPT_STORAGE_FAILED`).

## [0.7.0] — Phase 5: Analytics & Reports

Transforms Nova into a financial-insights platform. A reusable `com.nova.finance.analytics`
domain aggregates real transaction data into spending, cash-flow, category, budget, and goal
analytics; powers a new Analytics page, five additive dashboard widgets, and CSV/PDF exports.
No existing endpoint, DTO, or migration changed — backward compatible with Phases 1–4D.

### Added
- **Analytics domain (backend).** A single `AnalyticsService` orchestrates every aggregation
  from one in-memory load of the filtered transactions, then derives the spending overview,
  cash-flow trend (weekly/monthly/yearly bucketing), and category breakdown. Budget and goal
  analytics reuse the existing `BudgetCalculationService` and `GoalService` engines — no
  budget or goal math is duplicated.
- **Analytics API (`/api/analytics`).** Additive endpoints behind the existing auth envelope:
  `GET /overview`, `/spending`, `/cash-flow`, `/categories`, `/budgets`, `/goals`, and
  `POST /reports/export` (CSV or PDF). All figures are computed from real transaction data.
- **Report export.** `ReportExportService` (opencsv + OpenPDF) generates CSV and PDF reports
  from the same `AnalyticsOverviewResponse` the UI shows, so exports always match the applied
  filter. Added `com.opencsv:opencsv` and `com.github.librepdf:openpdf` dependencies.
- **Analytics page (`/analytics`).** Date-period (weekly/monthly/yearly/custom), account, and
  category filters; summary stat cards (income, expenses, net cash flow, savings rate);
  cash-flow area chart; category-breakdown donut; top-spending-categories and upcoming-goal
  tables; and one-click CSV/PDF export. Full loading, empty, and error states.
- **Dashboard analytics widgets.** Five additive sections (Spending Trends, Category
  Breakdown, Budget Distribution, Goal Progress) reusing the same analytics hooks and the
  existing `Card`/`StatCard`/`ChartCard`/`Progress` design-system primitives. The dashboard
  is extended, not redesigned.
- **Sidebar + routing.** The previously stubbed "Analytics" nav item is now live
  (`/analytics`); existing navigation is unchanged.

### Improved
- **Reuse over duplication.** Cash-flow/category aggregation share one transaction load;
  budget/goal analytics delegate to the established calculation engines (D-5-1, D-5-2).
- **Filter consistency.** Period presets resolve to the same UTC half-open windows used
  elsewhere in Nova (D-5-3).

### Fixed
- None in this phase. (See `BUG_TRACKER.md` for carried-forward items; regression suite
  remains green.)

## [0.7.0] — Phase 4D: Premium UI & Design System Refinement

This phase is visual-only. No backend, API, business logic, or data-model changes
were made; every workflow and route is preserved. The goal is a premium, calm,
SaaS-grade interface built on the existing architecture.

### Added
- **Atmospheric CSS background.** A fixed, pure-CSS four-layer backdrop (deep navy
  base gradient, three slow-drifting glow blobs — blue aurora / purple nebula / cyan
  glow — and a soft vignette). No image assets, no video; fully GPU-friendly. Driven
  by theme tokens so it adapts to light and dark.
- **Glass surface system.** `Card`, `StatCard`, `ChartCard`, `Dialog`, `Toast`, the
  `Sidebar`, and the auth brand panel now use a reusable `.glass` / `.glass-strong`
  treatment (`backdrop-filter: blur`, translucent fill, subtle border, layered
  shadow). The app shell is transparent so the atmosphere shows through.
- **Centralized design tokens.** New CSS variables for the atmosphere palette, glass
  colors/alpha/blur/border/shadow, and focus ring, plus Tailwind tokens for glow
  colors, glass blur, premium easing, motion durations, and glow/card-hover shadows.
  Future phases consume these tokens instead of hard-coded values.
- **Custom chart tooltip + legend.** `components/ui/chart-tooltip.tsx` provides a
  glass-styled `ChartTooltip` and a compact `ChartLegend`, replacing Recharts' flat
  default tooltip. Dashboard charts adopt it.
- **`success` button variant.** The `Button` primitive now standardizes six variants
  (primary, secondary, outline, ghost, danger, success) with consistent radius,
  easing, hover lift, shadows, and focus states.
- **Reduced-motion support.** Ambient drift and non-essential motion respect
  `prefers-reduced-motion`.

### Improved
- **Sidebar.** Glass panel that blends with the atmosphere, a refined active
  indicator (accent bar + tinted pill + hover slide), icon hover scale, depth, and
  spacing. Version marker advanced to Phase 4D (v0.7.0).
- **Topbar.** Time-based greeting hierarchy ("Good morning/afternoon/evening"), a
  profile block (name + email) beside the avatar, and standardized action buttons;
  now a translucent glass bar.
- **Stat cards.** Trend rendered as a soft pill, refined typography hierarchy, hover
  lift with a primary glow, and improved spacing/whitespace.
- **Forms.** Inputs and selects use a consistent radius, smoother transitions, refined
  focus rings (border highlight + ring, no shape change on focus), and hover states.
  Dark-mode date-picker indicators are lightened for visibility.
- **Progress.** Gradient fill with a subtle glow and rounded ends; animation stays
  reduced-motion aware.
- **Charts.** Softer, lower-opacity grid; dashed hover cursor; glowing active dots;
  inline legend; tighter tooltip styling — all within the existing Recharts library.
- **Accessibility.** Global focus-visible ring no longer alters element shape on
  focus; keyboard navigation and contrast preserved.

### Fixed
- None in this phase. (See `BUG_TRACKER.md` for carried-forward items.)

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
