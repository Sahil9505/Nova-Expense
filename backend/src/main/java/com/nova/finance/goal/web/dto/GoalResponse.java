package com.nova.finance.goal.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API-safe projection of a {@link com.nova.finance.goal.Goal} with its derived
 * {@link GoalProgress}. The owning user is never included; the goal is implicitly
 * scoped to the caller.
 */
public record GoalResponse(
        UUID id,
        String name,
        String description,
        String type,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        LocalDate targetDate,
        boolean active,
        boolean paused,
        Instant createdAt,
        Instant updatedAt,
        GoalProgress progress
) {
}
