package com.nova.finance.receipt.storage;

import com.nova.finance.receipt.ReceiptProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local-filesystem {@link ReceiptStorage}. Objects are written under a per-user
 * subdirectory of a configurable root, keyed as {@code <userId>/<uuid>.<ext>}.
 *
 * <p>This is the default backend and is fully sufficient for single-node
 * deployments. It reads and writes bytes only; swapping to Cloudinary/S3/MinIO
 * later means adding another {@link ReceiptStorage} bean and pointing
 * {@code nova.receipt.storage.backend} at it.</p>
 */
@Component
public class LocalReceiptStorage implements ReceiptStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalReceiptStorage.class);

    private final Path root;

    public LocalReceiptStorage(ReceiptProperties properties) {
        this.root = Path.of(properties.storage().localDir()).toAbsolutePath().normalize();
    }

    @Override
    public String backend() {
        return "local";
    }

    @Override
    public String store(String userId, byte[] bytes, String contentType, String filename) {
        String safeUser = sanitize(userId);
        String key = safeUser + "/" + UUID.randomUUID() + extensionFor(contentType, filename);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "upload-", ".part");
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return key;
        } catch (IOException exception) {
            throw new ReceiptStorageException("Could not store the receipt file.", exception);
        }
    }

    @Override
    public StoredFile load(String key, String contentType) {
        Path source = resolve(key);
        try {
            if (!Files.exists(source)) {
                throw new ReceiptStorageException("The stored receipt file is missing.");
            }
            return new StoredFile(Files.readAllBytes(source), contentType);
        } catch (IOException exception) {
            throw new ReceiptStorageException("Could not read the stored receipt file.", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            log.warn("Could not delete stored receipt {}", key, exception);
        }
    }

    /** Resolves a key under the root and refuses any path that escapes it. */
    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new ReceiptStorageException("Invalid storage key.");
        }
        return resolved;
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String extensionFor(String contentType, String filename) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                String ext = filename.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
                if (!ext.isEmpty() && ext.length() <= 5) {
                    return "." + ext;
                }
            }
        }
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
