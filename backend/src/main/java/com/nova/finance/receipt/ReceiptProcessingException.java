package com.nova.finance.receipt;

import com.nova.common.exception.ErrorCode;
import com.nova.common.exception.NovaException;

/**
 * Base exception for the Receipt domain. Carries a stable {@link ErrorCode} so the
 * global handler can translate it into the standard {@link com.nova.common.api.ApiResponse}
 * envelope. OCR and storage failures extend this so a single handler covers the pipeline.
 */
public class ReceiptProcessingException extends NovaException {

    public ReceiptProcessingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReceiptProcessingException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ReceiptProcessingException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
