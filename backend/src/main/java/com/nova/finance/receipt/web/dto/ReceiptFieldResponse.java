package com.nova.finance.receipt.web.dto;

/**
 * API projection of a single extracted field: its value, confidence (0–100), and a
 * convenience {@code lowConfidence} flag the UI uses to highlight values worth a
 * second look. {@code value} is serialized as-is (string or number).
 */
public record ReceiptFieldResponse(Object value, Integer confidence, boolean lowConfidence) {
}
