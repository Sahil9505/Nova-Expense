package com.nova.finance.goal.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for logging a contribution toward a goal. The {@code amount} is always
 * positive; an optional {@code note} and {@code contributedAt} date capture context.
 */
public record AddGoalContributionRequest(

        @NotNull(message = "Contribution amount is required")
        @Positive(message = "Contribution amount must be greater than zero")
        @DecimalMax(value = "99999999999999.9999", message = "Contribution amount is too large")
        BigDecimal amount,

        @Size(max = 255, message = "Note is too long")
        String note,

        LocalDate contributedAt
) {
}
