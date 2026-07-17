package com.nova.finance.budget.web.dto;

import com.nova.finance.category.Category;

import java.util.UUID;

/**
 * Minimal category projection embedded in a {@link BudgetResponse} so the UI can show
 * the budget's scope without an extra round-trip. Mirrors the {@code CategoryRef}
 * shape used elsewhere in the finance API.
 */
public record CategoryRef(
        UUID id,
        String name,
        String type,
        String color,
        String icon
) {
    public static CategoryRef from(Category category) {
        return new CategoryRef(
                category.getId(),
                category.getName(),
                category.getType().name(),
                category.getColor(),
                category.getIcon());
    }
}
