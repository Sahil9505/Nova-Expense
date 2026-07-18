package com.nova.finance.receipt;

import com.nova.finance.receipt.ocr.OcrException;
import com.nova.finance.receipt.ocr.OcrProvider;
import com.nova.finance.receipt.ocr.OcrResult;
import com.nova.finance.receipt.parsing.ReceiptConfidenceService;
import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.parsing.ReceiptParsingService;
import com.nova.finance.receipt.storage.ReceiptStorage;
import com.nova.finance.receipt.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Orchestrates the extraction half of the pipeline for a single receipt:
 * <pre>Storage load → OCR → Normalization (parse) → Confidence scoring → persist</pre>
 *
 * <p>Each stage is a separate, replaceable collaborator. This service only sequences
 * them and records the outcome on the {@link Receipt}. Extraction is resilient: an
 * OCR failure (unavailable engine, timeout, corrupt image) is caught and the receipt
 * is marked {@code FAILED} with a message, so the user can still enter the transaction
 * manually. It never invents data — a failed or empty scan yields no draft fields.</p>
 */
@Service
public class ReceiptOcrService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptOcrService.class);

    private final OcrProvider ocrProvider;
    private final ReceiptParsingService parsingService;
    private final ReceiptConfidenceService confidenceService;
    private final ReceiptStorage storage;
    private final ReceiptRepository receiptRepository;

    public ReceiptOcrService(
            OcrProvider ocrProvider,
            ReceiptParsingService parsingService,
            ReceiptConfidenceService confidenceService,
            ReceiptStorage storage,
            ReceiptRepository receiptRepository) {
        this.ocrProvider = ocrProvider;
        this.parsingService = parsingService;
        this.confidenceService = confidenceService;
        this.storage = storage;
        this.receiptRepository = receiptRepository;
    }

    /**
     * Runs extraction for a receipt and persists the result. Safe to call more than
     * once (re-processing overwrites the previous draft). Never throws for an OCR
     * failure — the receipt is marked {@code FAILED} instead.
     */
    @Transactional
    public Receipt process(Receipt receipt) {
        receipt.setStatus(Receipt.Status.PROCESSING);
        receipt.setStatusMessage(null);
        receiptRepository.saveAndFlush(receipt);

        if (!ocrProvider.isAvailable()) {
            log.warn("OCR provider '{}' is unavailable; receipt {} needs manual entry.",
                    ocrProvider.name(), receipt.getId());
            receipt.setStatus(Receipt.Status.FAILED);
            receipt.setStatusMessage("Automatic scanning is unavailable. Enter the details manually.");
            receipt.setOcrProvider(ocrProvider.name());
            return receiptRepository.save(receipt);
        }

        try {
            StoredFile file = storage.load(receipt.getStorageKey(), receipt.getContentType());
            OcrResult ocr = ocrProvider.extractText(file.bytes(), receipt.getContentType());

            ReceiptExtractedFields fields = parsingService.parse(ocr.text());
            confidenceService.populate(fields);

            receipt.setOcrProvider(ocr.provider());
            receipt.setOcrText(trim(ocr.text()));
            receipt.setExtractedFields(fields);
            receipt.setOverallConfidence(fields.getOverallConfidence());
            if (fields.getCurrency() != null) {
                receipt.setCurrency(fields.getCurrency().getValue());
            }
            receipt.setExtractedAt(OffsetDateTime.now());
            receipt.setStatus(Receipt.Status.EXTRACTED);
            receipt.setStatusMessage(null);
            return receiptRepository.save(receipt);
        } catch (OcrException exception) {
            log.info("OCR could not process receipt {}: {}", receipt.getId(), exception.getMessage());
            receipt.setStatus(Receipt.Status.FAILED);
            receipt.setStatusMessage(exception.getMessage());
            receipt.setOcrProvider(ocrProvider.name());
            return receiptRepository.save(receipt);
        } catch (RuntimeException exception) {
            log.error("Unexpected failure processing receipt {}", receipt.getId(), exception);
            receipt.setStatus(Receipt.Status.FAILED);
            receipt.setStatusMessage("We could not process this receipt. Enter the details manually.");
            receipt.setOcrProvider(ocrProvider.name());
            return receiptRepository.save(receipt);
        }
    }

    /** Keeps the stored OCR text bounded so a huge scan can't bloat a row. */
    private String trim(String text) {
        if (text == null) {
            return null;
        }
        int max = 10_000;
        return text.length() > max ? text.substring(0, max) : text;
    }
}
