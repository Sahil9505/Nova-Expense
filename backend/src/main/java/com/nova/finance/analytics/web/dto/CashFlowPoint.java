package com.nova.finance.analytics.web.dto;

import java.math.BigDecimal;

/**
 * A single time bucket of the cash-flow trend.
 *
 * @param periodKey stable sort key (e.g. "2026-07" for monthly, "2026-W30" for weekly)
 * @param label     human-friendly bucket label (e.g. "Jul", "W30", "2026")
 * @param income    INCOME in the bucket
 * @param expenses  EXPENSE in the bucket
 * @param net       {@code income - expenses} for the bucket
 */
public record CashFlowPoint(
        String periodKey,
        String label,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal net
) {
}
