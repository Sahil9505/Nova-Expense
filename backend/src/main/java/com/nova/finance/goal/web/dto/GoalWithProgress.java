package com.nova.finance.goal.web.dto;

/**
 * A goal paired with its derived progress, used inside the summary payload so the
 * dashboard widgets can render each goal without a second request.
 */
public record GoalWithProgress(
        GoalResponse goal,
        GoalProgress progress
) {
}
