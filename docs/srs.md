# StockFlow — Software Requirements Specification (SRS)

> Turunan resmi dari `docs/design-system.md`, disusun mengikuti struktur umum
> IEEE 830. Dokumen ini menetapkan **apa** yang harus dibangun; **bagaimana**
> rinciannya ada pada `fsd.md`, sedangkan keputusan arsitekturnya pada
> `architecture-plan.md`.

| Item | Keterangan |
| --- | --- |
| Nama Sistem | StockFlow |
| Artifact | `com.stockflow:stockflow-service` |
| Versi Dokumen | Draft 1.0 |
| Basis | `docs/design-system.md` |
| Dokumen Terkait | `architecture-plan.md`, `fsd.md` |

---

## 1. Pendahuluan

### 1.1 Tujuan Dokumen

Menetapkan kebutuhan fungsional dan non-fungsional StockFlow sebagai acuan
pengembangan, pengujian, dan penerimaan rilis awal.

### 1.2 Ruang Lingkup Produk

StockFlow adalah backend inventory management berbasis REST API dan event-driven
integration. Sistem menyediakan manajemen produk, gudang, stok, mutasi stok
(stock in/out/adjustment/transfer), histori transaksi, serta publikasi event
perubahan stok ke Kafka.

### 1.3 Definisi, Akronim, dan Singkatan

| Istilah | Definisi |
| --- | --- |
| **SKU** | *Stock Keeping Unit* — kode unik barang |
| **Inventory** | Jumlah stok untuk kombinasi produk dan gudang |
| **Inventory Transaction** | Catatan histori perubahan stok (append-only) |
| **Stock In** | Penambahan stok |
| **Stock Out** | Pengurangan stok |
| **Adjustment** | Penyesuaian stok ke nilai tertentu |
| **Transfer** | Perpindahan stok antar gudang |
| **Outbox** | Tabel penyimpan sementara event sebelum dipublikasikan |
| **Idempotency Key** | Identitas unik request agar retry tidak menggandakan efek |
| **Overselling** | Kondisi stok terpakai melebihi jumlah tersedia |
| **At-least-once** | Jaminan pengiriman: pesan tidak hilang, tetapi bisa ganda |
| **DTO** | *Data Transfer Object* |
| **DLQ** | *Dead Letter Queue* |

### 1.4 Referensi

1. `docs/design-system.md` (StockFlow — Inventory Management Backend System)
2. `docs/architecture-plan.md`
3. `docs/fsd.md`

### 1.5 Ikhtisar Dokumen

§2 deskripsi umum; §3 kebutuhan fungsional (FR); §4 kebutuhan non-fungsional (NFR);
§5 antarmuka eksternal; §6 kebutuhan data; §7 matriks keterlacakan; §8 kriteria rilis.

---

## 2. Deskripsi Umum

### 2.1 Perspektif Produk

StockFlow adalah sistem baru (greenfield) yang menjadi **source of truth** untuk
data persediaan. Sistem lain berintegrasi melalui REST untuk operasi tulis/baca
dan melalui Kafka untuk menerima notifikasi perubahan stok.

### 2.2 Fungsi Produk

| No | Modul | Fungsi |
| --- | --- | --- |
| F-1 | Product Management | CRUD produk dan perubahan status aktif |
| F-2 | Warehouse Management | CRUD gudang |
| F-3 | Inventory Management | Baca stok per produk/gudang dan daftar stok |
| F-4 | Stock In | Penambahan stok disertai pencatatan transaksi |
| F-5 | Stock Out | Pengurangan stok dengan pencegahan stok negatif |
| F-6 | Stock Adjustment | Penyesuaian stok ke nilai tertentu |
| F-7 | Stock Transfer | Perpindahan stok antar gudang |
| F-8 | Transaction History | Histori seluruh mutasi stok |
| F-9 | Event Publishing | Publikasi event perubahan stok ke Kafka via outbox |
| F-10 | Monitoring | Health check dan metrik dasar |

### 2.3 Karakteristik Pengguna

| Aktor | Tipe | Deskripsi | Role usulan |
| --- | --- | --- | --- |
| Operator Gudang | Manusia | Menjalankan stock in/out dan adjustment | `INVENTORY_WRITE` |
| Supervisor Gudang | Manusia | Mengelola master produk & gudang | `WAREHOUSE_ADMIN` |
| Analis | Manusia | Membaca stok dan histori | `INVENTORY_READ` |
| Sistem Eksternal (ERP/OMS) | Sistem | Mengirim mutasi & menerima event | `INVENTORY_WRITE` / consumer |
| Operations | Manusia | Memantau health & metrik | — |

