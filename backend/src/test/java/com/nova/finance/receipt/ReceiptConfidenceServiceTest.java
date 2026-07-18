package com.nova.finance.receipt;

import com.nova.finance.receipt.parsing.ReceiptConfidenceService;
import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.parsing.ReceiptField;
import com.nova.finance.receipt.parsing.ReceiptItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Confidence scoring is deterministic and explainable. */
class ReceiptConfidenceServiceTest {

    private final ReceiptConfidenceService service = new ReceiptConfidenceService();

    @Test
    void scoresACompleteReceiptHigh() {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        fields.setMerchant(field("WALMART", null));
        fields.setDate(field("2026-03-14", null));
        fields.setCurrency(field("USD", null));
        fields.setSubtotal(amount("10.73"));
        fields.setTax(amount("0.86"));
        fields.setTotal(amount("11.59"));

        service.populate(fields);

        assertThat(fields.getOverallConfidence()).isNotNull();
        assertThat(fields.getOverallConfidence()).isGreaterThan(70);
        assertThat(fields.getTotal().getConfidence()).isGreaterThanOrEqualTo(80);
        assertThat(fields.getMerchant().getConfidence()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void boostsWhenAmountsReconcile() {
        ReceiptExtractedFields reconciled = build(10.73, 0.86, 11.59);
        ReceiptExtractedFields notReconciled = build(10.73, 0.86, 12.00);

        assertThat(reconciled.getOverallConfidence())
                .isGreaterThan(notReconciled.getOverallConfidence());
    }

    @Test
    void missingFieldsScoreZeroAndStayNull() {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        // nothing set -> no draft
        service.populate(fields);
        assertThat(fields.getOverallConfidence()).isNull();

        fields.setTotal(amount("5.00"));
        fields.setMerchant(field("X", null)); // too short
        service.populate(fields);
        assertThat(fields.getMerchant().getConfidence()).isLessThan(40);
        assertThat(fields.getOverallConfidence()).isNotNull();
    }

    @Test
    void individualScorers() {
        assertThat(service.scoreCurrency("USD")).isGreaterThan(service.scoreCurrency("usd"));
        assertThat(service.scoreCurrency(null)).isZero();
        assertThat(service.scoreAmount(BigDecimal.ZERO, true)).isEqualTo(20);
        assertThat(service.scoreAmount(new BigDecimal("12.34"), true)).isGreaterThan(80);
        assertThat(service.scoreTime("18:42")).isGreaterThan(80);
        assertThat(service.scoreTime("99:99")).isLessThan(50);
        assertThat(service.scoreIsoDate("2026-03-14")).isGreaterThan(80);
    }

    @Test
    void itemsGetScored() {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        fields.setItems(java.util.List.of(
                new ReceiptItem("MILK", amount("2.99")),
                new ReceiptItem("BREAD", amount("3.49"))));
        service.populate(fields);
        assertThat(fields.getItems()).hasSize(2);
        assertThat(fields.getItems().get(0).getAmount().getConfidence()).isNotNull();
    }

    private ReceiptExtractedFields build(double subtotal, double tax, double total) {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        fields.setCurrency(field("USD", null));
        fields.setSubtotal(amount(String.valueOf(subtotal)));
        fields.setTax(amount(String.valueOf(tax)));
        fields.setTotal(amount(String.valueOf(total)));
        service.populate(fields);
        return fields;
    }

    private <T> ReceiptField<T> field(T value, Integer confidence) {
        return new ReceiptField<>(value, confidence);
    }

    private ReceiptField<BigDecimal> amount(String value) {
        return new ReceiptField<>(new BigDecimal(value), null);
    }
}
