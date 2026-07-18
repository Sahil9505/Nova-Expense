package com.nova.finance.receipt;

import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.web.dto.ReceiptFieldsResponse;
import com.nova.finance.receipt.web.dto.ReceiptItemResponse;
import com.nova.finance.receipt.web.dto.ReceiptFieldResponse;
import com.nova.finance.receipt.web.dto.ReceiptResponse;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps a {@link Receipt} (and its extracted fields) to its API projections.
 *
 * <p>The extracted {@link ReceiptExtractedFields} is a pure value object, so it is
 * mapped 1:1 into {@link ReceiptFieldsResponse}; confidence scores travel with
 * the values. Status is projected as its name string. No user or storage path
 * is ever surfaced.</p>
 */
@Mapper(componentModel = "spring")
public interface ReceiptMapper {

    @Mapping(target = "status", expression = "java(receipt.getStatus() != null ? receipt.getStatus().name() : null)")
    @Mapping(target = "fields", source = "extractedFields")
    ReceiptResponse toResponse(Receipt receipt);

    @Mapping(target = "status", expression = "java(receipt.getStatus() != null ? receipt.getStatus().name() : null)")
    ReceiptSummaryResponse toSummary(Receipt receipt);

    List<ReceiptSummaryResponse> toSummaryList(List<Receipt> receipts);

    // ----- extracted fields -----

    default ReceiptFieldsResponse toFields(ReceiptExtractedFields fields) {
        if (fields == null) {
            return null;
        }
        return new ReceiptFieldsResponse(
                toField(fields.getMerchant()),
                toField(fields.getDate()),
                toField(fields.getTime()),
                toField(fields.getCurrency()),
                toField(fields.getSubtotal()),
                toField(fields.getTax()),
                toField(fields.getDiscount()),
                toField(fields.getTotal()),
                toField(fields.getPaymentMethod()),
                toField(fields.getReceiptNumber()),
                toItemList(fields.getItems()),
                fields.getOverallConfidence());
    }

    default <T> ReceiptFieldResponse toField(com.nova.finance.receipt.parsing.ReceiptField<T> field) {
        if (field == null) {
            return null;
        }
        return new ReceiptFieldResponse(field.getValue(), field.getConfidence(), field.isLowConfidence());
    }

    default List<ReceiptItemResponse> toItemList(List<com.nova.finance.receipt.parsing.ReceiptItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> new ReceiptItemResponse(item.getName(), toField(item.getAmount())))
                .toList();
    }
}
