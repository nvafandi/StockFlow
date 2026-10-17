package com.stockflow.inventory.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockflow.inventory.domain.InventoryTransaction;
import com.stockflow.inventory.domain.TransactionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Membangun payload JSON event inventory (FSD §5.3).
 *
 * <p>Timestamp selalu ditulis sebagai ISO-8601 eksplisit agar format payload tidak
 * bergantung pada konfigurasi Jackson.
 */
@ApplicationScoped
public class StockEventPayloadFactory {

    private static final int EVENT_VERSION = 1;

    private final ObjectMapper objectMapper;

    @Inject
    public StockEventPayloadFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String eventType(TransactionType type) {
        return switch (type) {
            case IN -> "StockInCompleted";
            case OUT -> "StockOutCompleted";
            case ADJUSTMENT -> "StockAdjusted";
            case TRANSFER_IN, TRANSFER_OUT -> "StockTransferred";
        };
    }

    public String aggregateType() {
        return "Inventory";
    }

    public String payload(UUID eventId, UUID aggregateId, TransactionType type, InventoryTransaction transaction) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("eventId", eventId.toString());
        node.put("eventType", eventType(type));
        node.put("eventVersion", EVENT_VERSION);
        node.put("occurredAt", transaction.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("aggregateId", aggregateId.toString());
        node.put("productId", transaction.productId.toString());
        node.put("warehouseId", transaction.warehouseId.toString());
        node.put("quantityBefore", transaction.quantityBefore);
        node.put("quantityChanged", transaction.quantityDelta);
        node.put("quantityAfter", transaction.quantityAfter);
        node.put("transactionId", transaction.id.toString());
        if (transaction.referenceId != null) {
            node.put("referenceId", transaction.referenceId);
        }
        return node.toString();
    }
}
