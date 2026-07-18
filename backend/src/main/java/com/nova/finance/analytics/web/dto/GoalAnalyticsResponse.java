package com.nova.finance.analytics.web.dto;

import com.nova.finance.goal.web.dto.GoalSummaryResponse;
import com.nova.finance.goal.web.dto.GoalWithProgress;

import java.math.BigDecimal;
import java.util.List;

/**
 * Goal progress for the Analytics view. Wraps the existing, reusable
 * {@link GoalSummaryResponse} (active/achieved/paused/overdue counts, totals, overall
 * percent, and every goal with derived progress) and adds:
 *
 * <ul>
 *   <li>{@code upcomingDeadlines} — active, not-yet-achieved goals ordered by their
 *       target date (soonest first), so the Analytics page can show what's due.</li>
 *   <li>{@code contributionTotal} — the sum of {@code currentAmount} across active
 *       goals (total money currently saved toward goals).</li>
 * </ul>
 *
 * No goal math is re-derived here; the figures come straight from
 * {@code GoalService.summary(...)} via {@link GoalSummaryResponse}.
 *
 * @param goalSummary        the reusable Goal roll-up
 * @param upcomingDeadlines active goals not yet achieved, target date ascending
 * @param contributionTotal sum of saved amounts across active goals
 * @param currency          the owner's preferred currency (for display)
 */
public record GoalAnalyticsResponse(
        GoalSummaryResponse goalSummary,
        List<GoalWithProgress> upcomingDeadlines,
        BigDecimal contributionTotal,
        String currency
) {
}
