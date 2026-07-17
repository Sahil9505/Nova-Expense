package com.nova.finance.goal;

/**
 * Lifecycle of a goal. Most states are derived from progress and the target date;
 * {@code PAUSED} is the only user-managed input (set via the API). {@code ACHIEVED}
 * takes precedence so a completed goal is never reported as paused or overdue.
 *
 * <p>This is a pure, presentation-agnostic domain value so future modules (Analytics,
 * AI) can reason about goal health without depending on the web layer.</p>
 */
public enum GoalStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ACHIEVED,
    OVERDUE,
    PAUSED
}
