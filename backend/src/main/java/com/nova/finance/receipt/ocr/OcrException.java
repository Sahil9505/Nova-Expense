package com.nova.finance.receipt.ocr;

import com.nova.common.exception.ErrorCode;
import com.nova.finance.receipt.ReceiptProcessingException;

/**
 * Raised when optical character recognition cannot run or returns nothing usable.
 * Covers an unavailable engine, a timeout, a corrupted image, or empty output.
 * The pipeline catches this and marks the receipt {@code FAILED} so the user can
 * fall back to manual entry instead of seeing a hard error.
 */
public class OcrException extends ReceiptProcessingException {

    public OcrException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public OcrException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
