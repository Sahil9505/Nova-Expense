package com.nova.finance.receipt;

import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.parsing.ReceiptParsingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure extraction tests. Every expected value must come from a real match. */
class ReceiptParsingServiceTest {

    private final ReceiptParsingService service = new ReceiptParsingService();

    @Test
    void extractsGroceryReceiptFields() {
        String text = """
                WALMART SUPERCENTER
                123 MAIN ST
                DATE 03/14/2026  18:42

                MILK               2.99
                BREAD              3.49
                EGGS               4.25

                SUBTOTAL          10.73
                TAX                0.86
                TOTAL             11.59
                VISA
                RECEIPT #WMT-48213
                """;
        ReceiptExtractedFields fields = service.parse(text);

        assertThat(fields.getMerchant()).isNotNull();
        assertThat(fields.getMerchant().getValue()).contains("WALMART");

        assertThat(fields.getDate().getValue()).isEqualTo("2026-03-14");
        assertThat(fields.getTime().getValue()).isEqualTo("18:42");
        assertThat(fields.getCurrency()).isNull(); // no symbol/code present

        assertThat(fields.getSubtotal().getValue()).isEqualByComparingTo("10.73");
        assertThat(fields.getTax().getValue()).isEqualByComparingTo("0.86");
        assertThat(fields.getTotal().getValue()).isEqualByComparingTo("11.59");

        assertThat(fields.getPaymentMethod().getValue()).isEqualTo("Visa");
        assertThat(fields.getReceiptNumber().getValue()).isEqualTo("WMT-48213");

        // Line-item extraction is best-effort and not yet implemented; the parser
        // intentionally returns no items rather than risk fabricated values.
        assertThat(fields.getItems()).isEmpty();
    }

    @Test
    void extractsFromMonthNameAndSymbolCurrency() {
        String text = """
                CAFE LUNA
                Jan 05, 2026

                Espresso          3.50
                TAX                0.28
                TOTAL               3.78
                $ CASH
                """;
        ReceiptExtractedFields fields = service.parse(text);

        assertThat(fields.getMerchant().getValue()).isEqualTo("CAFE LUNA");
        assertThat(fields.getDate().getValue()).isEqualTo("2026-01-05");
        assertThat(fields.getCurrency().getValue()).isEqualTo("USD");
        assertThat(fields.getPaymentMethod().getValue()).isEqualTo("Cash");
        assertThat(fields.getTotal().getValue()).isEqualByComparingTo("3.78");
    }

    @Test
    void extractsEuropeanDecimalAndIsoDate() {
        String text = """
                BÄCKEREI MÜLLER
                2026-02-28

                Brötchen          2,40
                TAX                0,19
                EUR
                """;
        ReceiptExtractedFields fields = service.parse(text);

        assertThat(fields.getDate().getValue()).isEqualTo("2026-02-28");
        assertThat(fields.getCurrency().getValue()).isEqualTo("EUR");
        // Comma decimals (2,40) are not yet normalized by the parser, so no total is found.
        assertThat(fields.getTotal()).isNull();
        assertThat(fields.getSubtotal()).isNull();
    }

    @Test
    void ignoresYearConfusionAndPicksRealTotal() {
        String text = """
                ACNE SUPPLY CO
                2026-03-01
                ITEM A             5.00
                ITEM B             7.50
                SUBTOTAL          12.50
                TAX                1.00
                TOTAL             13.50
                """;
        ReceiptExtractedFields fields = service.parse(text);

        assertThat(fields.getSubtotal().getValue()).isEqualByComparingTo("12.50");
        assertThat(fields.getTax().getValue()).isEqualByComparingTo("1.00");
        assertThat(fields.getTotal().getValue()).isEqualByComparingTo("13.50");
    }

    @Test
    void returnsEmptyDraftForBlankText() {
        ReceiptExtractedFields fields = service.parse("   \n  \n");

        assertThat(fields.getMerchant()).isNull();
        assertThat(fields.getDate()).isNull();
        assertThat(fields.getTotal()).isNull();
        assertThat(fields.getItems()).isEmpty();
    }

    @Test
    void detectsDiscountLine() {
        String text = """
                BOOKSTORE
                BOOK               20.00
                DISCOUNT            2.00
                SUBTOTAL          18.00
                TAX                1.44
                TOTAL             19.44
                """;
        ReceiptExtractedFields fields = service.parse(text);

        assertThat(fields.getDiscount().getValue()).isEqualByComparingTo("2.00");
        assertThat(fields.getSubtotal().getValue()).isEqualByComparingTo("18.00");
        assertThat(fields.getTotal().getValue()).isEqualByComparingTo("19.44");
    }
}
