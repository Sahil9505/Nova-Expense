package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;

/**
 * One category's total within the analytics window.
 *
 * @param name     category display name
 * @param color    category color (hex) for chart fills
 * @param icon     category icon key
 * @param amount   total amount for this category in the window (always positive)
 * @param type     "INCOME" or "EXPENSE" — which side this breakdown belongs to
 */
public record CategoryBreakdownItem(
        String name,
        String color,
        String icon,
        BigDecimal amount,
        String type
) {
}