### 2.4 Batasan

| ID | Batasan |
| --- | --- |
| C-01 | Java 21+ dan Quarkus (versi sesuai BOM yang dipilih) |
| C-02 | Database wajib PostgreSQL |
| C-03 | Skema database dikelola Flyway; Hibernate memakai strategi `validate` |
| C-04 | API memakai DTO; entity tidak diekspos |
| C-05 | Integrasi async ke sistem lain hanya melalui Kafka |
| C-06 | Versi dependency dikelola melalui Quarkus BOM |

### 2.5 Asumsi dan Ketergantungan

| ID | Asumsi |
| --- | --- |
| A-01 | PostgreSQL dan Kafka tersedia saat runtime |
| A-02 | Client dapat mengirim `Idempotency-Key` pada operasi mutasi |
| A-03 | Consumer eksternal menangani duplikasi event (at-least-once) |
| A-04 | Kuantitas stok berupa bilangan bulat (`BIGINT`) pada rilis awal |

---

## 3. Kebutuhan Fungsional (FR)

Prioritas: **W** = Wajib, **S** = Seharusnya, **B** = Bisa.

### 3.1 Product Management

#### FR-PROD-01 — Membuat Produk

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `POST /api/v1/products` |

**Deskripsi:** Sistem harus membuat produk baru dengan `sku` unik.

**Kriteria penerimaan:**

1. Request valid → `201 Created` dengan data produk yang dibuat.
2. `sku` duplikat → ditolak (unique constraint) dengan response error yang jelas.
3. Field wajib kosong → `400 Bad Request`.

#### FR-PROD-02 — Daftar Produk

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/products` |

**Deskripsi:** Mengembalikan daftar produk dengan pagination.

**Kriteria penerimaan:**

1. Response `200 OK` memuat `data` dan metadata pagination.
2. Mendukung filter minimal berdasarkan status aktif dan pencarian `sku`/`name`.

#### FR-PROD-03 — Detail Produk

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/products/{id}` |

**Kriteria penerimaan:** `200 OK` bila ada; `404 Not Found` bila tidak.

#### FR-PROD-04 — Memperbarui Produk

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `PUT /api/v1/products/{id}` |

**Kriteria penerimaan:**

1. Perubahan tersimpan dan `updated_at` diperbarui.
2. `sku` tidak boleh berubah menjadi milik produk lain.

#### FR-PROD-05 — Mengubah Status Produk

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `PATCH /api/v1/products/{id}/status` |

**Deskripsi:** Mengaktifkan/menonaktifkan produk tanpa mengubah data lain.

**Kriteria penerimaan:**

1. Produk nonaktif tidak dapat dipakai pada operasi mutasi stok baru
   (harus dikembalikan sebagai `422`/`409` sesuai kebijakan §5.4).
2. Produk yang sudah memiliki stok tidak dihapus, hanya dinonaktifkan.

### 3.2 Warehouse Management

#### FR-WH-01 — Membuat Gudang

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `POST /api/v1/warehouses` |

**Kriteria penerimaan:** `code` unik; pelanggaran → ditolak.

#### FR-WH-02 — Daftar Gudang

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/warehouses` |

#### FR-WH-03 — Detail Gudang

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/warehouses/{id}` |

#### FR-WH-04 — Memperbarui Gudang

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `PUT /api/v1/warehouses/{id}` |

### 3.3 Inventory Management

#### FR-INV-01 — Daftar Stok

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/inventory` |

**Deskripsi:** Daftar stok dengan pagination, dapat difilter berdasarkan produk
dan/atau gudang.

#### FR-INV-02 — Stok per Produk & Gudang

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/inventory/{productId}/{warehouseId}` |

**Kriteria penerimaan:**

1. Bila kombinasi belum pernah ada, kembalikan `200 OK` dengan kuantitas `0`
   **atau** `404` sesuai keputusan kontrak yang ditetapkan (lihat ADR pada
   `architecture-plan.md` §15); keputusan harus konsisten dan teruji.
2. Nilai `quantity` dan `updated_at` akurat.

#### FR-INV-03 — Stock In

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `POST /api/v1/inventory/stock-in` |
| Header khusus | `Idempotency-Key` (wajib) |

**Deskripsi:** Menambah stok produk pada gudang tertentu.

**Alur wajib:**

