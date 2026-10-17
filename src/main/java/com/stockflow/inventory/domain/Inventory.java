package com.stockflow.inventory.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Jumlah stok per kombinasi produk dan gudang.
 *
 * <p>{@code id} dipakai sebagai {@code aggregateId} pada event outbox (FSD §7.3).
 */
@Entity
@Table(name = "inventory")
public class Inventory extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "product_id", nullable = false)
    public UUID productId;

    @Column(name = "warehouse_id", nullable = false)
    public UUID warehouseId;

    @Column(name = "quantity", nullable = false)
    public long quantity;

    @Column(name = "version", nullable = false)
    public long version;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;
}
