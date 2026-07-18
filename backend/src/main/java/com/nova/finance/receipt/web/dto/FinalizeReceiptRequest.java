package com.nova.finance.receipt.web.dto;

import com.nova.finance.transaction.Transaction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The user's final, editable decision for a receipt, sent when they confirm the
 * transaction. Account and category are chosen by the user; the amount, merchant,
 * currency, and date default to the extracted draft but can be corrected.
 *
 * <p>Validation mirrors {@code CreateTransactionRequest} so the same rules apply
 * when the backend reuses the existing transaction-creation flow.</p>
 */
public record FinalizeReceiptRequest(

        @NotNull(message = "An account is required")
        UUID accountId,

        @NotNull(message = "A category is required")
        UUID categoryId,

        @NotNull(message = "Transaction type is required")
        Transaction.Type type,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 255, message = "Merchant is too long")
        String merchant,

        @Size(max = 255, message = "Note is too long")
        String note,

        @NotEmpty(message = "Currency is required")
        @Size(min = 3, max = 8, message = "Currency must be 3–8 characters")
        String currency,

        @NotNull(message = "A date is required")
        OffsetDateTime occurredAt
) {
}
