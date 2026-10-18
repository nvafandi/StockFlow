package com.stockflow.inventory.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Histori perubahan stok. Bersifat <b>append-only</b> (FSD §7.4): tidak pernah diubah
 * maupun dihapus; koreksi dilakukan lewat transaksi baru.
 *
 * <p>{@code @Index} di bawah hanya mendokumentasikan indeks milik Flyway V4 —
 * {@code hibernate-orm.schema-management.strategy=validate}, jadi Hibernate tidak membuat
 * indeks apa pun saat runtime.
 */
@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_inventory_transactions_product", columnList = "product_id, created_at"),
        @Index(name = "idx_inventory_transactions_warehouse", columnList = "warehouse_id, created_at"),
        @Index(name = "idx_inventory_transactions_reference", columnList = "reference_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class InventoryTransaction extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "quantity_delta", nullable = false)
    private long quantityDelta;

    @Column(name = "quantity_before", nullable = false)
    private long quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private long quantityAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "idempotency_key", length = 150)
    private String idempotencyKey;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "reason", length = 50)
    private String reason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
