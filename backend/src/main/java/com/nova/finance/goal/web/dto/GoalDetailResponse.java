package com.nova.finance.goal.web.dto;

import java.util.List;

/**
 * A goal with its full contribution history. Returned by the detail and "add
 * contribution" endpoints so the client gets the goal, its progress, and the timeline
 * in a single request.
 */
public record GoalDetailResponse(
        GoalResponse goal,
        List<GoalContributionResponse> contributions
) {
}
