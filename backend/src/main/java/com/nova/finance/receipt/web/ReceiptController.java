package com.nova.finance.receipt.web;

import com.nova.auth.security.NovaUserPrincipal;
import com.nova.common.api.ApiResponse;
import com.nova.finance.receipt.ReceiptService;
import com.nova.finance.receipt.web.dto.FinalizeReceiptRequest;
import com.nova.finance.receipt.web.dto.ReceiptDraftResponse;
import com.nova.finance.receipt.web.dto.ReceiptResponse;
import com.nova.finance.receipt.web.dto.ReceiptSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST surface for Smart Receipt Capture. Every endpoint follows the standard
 * {@link ApiResponse} envelope, except the image endpoint which streams bytes.
 *
 * <p>Endpoints: upload, recent uploads, receipt detail, raw image, OCR
 * processing, the editable draft, and finalize. Finalize is the only path
 * that creates a transaction, and it reuses the transaction API.</p>
 */
@RestController
@RequestMapping("/api/receipts")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReceiptResponse> upload(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Receipt uploaded.", receiptService.upload(principal.getUserId(), file));
    }

    @GetMapping("/recent")
    public ApiResponse<List<ReceiptSummaryResponse>> recent(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.ok(receiptService.recent(principal.getUserId(), limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReceiptResponse> get(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(receiptService.get(principal.getUserId(), id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        com.nova.finance.receipt.storage.StoredFile file =
                receiptService.image(principal.getUserId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.contentType() != null ? file.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(file.bytes());
    }

    @PostMapping("/{id}/process")
    public ApiResponse<ReceiptResponse> process(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok("Receipt processed.", receiptService.process(principal.getUserId(), id));
    }

    @GetMapping("/{id}/draft")
    public ApiResponse<ReceiptDraftResponse> draft(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(receiptService.draft(principal.getUserId(), id));
    }

    @PostMapping("/{id}/finalize")
    public ApiResponse<com.nova.finance.transaction.web.dto.TransactionResponse> finalize(
            @AuthenticationPrincipal NovaUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody FinalizeReceiptRequest request) {
        return ApiResponse.ok(
                "Transaction created.",
                receiptService.finalize(principal.getUserId(), id, request));
    }
}
