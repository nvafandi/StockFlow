package com.stockflow.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit test tanpa database untuk perhitungan {@code payload_hash} (FSD §6.2).
 */
class PayloadHasherTest {

    @Test
    @DisplayName("hash payload stabil untuk payload identik")
    void hashStabil() {
        assertEquals(PayloadHasher.sha256("a|b|1||"), PayloadHasher.sha256("a|b|1||"));
        assertEquals(64, PayloadHasher.sha256("a|b|1||").length());
    }

    @Test
    @DisplayName("hash payload berbeda ketika isi request berbeda")
    void hashBerbedaUntukPayloadBerbeda() {
        assertNotEquals(PayloadHasher.sha256("a|b|1||"), PayloadHasher.sha256("a|b|2||"));
    }

    @Test
    @DisplayName("command menghitung hash yang sama untuk request yang sama")
    void commandHashKonsisten() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();

        String first = StockMutationCommand.of(productId, warehouseId, 100, "PO-1", "key-1", null).payloadHash();
        String second = StockMutationCommand.of(productId, warehouseId, 100, "PO-1", "key-2", null).payloadHash();
        String differentQuantity = StockMutationCommand.of(productId, warehouseId, 101, "PO-1", "key-1", null)
                .payloadHash();

        assertEquals(first, second, "Idempotency-Key tidak boleh mempengaruhi hash payload");
        assertNotEquals(first, differentQuantity);
    }
}
