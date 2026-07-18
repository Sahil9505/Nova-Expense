package com.nova.finance.receipt;

import com.nova.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptValidationServiceTest {

    /** A real, decodable 2x2 PNG so the image-decode check passes. */
    private static byte[] validPng() {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private final ReceiptProperties props = new ReceiptProperties(
            null,
            java.util.List.of("image/png", "image/jpeg", "image/jpg", "image/webp"),
            new ReceiptProperties.Storage("local", "./target/test-receipts"),
            new ReceiptProperties.Ocr("tesseract", "tesseract", "eng", 20_000L));

    private final ReceiptValidationService service = new ReceiptValidationService(props);

    @Test
    void acceptsValidPng() {
        MultipartFile file = new MockMultipartFile(
                "file", "shop.png", "image/png", validPng());
        service.validate(file); // no throw
        assertThat(service.resolveContentType(file)).isEqualTo("image/png");
    }

    @Test
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(ReceiptProcessingException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_INVALID_IMAGE);
    }

    @Test
    void rejectsUnsupportedType() {
        ReceiptProperties noTypes = new ReceiptProperties(
                10L * 1024 * 1024,
                java.util.List.of(),
                new ReceiptProperties.Storage("local", "./target/test-receipts"),
                new ReceiptProperties.Ocr("tesseract", "tesseract", "eng", 20_000L));
        ReceiptValidationService strict = new ReceiptValidationService(noTypes);

        MultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", new byte[]{1, 2, 3, 4});
        assertThatThrownBy(() -> strict.validate(file))
                .isInstanceOf(ReceiptProcessingException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_UNSUPPORTED_TYPE);
    }

    @Test
    void rejectsOversizedFile() {
        // max = 1 byte -> any real image is too large.
        ReceiptProperties tiny = new ReceiptProperties(
                1L,
                java.util.List.of("image/png"),
                new ReceiptProperties.Storage("local", "./target/test-receipts"),
                new ReceiptProperties.Ocr("tesseract", "tesseract", "eng", 20_000L));
        ReceiptValidationService strict = new ReceiptValidationService(tiny);

        byte[] pngBytes = validPng();
        MultipartFile file = new MockMultipartFile(
                "file", "shop.png", "image/png", pngBytes);
        assertThatThrownBy(() -> strict.validate(file))
                .isInstanceOf(ReceiptProcessingException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_FILE_TOO_LARGE);
    }

    @Test
    void rejectsCorruptedImage() {
        // A PNG that decodes to a zero-sized image is treated as corrupted.
        byte[] corrupted = new byte[]{
                (byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A,
                (byte) 0x1A, (byte) 0x0A, 0, 0, 0, 0};
        MultipartFile file = new MockMultipartFile(
                "file", "broken.png", "image/png", corrupted);
        assertThatThrownBy(() -> service.validate(file))
                .isInstanceOf(ReceiptProcessingException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECEIPT_INVALID_IMAGE);
    }
}