1. Validasi produk dan gudang (harus ada dan aktif).
2. Validasi `quantity > 0`.
3. Mulai database transaction.
4. Ambil atau buat record `inventory` secara aman (upsert aman untuk kondisi bersamaan).
5. Tambahkan jumlah stok.
6. Tulis `inventory_transactions` bertipe `IN`.
7. Tulis event ke `outbox_events`.
8. Commit transaksi.

**Kriteria penerimaan:**

1. `quantityBefore`, `quantityChanged`, `quantityAfter` pada response konsisten.
2. Request berulang dengan `Idempotency-Key` sama **tidak** menambah stok dua kali
   dan mengembalikan hasil transaksi yang sama.
3. Kegagalan pada langkah mana pun membatalkan seluruh transaksi.

#### FR-INV-04 — Stock Out

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `POST /api/v1/inventory/stock-out` |
| Header khusus | `Idempotency-Key` (wajib) |

**Deskripsi:** Mengurangi stok dengan jaminan stok tidak menjadi negatif.

**Alur wajib:**

1. Validasi produk dan gudang.
2. Validasi `quantity > 0`.
3. Mulai database transaction.
4. Lock record inventory **atau** gunakan *atomic conditional update*.
5. Pastikan stok mencukupi.
6. Kurangi stok.
7. Tulis `inventory_transactions` bertipe `OUT`.
8. Tulis event ke `outbox_events`.
9. Commit transaksi.

**Kriteria penerimaan:**

1. Stok tidak mencukupi → `409 Conflict`, **tanpa** perubahan data dan **tanpa** event.
2. Dua request bersamaan yang totalnya melebihi stok: tepat satu berhasil, sisanya
   `409 Conflict`.
3. Stok tidak pernah bernilai negatif, terbukti melalui integration test konkurensi.

#### FR-INV-05 — Stock Adjustment

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `POST /api/v1/inventory/adjustments` |
| Header khusus | `Idempotency-Key` (wajib) |

**Deskripsi:** Menyesuaikan stok ke nilai target, dengan alasan/notes wajib.

**Kriteria penerimaan:**

1. `quantity_delta` = nilai target − stok saat ini, dan tercatat pada transaksi.
2. Nilai target tidak boleh negatif.
3. `notes` wajib agar audit trail bermakna.

#### FR-INV-06 — Stock Transfer

| Atribut | Nilai |
| --- | --- |
| Prioritas | S (dirancang; implementasi dapat bertahap) |
| Endpoint | `POST /api/v1/inventory/transfers` |
| Header khusus | `Idempotency-Key` (wajib) |

**Deskripsi:** Memindahkan stok dari gudang sumber ke gudang tujuan.

**Kriteria penerimaan:**

1. Terbentuk **dua** transaksi: `TRANSFER_OUT` pada sumber dan `TRANSFER_IN` pada tujuan.
2. Keduanya berada dalam **satu transaksi database**.
3. Sumber dan tujuan tidak boleh sama.
4. Stok sumber harus mencukupi (aturan sama dengan stock out).

#### FR-INV-07 — Histori Transaksi

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |
| Endpoint | `GET /api/v1/inventory/transactions` |

**Deskripsi:** Menyediakan histori mutasi stok dengan pagination dan filter
(produk, gudang, tipe transaksi, rentang tanggal, `referenceId`).

**Kriteria penerimaan:** hasil terurut menurun berdasarkan `created_at` secara default.

### 3.4 Event Publishing

#### FR-EVT-01 — Pencatatan Event ke Outbox

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Deskripsi:** Setiap mutasi stok harus menulis satu baris `outbox_events` pada
transaksi yang sama dengan perubahan stok.

**Kriteria penerimaan:** tidak ada mutasi stok ter-commit tanpa baris outbox yang
menyertainya, dan sebaliknya.

#### FR-EVT-02 — Publikasi Event ke Kafka

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Deskripsi:** Publisher harus membaca event yang belum dipublikasikan dan
mengirimkannya ke Kafka, lalu menandainya sebagai published.

**Kriteria penerimaan:**

1. Event yang berhasil dikirim memiliki `published_at` terisi.
2. Event yang gagal tetap belum published dan akan dicoba kembali.
3. Publisher tidak memblokir response API (publikasi terjadi setelah commit).
4. Key pesan = `{productId}:{warehouseId}`.

#### FR-EVT-03 — Retry dan Dead-Letter

| Atribut | Nilai |
| --- | --- |
| Prioritas | S |

