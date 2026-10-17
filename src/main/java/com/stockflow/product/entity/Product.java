package com.stockflow.product.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Master data produk.
 *
 * <p>Entity tidak pernah keluar dari batas modul; API selalu memakai DTO (FSD §2.2, AD-5).
 * Field bersifat privat dengan getter/setter hasil Lombok — Lombok dipakai <b>hanya</b> pada
 * entity, sedangkan DTO tetap {@code record}.
 *
 * <p>{@code id} ditetapkan aplikasi (UUID); {@code @GeneratedValue} belum dipakai
 * (lihat Deferred registry issue F21 pada refactor-plan §4).
 */
@Entity
@Table(name = "products", indexes = @Index(name = "idx_products_name", columnList = "name"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, of = "id")
public class Product extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
