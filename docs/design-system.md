StockFlow — Inventory Management Backend System
1. Project Overview

StockFlow adalah backend system untuk mengelola persediaan barang (inventory management) menggunakan pendekatan REST API dan event-driven architecture.

Sistem dibangun menggunakan Quarkus sebagai backend framework, PostgreSQL sebagai database utama, dan Apache Kafka sebagai event streaming platform untuk mengirimkan perubahan stok ke sistem lain secara asynchronous.

Technology Stack
Teknologi	Fungsi
Java 21+	Bahasa pemrograman
Quarkus	Backend framework
RESTEasy Reactive / Quarkus REST	REST API
Hibernate ORM with Panache	ORM dan akses database
PostgreSQL	Database relasional
Apache Kafka	Event streaming dan asynchronous messaging
SmallRye Reactive Messaging	Integrasi Kafka dengan Quarkus
Flyway	Database migration
Bean Validation	Validasi request
JUnit 5	Unit testing
Testcontainers	Integration testing
Docker Compose	Local development environment
Catatan: Gunakan versi Java dan dependency yang kompatibel dengan versi Quarkus yang dipilih. Gunakan Quarkus BOM untuk mengelola versi dependency.
2. Project Objectives

Tujuan utama StockFlow:

Mengelola data produk dan kategori barang.
Mengelola stok berdasarkan produk dan gudang.
Mencatat setiap perubahan stok sebagai inventory transaction.
Mencegah stok menjadi negatif.
Menyediakan REST API untuk membaca dan mengubah data stok.
Menerbitkan event perubahan stok melalui Kafka.
Mendukung integrasi dengan sistem eksternal.
Menyediakan audit trail untuk setiap mutasi stok.
Scope of Initial Release

Versi pertama berfokus pada:

Product Management
Warehouse Management
Inventory Management
Stock Adjustment
Stock In dan Stock Out
Inventory Transaction History
Kafka Event Publishing
Basic Monitoring dan Health Check

Fitur purchase order, supplier management, reservation, dan stock transfer antar-gudang dapat ditambahkan pada fase berikutnya.

3. System Architecture

StockFlow menggunakan pendekatan modular monolith dengan event-driven integration.

Pada tahap awal, business logic tetap berada dalam satu aplikasi Quarkus. Kafka digunakan untuk mendistribusikan event ke sistem lain tanpa membuat semua proses bisnis bergantung pada komunikasi synchronous.

flowchart TB
CLIENT[Client / Frontend]
API[Quarkus REST API]
APP[Application Services]
DOMAIN[Domain Logic]
DB[(PostgreSQL)]
OUTBOX[(Outbox Events)]
RELAY[Outbox Publisher]
KAFKA[Apache Kafka]
CONSUMER[External Consumers]

    CLIENT --> API
    API --> APP
    APP --> DOMAIN
    DOMAIN --> DB
    DOMAIN --> OUTBOX
    OUTBOX --> RELAY
    RELAY --> KAFKA
    KAFKA --> CONSUMER

Component Responsibilities
Component	Responsibility
REST API	Menerima request HTTP dan mengembalikan response
Application Services	Mengorkestrasi use case bisnis
Domain Logic	Menjalankan aturan inventory dan validasi stok
PostgreSQL	Menyimpan data utama secara durable
Outbox Events	Menyimpan event yang menunggu dipublikasikan
Outbox Publisher	Mengirim event dari outbox ke Kafka
Kafka	Mendistribusikan event ke consumer
External Consumers	Memproses event untuk kebutuhan sistem lain
Architectural Principles
PostgreSQL adalah sumber kebenaran utama (source of truth) untuk stok.
Perubahan stok dan pencatatan transaksi dilakukan dalam satu database transaction.
Event yang harus dipublikasikan dicatat dalam tabel outbox pada transaksi yang sama.
Kafka digunakan untuk integrasi asynchronous, bukan sebagai pengganti database utama.
Consumer harus dirancang agar idempotent.
API menggunakan DTO dan tidak mengekspos entity database secara langsung.
4. Suggested Project Structure

Struktur package menggunakan pendekatan modular berdasarkan domain bisnis.

