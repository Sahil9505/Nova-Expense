package com.nova.finance.receipt;

import com.nova.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Validates an uploaded receipt before it is stored or processed.
 *
 * <p>This is the "validation" stage of the pipeline, kept isolated so the rules
 * (type, size, decodability) are enforced in one place and reused by any future
 * intake path. Failures raise a {@link ReceiptProcessingException} with a specific
 * {@link ErrorCode} so the client gets an actionable, graceful error rather than a
 * generic 500.</p>
 */
@Service
public class ReceiptValidationService {

    private final long maxFileSizeBytes;
    private final List<String> allowedContentTypes;

    public ReceiptValidationService(ReceiptProperties properties) {
        this.maxFileSizeBytes = properties.maxFileSizeBytes();
        this.allowedContentTypes = properties.allowedContentTypes().stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .toList();
    }

    /** Validates presence, size, MIME type, and that the bytes decode as an image. */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ReceiptProcessingException(ErrorCode.RECEIPT_INVALID_IMAGE,
                    "No file was uploaded.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ReceiptProcessingException(ErrorCode.RECEIPT_FILE_TOO_LARGE,
                    "The file is larger than the " + (maxFileSizeBytes / (1024 * 1024)) + " MB limit.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new ReceiptProcessingException(ErrorCode.RECEIPT_UNSUPPORTED_TYPE,
                    "Only PNG, JPEG, JPG, and WEBP receipts are supported.");
        }

        assertDecodable(file, contentType);
    }

    /** Returns the normalized (lower-cased, base) MIME type for a validated file. */
    public String resolveContentType(MultipartFile file) {
        return normalizeContentType(file.getContentType());
    }

    private void assertDecodable(MultipartFile file, String contentType) {
        // WEBP has no ImageIO reader in the base JDK; skip the decode check for it
        // (type + size still guard the upload). Other types must decode to a raster.
        if ("image/webp".equals(contentType)) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ReceiptProcessingException(ErrorCode.RECEIPT_INVALID_IMAGE,
                        "The image is corrupted or unreadable.");
            }
        } catch (IOException exception) {
            throw new ReceiptProcessingException(ErrorCode.RECEIPT_INVALID_IMAGE,
                    "The image is corrupted or unreadable.", exception);
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }
}
