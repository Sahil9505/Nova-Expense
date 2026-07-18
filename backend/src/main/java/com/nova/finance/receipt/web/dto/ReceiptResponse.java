package com.nova.finance.receipt.web.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API-safe projection of a {@link com.nova.finance.receipt.Receipt}. Never exposes
 * the owning user or the raw storage path. The image is served from a separate
 * endpoint; here we only expose whether one is available and its metadata.
 */
public record ReceiptResponse(
        UUID id,
        String filename,
        String contentType,
        long fileSizeBytes,
        String status,
        String statusMessage,
        String ocrProvider,
        Integer overallConfidence,
        String currency,
        ReceiptFieldsResponse fields,
        UUID linkedTransactionId,
        OffsetDateTime extractedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
