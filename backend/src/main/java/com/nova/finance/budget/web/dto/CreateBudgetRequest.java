package com.nova.finance.budget.web.dto;

import com.nova.finance.budget.Budget;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload for creating a budget. The {@code amount} is always positive; the
 * {@code period} selects how often the limit resets. A {@code CUSTOM} period requires
 * both {@code startDate} and {@code endDate}; cross-field date rules are enforced in
 * the service. A budget may be scoped to a category ({@code categoryId}) or left null
 * for an overall budget.
 */
public record CreateBudgetRequest(

        @NotBlank(message = "Budget name is required")
        @Size(max = 120, message = "Budget name is too long")
        String name,

        @NotNull(message = "Budget amount is required")
        @Positive(message = "Budget amount must be greater than zero")
        @DecimalMax(value = "99999999999999.9999", message = "Budget amount is too large")
        BigDecimal amount,

        @NotNull(message = "Budget period is required")
        Budget.Period period,

        UUID categoryId,

        @Size(max = 255, message = "Description is too long")
        String description,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate
) {
}
