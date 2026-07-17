package com.nova.finance.budget.web.dto;

/**
 * A budget paired with its live intelligence. The {@code budget} is the unchanged
 * {@link BudgetResponse}; {@code metrics} carries the derived spent/remaining/status
 * so the UI can render progress without recomputing anything.
 */
public record BudgetMetricsResponse(
        BudgetResponse budget,
        BudgetMetrics metrics
) {
}
