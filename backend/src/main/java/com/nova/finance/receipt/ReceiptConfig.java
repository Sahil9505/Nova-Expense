package com.nova.finance.receipt;

import com.nova.finance.receipt.ocr.OcrProvider;
import com.nova.finance.receipt.storage.ReceiptStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;
import java.util.Locale;

/**
 * Wires the receipt module: enables {@link ReceiptProperties} and selects the
 * active storage backend and OCR provider from configuration.
 *
 * <p>Selection is by name, so adding a new {@link ReceiptStorage} or
 * {@link OcrProvider} implementation and pointing configuration at it is the only
 * change needed to switch backends — the service layer depends on the interfaces,
 * never the concretes.</p>
 */
@Configuration
@EnableConfigurationProperties(ReceiptProperties.class)
public class ReceiptConfig {

    private static final Logger log = LoggerFactory.getLogger(ReceiptConfig.class);

    /** The storage backend named by {@code nova.receipt.storage.backend}. */
    @Bean
    @Primary
    public ReceiptStorage activeReceiptStorage(List<ReceiptStorage> storages, ReceiptProperties properties) {
        String wanted = properties.storage().backend().toLowerCase(Locale.ROOT);
        return storages.stream()
                .filter(storage -> storage.backend().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("No receipt storage backend named '{}'; falling back to '{}'.",
                            wanted, storages.get(0).backend());
                    return storages.get(0);
                });
    }

    /** The OCR provider named by {@code nova.receipt.ocr.provider}. */
    @Bean
    @Primary
    public OcrProvider activeOcrProvider(List<OcrProvider> providers, ReceiptProperties properties) {
        String wanted = properties.ocr().provider().toLowerCase(Locale.ROOT);
        return providers.stream()
                .filter(provider -> provider.name().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("No OCR provider named '{}'; falling back to '{}'.",
                            wanted, providers.get(0).name());
                    return providers.get(0);
                });
    }
}
