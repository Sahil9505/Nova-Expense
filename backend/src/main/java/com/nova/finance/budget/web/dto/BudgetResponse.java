package com.nova.finance.budget.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API-safe projection of a {@link com.nova.finance.budget.Budget}. The owning user is
 * never included; the budget is implicitly scoped to the caller. When the budget is
 * category-scoped, {@code category} carries the minimal category reference.
 */
public record BudgetResponse(
        UUID id,
        String name,
        String description,
        BigDecimal amount,
        String period,
        CategoryRef category,
        boolean active,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {
}
