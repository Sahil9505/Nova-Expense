package com.nova.finance.budget.web.dto;

import com.nova.finance.budget.Budget;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Partial update for a budget. Every field is optional; only supplied fields change.
 * {@code active} toggles the budget (reactivation after a soft-delete); when omitted
 * the current value is kept. Cross-field date rules (custom period needs both dates)
 * are enforced in the service.
 */
public record UpdateBudgetRequest(

        @Size(max = 120, message = "Budget name is too long")
        String name,

        @Positive(message = "Budget amount must be greater than zero")
        @DecimalMax(value = "99999999999999.9999", message = "Budget amount is too large")
        BigDecimal amount,

        Budget.Period period,

        UUID categoryId,

        @Size(max = 255, message = "Description is too long")
        String description,

        LocalDate startDate,

        LocalDate endDate,

        Boolean active
) {
}
