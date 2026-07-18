-- Phase 6: Smart Receipt Capture
-- Receipts are stored separately from transactions: uploading a receipt never
-- creates a transaction. The extracted, confidence-scored fields live in a
-- TEXT column (JSON) kept by ReceiptExtractedFieldsConverter so future AI
-- fields are a schema-free addition. Conventions match V1 (UUID PK,
-- snake_case, TIMESTAMPTZ audit columns, explicit FKs/indexes).

CREATE TABLE receipts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    filename           VARCHAR(255),
    content_type       VARCHAR(64) NOT NULL,
    file_size_bytes    BIGINT NOT NULL,
    storage_key        VARCHAR(512) NOT NULL,
    status             VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    status_message     VARCHAR(512),
    ocr_provider       VARCHAR(64),
    ocr_text           TEXT,
    extracted_fields   TEXT,
    overall_confidence INT,
    currency           VARCHAR(8),
    extracted_at       TIMESTAMPTZ,
    linked_transaction_id UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_receipts_user ON receipts (user_id);
CREATE INDEX idx_receipts_user_created ON receipts (user_id, created_at DESC);
