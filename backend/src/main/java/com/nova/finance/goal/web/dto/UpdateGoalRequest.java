package com.nova.finance.goal.web.dto;

import com.nova.finance.goal.Goal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update for a goal. Every field is optional; only supplied fields change.
 * {@code active} toggles the lifecycle (reactivation after a soft-delete) and
 * {@code paused} parks a goal without losing it. Cross-field rules (e.g. a positive
 * target) are enforced in the service.
 */
public record UpdateGoalRequest(

        @Size(max = 120, message = "Goal name is too long")
        String name,

        Goal.Type type,

        @Positive(message = "Target amount must be greater than zero")
        @DecimalMax(value = "99999999999999.9999", message = "Target amount is too large")
        BigDecimal targetAmount,

        LocalDate targetDate,

        @Digits(integer = 18, fraction = 4, message = "Current amount is too large")
        BigDecimal currentAmount,

        @Size(max = 255, message = "Description is too long")
        String description,

        Boolean paused,

        Boolean active
) {
}
