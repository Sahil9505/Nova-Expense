package com.nova.finance.receipt;

import com.nova.finance.receipt.storage.LocalReceiptStorage;
import com.nova.finance.receipt.storage.ReceiptStorageException;
import com.nova.finance.receipt.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceiptStorageTest {

    @TempDir
    Path tempDir;

    private LocalReceiptStorage storage() {
        ReceiptProperties props = new ReceiptProperties(
                null,
                java.util.List.of("image/png", "image/jpeg"),
                new ReceiptProperties.Storage("local", tempDir.resolve("data").toString()),
                new ReceiptProperties.Ocr("tesseract", "tesseract", "eng", 20_000L));
        return new LocalReceiptStorage(props);
    }

    @Test
    void storesLoadsAndDeletes() {
        LocalReceiptStorage storage = storage();
        byte[] bytes = "image-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String key = storage.store("user-1", bytes, "image/png", "shop.png");
        assertThat(key).startsWith("user-1/");
        assertThat(key).endsWith(".png");

        StoredFile loaded = storage.load(key, "image/png");
        assertThat(loaded.bytes()).isEqualTo(bytes);
        assertThat(loaded.contentType()).isEqualTo("image/png");

        storage.delete(key);
        assertThatThrownBy(() -> storage.load(key, "image/png"))
                .isInstanceOf(ReceiptStorageException.class);
    }

    @Test
    void rejectsPathTraversalKey() {
        LocalReceiptStorage storage = storage();
        assertThatThrownBy(() -> storage.load("../escape.png", "image/png"))
                .isInstanceOf(ReceiptStorageException.class);
    }

    @Test
    void derivesExtensionFromFilename() {
        LocalReceiptStorage storage = storage();
        String key = storage.store("u", "x".getBytes(), "image/jpeg", "photo.JPG");
        assertThat(key).endsWith(".jpg");
    }
}
