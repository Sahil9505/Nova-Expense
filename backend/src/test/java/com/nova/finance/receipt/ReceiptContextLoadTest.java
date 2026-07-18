package com.nova.finance.receipt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test: the full context (including the receipt module) must load. */
@SpringBootTest
class ReceiptContextLoadTest {

    @Test
    void contextLoads() {
        // Success == Spring wired every receipt bean (properties, storage, OCR, services).
    }
}
