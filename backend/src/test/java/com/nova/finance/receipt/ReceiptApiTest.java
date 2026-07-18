package com.nova.finance.receipt;

import com.fasterxml.jackson.databind.JsonNode;
import com.nova.finance.receipt.ocr.OcrProvider;
import com.nova.finance.receipt.ocr.OcrResult;
import com.nova.finance.receipt.storage.ReceiptStorage;
import com.nova.finance.receipt.web.dto.FinalizeReceiptRequest;
import com.nova.finance.transaction.Transaction.Type;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end exercise of the receipt pipeline through the HTTP surface:
 * upload -> store -> process (OCR -> parse -> score) -> draft -> finalize,
 * plus the recent list and the error paths. OCR is mocked so the run is
 * deterministic and free of the (environment-dependent) native engine.
 */
class ReceiptApiTest extends com.nova.finance.AbstractFinanceApiTest {

    @MockBean
    private OcrProvider ocrProvider;

    @Autowired
    private ReceiptStorage receiptStorage;

    private static final String SAMPLE_OCR =
            "WALMART SUPERCENTER\nDATE 03/14/2026 18:42\n\n" +
            "MILK 2.99\nBREAD 3.49\nEGGS 4.25\n\n" +
            "SUBTOTAL 10.73\nTAX 0.86\nTOTAL $11.59\nVISA\nRECEIPT #WMT-48213\n";

    /** Registers a fresh user and returns the email, so the caller can log in. */
    private String registerAndEmail() throws Exception {
        String email = email();
        register(email, "password123");
        return email;
    }

    private String uploadReceipt(String token, byte[] bytes, String filename, String contentType)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, bytes);
        MvcResult result = mockMvc.perform(multipart("/api/receipts")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }

    private String createExpenseCategory(String token) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "ReceiptsGrocery" + UUID.randomUUID().toString().substring(0, 4));
        body.put("type", "EXPENSE");
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return parse(result).get("id").asText();
    }

    @Test
    void fullPipelineUploadProcessFinalize() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        String accountId = createAccount(token, "Checking", "CHECKING", "USD", 100);
        String categoryId = createExpenseCategory(token);

        when(ocrProvider.isAvailable()).thenReturn(true);
        when(ocrProvider.name()).thenReturn("mock");
        when(ocrProvider.extractText(any(), anyString()))
                .thenReturn(new OcrResult("mock", SAMPLE_OCR));

        byte[] png = minimalPng();
        String receiptId = uploadReceipt(token, png, "shop.png", "image/png");

        mockMvc.perform(get("/api/receipts/recent").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/receipts/" + receiptId + "/process")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult draft = mockMvc.perform(get("/api/receipts/" + receiptId + "/draft")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode suggestion = parse(draft).get("suggestion");
        assertThat(suggestion.get("amount").asText()).isEqualTo("11.59");
        assertThat(suggestion.get("merchant").asText()).isEqualTo("WALMART SUPERCENTER");
        assertThat(suggestion.get("currency").asText()).isEqualTo("USD");

        MvcResult before = mockMvc.perform(get("/api/transactions")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(parse(before).size()).isZero();

        FinalizeReceiptRequest finalize = new FinalizeReceiptRequest(
                UUID.fromString(accountId),
                UUID.fromString(categoryId),
                Type.EXPENSE,
                new BigDecimal("11.59"),
                "WALMART SUPERCENTER",
                "RECEIPT #WMT-48213",
                "USD",
                OffsetDateTime.parse("2026-03-14T18:42:00Z"));
        mockMvc.perform(post("/api/receipts/" + receiptId + "/finalize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(finalize)))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/api/transactions")
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode transactions = parse(after);
        assertThat(transactions.size()).isOne();
        assertThat(transactions.get(0).get("amount").asText()).isEqualTo("11.59");
        assertThat(transactions.get(0).get("merchant").asText()).isEqualTo("WALMART SUPERCENTER");

        MvcResult detail = mockMvc.perform(get("/api/receipts/" + receiptId)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode receipt = parse(detail);
        assertThat(receipt.get("status").asText()).isEqualTo("FINALIZED");
        assertThat(receipt.get("linkedTransactionId")).isNotNull();
    }

    @Test
    void servesImageBytes() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        when(ocrProvider.isAvailable()).thenReturn(true);
        when(ocrProvider.name()).thenReturn("mock");
        when(ocrProvider.extractText(any(), anyString()))
                .thenReturn(new OcrResult("mock", SAMPLE_OCR));

        String receiptId = uploadReceipt(token, minimalPng(), "shop.png", "image/png");

        mockMvc.perform(get("/api/receipts/" + receiptId + "/image")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void failsGracefullyWhenOcrUnavailable() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        when(ocrProvider.isAvailable()).thenReturn(false);

        String receiptId = uploadReceipt(token, minimalPng(), "shop.png", "image/png");
        MvcResult result = mockMvc.perform(post("/api/receipts/" + receiptId + "/process")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(result).get("status").asText()).isEqualTo("FAILED");
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "not an image".getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/receipts").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("RECEIPT_UNSUPPORTED_TYPE");
    }

    @Test
    void rejectsOversizedUpload() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        byte[] big = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        mockMvc.perform(multipart("/api/receipts").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cannotReadOtherUsersReceipt() throws Exception {
        String owner = accessToken(registerAndEmail(), "password123");
        String stranger = accessToken(registerAndEmail(), "password123");
        String receiptId = uploadReceipt(owner, minimalPng(), "shop.png", "image/png");

        mockMvc.perform(get("/api/receipts/" + receiptId)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalizeRejectsMissingAccount() throws Exception {
        String token = accessToken(registerAndEmail(), "password123");
        when(ocrProvider.isAvailable()).thenReturn(true);
        when(ocrProvider.name()).thenReturn("mock");
        when(ocrProvider.extractText(any(), anyString()))
                .thenReturn(new OcrResult("mock", SAMPLE_OCR));

        String receiptId = uploadReceipt(token, minimalPng(), "shop.png", "image/png");
        mockMvc.perform(post("/api/receipts/" + receiptId + "/process")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        FinalizeReceiptRequest finalize = new FinalizeReceiptRequest(
                null, null, Type.EXPENSE, new BigDecimal("11.59"),
                "X", null, "USD", OffsetDateTime.now());
        mockMvc.perform(post("/api/receipts/" + receiptId + "/finalize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(finalize)))
                .andExpect(status().is4xxClientError());
    }

    /** A real 1x1 PNG so the validator's image-decode check passes. */
    private byte[] minimalPng() {
        int[] bytes = {
                0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0x39, 0x70,
                0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
                0x0D, 0x0A, 0x2D, 0xBB, 0x00, 0x00, 0x00, 0x00,
                0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82
        };
        byte[] png = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            png[i] = (byte) bytes[i];
        }
        return png;
    }
}
