package com.nova.finance.receipt.web.dto;

import java.util.List;

/**
 * API projection of the structured draft extracted from a receipt. Every field may
 * be {@code null} when it was not found — the pipeline never fabricates values.
 */
public record ReceiptFieldsResponse(
        ReceiptFieldResponse merchant,
        ReceiptFieldResponse date,
        ReceiptFieldResponse time,
        ReceiptFieldResponse currency,
        ReceiptFieldResponse subtotal,
        ReceiptFieldResponse tax,
        ReceiptFieldResponse discount,
        ReceiptFieldResponse total,
        ReceiptFieldResponse paymentMethod,
        ReceiptFieldResponse receiptNumber,
        List<ReceiptItemResponse> items,
        Integer overallConfidence
) {
}
