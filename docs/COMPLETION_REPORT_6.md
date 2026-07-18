# Completion Report — Phase 6: Smart Receipt Capture

## 1. Executive Summary

Phase 6 delivers **Smart Receipt Capture**: a user uploads a photo or image of a receipt and
Nova extracts the financial details, scores each field for confidence, and presents an
editable transaction draft that the user reviews and confirms. No transaction is ever
created automatically — the human stays in control.

The feature is built as a **modular, stage-isolated pipeline** rather than "OCR." OCR is one
replaceable stage behind the `OcrProvider` interface, and storage is a separate, swappable
`ReceiptStorage` backend. The finalize step reuses the existing `TransactionService`, so
balances and ownership rules are unchanged and there are zero regressions.

Verification: **backend 144/144 tests pass**, **frontend typecheck + lint + production build
all pass**. All previous endpoints, DTOs, and migrations are untouched.

## 2. Architecture Validation Findings

Reviewed the application end-to-end through Phase 5 before writing code. Key findings that
shaped the design:

- **Transactions / Categories / Accounts / Dashboard** already expose a clean `ApiResponse`
  envelope and `useQuery`/`useMutation` hook pattern; the receipt feature reuses it exactly
  (`receiptsApi`, `useReceipts`).
- **Existing upload surface is minimal** — there was no reusable file-upload component. The
  new `ReceiptUploadDialog` is self-contained (drag/drop + picker + client-side
  type/size checks) rather than forcing a shared abstraction that didn't exist.
- **Transaction creation is the one stable path.** Reusing `TransactionService` from
  `ReceiptService.finalize` means receipt transactions obey the same validation, balance
  updates, and ownership scoping as manual ones.
- **No logic was duplicated.** Parsing, scoring, mapping, and storage are new, isolated
  collaborators; confidence scoring is deliberately separate from extraction.

## 3. Receipt Pipeline Design

```
Upload → Validation → Storage → OCR Extraction → Normalization (parse) →
Confidence Scoring → Transaction Draft → User Review → Transaction Creation
```

Each stage is its own collaborator:

| Stage | Class | Notes |
| --- | --- | --- |
| Validation | `ReceiptValidationService` | type, 10 MB size, decodable-image check; typed `ErrorCode`s |
| Storage | `ReceiptStorage` / `LocalReceiptStorage` | separate from transactions; key returned before persist |
| OCR | `OcrProvider` / `TesseractOcrProvider` | real Tesseract via CLI; `isAvailable()` degrades gracefully |
| Normalization | `ReceiptParsingService` | label/line-oriented text → structured fields; never invents |
| Scoring | `ReceiptConfidenceService` | deterministic 0–100 per field; `lowConfidence` flag (≤60) |
| Orchestration | `ReceiptService` | sequences the pipeline; `finalize` reuses `TransactionService` |

## 4. Backend Changes

- **New domain** `com.nova.finance.receipt`: `Receipt` entity, `ReceiptRepository`,
  `ReceiptService`, `ReceiptController`, `ReceiptMapper` (MapStruct), `ReceiptConfig`,
  `ReceiptProperties`, `ReceiptValidationService`, `ReceiptOcrService`,
  `ReceiptParsingService`, `ReceiptConfidenceService`, `ReceiptProcessingException`.
- **OCR abstraction** — `ocr/OcrProvider` interface + `ocr/TesseractOcrProvider`
  (CLI, real OCR) + `ocr/OcrResult` + `ocr/OcrException`.
- **Storage abstraction** — `storage/ReceiptStorage` interface + `storage/LocalReceiptStorage`
  (per-user directory) + `storage/StoredFile` + `storage/ReceiptStorageException`.
- **Parsing/confidence** — `parsing/ReceiptField<T>`, `ReceiptExtractedFields`,
  `ReceiptItem`, `ReceiptConfidenceService`, `ReceiptExtractedFieldsConverter`
  (JSON column; future AI fields are schema-free).
- **Migration** `V7__receipts.sql` — `receipts` table separate from transactions
  (UUID PK, `storage_key NOT NULL`, JSON `extracted_fields`, `linked_transaction_id`).
- **Error codes** — `RECEIPT_UNSUPPORTED_TYPE`, `RECEIPT_FILE_TOO_LARGE`,
  `RECEIPT_INVALID_IMAGE`, `RECEIPT_OCR_UNAVAILABLE`, `RECEIPT_PROCESSING_FAILED`,
  `RECEIPT_STORAGE_FAILED`; handled by a new `GlobalExceptionHandler` branch.
- **Config** — `nova.receipt.*` (upload limits, storage backend, OCR provider/language/timeout).
  Multipart limit raised to 12 MB so the service can return a friendly 10 MB error.

**Bug fixed during the build:** `upload` previously persisted the row before producing the
storage key, violating `storage_key NOT NULL` (HTTP 500). Now the file is stored first and
the receipt is inserted once with its key. (Tracked as B-6-1.)

## 5. Frontend Changes

