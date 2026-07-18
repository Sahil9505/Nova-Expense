package com.nova.finance.receipt.web.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lightweight receipt projection for lists and the dashboard "Recent Uploads" widget.
 * Omits the (potentially large) extracted fields and OCR text.
 */
public record ReceiptSummaryResponse(
        UUID id,
        String filename,
        String contentType,
        long fileSizeBytes,
        String status,
        Integer overallConfidence,
        String currency,
        UUID linkedTransactionId,
        OffsetDateTime extractedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