**Deskripsi:** Publikasi yang gagal dicoba ulang dengan backoff; kegagalan permanen
dipindahkan ke dead-letter dan dapat ditindaklanjuti.

**Kriteria penerimaan:** jumlah percobaan terbatas dan tercatat (kolom `attempts`),
dengan alarm bila backlog/umur event melewati ambang.

### 3.5 Monitoring

#### FR-MON-01 — Health Check

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Kriteria penerimaan:** readiness memeriksa **database** dan **Kafka**; health
tersedia pada root path yang dikonfigurasi.

#### FR-MON-02 — Metrik

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Kriteria penerimaan:** metrik pada `architecture-plan.md` §11.1 tersedia melalui
endpoint Prometheus.

### 3.6 Kebutuhan Lintas-Fungsi

#### FR-X-01 — Idempotency

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Deskripsi:** Seluruh operasi mutasi stok harus idempotent berdasarkan
`Idempotency-Key`, dengan **unique constraint database** sebagai pengaman utama.

#### FR-X-02 — Standard Response Envelope

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Deskripsi:** Semua response memakai `{ "success", "message", "data" }` dan error
memakai format konsisten (kode error, pesan, detail validasi, `traceId`).

#### FR-X-03 — Audit Trail

| Atribut | Nilai |
| --- | --- |
| Prioritas | W |

**Deskripsi:** Setiap mutasi stok tercatat permanen dengan aktor, waktu, tipe,
`quantity_before`, `quantity_delta`, `quantity_after`, dan `reference_id`.
Koreksi dilakukan dengan transaksi baru, bukan update histori.

---

## 4. Kebutuhan Non-Fungsional (NFR)

### 4.1 Performa & Skalabilitas

| ID | Kebutuhan | Target |
| --- | --- | --- |
| NFR-P-01 | Latency endpoint baca (p95) | ≤ 200 ms pada beban normal |
| NFR-P-02 | Latency endpoint mutasi (p95) | ≤ 500 ms (tidak termasuk publikasi Kafka) |
| NFR-P-03 | Publikasi event | Tunda maksimum 1 menit (interval publisher konfigurabel) |
| NFR-P-04 | Pagination | Semua endpoint list wajib mendukung pagination |
| NFR-P-05 | Throughput mutasi | Menangani beban konkuren tanpa stok negatif |

### 4.2 Keandalan & Konsistensi

| ID | Kebutuhan |
| --- | --- |
| NFR-R-01 | Perubahan stok, histori transaksi, dan outbox event tersimpan atomik dalam satu transaksi PostgreSQL |
| NFR-R-02 | Stok tidak pernah bernilai negatif, termasuk pada kondisi bersamaan |
| NFR-R-03 | Event bersifat at-least-once; event gagal harus dapat dipublikasikan ulang |
| NFR-R-04 | Duplikasi akibat retry tidak menghasilkan efektivitas ganda pada konsumen yang idempotent |
| NFR-R-05 | Respons API tidak boleh bergantung pada keberhasilan publikasi Kafka |

### 4.3 Keamanan

| ID | Kebutuhan |
| --- | --- |
| NFR-S-01 | Authentication OAuth2/OIDC dan role-based authorization |
| NFR-S-02 | Role: `INVENTORY_READ`, `INVENTORY_WRITE`, `WAREHOUSE_ADMIN` |
| NFR-S-03 | Validasi input dan batas ukuran request |
| NFR-S-04 | TLS untuk koneksi database dan Kafka |
| NFR-S-05 | Kafka ACL untuk producer/consumer |
| NFR-S-06 | Secret tidak boleh tersimpan di source code / file konfigurasi repo |
| NFR-S-07 | Audit log untuk operasi sensitif |

### 4.4 Pemeliharaan

| ID | Kebutuhan |
| --- | --- |
| NFR-M-01 | Struktur modular berdasarkan domain (product, warehouse, inventory, transaction, messaging, outbox, common) |
| NFR-M-02 | Skema database dikelola Flyway; migrasi dapat dijalankan dari lingkungan baru |
| NFR-M-03 | Nama channel Kafka pada kode identik dengan nama pada konfigurasi |
| NFR-M-04 | Satu library JSON saja yang dipakai secara konsisten |
| NFR-M-05 | Build lolos `mvn verify` termasuk unit test |

### 4.5 Observability

