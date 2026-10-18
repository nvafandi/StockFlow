# Refactoring Report: StockFlow

Standar acuan: **PruForce/Quarkus Engineering Standards** (skill `refactor-project`).
Tanggal: 2026-10-17 · Branch: `refactor/quarkus-standards` · Rencana eksekusi: [`refactor-plan.md`](refactor-plan.md)

---

## 1. Ringkasan

Refactor dilakukan sebagai 20 fase bergerbang dengan checkpoint compile di setiap fase. Semua
tahap yang diminta **selesai**; satu fase bersifat opsional dan di-`SKIP` dengan alasan
tertulis (P14 — metrik domain).

| Aspek | Sebelum | Sesudah |
| --- | --- | --- |
| Struktur layer | `{domain}/application`, `{domain}/domain`, `{domain}/infrastructure` | `{domain}/{api,service,repository,entity,mapper,constants,util}` |
| Envelope bersama | `common/response` (envelope + error + util bercampur) | `common/api`, `common/constants`, `common/exception` |
| Batas transaksi DB | di repository (`StockMutationRepository`) | di application service (`InventoryMutationService`) |
| Repository | kelas konkret | `interface` + `impl/` (DIP) |
| Exception | `BusinessException extends RuntimeException`, 4 mapper terpisah | `DomainException` (akar) + satu `GlobalExceptionHandler` |
| Entity | field publik, tanpa Javadoc standar | field privat + Lombok, `@Index`, Javadoc |
| DTO | punya `static from(...)` (logika konversi di DTO) | `record` murni + mapper per domain |
| Dokumentasi API | tanpa anotasi OpenAPI | `@Tag`/`@Operation`/`@APIResponse` |
| Test | 13 test (5 kelas) | **22 test** (7 kelas: 5 integrasi + 2 unit dengan mock) |
| File sumber utama | 51 | 61 |
| Referensi nama lama di dokumen | ada | nol |
| Build | — | `BUILD SUCCESS` (`mvnw.cmd -B -o clean test`) |

**Hasil akhir: 16/16 check validasi PASS, 22/22 test hijau, 0 pelanggaran standar pada 61 file,
aplikasi berjalan dengan `/q/health` = `UP`.**

---

## 2. Validation Report (16 checks)

