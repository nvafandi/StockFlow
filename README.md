# StockFlow Service

Backend **inventory management** StockFlow: REST API + event-driven integration,
dibangun dengan **Java 25+**, Quarkus, PostgreSQL, dan Flyway. Dokumen desain lengkap
ada di [`docs/`](docs/README.md).

## Status Implementasi

| Fase | Status | Cakupan |
| --- | --- | --- |
| Phase 1 — Foundation | Selesai | Quarkus + PostgreSQL + Flyway, response envelope, exception handling, unit test |
| Phase 2 — Master Data | Sebagian | `POST/GET /api/v1/products`, `POST/GET /api/v1/warehouses` |
| Phase 3 — Inventory Core | Sebagian | Stock in, stock out, histori transaksi, idempotency, concurrency control |
| Phase 4 — Kafka Integration | Belum | Event schema Kafka, publisher outbox, retry/DLQ |
| Phase 5 — Production Readiness | Belum | Auth, CI/CD, deployment; health & metrik dasar sudah aktif |

Detail rencana: [`docs/architecture-plan.md`](docs/architecture-plan.md) §16.

## Prasyarat

- JDK 25
- Maven 3.9+
- Docker (untuk PostgreSQL lokal dan test)

## Menjalankan

```bash
docker compose up -d postgres
mvn quarkus:dev
```

- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/q/health`
- Metrik: `http://localhost:8080/q/metrics`

Konfigurasi koneksi database dapat di-override dengan `DB_JDBC_URL`, `DB_USERNAME`,
dan `DB_PASSWORD`. Migrasi Flyway dijalankan otomatis saat startup.

## Pengujian

```bash
mvn test
```

Test integrasi memakai PostgreSQL sekali pakai melalui Dev Services (Testcontainers),
jadi Docker harus aktif.

| Test | Yang dibuktikan |
| --- | --- |
| `StockMutationApiTest` | Kontrak FS-07/FS-08: `201`/`200`/`400`/`404`/`409`, histori, dan event outbox |
| `StockMutationAtomicityTest` | Kegagalan di tengah transaksi membatalkan stok, histori, dan outbox sekaligus |
| `StockOutConcurrencyTest` | Dua stock-out bersamaan tidak menghasilkan overselling |
| `PayloadHasherTest` | Hash payload idempotency stabil dan sensitif terhadap perubahan isi request |

## Endpoint Rilis Saat Ini

| Method | Path | Keterangan |
| --- | --- | --- |
| `POST` | `/api/v1/products` | Membuat produk |
| `GET` | `/api/v1/products` | Daftar produk (pagination) |
| `GET` | `/api/v1/products/{id}` | Detail produk |
| `POST` | `/api/v1/warehouses` | Membuat gudang |
| `GET` | `/api/v1/warehouses` | Daftar gudang (pagination) |
| `GET` | `/api/v1/warehouses/{id}` | Detail gudang |
| `POST` | `/api/v1/inventory/stock-in` | Menambah stok (wajib `Idempotency-Key`) |
| `POST` | `/api/v1/inventory/stock-out` | Mengurangi stok (wajib `Idempotency-Key`) |
| `GET` | `/api/v1/inventory/{productId}/{warehouseId}` | Stok per produk–gudang |
| `GET` | `/api/v1/inventory/transactions` | Histori transaksi stok |

### Contoh

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8080/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-001","name":"Beras Premium","unit":"KG"}' | jq -r '.data.id')

WAREHOUSE_ID=$(curl -s -X POST http://localhost:8080/api/v1/warehouses \
  -H 'Content-Type: application/json' \
  -d '{"code":"WH-JKT-01","name":"Gudang Jakarta","location":"Jakarta"}' | jq -r '.data.id')

curl -X POST http://localhost:8080/api/v1/inventory/stock-in \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"warehouseId\":\"$WAREHOUSE_ID\",\"quantity\":100,\"referenceId\":\"PO-2026-0001\"}"
```

## Desain Transaksi Database

**Batas transaksi database berada di application service, bukan di repository.** Satu
method service use case (`InventoryMutationService.applyStockIn`) mewakili satu transaksi dan
menulis seluruh tabel terkait secara atomik (`inventory`, `inventory_transactions`,
`outbox_events`). Orkestrator `InventoryService` tidak transaksional — pemisahan ini wajib agar
pemulihan balapan `Idempotency-Key` dapat membaca hasil transaksi pemenang setelah transaksi
yang gagal di-`rollback`.

```java
@ApplicationScoped
public class InventoryMutationService {

    @Transactional   // ← batas transaksi database untuk seluruh use case stock in
    public StockMutationResult applyStockIn(StockMutationCommand command) {
        // 1. Idempotency-Key diperiksa di dalam transaksi
        // 2. mutasi inventory lewat satu statement SQL atomik
        // 3. histori ke inventory_transactions
        // 4. event ke outbox_events (transaksi yang sama)
    }
}
```

Konsekuensinya: kegagalan pada langkah mana pun membatalkan seluruh transaksi — tidak
ada stok berubah tanpa histori, dan tidak ada event ditulis tanpa mutasi. Aturan lengkap
ada di [`docs/architecture-plan.md`](docs/architecture-plan.md) §4.3 dan
[`docs/fsd.md`](docs/fsd.md) §9.3.

## Struktur Package

```
src/main/java/com/stockflow/
├── common/       # api (envelope + trace id), constants, exception (DomainException, handler global)
├── product/      # master data produk (api/rest, api/dto, service, repository, entity, mapper)
├── warehouse/    # master data gudang (struktur sama dengan product)
├── inventory/    # mutasi stok & histori (service transaksional, repository, entity, mapper, constants, util)
└── outbox/       # transactional outbox (entity, repository, publisher di Phase 4)
```

Aturan dependensi antar-modul ada di `docs/architecture-plan.md` §4.2.
