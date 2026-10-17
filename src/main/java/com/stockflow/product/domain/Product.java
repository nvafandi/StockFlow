package com.stockflow.product.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Master data produk.
 *
 * <p>Entity tidak pernah keluar dari batas modul; API selalu memakai DTO (FSD §2.2, AD-5).
 */
@Entity
@Table(name = "products")
public class Product extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "sku", nullable = false, length = 100)
    public String sku;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "description", columnDefinition = "text")
    public String description;

    @Column(name = "unit", nullable = false, length = 30)
    public String unit;

    @Column(name = "active", nullable = false)
    public boolean active;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;
}
