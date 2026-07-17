-- Phase 4C (Financial Goals): introduce the goals domain and contribution history.
-- V1/V3/V4 are never edited; every change ships as a new migration so production
-- keeps running against this Flyway-managed schema under ddl-auto=validate.

-- ---------------------------------------------------------------------------
-- goals: a user's long-term financial objective (savings, debt payoff, custom).
-- `current_amount` is the maintained running total of contributions (a single
-- source of truth, kept in sync transactionally with goal_contributions — the
-- same pattern Nova uses to keep account balances in sync with transactions).
-- `paused` lets a user park a goal without losing it; `is_active` is the
-- lifecycle flag (soft-delete, mirroring budgets/accounts).
-- ---------------------------------------------------------------------------
CREATE TABLE goals (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(255),
    goal_type      VARCHAR(32) NOT NULL,
    target_amount  NUMERIC(18, 4) NOT NULL,
    current_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    target_date    DATE NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    paused         BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_active ON goals (user_id, is_active);

-- ---------------------------------------------------------------------------
-- goal_contributions: the immutable history of money added toward a goal.
-- Each row carries its own amount/date/note; the goal's `current_amount` is
-- the derived running total so reads never recompute a sum. Removing a goal
-- cascades to its contributions.
-- ---------------------------------------------------------------------------
CREATE TABLE goal_contributions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id        UUID NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    amount         NUMERIC(18, 4) NOT NULL,
    note           VARCHAR(255),
    contributed_at DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_contributions_goal_id ON goal_contributions (goal_id);
CREATE INDEX idx_goal_contributions_user_id ON goal_contributions (user_id);
