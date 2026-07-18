package com.nova.finance.receipt.storage;

/**
 * The bytes and content type of a stored receipt, returned on retrieval.
 *
 * @param bytes       the raw file content
 * @param contentType the MIME type to serve it with
 */
public record StoredFile(byte[] bytes, String contentType) {
}
