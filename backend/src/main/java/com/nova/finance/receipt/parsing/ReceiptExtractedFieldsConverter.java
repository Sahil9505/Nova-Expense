package com.nova.finance.receipt.parsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists a {@link ReceiptExtractedFields} as a JSON string in a TEXT column.
 *
 * <p>Storing the draft as JSON (rather than a column per field) keeps the schema
 * stable as the parser adds fields, and lets future AI extractors attach new
 * keys without a migration. A single static {@link ObjectMapper} is used because
 * JPA instantiates converters itself, so constructor injection is not available.</p>
 */
@Converter(autoApply = false)
public class ReceiptExtractedFieldsConverter
        implements AttributeConverter<ReceiptExtractedFields, String> {

    private static final ObjectMapper WRITER = new ObjectMapper();
    private static final ObjectMapper READER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String convertToDatabaseColumn(ReceiptExtractedFields fields) {
        if (fields == null) {
            return null;
        }
        try {
            return WRITER.writeValueAsString(fields);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize receipt fields", exception);
        }
    }

    @Override
    public ReceiptExtractedFields convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return READER.readValue(dbData, ReceiptExtractedFields.class);
        } catch (JsonProcessingException exception) {
            // A malformed blob must never break the read path; treat it as "no draft".
            return null;
        }
    }
}
