package com.nova.finance.analytics.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Body for {@code POST /api/analytics/reports/export}. Carries the desired
 * {@link ExportFormat} and the exact {@link AnalyticsFilter} the report must respect, so
 * exports are generated from the Analytics domain (never from UI state).
 */
public record AnalyticsExportRequest(
        ExportFormat format,
        UUID userId,
        OffsetDateTime from,
        OffsetDateTime to,
        UUID accountId,
        UUID categoryId
) {
}