- **Types** (`@/types`) — `Receipt`, `ReceiptSummary`, `ReceiptFields`, `ReceiptField`,
  `ReceiptItem`, `ReceiptDraft`, `FinalizeReceiptPayload`, `ReceiptStatus`.
- **API** (`lib/api.ts`) — `receiptsApi` (upload/process/draft/finalize/recent/list) plus a
  `receiptImageUrl` blob helper for the raw image endpoint.
- **Hooks** (`hooks/useReceipts.ts`) — recent/list/detail/draft queries and upload/process/
  finalize mutations; finalize invalidates transactions, accounts, and dashboard exactly
  like every other transaction creation.
- **Validation** (`lib/validations.ts`) — `finalizeReceiptSchema` mirroring the backend's
  `FinalizeReceiptRequest`; `RECEIPT_TYPE_OPTIONS` (income/expense only — transfers are not
  supported for receipts).
- **Components** — `ReceiptUploadDialog` (drag/drop + validation), `ConfidenceIndicator` /
  `ConfidenceField` (highlight low-confidence values), `ReceiptPreview` (blob image with
  revoke-on-unmount), `ReceiptDraftForm` (editable draft, account/category selection,
  manual entry when OCR failed).
- **Pages** — `Receipts` (list + upload) and `ReceiptReviewPage` (image, extracted fields,
  editable draft, save). Auto-process kicks in on arriving at an unprocessed receipt.
- **Dashboard** — lightweight `RecentReceiptsWidget` (no redesign of existing sections).
- **Nav** — Sidebar gains a Receipts entry; version marker advanced to Phase 6 (v0.8.0).

## 6. OCR Integration

`OcrProvider` is the single contract: `name()`, `isAvailable()`, `extractText(image, type) →
OcrResult`. `TesseractOcrProvider` invokes the real Tesseract CLI (no bundled native lib),
bounded by `nova.receipt.ocr.timeout-seconds`, and honestly reports unavailability so the
pipeline degrades to `FAILED` + manual entry instead of failing the upload. Swapping in
Google Vision, AWS Textract, or a local ML model is a new `@Component` + a config value —
no pipeline or API change.

## 7. Storage Design

`ReceiptStorage` exposes only `store → key`, `load`, `delete` — storage-agnostic by design.
`LocalReceiptStorage` writes under `nova.receipt.storage.local.path/<userId>/<uuid>.<ext>`
and rejects path-traversal keys. Cloudinary / AWS S3 / MinIO are future `@Component`
implementations selected by `nova.receipt.storage.backend` in `ReceiptConfig`. Receipts are
kept in a dedicated table and lifecycle, fully separate from transactions.

## 8. Documentation Updates

- `DECISION_LOG.md` — D-6-1 (pipeline not OCR), D-6-2 (OCR provider abstraction),
  D-6-3 (storage abstraction), D-6-4 (confidence scoring) with context/alternatives/impact.
- `CHANGELOG.md` — new `[0.8.0] — Phase 6: Smart Receipt Capture` section; corrected the
  Phase 5 version to `0.7.0` for consistency.
- `BUG_TRACKER.md` — B-6-1 (storage_key constraint fix).
- `README.md` — "Smart Receipt Capture (Phase 6)" overview + API table, Receipts routes,
  and a Roadmap entry marking Phase 6 complete and pointing Phase 7 at the extension points.

## 9. Known Limitations

- **Line-item extraction is not implemented** — `items` is always empty. The parser
  intentionally returns no items rather than risk fabricated values; amount-level fields
  (subtotal/tax/total) are extracted.
- **European comma decimals** (e.g. `2,59`) are not yet normalized; such totals are left
  `null`. Period/dot decimals and ISO dates are supported.
- **OCR requires the Tesseract binary** to be installed for automatic extraction. Without
  it, uploads succeed and the receipt is marked `FAILED` for manual entry — a graceful
  degradation, not a failure.
- **Processing is synchronous** within the `POST /process` request (bounded by the OCR
  timeout). The client shows a processing state; this keeps the contract simple and is
  sufficient for single-receipt review. Async/batched processing is a future enhancement.
- **Out-of-scope Phase 6 items remain unbuilt** (by design): AI categorization, budgeting
  advice, natural-language chat, receipt semantic understanding, invoice support, batch
  uploads, expense reimbursement.

## 10. Readiness Assessment for Phase 7

The pipeline is **extension-ready**. A future AI/ML phase can:

- Drop in a new `OcrProvider` (or replace `ReceiptParsingService`) for richer extraction —
  no change to `ReceiptService`, the API, or the frontend review screen.
- Extend `ReceiptExtractedFields` with new fields; the JSON converter and `ReceiptMapper`
  carry them through without a schema change.
- Reuse `ReceiptConfidenceService.populate` or replace it with a learned scorer behind the
  same `ReceiptField` shape.

Backend (144 tests green) and frontend (typecheck/lint/build green) are stable and
regression-free, so Phase 7 can build on a clean, modular foundation.
