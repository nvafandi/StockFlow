package com.stockflow.common.exception;

/**
 * Konflik konkurensi: request harus diulang.
 *
 * <p>Dipakai bila balapan {@code Idempotency-Key} terjadi tetapi transaksi pemenang tidak
 * dapat ditemukan, sehingga hasilnya tidak boleh dikarang (FSD §6.1).
 */
public class ConcurrencyConflictException extends BusinessException {

    public ConcurrencyConflictException(String detail) {
        super(ErrorCode.CONCURRENCY_CONFLICT, "Konflik konkurensi, silakan ulangi request: " + detail);
    }
}