stockflow-service/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── README.md
├── PROJECT_DESIGN.md
├── src/
│   ├── main/
│   │   ├── java/com/stockflow/
│   │   │   ├── common/
│   │   │   │   ├── exception/
│   │   │   │   ├── response/
│   │   │   │   └── validation/
│   │   │   ├── product/
│   │   │   │   ├── api/
│   │   │   │   ├── application/
│   │   │   │   ├── domain/
│   │   │   │   └── infrastructure/
│   │   │   ├── warehouse/
│   │   │   │   ├── api/
│   │   │   │   ├── application/
│   │   │   │   ├── domain/
│   │   │   │   └── infrastructure/
│   │   │   ├── inventory/
│   │   │   │   ├── api/
│   │   │   │   ├── application/
│   │   │   │   ├── domain/
│   │   │   │   └── infrastructure/
│   │   │   ├── transaction/
│   │   │   ├── messaging/
│   │   │   │   ├── event/
│   │   │   │   ├── publisher/
│   │   │   │   └── consumer/
│   │   │   └── outbox/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           ├── V1__create_products.sql
│   │           ├── V2__create_warehouses.sql
│   │           ├── V3__create_inventory.sql
│   │           ├── V4__create_inventory_transactions.sql
│   │           └── V5__create_outbox_events.sql
│   └── test/
│       └── java/com/stockflow/
│           ├── product/
│           ├── inventory/
│           ├── transaction/
│           └── messaging/
└── deploy/
└── docker/

Package Responsibilities
api: REST resources, request DTO, response DTO.
application: use cases dan orchestration.
domain: domain models, business rules, dan domain services.
infrastructure: repository implementations dan integrasi persistence.
messaging: Kafka producer, consumer, event schema, dan konfigurasi messaging.
outbox: penyimpanan event dan mekanisme publikasi.
common: komponen lintas domain, exception, dan response standar.

Untuk implementasi Panache, repository dapat menggunakan PanacheRepository atau PanacheRepositoryBase, sementara application service bertanggung jawab atas transaksi bisnis.

5. Database Design

Database awal terdiri dari lima tabel utama: products, warehouses, inventory, inventory_transactions, dan outbox_events.

5.1 Products

Menyimpan master data barang.

Column	Type	Description
id	UUID	Primary key
sku	VARCHAR(100)	Kode unik barang
name	VARCHAR(255)	Nama barang
description	TEXT	Deskripsi barang
unit	VARCHAR(30)	Satuan barang
active	BOOLEAN	Status aktif
created_at	TIMESTAMPTZ	Waktu pembuatan
updated_at	TIMESTAMPTZ	Waktu pembaruan

Constraint: sku harus unik.

5.2 Warehouses

Menyimpan informasi gudang.

Column	Type	Description
id	UUID	Primary key
code	VARCHAR(50)	Kode gudang
name	VARCHAR(255)	Nama gudang
location	TEXT	Lokasi gudang
active	BOOLEAN	Status aktif
created_at	TIMESTAMPTZ	Waktu pembuatan
updated_at	TIMESTAMPTZ	Waktu pembaruan

Constraint: code harus unik.

5.3 Inventory

Menyimpan jumlah stok untuk setiap kombinasi produk dan gudang.

Column	Type	Description
id	UUID	Primary key
product_id	UUID	Foreign key ke products
warehouse_id	UUID	Foreign key ke warehouses
quantity	BIGINT	Jumlah stok tersedia
version	BIGINT	Versi data untuk kebutuhan concurrency
updated_at	TIMESTAMPTZ	Waktu pembaruan

Constraint:

UNIQUE(product_id, warehouse_id)
quantity >= 0

Gunakan BIGINT untuk jumlah stok apabila barang dihitung dalam unit utuh. Jika mendukung berat atau volume pecahan, tentukan precision numerik yang sesuai.

5.4 Inventory Transactions

Menyimpan histori perubahan stok.

Column	Type	Description
id	UUID	Primary key
product_id	UUID	Produk terkait
warehouse_id	UUID	Gudang terkait
transaction_type	VARCHAR(30)	IN, OUT, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT
quantity_delta	BIGINT	Perubahan stok bertanda positif atau negatif
quantity_before	BIGINT	Stok sebelum perubahan
quantity_after	BIGINT	Stok sesudah perubahan
reference_id	VARCHAR(100)	Referensi proses eksternal
idempotency_key	VARCHAR(150)	Identitas request unik
notes	TEXT	Keterangan transaksi
created_at	TIMESTAMPTZ	Waktu transaksi