| ID | Kebutuhan |
| --- | --- |
| NFR-O-01 | Health check database dan Kafka |
| NFR-O-02 | Metrik HTTP, bisnis, dan outbox backlog tersedia |
| NFR-O-03 | Correlation/trace ID dari request API hingga event Kafka |
| NFR-O-04 | Log terstruktur dengan konteks produk, gudang, dan transaksi |

### 4.6 Pengujian

| ID | Kebutuhan |
| --- | --- |
| NFR-T-01 | Unit test business rules tanpa database/Kafka |
| NFR-T-02 | Integration test dengan Testcontainers (PostgreSQL + Kafka) |
| NFR-T-03 | Test konkurensi untuk stock out |
| NFR-T-04 | Test idempotency untuk seluruh operasi mutasi |
| NFR-T-05 | Test rollback transaksi dan retry publisher |

---

## 5. Antarmuka Eksternal

### 5.1 REST API — Base Path `/api/v1`

**Product**

| Method | Endpoint | Deskripsi | FR |
| --- | --- | --- | --- |
| POST | `/products` | Membuat produk | FR-PROD-01 |
| GET | `/products` | Daftar produk | FR-PROD-02 |
| GET | `/products/{id}` | Detail produk | FR-PROD-03 |
| PUT | `/products/{id}` | Memperbarui produk | FR-PROD-04 |
| PATCH | `/products/{id}/status` | Mengubah status produk | FR-PROD-05 |

**Warehouse**

| Method | Endpoint | Deskripsi | FR |
| --- | --- | --- | --- |
| POST | `/warehouses` | Membuat gudang | FR-WH-01 |
| GET | `/warehouses` | Daftar gudang | FR-WH-02 |
| GET | `/warehouses/{id}` | Detail gudang | FR-WH-03 |
| PUT | `/warehouses/{id}` | Memperbarui gudang | FR-WH-04 |

**Inventory**

| Method | Endpoint | Deskripsi | FR |
| --- | --- | --- | --- |
| GET | `/inventory` | Daftar stok | FR-INV-01 |
| GET | `/inventory/{productId}/{warehouseId}` | Stok per produk & gudang | FR-INV-02 |
| POST | `/inventory/stock-in` | Menambah stok | FR-INV-03 |
| POST | `/inventory/stock-out` | Mengurangi stok | FR-INV-04 |
| POST | `/inventory/adjustments` | Penyesuaian stok | FR-INV-05 |
| GET | `/inventory/transactions` | Histori transaksi | FR-INV-07 |
| POST | `/inventory/transfers` | Transfer stok antar-gudang | FR-INV-06 |

### 5.2 Kontrak Request Stock In (contoh)

```
POST /api/v1/inventory/stock-in
Idempotency-Key: 8b56c9c1-unique-request-key
Content-Type: application/json
```

```json
{
  "productId": "c9f0a7d2-8c8d-4f25-a4a2-1e2d9b2c3f11",
  "warehouseId": "a1d4f8c2-2b9e-4a3d-8f5c-6e7a9b0c1d22",
  "quantity": 100,
  "referenceId": "PO-2026-0001",
  "notes": "Penerimaan barang dari supplier"
}
```

### 5.3 Kontrak Response Sukses

```json
{
  "success": true,
  "message": "Stock received successfully",
  "data": {
    "productId": "c9f0a7d2-8c8d-4f25-a4a2-1e2d9b2c3f11",
    "warehouseId": "a1d4f8c2-2b9e-4a3d-8f5c-6e7a9b0c1d22",
    "quantityBefore": 50,
    "quantityChanged": 100,
    "quantityAfter": 150,
    "transactionId": "f4b4d88d-5b47-4f67-bc6d-1a2b3c4d5e6f"
  }
}
```

> ID pada contoh merupakan placeholder untuk ilustrasi kontrak API.

### 5.4 HTTP Status Codes

| Status | Makna | Ketentuan pemakaian |
| --- | --- | --- |
| `200 OK` | Operasi berhasil | Baca, update, mutasi tanpa pembuatan resource |
| `201 Created` | Resource berhasil dibuat | Pembuatan produk, gudang, dan hasil mutasi stok |
| `400 Bad Request` | Request tidak valid | Kegagalan validasi input/format |
| `404 Not Found` | Resource tidak ditemukan | Produk/gudang/id tidak ada |
| `409 Conflict` | Konflik bisnis | **Stok tidak cukup**; duplikasi SKU/code |
| `422 Unprocessable Entity` | Aturan bisnis tidak terpenuhi | Dipakai konsisten untuk aturan non-stok (mis. produk nonaktif) |
| `500 Internal Server Error` | Kesalahan tidak tertangani | — |

