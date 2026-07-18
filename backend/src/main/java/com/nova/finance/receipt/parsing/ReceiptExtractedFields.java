package com.nova.finance.receipt.parsing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured result of reading a receipt: the detected fields, each wrapped in a
 * {@link ReceiptField} that carries its own confidence.
 *
 * <p>This is a pure data object. The pipeline (OCR -> parser -> confidence
 * scorer) produces it; the storage converter persists it as JSON and the API
 * returns it as-is. Keeping it free of JPA concerns means future AI extractors
 * can extend the same shape without a schema change.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ReceiptExtractedFields {

    private ReceiptField<String> merchant;
    /** ISO date, yyyy-MM-dd. */
    private ReceiptField<String> date;
    /** 24-hour time, HH:mm. */
    private ReceiptField<String> time;
    /** ISO 4217 code, e.g. "USD". */
    private ReceiptField<String> currency;
    private ReceiptField<BigDecimal> subtotal;
    private ReceiptField<BigDecimal> tax;
    private ReceiptField<BigDecimal> discount;
    private ReceiptField<BigDecimal> total;
    private ReceiptField<String> paymentMethod;
    private ReceiptField<String> receiptNumber;
    private List<ReceiptItem> items = new ArrayList<>();
    private Integer overallConfidence;
}
