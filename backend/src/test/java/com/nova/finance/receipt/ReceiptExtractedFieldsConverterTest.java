package com.nova.finance.receipt;

import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.parsing.ReceiptExtractedFieldsConverter;
import com.nova.finance.receipt.parsing.ReceiptField;
import com.nova.finance.receipt.parsing.ReceiptItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** The converter is the storage contract: JSON in, same shape out. */
class ReceiptExtractedFieldsConverterTest {

    private final ReceiptExtractedFieldsConverter converter = new ReceiptExtractedFieldsConverter();

    @Test
    void roundTripsFieldsWithConfidence() {
        ReceiptExtractedFields fields = new ReceiptExtractedFields();
        fields.setMerchant(new ReceiptField<>("WALMART", 75));
        fields.setTotal(new ReceiptField<>(new BigDecimal("11.59"), 85));
        fields.setCurrency(new ReceiptField<>("USD", 90));
        fields.setItems(java.util.List.of(
                new ReceiptItem("MILK", new ReceiptField<>(new BigDecimal("2.99"), 60))));
        fields.setOverallConfidence(80);

        String db = converter.convertToDatabaseColumn(fields);
        assertThat(db).contains("WALMART");
        assertThat(db).contains("11.59");
        assertThat(db).contains("\"overallConfidence\":80");

        ReceiptExtractedFields back = converter.convertToEntityAttribute(db);
        assertThat(back.getMerchant().getValue()).isEqualTo("WALMART");
        assertThat(back.getMerchant().getConfidence()).isEqualTo(75);
        assertThat(back.getTotal().getValue()).isEqualByComparingTo("11.59");
        assertThat(back.getCurrency().getValue()).isEqualTo("USD");
        assertThat(back.getItems()).hasSize(1);
        assertThat(back.getItems().get(0).getName()).isEqualTo("MILK");
        assertThat(back.getOverallConfidence()).isEqualTo(80);
    }

    @Test
    void nullsMapToNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void toleratesUnknownKeys() {
        ReceiptExtractedFields back = converter.convertToEntityAttribute(
                "{\"merchant\":{\"value\":\"SHOP\",\"confidence\":70},\"futureField\":123}");
        assertThat(back.getMerchant().getValue()).isEqualTo("SHOP");
        assertThat(back.getMerchant().getConfidence()).isEqualTo(70);
    }
}
