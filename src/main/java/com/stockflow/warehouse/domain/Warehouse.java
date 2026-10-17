package com.stockflow.warehouse.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "warehouses")
public class Warehouse extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "code", nullable = false, length = 50)
    public String code;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "location", columnDefinition = "text")
    public String location;

    @Column(name = "active", nullable = false)
    public boolean active;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;
}
