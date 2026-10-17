package com.stockflow.common.constants;

/**
 * Nilai konstan lintas domain.
 *
 * <p>Dipakai sebagai nilai anotasi (mis. {@code @DefaultValue}, {@code @Max}) sehingga batas
 * paging tidak ditulis ulang sebagai literal di setiap resource.
 */
public final class CommonConstants {

    /** Halaman pertama yang dipakai bila parameter {@code page} tidak dikirim. */
    public static final String DEFAULT_PAGE = "0";

    /** Ukuran halaman default bila parameter {@code size} tidak dikirim. */
    public static final String DEFAULT_PAGE_SIZE = "20";

    /** Batas bawah parameter {@code page}. */
    public static final int MIN_PAGE = 0;

    /** Batas bawah parameter {@code size}. */
    public static final int MIN_PAGE_SIZE = 1;

    /** Batas atas parameter {@code size} — melindungi database dari query terlalu besar. */
    public static final int MAX_PAGE_SIZE = 100;

    private CommonConstants() {
    }
}
