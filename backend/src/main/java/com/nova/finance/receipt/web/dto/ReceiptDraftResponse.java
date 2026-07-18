package com.nova.finance.receipt.web.dto;

import com.nova.finance.transaction.web.dto.CreateTransactionRequest;

/**
 * A suggested transaction draft derived from an extracted receipt, returned so the
 * UI can pre-fill the review form. The user edits this before anything is saved —
 * no transaction is ever created from a draft automatically.
 *
 * @param receipt    the source receipt and its extracted fields
 * @param suggestion a pre-filled create-transaction payload (amount, date, merchant,
 *                    currency); account and category are left for the user to choose
 */
public record ReceiptDraftResponse(
        ReceiptResponse receipt,
        CreateTransactionRequest suggestion
) {
}
