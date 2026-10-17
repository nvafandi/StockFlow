package com.stockflow.inventory.constants;

/**
 * Literal query modul inventory.
 *
 * <p>Semua SQL/JPQL dikumpulkan di sini agar tidak ada string query yang tersebar di
 * repository (standar repository &amp; constants).
 */
public final class InventoryQueryConstants {

    /**
     * Stock in dalam satu statement: upsert atomik + {@code RETURNING}.
     *
     * <p>{@code ON CONFLICT} menjadikan pemeriksaan "sudah ada?" dan penulisan sebagai satu
     * operasi atomik, sehingga dua request bersamaan tidak dapat saling menimpa
     * (read-modify-write tanpa lock).
     */
    public static final String INCREASE_SQL = """
            INSERT INTO inventory (id, product_id, warehouse_id, quantity, version, updated_at)
            VALUES (:id, :productId, :warehouseId, :quantity, 1, now())
            ON CONFLICT (product_id, warehouse_id)
            DO UPDATE SET quantity   = inventory.quantity + EXCLUDED.quantity,
                          version    = inventory.version + 1,
                          updated_at = now()
            RETURNING id, quantity
            """;

    /**
     * Stock out dalam satu statement: {@code UPDATE} bersyarat + {@code RETURNING}.
     *
     * <p>0 baris berarti stok tidak cukup atau baris inventory belum ada — dibedakan
     * pemanggil lewat {@code existsFor(...)}. Predikat {@code quantity >= :quantity}
     * membuat check-then-act tidak diperlukan.
     */
    public static final String DECREASE_SQL = """
            UPDATE inventory
               SET quantity   = quantity - :quantity,
                   version    = version + 1,
                   updated_at = now()
             WHERE product_id = :productId
               AND warehouse_id = :warehouseId
               AND quantity >= :quantity
            RETURNING id, quantity
            """;

    /** Kondisi JPQL inventory per produk + gudang. */
    public static final String INVENTORY_BY_PRODUCT_AND_WAREHOUSE = "productId = ?1 and warehouseId = ?2";

    /** Kondisi JPQL histori per produk + gudang. */
    public static final String TRANSACTION_BY_PRODUCT = "productId = :productId";

    /** Kondisi JPQL histori per gudang. */
    public static final String TRANSACTION_BY_WAREHOUSE = "warehouseId = :warehouseId";

    /** Kondisi JPQL netral (dipakai bila filter histori kosong). */
    public static final String NO_FILTER = "1 = 1";

    /** Sintaks penggabungan kondisi JPQL. */
    public static final String CONDITION_SEPARATOR = " and ";

    /** Urutan histori: terbaru lebih dulu (FSD §7.4). */
    public static final String ORDER_BY_CREATED_AT_DESC = " order by createdAt desc";

    private InventoryQueryConstants() {
    }
}
