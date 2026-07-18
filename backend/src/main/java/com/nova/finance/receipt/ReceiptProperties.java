package com.nova.finance.receipt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for the receipt pipeline, bound from {@code nova.receipt.*}.
 * Every value has a sensible default so the module is zero-config in development,
 * and each concern (upload limits, storage, OCR) can be tuned per deployment
 * without a code change.
 *
 * @param maxFileSizeBytes    largest accepted upload, in bytes (default 10 MB)
 * @param allowedContentTypes accepted MIME types
 * @param storage             storage backend settings
 * @param ocr                 OCR engine settings
 */
@ConfigurationProperties(prefix = "nova.receipt")
public record ReceiptProperties(
        Long maxFileSizeBytes,
        List<String> allowedContentTypes,
        Storage storage,
        Ocr ocr
) {

    private static final long DEFAULT_MAX_SIZE = 10L * 1024 * 1024;
    private static final List<String> DEFAULT_TYPES =
            List.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    public ReceiptProperties {
        if (maxFileSizeBytes == null || maxFileSizeBytes <= 0) {
            maxFileSizeBytes = DEFAULT_MAX_SIZE;
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = DEFAULT_TYPES;
        }
        if (storage == null) {
            storage = new Storage(null, null);
        }
        if (ocr == null) {
            ocr = new Ocr(null, null, null, null);
        }
    }

    /**
     * @param backend  which storage implementation to use ("local"); future:
     *                 "cloudinary", "s3", "minio"
     * @param localDir directory for the local backend
     */
    public record Storage(String backend, String localDir) {
        public Storage {
            if (backend == null || backend.isBlank()) {
                backend = "local";
            }
            if (localDir == null || localDir.isBlank()) {
                localDir = "./data/receipts";
            }
        }
    }

    /**
     * @param provider     preferred OCR provider name ("tesseract")
     * @param binaryPath   path to the tesseract executable
     * @param language     Tesseract language code (e.g. "eng")
     * @param timeoutMillis hard timeout for a single OCR run
     */
    public record Ocr(String provider, String binaryPath, String language, Long timeoutMillis) {
        public Ocr {
            if (provider == null || provider.isBlank()) {
                provider = "tesseract";
            }
            if (binaryPath == null || binaryPath.isBlank()) {
                binaryPath = "tesseract";
            }
            if (language == null || language.isBlank()) {
                language = "eng";
            }
            if (timeoutMillis == null || timeoutMillis <= 0) {
                timeoutMillis = 20_000L;
            }
        }
    }
}
