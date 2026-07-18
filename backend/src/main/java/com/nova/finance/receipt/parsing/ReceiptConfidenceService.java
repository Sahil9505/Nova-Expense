package com.nova.finance.receipt.parsing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Scores each extracted field and the receipt as a whole.
 *
 * <p>Confidence is a 0–100 integer derived from cheap, deterministic signals:
 * is the value present and well-formed, did a label keyword back it up, and do the
 * amounts reconcile (subtotal + tax ≈ total)? The weights are intentionally simple
 * and explainable — this is not a model, it is a heuristic that tells the user
 * which fields to double-check. Nothing here invents data; a missing field stays
 * {@code null} and scores 0.</p>
 */
@Service
public class ReceiptConfidenceService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Fills the confidence on every field and the overall score of {@code fields}. */
    public void populate(ReceiptExtractedFields fields) {
        Objects.requireNonNull(fields, "fields");

        if (fields.getMerchant() != null) {
            fields.getMerchant().setConfidence(scoreText(fields.getMerchant().getValue(), 55, 85));
        }
        if (fields.getDate() != null) {
            fields.getDate().setConfidence(scoreIsoDate(fields.getDate().getValue()));
        }
        if (fields.getTime() != null) {
            fields.getTime().setConfidence(scoreTime(fields.getTime().getValue()));
        }
        if (fields.getCurrency() != null) {
            fields.getCurrency().setConfidence(scoreCurrency(fields.getCurrency().getValue()));
        }
        if (fields.getSubtotal() != null) {
            fields.getSubtotal().setConfidence(scoreAmount(fields.getSubtotal().getValue(), true));
        }
        if (fields.getTax() != null) {
            fields.getTax().setConfidence(scoreAmount(fields.getTax().getValue(), true));
        }
        if (fields.getDiscount() != null) {
            fields.getDiscount().setConfidence(scoreAmount(fields.getDiscount().getValue(), true));
        }
        if (fields.getTotal() != null) {
            fields.getTotal().setConfidence(scoreAmount(fields.getTotal().getValue(), true));
        }
        if (fields.getPaymentMethod() != null) {
            fields.getPaymentMethod().setConfidence(scoreText(fields.getPaymentMethod().getValue(), 45, 80));
        }
        if (fields.getReceiptNumber() != null) {
            fields.getReceiptNumber().setConfidence(scoreText(fields.getReceiptNumber().getValue(), 40, 75));
        }
        for (ReceiptItem item : fields.getItems()) {
            if (item.getAmount() != null) {
                item.getAmount().setConfidence(scoreAmount(item.getAmount().getValue(), false));
            }
        }

        fields.setOverallConfidence(overall(fields));
    }

    // -----------------------------------------------------------------------
    // Per-field scorers
    // -----------------------------------------------------------------------

    /** A label-backed name: short gibberish scores low, a real word scores high. */
    public int scoreText(String value, int floor, int ceil) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2) {
            return 20;
        }
        int letters = (int) trimmed.chars().filter(Character::isLetter).count();
        if (letters == 0) {
            return 30; // all digits / symbols, unlikely to be a name
        }
        int score = floor + Math.min(ceil - floor, (trimmed.length() - 2) * 4);
        return clamp(score);
    }

    public int scoreIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            ISO_DATE.parse(value.trim());
            return 85;
        } catch (RuntimeException exception) {
            return 40;
        }
    }

    public int scoreTime(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return value.trim().matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$") ? 85 : 40;
    }

    public int scoreCurrency(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return value.trim().matches("^[A-Z]{3}$") ? 90 : 45;
    }

    /** An amount: positive and labelled is high; zero/negative is clearly wrong. */
    public int scoreAmount(BigDecimal value, boolean labelMatched) {
        if (value == null) {
            return 0;
        }
        if (value.signum() <= 0) {
            return 20;
        }
        int score = labelMatched ? 80 : 60;
        if (value.scale() <= 2) {
            score += 10;
        }
        return clamp(score);
    }

    // -----------------------------------------------------------------------
    // Overall
    // -----------------------------------------------------------------------

    /**
     * Weighted average of the non-null fields, boosted when the amounts reconcile.
     * Returns {@code null} when nothing was extracted.
     */
    Integer overall(ReceiptExtractedFields fields) {
        List<Integer> scores = new ArrayList<>();
        add(scores, fields.getMerchant());
        add(scores, fields.getDate(), 2);
        add(scores, fields.getCurrency());
        add(scores, fields.getSubtotal(), 2);
        add(scores, fields.getTax());
        add(scores, fields.getDiscount());
        add(scores, fields.getTotal(), 3);
        add(scores, fields.getPaymentMethod());
        add(scores, fields.getReceiptNumber());

        if (scores.isEmpty()) {
            return null;
        }

        int sum = 0;
        for (int score : scores) {
            sum += score;
        }
        int average = sum / scores.size();

        BigDecimal total = valueOf(fields.getTotal());
        BigDecimal subtotal = valueOf(fields.getSubtotal());
        BigDecimal tax = valueOf(fields.getTax());
        if (total != null && subtotal != null && tax != null
                && amountsReconcile(subtotal, tax, total)) {
            average = clamp(average + 15);
        }
        return average;
    }

    private void add(List<Integer> scores, ReceiptField<?> field) {
        add(scores, field, 1);
    }

    private void add(List<Integer> scores, ReceiptField<?> field, int weight) {
        if (field != null && field.getConfidence() != null) {
            for (int i = 0; i < weight; i++) {
                scores.add(field.getConfidence());
            }
        }
    }

    private BigDecimal valueOf(ReceiptField<BigDecimal> field) {
        return field == null ? null : field.getValue();
    }

    /** subtotal + tax within a cent of total. */
    boolean amountsReconcile(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
        BigDecimal sum = subtotal.add(tax);
        BigDecimal diff = sum.subtract(total).abs();
        return diff.compareTo(new BigDecimal("0.02")) <= 0;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
