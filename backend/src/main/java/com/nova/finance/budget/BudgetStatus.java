package com.nova.finance.budget;

/**
 * Health of a budget, derived from how much of its limit has been spent. Thresholds
 * are configurable via {@link BudgetProperties}; the defaults are &lt;80% healthy,
 * 80–99% warning, and ≥100% exceeded.
 *
 * <p>This is a pure, presentation-agnostic domain value so future modules (Analytics,
 * Financial Goals, AI) can reason about budget health without depending on the web
 * layer.</p>
 */
public enum BudgetStatus {
    HEALTHY,
    WARNING,
    EXCEEDED
}