**Keputusan konsistensi:** `409` khusus untuk stok tidak cukup dan konflik
keunikan; `422` untuk pelanggaran aturan bisnis lain. Pemakaian harus konsisten
agar client dapat menangani error secara deterministik.

### 5.5 Antarmuka Kafka

| Topic | Arah | Tujuan |
| --- | --- | --- |
| `stockflow.inventory.events.v1` | Out | Seluruh event inventory dengan `eventType` sebagai discriminator |
| `stockflow.inventory.stock-in.v1` | Out | (Opsi pemisahan) event penambahan stok |
| `stockflow.inventory.stock-out.v1` | Out | (Opsi pemisahan) event pengurangan stok |
| `stockflow.inventory.adjusted.v1` | Out | (Opsi pemisahan) event penyesuaian stok |
| `stockflow.inventory.transferred.v1` | Out | (Opsi pemisahan) event transfer stok |

**Key:** `{productId}:{warehouseId}`

### 5.6 Antarmuka Operasional

| Item | Nilai |
| --- | --- |
| Health endpoint | Root path `/q/health` (konfigurabel) |
| Metrics | Prometheus via `quarkus-micrometer-registry-prometheus` |
| Database | PostgreSQL (JDBC) |
| Broker | Apache Kafka |

---

## 6. Kebutuhan Data

| ID | Kebutuhan |
| --- | --- |
| DR-01 | Lima tabel utama: `products`, `warehouses`, `inventory`, `inventory_transactions`, `outbox_events` |
| DR-02 | `products.sku` unik; `warehouses.code` unik |
| DR-03 | `inventory` unik pada `(product_id, warehouse_id)` dan `CHECK (quantity >= 0)` |
| DR-04 | `inventory_transactions.idempotency_key` unik (pengaman idempotency) |
| DR-05 | `inventory_transactions` bersifat append-only |
| DR-06 | `quantity_before + quantity_delta = quantity_after` wajib konsisten |
| DR-07 | `inventory.version` disediakan untuk kebutuhan concurrency |
| DR-08 | `outbox_events.payload` disimpan sebagai JSONB |
| DR-09 | Index wajib: `(product_id, created_at)`, `(warehouse_id, created_at)`, `reference_id`, `created_at` pada outbox, dan index parsial untuk event belum dipublikasikan |
| DR-10 | Semua migrasi skema melalui Flyway dengan skrip versioned |

Skema kolom lengkap ada pada `fsd.md` §7.

---

## 7. Matriks Keterlacakan

| Requirement | Endpoint / Komponen | Fase |
| --- | --- | --- |
| FR-PROD-01..05 | `/products*`, modul `product` | Phase 2 |
| FR-WH-01..04 | `/warehouses*`, modul `warehouse` | Phase 2 |
| FR-INV-01..02 | `/inventory*` (baca), modul `inventory` | Phase 3 |
| FR-INV-03, FR-INV-04 | `/inventory/stock-in`, `/inventory/stock-out` | Phase 3 |
| FR-INV-05 | `/inventory/adjustments` | Phase 3 |
| FR-INV-06 | `/inventory/transfers` | Phase 3+ |
| FR-INV-07 | `/inventory/transactions` | Phase 3 |
| FR-EVT-01 | modul `outbox` | Phase 4 |
| FR-EVT-02 | modul `messaging/publisher` + scheduled job | Phase 4 |
| FR-EVT-03 | retry & dead-letter | Phase 4 |
| FR-MON-01, FR-MON-02 | `quarkus-smallrye-health`, `quarkus-micrometer` | Phase 5 |
| FR-X-01 | kolom `idempotency_key` + unique constraint | Phase 3 |
| FR-X-02 | `common/response` + exception handler | Phase 1 |
| FR-X-03 | `inventory_transactions` | Phase 3 |

---

## 8. Kriteria Rilis

Rilis awal diterima apabila seluruh Definition of Done pada
`architecture-plan.md` §18 terpenuhi, khususnya:

1. Stok tidak dapat menjadi negatif melalui request bersamaan.
2. Request berulang tidak menggandakan mutasi stok.
3. Perubahan stok dan outbox event tersimpan secara atomik.
4. Event dapat dipublikasikan ulang ketika terjadi kegagalan.
5. Migrasi database dapat dijalankan dari lingkungan baru.
6. Health check dan metrik tersedia.
7. Tidak ada secret produksi di source code.
