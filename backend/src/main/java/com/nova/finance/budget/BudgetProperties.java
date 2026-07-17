package com.nova.finance.budget;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Tunable thresholds that classify a budget's {@link BudgetStatus}. Bound to the
 * {@code nova.budget.*} namespace so health boundaries can be tuned per deployment
 * without a code change.
 *
 * <p>Both thresholds are fractions of the budget amount (not percentages):
 * {@code warningThreshold = 0.80} means a budget turns {@code WARNING} once 80% of the
 * limit is spent, and {@code exceededThreshold = 1.00} means it is {@code EXCEEDED}
 * once spending reaches the full limit. Sensible defaults are applied when a value is
 * absent so the module works with no configuration.</p>
 */
@ConfigurationProperties(prefix = "nova.budget")
public record BudgetProperties(
        BigDecimal warningThreshold,
        BigDecimal exceededThreshold
) {

    /** Default: a budget becomes WARNING at 80% of its limit. */
    public static final BigDecimal DEFAULT_WARNING = new BigDecimal("0.80");

    /** Default: a budget becomes EXCEEDED at 100% of its limit. */
    public static final BigDecimal DEFAULT_EXCEEDED = new BigDecimal("1.00");

    public BudgetProperties {
        if (warningThreshold == null) {
            warningThreshold = DEFAULT_WARNING;
        }
        if (exceededThreshold == null) {
            exceededThreshold = DEFAULT_EXCEEDED;
        }
    }
}
