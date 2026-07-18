package com.nova.finance.receipt;

import com.nova.common.exception.ResourceNotFoundException;
import com.nova.finance.receipt.parsing.ReceiptExtractedFields;
import com.nova.finance.receipt.storage.ReceiptStorage;
import com.nova.finance.receipt.storage.StoredFile;
import com.nova.finance.receipt.web.dto.FinalizeReceiptRequest;
import com.nova.finance.receipt.web.dto.ReceiptDraftResponse;
import com.nova.finance.receipt.web.dto.ReceiptResponse;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import com.nova.finance.transaction.Transaction;
import com.nova.finance.transaction.TransactionMapper;
import com.nova.finance.transaction.TransactionService;
import com.nova.finance.transaction.Transaction.Type;
import com.nova.finance.transaction.web.dto.CreateTransactionRequest;
import com.nova.finance.transaction.web.dto.TransactionResponse;
import com.nova.user.User;
import com.nova.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The public face of the receipt domain. Sequences the pipeline stages and
 * owns the user-scoped read/write rules:
 *
 * <pre>upload → validate → store (no transaction yet)
 * process → OCR → parse → score → persist draft
 * finalize → create a transaction from the (edited) draft</pre>
 *
 * <p>Uploading a receipt never creates a transaction; only {@link #finalize}
 * does, and it reuses the existing {@link TransactionService} so balances and
 * ownership checks stay exactly as they are everywhere else. A loose
 * {@code linkedTransactionId} records the result.</p>
 */
@Service
public class ReceiptService {

    private static final int MAX_RECENT = 8;

    private final ReceiptRepository receiptRepository;
    private final ReceiptStorage storage;
    private final ReceiptValidationService validationService;
    private final ReceiptOcrService ocrService;
    private final ReceiptMapper receiptMapper;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            ReceiptStorage storage,
            ReceiptValidationService validationService,
            ReceiptOcrService ocrService,
            ReceiptMapper receiptMapper,
            UserRepository userRepository,
            TransactionService transactionService,
            TransactionMapper transactionMapper) {
        this.receiptRepository = receiptRepository;
        this.storage = storage;
        this.validationService = validationService;
        this.ocrService = ocrService;
        this.receiptMapper = receiptMapper;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    /** Uploads and stores a receipt. Returns the stored receipt (status {@code UPLOADED}). */
    @Transactional
    public ReceiptResponse upload(UUID userId, MultipartFile file) {
        validationService.validate(file);
        User user = loadUser(userId);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new ReceiptProcessingException(
                    com.nova.common.exception.ErrorCode.RECEIPT_STORAGE_FAILED,
                    "Could not read the uploaded file.",
                    exception);
        }

        String contentType = validationService.resolveContentType(file);
        String key = storage.store(userId.toString(), bytes, contentType, file.getOriginalFilename());

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setFilename(file.getOriginalFilename());
        receipt.setContentType(contentType);
        receipt.setFileSizeBytes(file.getSize());
        receipt.setStorageKey(key);
        receipt.setStatus(Receipt.Status.UPLOADED);
        return receiptMapper.toResponse(receiptRepository.save(receipt));
    }

    /** Recent uploads for the dashboard widget. */
    @Transactional(readOnly = true)
    public List<ReceiptSummaryResponse> recent(UUID userId, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_RECENT));
        return receiptRepository.findTop8ByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(capped)
                .map(receiptMapper::toSummary)
                .toList();
    }

    /** Loads a receipt with its extracted draft (or {@code null} if not yet processed). */
    @Transactional(readOnly = true)
    public ReceiptResponse get(UUID userId, UUID id) {
        return receiptMapper.toResponse(loadOwned(id, userId));
    }

    /** Reads the stored image bytes for preview. */
    @Transactional(readOnly = true)
    public StoredFile image(UUID userId, UUID id) {
        Receipt receipt = loadOwned(id, userId);
        return storage.load(receipt.getStorageKey(), receipt.getContentType());
    }

    /**
     * Runs extraction for a receipt and returns it with a draft. Safe to call
     * again: a failed pass leaves {@code status=FAILED} and a message so the
     * user can still enter the transaction manually.
     */
    @Transactional
    public ReceiptResponse process(UUID userId, UUID id) {
        Receipt receipt = loadOwned(id, userId);
        Receipt processed = ocrService.process(receipt);
        return receiptMapper.toResponse(processed);
    }

    /** Builds the editable draft for the review screen. */
    @Transactional(readOnly = true)
    public ReceiptDraftResponse draft(UUID userId, UUID id) {
        Receipt receipt = loadOwned(id, userId);
        return new ReceiptDraftResponse(receiptMapper.toResponse(receipt), suggestion(receipt));
    }

    /**
     * Creates a transaction from the user's final, editable decision and links it
     * to the receipt. Never runs automatically — only on explicit user confirm.
     */
    @Transactional
    public TransactionResponse finalize(UUID userId, UUID id, FinalizeReceiptRequest request) {
        Receipt receipt = loadOwned(id, userId);
        if (receipt.getStatus() == Receipt.Status.FINALIZED) {
            throw new ReceiptProcessingException(
                    com.nova.common.exception.ErrorCode.BAD_REQUEST,
                    "This receipt has already been saved.");
        }

        CreateTransactionRequest create = new CreateTransactionRequest(
                request.amount(),
                request.type(),
                request.accountId(),
                null,
                request.categoryId(),
                blankToNull(request.merchant()),
                blankToNull(request.note()),
                normalizeCurrency(request.currency()),
                null,
                request.occurredAt());

        TransactionResponse transaction = transactionService.create(userId, create);

        receipt.setLinkedTransactionId(transaction.id());
        receipt.setStatus(Receipt.Status.FINALIZED);
        receipt.setStatusMessage(null);
        receiptRepository.save(receipt);

        return transaction;
    }

    // -----------------------------------------------------------------------
    // Draft suggestion
    // -----------------------------------------------------------------------

    private CreateTransactionRequest suggestion(Receipt receipt) {
        ReceiptExtractedFields fields = receipt.getExtractedFields();
        if (fields == null) {
            return new CreateTransactionRequest(
                    null, Type.EXPENSE, null, null, null, null, null, null, null, OffsetDateTime.now());
        }
        BigDecimal amount = valueOf(fields.getTotal() != null ? fields.getTotal() : fields.getSubtotal());
        String currency = fields.getCurrency() != null ? fields.getCurrency().getValue() : null;
        OffsetDateTime occurredAt = fields.getDate() != null && fields.getDate().getValue() != null
                ? parseDate(fields.getDate().getValue())
                : OffsetDateTime.now();
        String merchant = fields.getMerchant() != null ? fields.getMerchant().getValue() : null;
        return new CreateTransactionRequest(
                amount,
                Type.EXPENSE,
                null,
                null,
                null,
                blankToNull(merchant),
                null,
                normalizeCurrency(currency),
                null,
                occurredAt);
    }

    private OffsetDateTime parseDate(String isoDate) {
        try {
            return java.time.LocalDate.parse(isoDate).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        } catch (RuntimeException exception) {
            return OffsetDateTime.now();
        }
    }

    // -----------------------------------------------------------------------
    // Ownership + helpers
    // -----------------------------------------------------------------------

    private Receipt loadOwned(UUID id, UUID userId) {
        return receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", id.toString()));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private BigDecimal valueOf(com.nova.finance.receipt.parsing.ReceiptField<BigDecimal> field) {
        return field == null ? null : field.getValue();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            return null;
        }
        String normalized = currency.trim().toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
