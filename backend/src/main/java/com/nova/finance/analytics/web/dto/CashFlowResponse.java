package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Income vs. expense over time, bucketed by the requested {@link Granularity}. Suitable
 * for an area/bar chart on the Analytics page and the dashboard's Cash Flow widget.
 *
 * @param granularity the bucket size used for each point
 * @param currency    the owner's preferred currency (for display)
 * @param points      one point per bucket, ordered chronologically (oldest first)
 */
public record CashFlowResponse(
        Granularity granularity,
        String currency,
        List<CashFlowPoint> points
) {
}
