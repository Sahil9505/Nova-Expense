package com.nova.finance.receipt.ocr;

import com.nova.common.exception.ErrorCode;
import com.nova.finance.receipt.ReceiptProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * OCR backed by the Tesseract engine, invoked through its command-line binary.
 *
 * <p>Using the CLI (rather than a bundled native binding) keeps the JVM free of
 * platform-specific native libraries while still running real Tesseract OCR. The
 * provider is honest about availability: if the binary is not installed,
 * {@link #isAvailable()} returns {@code false} and the pipeline degrades to a
 * manual-entry draft instead of failing the upload. Each run is bounded by a
 * configurable timeout so a stuck process can never block a request thread
 * indefinitely.</p>
 *
 * <p>This class is one {@link OcrProvider} implementation. Replacing Tesseract
 * with a cloud or ML provider means adding another implementation and selecting
 * it via configuration — nothing else in the pipeline changes.</p>
 */
@Component
public class TesseractOcrProvider implements OcrProvider {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrProvider.class);

    private final ReceiptProperties.Ocr config;

    public TesseractOcrProvider(ReceiptProperties properties) {
        this.config = properties.ocr();
    }

    @Override
    public String name() {
        return "tesseract";
    }

    @Override
    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder(config.binaryPath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public OcrResult extractText(byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new OcrException(ErrorCode.RECEIPT_INVALID_IMAGE, "The image was empty.");
        }

        Path input = null;
        try {
            input = Files.createTempFile("nova-receipt-", suffixFor(contentType));
            Files.write(input, image);

            // "stdout" tells Tesseract to write recognised text to standard out.
            Process process = new ProcessBuilder(
                    config.binaryPath(),
                    input.toString(),
                    "stdout",
                    "-l", config.language())
                    .redirectErrorStream(false)
                    .start();

            String text;
            try (InputStream stdout = process.getInputStream()) {
                text = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
            }

            boolean finished = process.waitFor(config.timeoutMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new OcrException(ErrorCode.RECEIPT_PROCESSING_FAILED,
                        "The receipt scan timed out.");
            }
            if (process.exitValue() != 0) {
                throw new OcrException(ErrorCode.RECEIPT_INVALID_IMAGE,
                        "The image could not be read by the scanner.");
            }
            return new OcrResult(name(), text == null ? "" : text);
        } catch (IOException exception) {
            throw new OcrException(ErrorCode.RECEIPT_PROCESSING_FAILED,
                    "The receipt scanner failed to run.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OcrException(ErrorCode.RECEIPT_PROCESSING_FAILED,
                    "The receipt scan was interrupted.", exception);
        } finally {
            deleteQuietly(input);
        }
    }

    private String suffixFor(String contentType) {
        if (contentType == null) {
            return ".img";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".img";
        };
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not delete temp receipt file {}", path, exception);
        }
    }
}
