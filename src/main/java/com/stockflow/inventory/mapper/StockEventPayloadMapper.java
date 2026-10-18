package com.stockflow.inventory.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stockflow.inventory.constants.InventoryConstants;
import com.stockflow.inventory.entity.InventoryTransaction;
import com.stockflow.inventory.entity.TransactionType;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Membangun payload JSON event inventory (FSD §5.3).
 *
 * <p>Timestamp selalu ditulis sebagai ISO-8601 eksplisit agar format payload tidak
 * bergantung pada konfigurasi Jackson. Nama tipe event dan tipe agregat berada di
 * {@link InventoryConstants}; nama event per jenis mutasi dimiliki
 * {@link TransactionType#eventType()}.
 */
@ApplicationScoped
public class StockEventPayloadMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;

    public StockEventPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String aggregateType() {
        return InventoryConstants.AGGREGATE_TYPE_INVENTORY;
    }

    public String payload(UUID eventId, UUID aggregateId, TransactionType type, InventoryTransaction transaction) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("eventId", eventId.toString());
        node.put("eventType", type.eventType());
        node.put("eventVersion", InventoryConstants.EVENT_VERSION);
        node.put("occurredAt", transaction.getCreatedAt().format(TIMESTAMP_FORMAT));
        node.put("aggregateId", aggregateId.toString());
        node.put("productId", transaction.getProductId().toString());
        node.put("warehouseId", transaction.getWarehouseId().toString());
        node.put("quantityBefore", transaction.getQuantityBefore());
        node.put("quantityChanged", transaction.getQuantityDelta());
        node.put("quantityAfter", transaction.getQuantityAfter());
        node.put("transactionId", transaction.getId().toString());
        if (transaction.getReferenceId() != null) {
            node.put("referenceId", transaction.getReferenceId());
        }
        return node.toString();
    }
}
