package com.stockflow.inventory.constants;

/**
 * Nilai konstan modul inventory (tidak boleh ada literal tersebar di kode).
 */
public final class InventoryConstants {

    /** Nama header wajib pada endpoint mutasi stok (FSD §6.2). */
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** Panjang maksimum {@code Idempotency-Key} — sesuai kolom {@code idempotency_key VARCHAR(150)}. */
    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 150;

    /** Tipe agregat pada event outbox (FSD §5.3). */
    public static final String AGGREGATE_TYPE_INVENTORY = "Inventory";

    /** Versi skema payload event (FSD §5.3). */
    public static final int EVENT_VERSION = 1;

    /** Tipe event outbox untuk mutasi stok masuk. */
    public static final String EVENT_STOCK_IN_COMPLETED = "StockInCompleted";

    /** Tipe event outbox untuk mutasi stok keluar. */
    public static final String EVENT_STOCK_OUT_COMPLETED = "StockOutCompleted";

    /** Tipe event outbox untuk penyesuaian stok. */
    public static final String EVENT_STOCK_ADJUSTED = "StockAdjusted";

    /** Tipe event outbox untuk transfer stok antar gudang. */
    public static final String EVENT_STOCK_TRANSFERRED = "StockTransferred";

    private InventoryConstants() {
    }
}
