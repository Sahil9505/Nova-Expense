package com.nova.finance.receipt;

import com.nova.common.BaseEntity;
import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.parsing.ReceiptExtractedFieldsConverter;
import com.nova.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single uploaded receipt and the structured data extracted from it.
 *
 * <p>The receipt is stored separately from transactions: uploading a receipt never
 * creates a transaction on its own. The extracted fields (if any) live in
 * {@link #extractedFields} as JSON, deserialised by
 * {@link ReceiptExtractedFieldsConverter}. Before processing they are {@code null};
 * after a successful pass they carry the OCR result with a per-field confidence
 * score. A loose {@link #linkedTransactionId} reference records the transaction
 * produced at finalisation without coupling the two aggregates.</p>
 */
@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
public class Receipt extends BaseEntity {

    /** Lifecycle of a receipt through the capture pipeline. */
    public enum Status {
        /** Uploaded and stored; nothing extracted yet. */
        UPLOADED,
        /** Extraction is running. */
        PROCESSING,
        /** Extraction finished; a draft is available. */
        EXTRACTED,
        /** Extraction failed; the user can still enter details manually. */
        FAILED,
        /** A transaction was created from this receipt. */
        FINALIZED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    /** Opaque key the storage backend uses to retrieve the bytes. */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "status_message", length = 512)
    private String statusMessage;

    /** Which OCR provider produced the text (e.g. "tesseract"). */
    @Column(name = "ocr_provider", length = 64)
    private String ocrProvider;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Convert(converter = ReceiptExtractedFieldsConverter.class)
    @Column(name = "extracted_fields", columnDefinition = "TEXT")
    private ReceiptExtractedFields extractedFields;

    @Column(name = "overall_confidence")
    private Integer overallConfidence;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "extracted_at")
    private OffsetDateTime extractedAt;

    /** Loose reference to the transaction created at finalisation (no FK). */
    @Column(name = "linked_transaction_id")
    private UUID linkedTransactionId;
}
