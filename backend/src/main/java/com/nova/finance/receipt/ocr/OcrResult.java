package com.nova.finance.receipt.ocr;

/**
 * Raw output of an OCR pass: the recognised text and the name of the provider
 * that produced it. Providers return text only; structured field extraction is a
 * separate, provider-agnostic stage.
 *
 * @param provider identifier of the engine (e.g. "tesseract")
 * @param text     recognised plain text, never {@code null} (may be empty)
 */
public record OcrResult(String provider, String text) {
}