| # | Check | Status | Bukti / Catatan |
| --- | --- | --- | --- |
| 1 | Architecture (semua layer ada) | PASS | `product`, `warehouse`, `inventory` lengkap (api/rest, api/dto, service, repository+impl, entity, mapper); `outbox` = entity + repository (service/publisher menyusul Phase 4 sesuai roadmap); `exception` & `api` di `common` (pseudo-function). Layer `config/` **tidak dibuat**: tidak ada konfigurasi per-domain — seluruh konfigurasi ada di `application.properties` (`@ConfigProperty` tidak diperlukan). |
| 2 | Naming `{function}.{layer}` | PASS | `com.stockflow.{product,warehouse,inventory,outbox}.{api,service,repository,entity,mapper,...}`; `common` sebagai pseudo-function. Pemetaan dari standar (yang memakai prefix korporat) dicatat sebagai *naming mapping*, bukan deviasi arsitektur. |
| 3 | Injection (tanpa field injection) | PASS | `@Inject` hanya tersisa di kelas test `@QuarkusTest` (idiomatik); `src/main` = 0. Seluruh bean memakai constructor injection. |
| 4 | DTO terpisah request/response + `@Valid` | PASS | 4 request + 5 response DTO di `api/dto/request|response`; `@Valid` pada ketiga endpoint POST. |
| 5 | Response selalu `ApiResponse<T>` | PASS | `RestResponse<ApiResponse<T>>` (create/stockIn/stockOut) dan `ApiResponse<PageResponse<T>>`/`ApiResponse<T>` (list/get) — tidak ada `Response` mentah. |
| 6 | Exception extends `DomainException` | PASS | 9 exception domain → `BusinessException` → `DomainException` (abstrak). `extends RuntimeException` hanya pada `DomainException`. |
| 7 | JBoss Logging; tanpa `System.out` | PASS | `Logger` di `ProductService`, `WarehouseService`, `InventoryService`, `InventoryMutationService`, `GlobalExceptionHandler`; 0 `System.out`/`printStackTrace`. |
| 8 | Unit test + mock **dan** `@QuarkusTest` | PASS | 2 unit test baru (`InventoryServiceTest` 6 test, `ProductServiceTest` 3 test) + 5 kelas `@QuarkusTest` (13 test) = 22 test. |
| 9 | Dokumentasi (OpenAPI + Javadoc) | PASS | `@Tag`/`@Operation`/`@APIResponse` pada 3 resource; Javadoc kelas pada seluruh 63 tipe public `src/main`. |
| 10 | Magic value & query di `constants/` | PASS | `CommonConstants` (paging), `InventoryConstants` (header, panjang, tipe/versi event), `InventoryQueryConstants` (SQL + fragmen JPQL). Sisa konstanta privat ada di kelas pemiliknya (mis. `TIMESTAMP_FORMAT`). |
| 11 | `@Transactional` pada operasi tulis | PASS | `ProductService.create`, `WarehouseService.create`, `InventoryMutationService.applyStockIn/applyStockOut/findReused`; repository tanpa `@Transactional`. |
| 12 | Bean Validation pada request DTO | PASS | `@NotNull`, `@Positive`, `@Size`, `@NotBlank` lengkap pada 4 request DTO + `@Min`/`@Max` parameter paging. |
| 13 | Iterasi koleksi memakai Streams | PASS (dengan 1 pengecualian terdokumentasi) | `stream().map(...)` di service & `GlobalExceptionHandler`; satu `for` loop di `InventoryService` menyusuri **rantai `Throwable`** (bukan koleksi) dan sudah dikomentari; `ArrayList`/`HashMap` di `InventoryTransactionRepositoryImpl` dipakai untuk membangun query dinamis, bukan iterasi. |
| 14 | CDI (tanpa `@Named`, tanpa konstruktor dummy) | PASS | 0 `@Named`; tidak ada konstruktor no-args dummy; bean resource `@ApplicationScoped` dengan constructor injection. |
| 15 | SOLID | PASS | Lihat tabel di bawah. |
| 16 | Typed contract (tanpa `Map`/`Object`/`JsonNode` di API) | PASS | Signatur resource & DTO hanya tipe eksplisit (`ApiResponse<...>`, record DTO). `Map`/`Object` hanya di internal repository (parameter query) dan `ObjectMapper`, bukan kontrak API. |

### SOLID Compliance

| Prinsip | Status | Catatan |
| --- | --- | --- |
| SRP | PASS | Resource (validasi bentuk) · Service (orkestrasi/use case) · `InventoryMutationService` (batas transaksi) · Repository (akses data) · Mapper (konversi) · `GlobalExceptionHandler` (error). Pemisahan `InventoryService`/`InventoryMutationService` justru *memisahkan* dua tanggung jawab yang dulu bercampur. |
| OCP | PASS | Nama event per jenis mutasi dimiliki `TransactionType.eventType()` (bukan `switch` di mapper); pemetaan constraint DB memakai tabel `Map<String, ErrorCode>` (bukan `switch`). Kedua `switch` yang ditemukan pada P18 dihilangkan. |
| LSP | PASS | Hierarki exception seragam: setiap subclass membawa `ErrorCode` valid dan dapat ditangani `GlobalExceptionHandler` tanpa perlakuan khusus. |
| ISP | PASS | Interface repository ramping (hanya operasi yang dipakai); tidak ada method "cadangan" yang tak terpakai. |
| DIP | PASS | Service bergantung pada interface repository (`ProductRepository`, dst.); implementasi berada di `impl/` dan disuntik CDI. |

**Fully Compliant: YES · Blocks Refactoring: NO**

---

## 3. Tree Map Comparison

`tree` dan `graphify` tidak tersedia di mesin ini (`SKIPPED — missing coreutils`, lihat
`refactor-plan.md` R6). Baseline karena itu diambil dari inventaris P01/P02 (jumlah file +
layer lama pada F5) dan perbandingan di bawah dibuat sebagai **cacah per paket** dengan
semantik yang sama.

### Before Refactoring

```
src/main/java/com/stockflow/            # 51 file
├── common/response/                    # envelope + ErrorCode + ErrorResponse + TraceId (bercampur)
├── product/application|domain|infrastructure/
├── warehouse/application|domain|infrastructure/
├── inventory/application|domain|infrastructure/   # StockMutationRepository = batas transaksi
└── outbox/domain|repository/
docs/                                    # 6 dokumen (satu di antaranya hasil generate)
test: 5 kelas (StockMutationApiTest, StockMutationAtomicityTest, StockOutConcurrencyTest,
      PayloadHasherTest, support/MasterData) — 13 test
```

