-- Phase 4C fixup: goal_contributions was created in V5 without the `updated_at`
-- audit column, but GoalContribution extends BaseEntity, which maps a NOT NULL
-- `updated_at` (maintained by Spring Data JPA auditing). Under ddl-auto=validate
-- the mismatch fails schema validation at startup. V5 is already applied, so we
-- add the column in a new migration rather than editing V5 (Flyway convention:
-- never edit an applied migration). The `goals` table already has this column.
ALTER TABLE goal_contributions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
