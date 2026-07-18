package com.nova.finance.receipt.storage;

/**
 * Storage for receipt image bytes, kept entirely separate from transaction data.
 *
 * <p>The contract is deliberately minimal — store, load, delete by an opaque key —
 * so the business logic never depends on where bytes live. The local-disk
 * implementation ships today; Cloudinary, AWS S3, or MinIO backends can be added
 * by implementing this interface and selecting one via configuration, with no
 * change to the service, controller, or pipeline.</p>
 */
public interface ReceiptStorage {

    /** Short identifier of the backend, e.g. {@code "local"}. */
    String backend();

    /**
     * Persists bytes and returns an opaque key used to retrieve them later.
     *
     * @param userId      owner, used to namespace stored objects
     * @param bytes       file content
     * @param contentType MIME type
     * @param filename    original filename (for a human-friendly extension)
     * @return storage key to persist on the receipt
     * @throws ReceiptStorageException if the write fails
     */
    String store(String userId, byte[] bytes, String contentType, String filename);

    /**
     * Loads previously stored bytes.
     *
     * @throws ReceiptStorageException if the object is missing or cannot be read
     */
    StoredFile load(String key, String contentType);

    /** Best-effort delete; never throws for a missing object. */
    void delete(String key);
}