### After Refactoring

```
src/main/java/com/stockflow/            # 61 file
├── common/api                          4   ApiResponse, PageResponse, ErrorResponse, TraceId
├── common/constants                    1   CommonConstants
├── common/exception                   13   DomainException + 9 exception + ErrorCode + GlobalExceptionHandler
├── product/api                         3   rest/ + dto/request + dto/response
│   ├── entity                          1   Product (Lombok, @Index)
│   ├── mapper                          1   ProductMapper
│   ├── repository                      2   ProductRepository + impl/
│   └── service                         1   ProductService
├── warehouse/… (struktur sama)        8
├── inventory/api                       6   rest/ + 2 request + 3 response DTO
│   ├── constants                       2   InventoryConstants + InventoryQueryConstants
│   ├── entity                          3   Inventory, InventoryTransaction, TransactionType
│   ├── mapper                          2   InventoryMapper, StockEventPayloadMapper
│   ├── repository                      5   InventoryRepository + impl/, InventoryTransactionRepository + impl/, InventoryMutation
│   ├── service                         5   InventoryService, InventoryMutationService, Command, Result, Outcome
│   └── util                            1   PayloadHasher
└── outbox/entity|repository            3   OutboxEvent + OutboxEventRepository + impl/
test: 7 kelas — 22 test (13 integrasi + 9 unit)
```

### Changes

| Kategori | Jumlah | Rincian |
| --- | --- | --- |
| **File baru** | 24 | 5 interface repository (dipecah dari kelas) + 5 `impl/`, 3 mapper, `InventoryMutationService`, `StockMutationOutcome`, `DomainException`, `GlobalExceptionHandler`, `CommonConstants`, `InventoryQueryConstants`, `TransactionType`, 2 kelas unit test |
| **File dihapus** | 5 | `StockMutationRepository` (P10), 4 exception mapper (P12) |
| **File dipindah/di-rename** | 43 | 38 file pada P07 (layer lama → standar) + `StockEventPayloadFactory`→`StockEventPayloadMapper` + 4 test |
| **File dipertahankan (isi disesuaikan)** | 15 | entity, service, resource, DTO, `common/api` |

**Net change: +10 file `src/main` (51 → 61) dan +2 kelas test (5 → 7).**

Kenaikan jumlah file adalah konsekuensi langsung dari standar yang diminta: interface +
implementasi (DIP) memecah 5 repository menjadi 10 file, dan pemisahan tanggung jawab
menambah mapper, constants, serta service transaksional.

---

## 4. Status 31 Temuan

