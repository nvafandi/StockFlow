package com.stockflow.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockflow.inventory.repository.InventoryRepository;
import com.stockflow.inventory.repository.InventoryTransactionRepository;
import com.stockflow.outbox.repository.OutboxEventRepository;
import com.stockflow.support.MasterData;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pengujian kontrak API mutasi stok (FS-07 / FS-08) beserta efek sampingnya pada
 * histori transaksi dan tabel outbox.
 */
@QuarkusTest
class StockMutationApiTest {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @jakarta.inject.Inject
    InventoryRepository inventoryRepository;

    @jakarta.inject.Inject
    InventoryTransactionRepository inventoryTransactionRepository;

    @jakarta.inject.Inject
    OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("stock-in membuat stok, satu histori, dan satu event outbox dalam satu transaksi")
    void stockInMencatatStokHistoriDanOutbox() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();
        String idempotencyKey = UUID.randomUUID().toString();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body("""
                        {
                          "productId": "%s",
                          "warehouseId": "%s",
                          "quantity": 100,
                          "referenceId": "PO-2026-0001",
                          "notes": "Penerimaan barang dari supplier"
                        }
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(201)
                .body("success", is(true))
                .body("data.quantityBefore", is(0))
                .body("data.quantityChanged", is(100))
                .body("data.quantityAfter", is(100));

        var inventory = inventoryRepository.findByProductAndWarehouse(productId, warehouseId).orElseThrow();
        assertEquals(100L, inventory.getQuantity());

        var transaction = inventoryTransactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertEquals(0L, transaction.getQuantityBefore());
        assertEquals(100L, transaction.getQuantityAfter());
        assertEquals("IN", transaction.getTransactionType().name());

        var event = outboxEventRepository.find("aggregateId", inventory.getId()).firstResult();
        assertEquals("StockInCompleted", event.getEventType());

        var payload = readTree(event.getPayload());
        assertEquals("StockInCompleted", payload.get("eventType").asText());
        assertEquals(inventory.getId().toString(), payload.get("aggregateId").asText());
        assertEquals(0, payload.get("quantityBefore").asInt());
        assertEquals(100, payload.get("quantityChanged").asInt());
        assertEquals(100, payload.get("quantityAfter").asInt());
        assertEquals(transaction.getId().toString(), payload.get("transactionId").asText());

        assertEquals(1, inventoryRepository.count("id", inventory.getId()));
        assertEquals(1, inventoryTransactionRepository.count("id", transaction.getId()));
    }

    @Test
    @DisplayName("Idempotency-Key yang sama dengan payload sama mengembalikan hasil lama tanpa mutasi baru")
    void idempotencyKeySamaMengembalikanHasilLama() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();
        String idempotencyKey = UUID.randomUUID().toString();
        String body = """
                {
                  "productId": "%s",
                  "warehouseId": "%s",
                  "quantity": 25
                }
                """.formatted(productId, warehouseId);

        String firstTransactionId = given()
                .contentType(JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when().post("/api/v1/inventory/stock-in")
                .then().statusCode(201)
                .extract().jsonPath().getString("data.transactionId");

        String secondTransactionId = given()
                .contentType(JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(200)
                .body("data.quantityAfter", is(25))
                .extract().jsonPath().getString("data.transactionId");

        assertEquals(firstTransactionId, secondTransactionId);
        assertEquals(25L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().getQuantity());
        assertEquals(1L, inventoryTransactionRepository.count("idempotencyKey", idempotencyKey));
    }

    @Test
    @DisplayName("Idempotency-Key yang sama dengan payload berbeda ditolak 409 IDEMPOTENCY_KEY_REUSED")
    void idempotencyKeySamaPayloadBerbedaDitolak() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();
        String idempotencyKey = UUID.randomUUID().toString();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 10}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then().statusCode(201);

        given()
                .contentType(JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 999}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(409)
                .body("error.code", is("IDEMPOTENCY_KEY_REUSED"));

        assertEquals(10L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().getQuantity());
    }

    @Test
    @DisplayName("stock-out melebihi stok ditolak 409 dan tidak meninggalkan jejak apapun")
    void stockOutMelebihiStokDitolakTanpaJejak() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 10}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then().statusCode(201);

        long transactionsBefore = inventoryTransactionRepository.count();
        long outboxBefore = outboxEventRepository.count();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 11}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-out")
                .then()
                .statusCode(409)
                .body("error.code", is("INSUFFICIENT_STOCK"));

        assertEquals(10L, inventoryRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElseThrow().getQuantity());
        assertEquals(transactionsBefore, inventoryTransactionRepository.count());
        assertEquals(outboxBefore, outboxEventRepository.count());
    }

    @Test
    @DisplayName("stock-out dari kombinasi yang belum punya stok ditolak 404 INVENTORY_NOT_FOUND")
    void stockOutTanpaBarisInventoryDitolak() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 1}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-out")
                .then()
                .statusCode(404)
                .body("error.code", is("INVENTORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("Idempotency-Key wajib pada mutasi stok")
    void idempotencyKeyWajib() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        given()
                .contentType(JSON)
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 5}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(400)
                .body("error.code", is("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("produk tidak dikenal ditolak 404 PRODUCT_NOT_FOUND")
    void produkTidakDikenalDitolak() {
        UUID warehouseId = MasterData.createWarehouse();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 5}
                        """.formatted(UUID.randomUUID(), warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(404)
                .body("error.code", is("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("quantity nol ditolak 400 oleh Bean Validation")
    void quantityNolDitolak() {
        UUID productId = MasterData.createProduct();
        UUID warehouseId = MasterData.createWarehouse();

        given()
                .contentType(JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"productId": "%s", "warehouseId": "%s", "quantity": 0}
                        """.formatted(productId, warehouseId))
                .when().post("/api/v1/inventory/stock-in")
                .then()
                .statusCode(400)
                .body("error.code", is("VALIDATION_ERROR"));
    }

    private JsonNode readTree(String json) {
        try {
            return JSON_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new AssertionError("payload outbox bukan JSON yang valid: " + json, e);
        }
    }
}
