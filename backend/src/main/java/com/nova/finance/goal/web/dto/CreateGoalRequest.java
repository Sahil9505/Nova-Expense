package com.nova.finance.goal.web.dto;

import com.nova.finance.goal.Goal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating a goal. The {@code targetAmount} is always positive and a
 * {@code targetDate} is required; an optional {@code currentAmount} seeds the running
 * total for goals already in progress. The {@code type} selects the goal's intent.
 */
public record CreateGoalRequest(

        @NotBlank(message = "Goal name is required")
        @Size(max = 120, message = "Goal name is too long")
        String name,

        @NotNull(message = "Goal type is required")
        Goal.Type type,

        @NotNull(message = "Target amount is required")
        @Positive(message = "Target amount must be greater than zero")
        @DecimalMax(value = "99999999999999.9999", message = "Target amount is too large")
        BigDecimal targetAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate,

        @Digits(integer = 18, fraction = 4, message = "Current amount is too large")
        BigDecimal currentAmount,

        @Size(max = 255, message = "Description is too long")
        String description
) {
}
