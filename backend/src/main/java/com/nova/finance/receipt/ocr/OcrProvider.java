package com.nova.finance.receipt.ocr;

/**
 * Optical character recognition behind a single, replaceable contract.
 *
 * <p>OCR is one implementation detail of the receipt pipeline, not its identity.
 * Tesseract is the current provider; a future cloud or ML provider can be dropped
 * in by implementing this interface and being selected by configuration — no other
 * part of the system changes. Providers turn image bytes into plain text only;
 * parsing, normalization, and confidence scoring live downstream.</p>
 */
public interface OcrProvider {

    /** Short identifier recorded on the receipt, e.g. {@code "tesseract"}. */
    String name();

    /** Whether the provider can run right now (binary present, credentials set, …). */
    boolean isAvailable();

    /**
     * Recognises text in an image.
     *
     * @param image       the raw image bytes
     * @param contentType the MIME type of the image (e.g. {@code image/png})
     * @return the recognised text and provider name
     * @throws OcrException if the engine is unavailable, times out, or the image
     *                      cannot be read
     */
    OcrResult extractText(byte[] image, String contentType);
}
