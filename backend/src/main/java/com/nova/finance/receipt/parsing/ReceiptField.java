package com.nova.finance.receipt.parsing;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single extracted value paired with the confidence the pipeline has in it.
 *
 * <p>Confidence is a 0–100 integer. {@code null} means the field was not
 * found at all. The frontend uses {@link #isLowConfidence()} to decide
 * when to flag a value for the user to review. We never invent values, so a
 * missing field is reported as {@code null} rather than a guess.</p>
 *
 * @param <T> the value type (usually {@code String} or {@code BigDecimal})
 */
@Getter
@Setter
@NoArgsConstructor
public class ReceiptField<T> {

    /** Confidence at or below which a field is surfaced as "please check". */
    public static final int LOW_CONFIDENCE = 60;

    private T value;
    private Integer confidence;

    public ReceiptField(T value, Integer confidence) {
        this.value = value;
        this.confidence = confidence;
    }

    /** A field is low-confidence when it is missing or scored below the threshold. */
    public boolean isLowConfidence() {
        return confidence == null || confidence < LOW_CONFIDENCE;
    }
}
