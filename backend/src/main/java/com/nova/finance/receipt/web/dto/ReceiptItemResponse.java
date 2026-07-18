package com.nova.finance.receipt.web.dto;

/** API projection of a detected line item. */
public record ReceiptItemResponse(String name, ReceiptFieldResponse amount) {
}