| # | Ringkas | Fase | Status |
| --- | --- | --- | --- |
| F1 | Maven wrapper tidak ada | P06 | ✅ Selesai (`mvnw`, `mvnw.cmd`, `.mvn/wrapper`) |
| F2 | `quarkus-smallrye-openapi` belum ada | P06 | ✅ Selesai |
| F3 | Lombok belum ada | P06 | ✅ Selesai (1.18.48, `provided` + annotation processor) |
| F4 | `quarkus-junit-mockito` tidak dipakai | P16 | ✅ Selesai (dipakai unit test P15) |
| F5 | Layer `application`/`domain`/`infrastructure` | P07 | ✅ Selesai → `{api,service,repository,entity,...}` |
| F6 | `api/dto` belum dipisah request/response | P07 | ✅ Selesai (11 DTO) |
| F7 | Belum ada `mapper/`, `constants/`, `config/`, `util/` | P07 | ✅ Selesai (`mapper`, `constants`, `util`); `config/` = N/A, tanpa konfigurasi per-domain |
| F8 | `common/response` bercampur | P07 | ✅ Selesai → `common/api`, `common/constants`, `common/exception` |
| F9 | Pohon struktur dokumen tidak sesuai kode | P07 | ✅ Selesai (plan §4, design-system §4) |
| F10 | 3 resource tanpa `@ApplicationScoped` | P08 | ✅ Selesai |
| F11 | `@Inject` redundan pada konstruktor tunggal | P08 | ✅ Selesai |
| F12 | 3 endpoint mengembalikan `Response` tak bertipe | P09 | ✅ Selesai (`RestResponse<ApiResponse<T>>`) |
| F13 | Batas transaksi DB di repository | P10 | ✅ Selesai (pindah ke `InventoryMutationService`; ADR-09 direvisi) |
| F14 | Service tanpa JBoss Logging | P10 | ✅ Selesai |
| F15 | Magic constant `IDEMPOTENCY_KEY_MAX_LENGTH` | P10 | ✅ Selesai → `InventoryConstants` |
| F16 | Loop manual rantai `Throwable` | P10 | ✅ Selesai (tetap loop, dengan komentar alasannya — rantai `Throwable`, bukan koleksi; check 13 mengizinkan) |
| F17 | Dokumen menyatakan transaksi di repository | P10 | ✅ Selesai (4 dokumen + ADR-09) |
| F18 | Repository kelas konkret | P11 | ✅ Selesai (5 interface + 5 `impl/`) |
| F19 | SQL/JPQL inline | P11 | ✅ Selesai → `InventoryQueryConstants` |
| F20 | Event/aggregate type inline | P11/P18 | ✅ Selesai → `InventoryConstants` + `TransactionType.eventType()` |
| F21 | `@Version` pada `products`/`warehouses` | P11 | ⚠️ **Ditutup sebagai departure** — butuh migrasi Flyway V6 dan mengubah perilaku locking; dicatat di Javadoc entity (Deferred Registry) |
| F22 | `@Index` tidak dideklarasikan | P11 | ✅ Selesai (V1/V4/V5 tertuang di entity); indeks *partial* `idx_outbox_pending` tidak dapat dinyatakan JPA → didokumentasikan |
| F23 | Timestamp diisi manual di repository | P11 | ✅ Selesai — dipertahankan eksplisit (`OffsetDateTime.now(UTC)` / SQL `now()`) sebagai departure beralasan (UTC eksplisit, berlaku untuk native INSERT) |
| F24 | `InventoryMutation` di layer infrastructure | P11 | ✅ Selesai → `inventory/repository` (proyeksi hasil query) |
| F25 | `BusinessException extends RuntimeException` | P12 | ✅ Selesai → `DomainException` |
| F26 | 4 exception mapper terpisah | P12 | ✅ Selesai → `GlobalExceptionHandler` |
| F27 | Tanpa `@Tag`/`@Operation`/`@APIResponse` | P13 | ✅ Selesai (verifikasi `/q/openapi` 16 KB) |
| F28 | 5 test merujuk package lama | P15 | ✅ Selesai (assertion kontrak tidak diubah) |
| F29 | Belum ada unit test + mock | P15 | ✅ Selesai (9 test unit) |
| F30 | Ekspor dokumen harus diregenerasi | P16 | ✅ Selesai (3.528 baris / 130 KB) |
| F31 | Referensi nama kelas lama di dokumen | P16 | ✅ Selesai (grep bersih) |

**Tuntas: 29/31. Ditutup sebagai departure terdokumentasi: 2 (F21, F23).**

---

## 5. Deviasi & Pengecualian Terdokumentasi

1. **Naming mapping** — standar memakai prefix korporat `{org}.{domain}.{function}.{layer}`;
   StockFlow memakai `com.stockflow.{function}.{layer}`. Deviasi kosmetik, bukan struktural.
2. **Tanpa paket `config/`** — tidak ada konfigurasi per-domain; `@ConfigProperty` tidak dipakai
   karena semua nilai berasal dari `application.properties` (`%dev`/`%prod`/environment).
3. **`@Transactional(readOnly = true)` tidak dapat dipakai** — `jakarta.transaction.Transactional`
   tidak memiliki atribut `readOnly` (contoh di skill berasal dari Spring). Dipakai `@Transactional`
   biasa; dicatat di Deferred Registry.
4. **`service` tanpa pasangan interface + impl** — mengikuti Recipe 10 skill (satu kelas per domain),
   dengan satu pengecualian bean transaksional `InventoryMutationService` yang wajib terpisah untuk
   mencegah *rollback-only* (risiko R1).
5. **`@Inject` pada field test** — dibiarkan (idiomatik `@QuarkusTest`); check 3/14 menyasar kode produksi.
6. **`@Version` tidak diadopsi** (F21) dan **timestamp manual dipertahankan** (F23) — dicatat sebagai
   departure beralasan pada `refactor-plan.md` §4.
