package com.nova.finance.analytics;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Filter criteria for the Analytics domain. Every field is optional; a {@code null}
 * dimension means "no constraint on this axis". The window {@code [from, to)} is
 * half-open (inclusive start, exclusive end), matching the rest of Nova's date
 * handling so day-boundary transactions are never double-counted.
 *
 * <p>Transaction-based analytics (cash flow, categories, spending overview) honor the
 * full filter. Budget and goal analytics intentionally ignore the date window and
 * always reflect each budget's / goal's <em>current</em> period — a budget's health in
 * March is meaningless when the user is inspecting June (see DECISION_LOG D-5-x).</p>
 */
public record AnalyticsFilter(
        UUID userId,
        OffsetDateTime from,
        OffsetDateTime to,
        UUID accountId,
        UUID categoryId
) {

    public static AnalyticsFilter forRange(
            UUID userId, OffsetDateTime from, OffsetDateTime to, UUID accountId, UUID categoryId) {
        return new AnalyticsFilter(userId, from, to, accountId, categoryId);
    }
}
