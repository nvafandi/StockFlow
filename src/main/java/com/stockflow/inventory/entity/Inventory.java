package com.stockflow.inventory.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Jumlah stok per kombinasi produk dan gudang.
 *
 * <p>{@code id} dipakai sebagai {@code aggregateId} pada event outbox (FSD §7.3).
 *
 * <p>Mutasi baris ini <b>tidak</b> lewat entity, melainkan lewat statement SQL atomik pada
 * {@code InventoryRepository} (upsert / conditional update) agar aman terhadap request
 * bersamaan; kolom {@code version} dinaikkan oleh statement yang sama. Optimistic locking
 * berbasis {@code @Version} belum diaktifkan — lihat Deferred registry issue F22 pada
 * refactor-plan §4.
 */
@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Inventory extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
