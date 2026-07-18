package com.nova.finance.receipt.parsing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line item detected on a receipt (e.g. "MILK 2.99"). Items are
 * optional and frequently noisy in OCR output, so they are reported best-effort
 * with their own confidence score.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReceiptItem {

    private String name;
    private ReceiptField<BigDecimal> amount;

    public ReceiptItem(String name, ReceiptField<BigDecimal> amount) {
        this.name = name;
        this.amount = amount;
    }
}