Constraint dan index yang disarankan:

Foreign key ke produk dan gudang.
Unique constraint pada idempotency_key untuk mencegah transaksi ganda.
Index pada (product_id, created_at).
Index pada (warehouse_id, created_at).
Index pada reference_id jika sering digunakan untuk pencarian.

Kolom quantity_delta harus konsisten dengan quantity_before dan quantity_after. Transaksi tidak boleh diubah secara sembarangan setelah tercatat; koreksi dilakukan melalui transaksi baru.

5.5 Outbox Events

Menyimpan event yang akan dipublikasikan ke Kafka.

Column	Type	Description
id	UUID	Event ID
aggregate_type	VARCHAR(100)	Jenis aggregate
aggregate_id	UUID	ID aggregate terkait
event_type	VARCHAR(150)	Jenis event
payload	JSONB	Payload event
created_at	TIMESTAMPTZ	Waktu event dibuat
published_at	TIMESTAMPTZ	Waktu event berhasil dipublikasikan
attempts	INTEGER	Jumlah percobaan publikasi

Index yang disarankan:

Index pada created_at.
Index parsial untuk event yang belum dipublikasikan.
Mekanisme retry dan monitoring untuk event yang gagal.
Entity Relationship
erDiagram
PRODUCTS ||--o{ INVENTORY : has
WAREHOUSES ||--o{ INVENTORY : stores
PRODUCTS ||--o{ INVENTORY_TRANSACTIONS : records
WAREHOUSES ||--o{ INVENTORY_TRANSACTIONS : records
INVENTORY ||--o{ OUTBOX_EVENTS : emits

    PRODUCTS {
        uuid id PK
        string sku UK
        string name
    }
    WAREHOUSES {
        uuid id PK
        string code UK
        string name
    }
    INVENTORY {
        uuid id PK
        uuid product_id FK
        uuid warehouse_id FK
        bigint quantity
        bigint version
    }
    INVENTORY_TRANSACTIONS {
        uuid id PK
        uuid product_id FK
        uuid warehouse_id FK
        string transaction_type
        bigint quantity_delta
    }
    OUTBOX_EVENTS {
        uuid id PK
        string event_type
        jsonb payload
        timestamp published_at
    }


Catatan: Relasi outbox terhadap inventory pada diagram bersifat konseptual. Pada implementasi, outbox menggunakan aggregate_type dan aggregate_id sehingga dapat mencatat event dari berbagai domain.

6. REST API Design

Base path:

/api/v1

Product API
Method	Endpoint	Description
POST	/products	Membuat produk
GET	/products	Mendapatkan daftar produk
GET	/products/{id}	Detail produk
PUT	/products/{id}	Memperbarui produk
PATCH	/products/{id}/status	Mengubah status produk
Warehouse API
Method	Endpoint	Description
POST	/warehouses	Membuat gudang
GET	/warehouses	Daftar gudang
GET	/warehouses/{id}	Detail gudang
PUT	/warehouses/{id}	Memperbarui gudang
Inventory API
Method	Endpoint	Description
GET	/inventory	Daftar stok
GET	/inventory/{productId}/{warehouseId}	Stok per produk dan gudang
POST	/inventory/stock-in	Menambah stok
POST	/inventory/stock-out	Mengurangi stok
POST	/inventory/adjustments	Penyesuaian stok
GET	/inventory/transactions	Histori transaksi
POST	/inventory/transfers	Transfer stok antar-gudang

Untuk stock-in, stock-out, dan adjustments, gunakan idempotency key pada request agar retry dari client tidak menyebabkan perubahan stok ganda.

Example: Stock In Request

POST /api/v1/inventory/stock-in

{
"productId": "c9f0a7d2-8c8d-4f25-a4a2-1e2d9b2c3f11",
"warehouseId": "a1d4f8c2-2b9e-4a3d-8f5c-6e7a9b0c1d22",
"quantity": 100,
"referenceId": "PO-2026-0001",
"notes": "Penerimaan barang dari supplier"
}


Header:

Idempotency-Key: 8b56c9c1-unique-request-key

Example: Success Response
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


ID pada contoh merupakan placeholder untuk ilustrasi kontrak API.

HTTP Status Codes
200 OK: operasi berhasil.
201 Created: resource berhasil dibuat.
400 Bad Request: request tidak valid.
404 Not Found: resource tidak ditemukan.
409 Conflict: stok tidak cukup atau terjadi konflik bisnis.
422 Unprocessable Entity: aturan bisnis tidak terpenuhi, jika digunakan secara konsisten.
500 Internal Server Error: kesalahan yang tidak tertangani.
7. Inventory Business Rules
   Stock In
   Validasi produk dan gudang.
   Validasi jumlah harus lebih besar dari nol.
   Mulai database transaction.
   Ambil atau buat record inventory secara aman.
   Tambahkan jumlah stok.
   Simpan inventory transaction.
   Simpan event ke outbox.
   Commit transaksi.
   Stock Out
   Validasi produk dan gudang.
   Validasi jumlah harus lebih besar dari nol.
   Mulai database transaction.
   Lock record inventory atau gunakan atomic conditional update.
   Pastikan stok mencukupi.
   Kurangi stok.
   Simpan inventory transaction.
   Simpan event ke outbox.
   Commit transaksi.
   Concurrency Control

Gunakan salah satu pendekatan berikut:

Pessimistic locking melalui SELECT ... FOR UPDATE.
Atomic update, misalnya UPDATE inventory SET quantity = quantity - :qty WHERE product_id = :productId AND warehouse_id = :warehouseId AND quantity >= :qty, kemudian validasi jumlah row yang diperbarui.
Optimistic locking menggunakan kolom versi, jika sesuai dengan pola akses.

Untuk operasi stok, conditional update atau pessimistic locking sering lebih mudah untuk memastikan tidak terjadi overselling akibat request bersamaan.

Idempotency

Setiap operasi mutasi stok harus memiliki identitas request unik.

Jika request yang sama dikirim ulang, sistem mengembalikan hasil transaksi yang sudah ada, bukan menjalankan perubahan stok sekali lagi.

Gunakan unique constraint di database sebagai pengaman utama, bukan hanya pemeriksaan di level aplikasi.

8. Kafka Event Design

Kafka digunakan untuk memberitahukan sistem lain ketika stok berubah.

Suggested Topics
Topic	Purpose
stockflow.inventory.stock-in.v1	Event penambahan stok
stockflow.inventory.stock-out.v1	Event pengurangan stok
stockflow.inventory.adjusted.v1	Event penyesuaian stok
stockflow.inventory.transferred.v1	Event transfer stok

Pada tahap awal, seluruh event inventory juga dapat menggunakan satu topic stockflow.inventory.events.v1 dengan eventType sebagai discriminator. Pemisahan topic dilakukan ketika kebutuhan operasional dan consumer sudah jelas.

Event Schema

Contoh payload StockInCompleted:

{
"eventId": "b6e7a4d1-2c8f-4a5b-9d3e-7f1a2b3c4d5e",
"eventType": "StockInCompleted",
"eventVersion": 1,
"occurredAt": "2026-10-16T14:00:00Z",
"aggregateId": "c9f0a7d2-8c8d-4f25-a4a2-1e2d9b2c3f11",
"productId": "c9f0a7d2-8c8d-4f25-a4a2-1e2d9b2c3f11",
"warehouseId": "a1d4f8c2-2b9e-4a3d-8f5c-6e7a9b0c1d22",
"quantityBefore": 50,
"quantityChanged": 100,
"quantityAfter": 150,
"transactionId": "f4b4d88d-5b47-4f67-bc6d-1a2b3c4d5e6f",
"referenceId": "PO-2026-0001"
}


Gunakan UUID yang berbeda untuk eventId dan transactionId. aggregateId sebaiknya merepresentasikan identitas aggregate yang konsisten; jika aggregate inventory adalah kombinasi produk dan gudang, tentukan representasi identitasnya secara eksplisit.

Kafka Message Key

Gunakan key yang konsisten, misalnya:

{productId}:{warehouseId}

Dengan demikian, event untuk kombinasi produk dan gudang yang sama dapat dipartisi secara konsisten, sehingga membantu menjaga urutan event dalam partition.

Delivery Semantics
Outbox mengurangi risiko perubahan database berhasil tetapi event hilang.
Publisher dapat mengirim event lebih dari satu kali ketika terjadi retry.
Consumer harus idempotent berdasarkan eventId.
Gunakan retry dengan backoff dan dead-letter topic untuk kegagalan permanen.
Jangan mengasumsikan urutan global antar-partition.
Gunakan schema versioning untuk evolusi event.

Outbox dengan publisher biasa memberikan pola publikasi yang umumnya bersifat at-least-once. Jika menggunakan CDC seperti Debezium, outbox dapat dipublikasikan berdasarkan perubahan database. Pilih satu mekanisme publikasi yang jelas dan hindari menjalankan dua publisher untuk event yang sama.

9. Quarkus Configuration

Contoh konfigurasi dasar pada application.properties:

quarkus.application.name=stockflow-service

# PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME:stockflow}
quarkus.datasource.password=${DB_PASSWORD:stockflow}
quarkus.datasource.jdbc.url=${DB_JDBC_URL:jdbc:postgresql://localhost:5432/stockflow}

# Hibernate ORM
quarkus.hibernate-orm.schema-management.strategy=validate

# Flyway
quarkus.flyway.migrate-at-start=true

# Kafka
kafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

# Health and observability
quarkus.smallrye-health.root-path=/q/health
quarkus.micrometer.export.prometheus.enabled=true


Contoh konfigurasi ini menggunakan JDBC untuk persistence dan membutuhkan dependency yang sesuai. Untuk deployment produksi, atur kredensial melalui secret manager atau environment yang aman.

Untuk Kafka, konfigurasi channel producer dan consumer sebaiknya didefinisikan secara eksplisit menggunakan SmallRye Reactive Messaging, misalnya dengan mp.messaging.outgoing dan mp.messaging.incoming, sesuai kebutuhan masing-masing channel.

Suggested Quarkus Extensions
quarkus-rest-jackson
quarkus-hibernate-orm-panache
quarkus-jdbc-postgresql
quarkus-flyway
quarkus-hibernate-validator
quarkus-smallrye-reactive-messaging-kafka
quarkus-smallrye-health
quarkus-micrometer-registry-prometheus
quarkus-junit
quarkus-test-security jika pengujian keamanan diperlukan

Pastikan extension test diletakkan pada scope yang tepat dan nama extension diverifikasi terhadap versi Quarkus yang digunakan.

10. Transaction and Outbox Workflow
    sequenceDiagram
    participant C as Client
    participant API as Quarkus API
    participant DB as PostgreSQL
    participant OB as Outbox Publisher
    participant K as Kafka

    C->>API: POST stock-in
    API->>DB: Begin transaction
    API->>DB: Update inventory
    API->>DB: Insert inventory transaction
    API->>DB: Insert outbox event
    API->>DB: Commit transaction
    API-->>C: 201 Created
    OB->>DB: Read unpublished events
    OB->>K: Publish event
    K-->>OB: Broker acknowledgment
    OB->>DB: Mark event as published


Pembaruan inventory, penyimpanan histori, dan penulisan outbox harus berada dalam satu transaksi PostgreSQL.

Publikasi ke Kafka berlangsung setelah transaksi database berhasil. Jika proses publikasi gagal, event tetap berada di outbox dan dapat dicoba kembali.

Jika menggunakan beberapa publisher, tambahkan mekanisme klaim event yang aman, lease atau locking, dan kebijakan retry agar event tidak diproses secara bersamaan tanpa kontrol.

11. Security

Untuk deployment produksi, tambahkan:

Authentication menggunakan OAuth2/OIDC.
Role-based authorization.
Role seperti INVENTORY_READ, INVENTORY_WRITE, dan WAREHOUSE_ADMIN.
Validasi input dan batas ukuran request.
TLS untuk koneksi database dan Kafka.
Kafka ACL untuk membatasi akses producer dan consumer.
Secret management untuk kredensial.
Audit log untuk operasi sensitif.

Jangan menyimpan password atau secret produksi langsung di source code maupun file konfigurasi yang masuk ke repository.

12. Testing Strategy
    Unit Tests

Uji business rules tanpa bergantung pada database atau Kafka.

Contoh:

Stock in menambah jumlah stok.
Stock out mengurangi jumlah stok.
Stock out ditolak ketika stok tidak cukup.
Quantity nol atau negatif ditolak.
Produk atau gudang tidak ditemukan.
Request dengan idempotency key yang sama tidak menggandakan transaksi.
Integration Tests

Gunakan PostgreSQL dan Kafka melalui Testcontainers untuk menguji:

Repository dan constraint database.
Transaction rollback.
Concurrent stock-out requests.
Penerbitan event dari outbox.
Retry publisher.
Idempotensi consumer.
Kegagalan koneksi Kafka.
API Tests

Uji status HTTP, request validation, response schema, pagination, dan error response.

13. Observability

Metrik yang disarankan:

Jumlah request dan error HTTP.
Latency endpoint.
Jumlah stock-in dan stock-out.
Jumlah konflik stok.
Jumlah event outbox yang menunggu publikasi.
Umur event tertua yang belum dipublikasikan.
Kafka publish failure dan retry count.
Consumer lag jika aplikasi memiliki consumer sendiri.

Gunakan health check untuk database dan Kafka sesuai kebutuhan readiness aplikasi. Tambahkan correlation ID atau trace ID agar request API dapat ditelusuri hingga event Kafka.

14. Development Roadmap
    Phase 1 — Foundation
    Inisialisasi project Quarkus.
    Setup PostgreSQL, Docker Compose, dan Flyway.
    Implementasi exception handling dan response standar.
    Setup unit testing.
    Phase 2 — Master Data
    CRUD produk.
    CRUD gudang.
    Validasi dan database constraints.
    Phase 3 — Inventory Core
    Stock-in dan stock-out.
    Inventory transaction history.
    Concurrency control.
    Idempotency.
    Stock adjustment.
    Phase 4 — Kafka Integration
    Definisi event schema.
    Implementasi transactional outbox.
    Publisher ke Kafka.
    Consumer idempotent jika diperlukan.
    Retry dan dead-letter handling.
    Phase 5 — Production Readiness
    Authentication dan authorization.
    Observability.
    Integration testing.
    CI/CD.
    Deployment dan backup strategy.
15. Definition of Done

Project dianggap siap untuk rilis awal apabila:

Semua endpoint utama memiliki validasi.
Stok tidak dapat menjadi negatif melalui request bersamaan.
Setiap mutasi stok tercatat dalam histori transaksi.
Request berulang tidak menggandakan mutasi stok.
Perubahan stok dan outbox event tersimpan secara atomik.
Event dapat dipublikasikan ulang ketika terjadi kegagalan.
Consumer yang relevan mendukung idempotensi.
Unit test dan integration test berjalan.
Database migration dapat dijalankan dari lingkungan baru.
Health check dan metrik tersedia.
Konfigurasi produksi tidak menyimpan secret di source code.
16. Future Enhancements

Fitur yang dapat ditambahkan setelah versi awal:

Stock reservation untuk checkout atau order processing.
Multi-warehouse transfer dengan transaksi yang terkontrol.
Reorder point dan low-stock alert.
Supplier dan purchase order management.
Batch dan expiry date tracking.
Stock opname dan reconciliation.
Audit reporting.
Schema Registry dan Avro/Protobuf.
CDC menggunakan Debezium untuk publikasi outbox.
Kubernetes deployment dan autoscaling.
17. Final Recommendation

Gunakan nama StockFlow dengan artifact ID stockflow-service.

Mulailah dengan modular monolith berbasis Quarkus, PostgreSQL sebagai sumber data utama, dan Apache Kafka untuk integrasi event-driven. Terapkan transaksi database yang aman, idempotency, inventory history, serta transactional outbox sejak awal.

Pendekatan ini memberikan fondasi yang relatif sederhana untuk dikembangkan, tetapi tetap mendukung integrasi dengan order management, procurement, reporting, dan sistem eksternal di masa depan.