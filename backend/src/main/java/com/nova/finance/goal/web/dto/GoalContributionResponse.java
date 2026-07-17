package com.nova.finance.goal.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single contribution toward a goal. Immutable history — the goal's running total
 * is the maintained summary; these rows render the progress timeline.
 */
public record GoalContributionResponse(
        UUID id,
        BigDecimal amount,
        String note,
        LocalDate contributedAt
) {
}
