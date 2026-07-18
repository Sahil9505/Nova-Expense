package com.nova.finance.receipt.storage;

import com.nova.common.exception.ErrorCode;
import com.nova.finance.receipt.ReceiptProcessingException;

/**
 * Raised when a receipt cannot be written to or read from the storage backend.
 * Storage is behind an interface, so this is the single failure contract that the
 * local, Cloudinary, S3, or MinIO implementations surface.
 */
public class ReceiptStorageException extends ReceiptProcessingException {

    public ReceiptStorageException(String message) {
        super(ErrorCode.RECEIPT_STORAGE_FAILED, message);
    }

    public ReceiptStorageException(String message, Throwable cause) {
        super(ErrorCode.RECEIPT_STORAGE_FAILED, message, cause);
    }
}
