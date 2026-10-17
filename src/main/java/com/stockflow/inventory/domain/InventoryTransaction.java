package com.stockflow.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Histori perubahan stok. Bersifat <b>append-only</b> (FSD §7.4): tidak pernah diubah
 * maupun dihapus; koreksi dilakukan lewat transaksi baru.
 */
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @Column(name = "warehouse_id", nullable = false)
    public UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    public TransactionType transactionType;

    @Column(name = "quantity_delta", nullable = false)
    public long quantityDelta;

    @Column(name = "quantity_before", nullable = false)
    public long quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    public long quantityAfter;

    @Column(name = "reference_id", length = 100)
    public String referenceId;

    @Column(name = "idempotency_key", length = 150)
    public String idempotencyKey;

    @Column(name = "payload_hash", length = 64)
    public String payloadHash;

    @Column(name = "reason", length = 50)
    public String reason;

    @Column(name = "notes", columnDefinition = "text")
    public String notes;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
