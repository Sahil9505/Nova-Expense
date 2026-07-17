-- Phase 4A (Budget Foundation): complete the budgets table.
-- V1 created budgets without `description` or `is_active`; this migration adds them
-- so the Budget entity (which is not yet exposed via any API) stays in sync with the
-- Flyway-managed schema under ddl-auto=validate. V1/V3 are never edited.

ALTER TABLE budgets ADD COLUMN description VARCHAR(255);
ALTER TABLE budgets ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- Speed up the active/inactive filtering the Budget API exposes.
CREATE INDEX idx_budgets_active ON budgets (user_id, is_active);
