package com.nova.finance.receipt.parsing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns raw OCR text into a structured {@link ReceiptExtractedFields} draft.
 *
 * <p>This is the "normalization" stage of the pipeline and it is deliberately
 * conservative: every value comes from a real match in the text. When a field
 * cannot be found it is left {@code null} — the parser never fabricates a value.
 * Confidence is assigned later by {@link ReceiptConfidenceService}, keeping
 * extraction and scoring independent and testable.</p>
 *
 * <p>The heuristics are line- and label-oriented (the layout OCR preserves best):
 * amounts are read from lines containing keywords like TOTAL / SUBTOTAL / TAX;
 * dates and times from common formats; currency from a symbol or code. This is a
 * pure text transform with no I/O, so a future ML extractor can replace it behind
 * the same return type without touching the rest of the system.</p>
 */
@Service
public class ReceiptParsingService {

    private static final Pattern AMOUNT =
            Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:[,\\s]\\d{3})*(?:\\.\\d{2})|\\d+\\.\\d{2})(?![\\d])");
    private static final Pattern TOTAL_LINE =
            Pattern.compile("(?i)\\b(grand\\s*total|total\\s*due|balance\\s*due|amount\\s*due|total)\\b");
    private static final Pattern SUBTOTAL_LINE =
            Pattern.compile("(?i)\\b(sub[\\s-]*total)\\b");
    private static final Pattern TAX_LINE =
            Pattern.compile("(?i)\\b(sales\\s*tax|vat|gst|hst|tax)\\b");
    private static final Pattern DISCOUNT_LINE =
            Pattern.compile("(?i)\\b(discount|savings|coupon|promo)\\b");
    private static final Pattern PAYMENT_LINE =
            Pattern.compile("(?i)\\b(visa|mastercard|master\\s*card|amex|american\\s*express|discover|"
                    + "debit|credit\\s*card|credit|cash|paypal|apple\\s*pay|google\\s*pay|contactless)\\b");
    private static final Pattern RECEIPT_NO_LINE =
            Pattern.compile("(?i)\\b(receipt|invoice|order|ref(?:erence)?|trans(?:action)?|tran)\\b"
                    + "\\s*(?:#|no\\.?|number|id)?\\s*[:#]?\\s*([A-Za-z0-9][A-Za-z0-9-]{2,})");
    private static final Pattern TIME =
            Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)(?::[0-5]\\d)?\\s*([AaPp][Mm])?\\b");
    private static final Pattern CURRENCY_CODE =
            Pattern.compile("\\b(USD|EUR|GBP|JPY|CAD|AUD|INR|SGD|CHF|CNY|NZD|HKD)\\b");

    private static final List<DatePattern> DATE_PATTERNS = List.of(
            new DatePattern(Pattern.compile("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b"), true),
            new DatePattern(Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b"), false),
            new DatePattern(Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2})\\b"), false)
    );
    private static final Pattern MONTH_NAME_DATE = Pattern.compile(
            "(?i)\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b");
    private static final List<String> MONTHS = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    /**
     * Parses raw OCR text into a draft. Values are read directly from the text;
     * anything not found is left {@code null}. Confidence is not set here.
     */
    public ReceiptExtractedFields parse(String rawText) {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        if (rawText == null || rawText.isBlank()) {
            return fields;
        }

        String[] rawLines = rawText.split("\\r?\\n");
        List<String> lines = normalizeLines(rawLines);

        setIfPresent(fields::setMerchant, detectMerchant(lines));
        setIfPresent(fields::setDate, detectDate(rawText));
        setIfPresent(fields::setTime, detectTime(rawText));
        setIfPresent(fields::setCurrency, detectCurrency(rawText));
        setIfPresent(fields::setSubtotal, detectLabeledAmount(lines, SUBTOTAL_LINE, null));
        setIfPresent(fields::setTax, detectLabeledAmount(lines, TAX_LINE, SUBTOTAL_LINE));
        setIfPresent(fields::setDiscount, detectLabeledAmount(lines, DISCOUNT_LINE, null));
        setIfPresent(fields::setTotal, detectTotal(lines));
        setIfPresent(fields::setPaymentMethod, detectPayment(lines));
        setIfPresent(fields::setReceiptNumber, detectReceiptNumber(lines));

        return fields;
    }

    // -----------------------------------------------------------------------
    // Detectors
    // -----------------------------------------------------------------------

    /** The first substantial line that is not itself an amount/label is the merchant. */
    private ReceiptField<String> detectMerchant(List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 3) {
                continue;
            }
            if (AMOUNT.matcher(trimmed).find()) {
                continue;
            }
            long letters = trimmed.chars().filter(Character::isLetter).count();
            if (letters < 3) {
                continue;
            }
            if (TOTAL_LINE.matcher(trimmed).find() || TAX_LINE.matcher(trimmed).find()) {
                continue;
            }
            return field(collapse(trimmed));
        }
        return null;
    }

    private ReceiptField<String> detectDate(String text) {
        Matcher named = MONTH_NAME_DATE.matcher(text);
        if (named.find()) {
            int month = MONTHS.indexOf(named.group(1).toLowerCase(Locale.ROOT)) + 1;
            Integer day = parseIntOrNull(named.group(2));
            Integer year = parseIntOrNull(named.group(3));
            LocalDate date = safeDate(year, month, day);
            if (date != null) {
                return field(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        }
        for (DatePattern datePattern : DATE_PATTERNS) {
            Matcher matcher = datePattern.pattern.matcher(text);
            if (matcher.find()) {
                LocalDate date = datePattern.isoOrder
                        ? safeDate(parseIntOrNull(matcher.group(1)), parseIntOrNull(matcher.group(2)),
                        parseIntOrNull(matcher.group(3)))
                        : fromDayMonthYear(matcher.group(1), matcher.group(2), matcher.group(3));
                if (date != null) {
                    return field(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        }
        return null;
    }

    private ReceiptField<String> detectTime(String text) {
        Matcher matcher = TIME.matcher(text);
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            String meridiem = matcher.group(3);
            if (meridiem != null) {
                boolean pm = meridiem.equalsIgnoreCase("pm");
                if (pm && hour < 12) {
                    hour += 12;
                } else if (!pm && hour == 12) {
                    hour = 0;
                }
            }
            if (hour <= 23 && minute <= 59) {
                return field(String.format("%02d:%02d", hour, minute));
            }
        }
        return null;
    }

    private ReceiptField<String> detectCurrency(String text) {
        Matcher code = CURRENCY_CODE.matcher(text);
        if (code.find()) {
            return field(code.group(1).toUpperCase(Locale.ROOT));
        }
        if (text.contains("$")) {
            return field("USD");
        }
        if (text.contains("€")) {
            return field("EUR");
        }
        if (text.contains("£")) {
            return field("GBP");
        }
        if (text.contains("₹")) {
            return field("INR");
        }
        if (text.contains("¥")) {
            return field("JPY");
        }
        return null;
    }

    /**
     * The total is the amount on the most specific total line. We prefer
     * "grand total" / "total due" over a bare "total", and never treat a
     * "subtotal" line as the total.
     */
    private ReceiptField<BigDecimal> detectTotal(List<String> lines) {
        BigDecimal best = null;
        int bestRank = -1;
        for (String line : lines) {
            if (SUBTOTAL_LINE.matcher(line).find()) {
                continue;
            }
            Matcher matcher = TOTAL_LINE.matcher(line);
            if (matcher.find()) {
                BigDecimal amount = lastAmount(line);
                if (amount == null) {
                    continue;
                }
                int rank = rankTotalKeyword(matcher.group(1));
                if (rank > bestRank) {
                    bestRank = rank;
                    best = amount;
                }
            }
        }
        return best == null ? null : field(best);
    }

    private int rankTotalKeyword(String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (normalized) {
            case "grand total" -> 4;
            case "total due", "balance due", "amount due" -> 3;
            case "total" -> 1;
            default -> 2;
        };
    }

    /**
     * Reads the amount from the first line matching {@code label}. A line matching
     * {@code excludeIfMatches} is skipped so "tax" never picks up a "subtotal" row.
     */
    private ReceiptField<BigDecimal> detectLabeledAmount(List<String> lines, Pattern label, Pattern excludeIfMatches) {
        for (String line : lines) {
            if (excludeIfMatches != null && excludeIfMatches.matcher(line).find()) {
                continue;
            }
            if (label.matcher(line).find()) {
                BigDecimal amount = lastAmount(line);
                if (amount != null) {
                    return field(amount);
                }
            }
        }
        return null;
    }

    private ReceiptField<String> detectPayment(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = PAYMENT_LINE.matcher(line);
            if (matcher.find()) {
                return field(normalizePayment(matcher.group(1)));
            }
        }
        return null;
    }

    private ReceiptField<String> detectReceiptNumber(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = RECEIPT_NO_LINE.matcher(line);
            if (matcher.find()) {
                String candidate = matcher.group(2);
                if (candidate != null && candidate.chars().anyMatch(Character::isDigit)) {
                    return field(candidate);
                }
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<String> normalizeLines(String[] rawLines) {
        return java.util.Arrays.stream(rawLines)
                .map(line -> line.replace('\t', ' ').trim())
                .filter(line -> !line.isEmpty())
                .toList();
    }

    /** The last amount on a line — totals sit at the end of "TOTAL ..... 12.34". */
    private BigDecimal lastAmount(String line) {
        Matcher matcher = AMOUNT.matcher(line);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last == null ? null : toBigDecimal(last);
    }

    private BigDecimal toBigDecimal(String raw) {
        String cleaned = raw.replace(",", "").replace(" ", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizePayment(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return switch (normalized) {
            case "master card" -> "Mastercard";
            case "american express" -> "Amex";
            case "credit card" -> "Credit Card";
            case "apple pay" -> "Apple Pay";
            case "google pay" -> "Google Pay";
            default -> capitalize(normalized);
        };
    }

    private String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String collapse(String value) {
        return value.replaceAll("\\s{2,}", " ").trim();
    }

    private LocalDate fromDayMonthYear(String a, String b, String yearRaw) {
        Integer first = parseIntOrNull(a);
        Integer second = parseIntOrNull(b);
        Integer year = parseIntOrNull(yearRaw);
        if (year != null && year < 100) {
            year += 2000;
        }
        // Ambiguous d/m vs m/d: if the first field can't be a month, treat it as the day.
        if (first != null && first > 12 && second != null && second <= 12) {
            return safeDate(year, second, first);
        }
        // Default to month/day (most receipts in the supported locales), fall back to day/month.
        LocalDate monthFirst = safeDate(year, first, second);
        if (monthFirst != null) {
            return monthFirst;
        }
        return safeDate(year, second, first);
    }

    private LocalDate safeDate(Integer year, Integer month, Integer day) {
        if (year == null || month == null || day == null) {
            return null;
        }
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private <T> ReceiptField<T> field(T value) {
        return new ReceiptField<>(value, null);
    }

    private <T> void setIfPresent(java.util.function.Consumer<ReceiptField<T>> setter, ReceiptField<T> value) {
        if (value != null && value.getValue() != null) {
            setter.accept(value);
        }
    }

    private record DatePattern(Pattern pattern, boolean isoOrder) {
    }
}