7. **`@Index` partial** — `idx_outbox_pending` memakai predikat `WHERE published_at IS NULL` yang tidak
   dapat dinyatakan anotasi JPA; di entity hanya didokumentasikan, kebenaran tetap milik Flyway.
8. **Tooling** — script `check-*.sh` tidak dapat dijalankan (tanpa coreutils) dan `graphify`/`tree`
   tidak tersedia; digantikan checklist manual 16 butir + cacah paket. Check 5 (`/q/health`) tetap
   dijalankan sungguhan (tidak SKIP).
9. **`mvn` vs `mvnw.cmd`** — `mvn` tidak terpasang global; seluruh build memakai wrapper repo.

---

## 6. Bukti Verifikasi

| Bukti | Perintah / cara | Hasil |
| --- | --- | --- |
| Build bersih | `mvnw.cmd -B -o clean test` | `BUILD SUCCESS` |
| Test suite | sama | **22/22 hijau** (8 API + 1 atomicity + 1 concurrency + 3 PayloadHasher + 6 + 3 unit) |
| Tanpa Spring | grep `org.springframework\|@Autowired\|@Component\|spring-boot` di `src/` | 0 hasil |
| Quarkus aktif | `pom.xml` + log startup | BOM 3.40.1; fitur: agroal, cdi, flyway, hibernate-orm-panache, rest-jackson, micrometer, smallrye-health, smallrye-openapi |
| Startup + health | dev mode + PostgreSQL 17 sementara | `started in 6.485s`; Flyway menerapkan V1–V5; `/q/health` & `/ready` = `UP` (DB `UP`), `/live` = `UP` |
| OpenAPI | `GET /q/openapi` | 16.053 byte (anotasi P13 terbaca) |
| Standar | checklist manual 16 butir + skrip `standards_check.py` | 16/16 PASS; 0 pelanggaran pada 61 file |
| Dokumen konsisten | grep nama lama di `docs/` + `src/` | 0 hasil |

---

## 7. Risiko Tersisa & Rekomendasi

| # | Risiko / utang | Dampak | Rekomendasi |
| --- | --- | --- | --- |
| 1 | `outbox` belum punya publisher/service (Phase 4) | Event tersimpan di DB tetapi belum dipublikasikan | Implementasi `outbox/service` + scheduled publisher; pola sudah tersedia di `architecture-plan.md` §6–§7 |
| 2 | Tanpa `@Version` pada `products`/`warehouses` (F21) | Update bersamaan pada master data bisa saling menimpa | Bila master data mulai punya update selain `active`, tambahkan Flyway V6 + `@Version` |
| 3 | Uji concurrency hanya untuk stock-out; stock-in mengandalkan `ON CONFLICT` | Regresi pada jalur upsert tidak tertangkap test | Tambah test stock-in bersamaan (dua request paralel ke produk/gudang baru) |
| 4 | `GlobalExceptionHandler.handleUnexpected` mengembalikan `Response` mentah untuk `WebApplicationException` | Header `X-Correlation-Id` tidak selalu ada pada 404 routing | Bila traceabilitas wajib untuk semua error, petakan juga `WebApplicationException` ke envelope |
| 5 | Tidak ada instrumentasi metrik domain (P14 SKIP) | Sulit melihat laju mutasi stok per operasi | Tambahkan counter `inventory.stock_in.completed` dsb. mengikuti naming §11 |
| 6 | Test integrasi butuh Docker | CI tanpa Docker tidak dapat menjalankan 13 test integrasi | Pastikan runner CI menyediakan Docker (Dev Services) |
| 7 | Peringatan Lombok `sun.misc.Unsafe` saat compile | Bising, bukan kegagalan | Pantau rilis Lombok berikutnya; naikkan versi bila sudah bersih |

---

## 8. Kesimpulan

Refactor 20 fase tuntas dengan **16/16 check validasi PASS**, **22/22 test hijau**, dan
**0 pelanggaran standar** pada 61 file sumber utama. Kontrak API publik (envelope
`{success,message,data}` / `{success,message,error:{code,details,traceId}}`, status 201/200
berbasis `Idempotency-Key`, header `X-Correlation-Id`) **tidak berubah** — perubahan seluruhnya
bersifat struktural dan internal. Dua butir standar (F21 `@Version`, F23 timestamp) ditutup
sebagai *departure* terdokumentasi dengan alasan teknis, bukan diabaikan tanpa catatan.
