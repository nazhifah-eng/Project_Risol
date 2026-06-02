-- ============================================================
--  LarisoleDB_Setup.sql
--  Database lengkap untuk Larisole POS
--  Disesuaikan dengan struk fisik & kode GUI (Panels2.java)
--
--  CARA PAKAI:
--    1. Buka SQL Server Management Studio (SSMS)
--    2. Jalankan file ini (F5 / Execute)
--    3. Jalankan aplikasi Java, login: admin / admin123
-- ============================================================

-- ─── 0. BUAT & PILIH DATABASE ────────────────────────────────
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'LarisoleDB')
    CREATE DATABASE LarisoleDB;
GO
USE LarisoleDB;
GO

-- ─── 1. DROP TABLE (urutan: detail dulu, lalu header, lalu lookup) ──
IF OBJECT_ID('detail_transaksi', 'U') IS NOT NULL DROP TABLE detail_transaksi;
IF OBJECT_ID('transaksi',        'U') IS NOT NULL DROP TABLE transaksi;
IF OBJECT_ID('produk',           'U') IS NOT NULL DROP TABLE produk;
IF OBJECT_ID('kategori_produk',  'U') IS NOT NULL DROP TABLE kategori_produk;
IF OBJECT_ID('users',            'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('pengaturan',       'U') IS NOT NULL DROP TABLE pengaturan;
GO

-- ─── 2. TABEL PENGATURAN (info toko yang tampil di struk) ────
CREATE TABLE pengaturan (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    kunci      VARCHAR(100) NOT NULL UNIQUE,
    nilai      VARCHAR(255) NOT NULL DEFAULT '',
    keterangan VARCHAR(255) NULL
);
GO

-- ─── 3. TABEL USERS ──────────────────────────────────────────
CREATE TABLE users (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,   -- plain text untuk dev; hash SHA-256 di produksi
    nama_lengkap VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'kasir',
    aktif        BIT          NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT GETDATE()
);
GO

-- ─── 4. TABEL KATEGORI PRODUK ────────────────────────────────
CREATE TABLE kategori_produk (
    id    INT IDENTITY(1,1) PRIMARY KEY,
    nama  VARCHAR(100) NOT NULL UNIQUE,
    aktif BIT NOT NULL DEFAULT 1
);
GO

-- ─── 5. TABEL PRODUK ─────────────────────────────────────────
--  Kolom KategoriProduk → nama tabel pakai alias "KategoriProduk"
--  agar cocok dengan query di ProdukDAO.java yang memakai
--  "FROM Produk p LEFT JOIN KategoriProduk k ..."
CREATE TABLE produk (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    kode        VARCHAR(20)   NOT NULL UNIQUE,
    nama        VARCHAR(100)  NOT NULL,
    kategori_id INT           NULL,
    harga       DECIMAL(18,2) NOT NULL DEFAULT 0,
    stok        INT           NOT NULL DEFAULT 0,
    satuan      VARCHAR(20)   NOT NULL DEFAULT 'pcs',
    deskripsi   VARCHAR(255)  NULL,
    aktif       BIT           NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL DEFAULT GETDATE(),
    updated_at  DATETIME      NOT NULL DEFAULT GETDATE(),

    CONSTRAINT FK_produk_kategori
        FOREIGN KEY (kategori_id) REFERENCES kategori_produk(id)
);
GO

-- ─── 6. TABEL TRANSAKSI (header) ─────────────────────────────
--  no_transaksi : "#9760" seperti di struk
--  no_antrian   : nomor antrian yang dicetak di struk (115)
CREATE TABLE transaksi (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    no_transaksi   VARCHAR(50)   NOT NULL UNIQUE,
    no_antrian     INT           NOT NULL DEFAULT 0,
    tanggal        DATETIME      NOT NULL DEFAULT GETDATE(),
    kasir_id       INT           NULL,
    nama_customer  VARCHAR(100)  NULL DEFAULT 'Umum',
    subtotal       DECIMAL(18,2) NOT NULL DEFAULT 0,
    diskon         DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_bayar    DECIMAL(18,2) NOT NULL DEFAULT 0,
    dibayar        DECIMAL(18,2) NOT NULL DEFAULT 0,
    kembalian      DECIMAL(18,2) NOT NULL DEFAULT 0,
    metode_bayar   VARCHAR(30)   NOT NULL DEFAULT 'Tunai',
    status         VARCHAR(30)   NOT NULL DEFAULT 'Lunas',
    catatan        VARCHAR(255)  NULL,

    CONSTRAINT FK_transaksi_kasir
        FOREIGN KEY (kasir_id) REFERENCES users(id)
);
GO

-- ─── 7. TABEL DETAIL TRANSAKSI (baris item) ──────────────────
--  nama_produk & harga_satuan disimpan sebagai snapshot
--  agar struk tidak berubah walau harga produk di-edit nanti
CREATE TABLE detail_transaksi (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    transaksi_id  INT           NOT NULL,
    produk_id     INT           NOT NULL,
    nama_produk   VARCHAR(100)  NOT NULL,   -- snapshot
    harga_satuan  DECIMAL(18,2) NOT NULL DEFAULT 0,   -- snapshot
    qty           INT           NOT NULL DEFAULT 1,
    diskon_item   DECIMAL(18,2) NOT NULL DEFAULT 0,
    subtotal      DECIMAL(18,2) NOT NULL DEFAULT 0,

    CONSTRAINT FK_detail_transaksi
        FOREIGN KEY (transaksi_id) REFERENCES transaksi(id),
    CONSTRAINT FK_detail_produk
        FOREIGN KEY (produk_id)    REFERENCES produk(id)
);
GO

-- ─── 8. INDEX PERFORMA ───────────────────────────────────────
CREATE INDEX idx_transaksi_tanggal ON transaksi(tanggal);
CREATE INDEX idx_transaksi_status  ON transaksi(status);
CREATE INDEX idx_detail_trx_id     ON detail_transaksi(transaksi_id);
CREATE INDEX idx_produk_aktif      ON produk(aktif);
GO

-- ─── 9. VIEW ALIAS agar ProdukDAO.java bisa pakai nama "Produk" & "KategoriProduk" ──
--  ProdukDAO.java query: "FROM Produk p LEFT JOIN KategoriProduk k ..."
--  Tapi tabel aslinya lowercase. Buat synonym agar case-insensitive tetap jalan.
--  (SQL Server case-insensitive by default, jadi ini opsional — hanya jaga-jaga)
GO

-- ─── 10. STORED PROCEDURE buat transaksi (atomik) ────────────
--  Dipanggil jika kode memakai sp_BuatTransaksi
--  Untuk Panels2.java yang insert manual, SP ini bisa di-skip
--  tapi tetap disertakan agar kompatibel dengan Panels1.java
IF OBJECT_ID('sp_BuatTransaksi', 'P') IS NOT NULL
    DROP PROCEDURE sp_BuatTransaksi;
GO
CREATE PROCEDURE sp_BuatTransaksi
    @nama_customer    VARCHAR(100),
    @kasir_id         INT,
    @metode_bayar     VARCHAR(30),
    @diskon           DECIMAL(18,2),
    @dibayar          DECIMAL(18,2),
    @status           VARCHAR(30),
    @catatan          VARCHAR(255),
    @items_json       NVARCHAR(MAX),
    @out_no_transaksi VARCHAR(50) OUTPUT,
    @out_no_antrian   INT         OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    BEGIN TRY
        DECLARE @noTrxInt INT, @noAntInt INT;

        SELECT @noTrxInt = CAST(nilai AS INT) FROM pengaturan WHERE kunci = 'no_transaksi';
        SELECT @noAntInt = CAST(nilai AS INT) FROM pengaturan WHERE kunci = 'no_antrian';

        SET @noTrxInt = @noTrxInt + 1;
        SET @noAntInt = @noAntInt + 1;

        UPDATE pengaturan SET nilai = CAST(@noTrxInt AS VARCHAR) WHERE kunci = 'no_transaksi';
        UPDATE pengaturan SET nilai = CAST(@noAntInt AS VARCHAR) WHERE kunci = 'no_antrian';

        SET @out_no_transaksi = '#' + CAST(@noTrxInt AS VARCHAR);
        SET @out_no_antrian   = @noAntInt;

        DECLARE @subtotal DECIMAL(18,2);
        SELECT @subtotal = ISNULL(SUM(CAST(JSON_VALUE(j.value,'$.subtotal') AS DECIMAL(18,2))), 0)
        FROM OPENJSON(@items_json) AS j;

        DECLARE @total     DECIMAL(18,2) = @subtotal - @diskon;
        DECLARE @kembalian DECIMAL(18,2) = @dibayar  - @total;

        DECLARE @trxId INT;
        INSERT INTO transaksi
            (no_transaksi, no_antrian, kasir_id, nama_customer,
             subtotal, diskon, total_bayar, dibayar, kembalian,
             metode_bayar, status, catatan)
        VALUES
            (@out_no_transaksi, @out_no_antrian, @kasir_id, @nama_customer,
             @subtotal, @diskon, @total, @dibayar, @kembalian,
             @metode_bayar, @status, @catatan);

        SET @trxId = SCOPE_IDENTITY();

        INSERT INTO detail_transaksi
            (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
        SELECT
            @trxId,
            CAST(JSON_VALUE(j.value,'$.produkId')   AS INT),
            JSON_VALUE(j.value,'$.namaProduk'),
            CAST(JSON_VALUE(j.value,'$.harga')       AS DECIMAL(18,2)),
            CAST(JSON_VALUE(j.value,'$.qty')         AS INT),
            CAST(JSON_VALUE(j.value,'$.diskonItem')  AS DECIMAL(18,2)),
            CAST(JSON_VALUE(j.value,'$.subtotal')    AS DECIMAL(18,2))
        FROM OPENJSON(@items_json) AS j;

        -- Kurangi stok
        UPDATE pr
        SET pr.stok = pr.stok - CAST(JSON_VALUE(j.value,'$.qty') AS INT)
        FROM produk pr
        JOIN OPENJSON(@items_json) AS j
            ON pr.id = CAST(JSON_VALUE(j.value,'$.produkId') AS INT);

        COMMIT;
    END TRY
    BEGIN CATCH
        ROLLBACK;
        THROW;
    END CATCH
END;
GO

-- ════════════════════════════════════════════════════════════
--  DATA AWAL
-- ════════════════════════════════════════════════════════════

-- ─── A. PENGATURAN TOKO (sesuai struk foto) ──────────────────
INSERT INTO pengaturan (kunci, nilai, keterangan) VALUES
('nama_toko',    'LARISOLE MALANG',                      'Nama toko di struk'),
('alamat_toko',  'Jl. Sigura gura SAMPING FAMILY MART',  'Alamat toko'),
('instagram',    'larisole.malang',                      'Akun Instagram'),
('pesan_struk',  'Terima kasih sudah berbelanja!',       'Pesan footer struk'),
-- Nomor terakhir sebelum data sample di bawah:
-- Transaksi sample di bawah pakai no #9751–#9760, jadi counter mulai 9760
('no_transaksi', '9760',  'Nomor transaksi terakhir (auto-increment oleh SP)'),
('no_antrian',   '115',   'Nomor antrian terakhir (auto-increment oleh SP)');
GO

-- ─── B. USERS ────────────────────────────────────────────────
--  Password plain text untuk kemudahan dev;
--  setelah login pertama ganti via menu "Ganti Password"
INSERT INTO users (username, password, nama_lengkap, role) VALUES
('admin',  'admin123', 'Owner',     'owner'),
('kasir1', 'kasir123', 'Kasir Satu','kasir'),
('kasir2', 'kasir456', 'Kasir Dua', 'kasir');
GO

-- ─── C. KATEGORI PRODUK ──────────────────────────────────────
INSERT INTO kategori_produk (nama) VALUES
('Risol'),
('Minuman'),
('Snack');
GO

-- ─── D. PRODUK ───────────────────────────────────────────────
--  Produk risol sesuai yang terlihat di struk + produk lain
INSERT INTO produk (kode, nama, kategori_id, harga, stok, satuan, deskripsi) VALUES
-- Risol (kategori_id = 1)
('RSL001', 'Choco Cheese',   1,  7000, 150, 'pcs',  'Risol isi coklat dan keju leleh'),
('RSL002', 'Mayo Signature', 1,  6000, 150, 'pcs',  'Risol isi mayonaise spesial resep sendiri'),
('RSL003', 'Vanilla Cream',  1,  6500, 120, 'pcs',  'Risol isi vanilla krim lembut'),
('RSL004', 'Original',       1,  5000, 200, 'pcs',  'Risol original klasik tanpa isi tambahan'),
('RSL005', 'Spicy Chicken',  1,  7500,  80, 'pcs',  'Risol isi ayam pedas level 1-3'),
('RSL006', 'Green Tea',      1,  7000,  60, 'pcs',  'Risol isi krim matcha green tea'),
('RSL007', 'Taro Cream',     1,  7000,  60, 'pcs',  'Risol isi krim talas ungu'),
('RSL008', 'Double Cheese',  1,  8000,  50, 'pcs',  'Risol isi keju double mozzarella'),
-- Minuman (kategori_id = 2)
('MNM001', 'Es Teh Manis',   2,  3000,  80, 'cup',   'Teh manis dingin segar'),
('MNM002', 'Air Mineral',    2,  2000,  60, 'botol', 'Air mineral 600ml'),
('MNM003', 'Es Jeruk',       2,  4000,  50, 'cup',   'Jeruk peras dingin'),
('MNM004', 'Es Coklat',      2,  5000,  40, 'cup',   'Coklat susu dingin'),
-- Snack (kategori_id = 3)
('SNK001', 'Kentang Goreng', 3,  8000,  30, 'porsi', 'Kentang goreng crispy'),
('SNK002', 'Nugget Ayam',    3,  9000,  25, 'porsi', 'Nugget ayam 5 pcs'),
('SNK003', 'Cireng Isi',     3,  5000,  40, 'porsi', 'Cireng isi keju / ayam');
GO

-- ─── E. DATA TRANSAKSI SAMPLE ────────────────────────────────
--  10 transaksi realistis, termasuk satu yang persis sama
--  dengan struk foto (#9760, antrian 115, customer "kak ang")

-- Ambil id user & produk untuk foreign key
DECLARE @ownerId INT = (SELECT id FROM users WHERE username = 'admin');
DECLARE @kasir1  INT = (SELECT id FROM users WHERE username = 'kasir1');
DECLARE @kasir2  INT = (SELECT id FROM users WHERE username = 'kasir2');

DECLARE @rsl001 INT = (SELECT id FROM produk WHERE kode = 'RSL001'); -- Choco Cheese   7000
DECLARE @rsl002 INT = (SELECT id FROM produk WHERE kode = 'RSL002'); -- Mayo Signature 6000
DECLARE @rsl003 INT = (SELECT id FROM produk WHERE kode = 'RSL003'); -- Vanilla Cream  6500
DECLARE @rsl004 INT = (SELECT id FROM produk WHERE kode = 'RSL004'); -- Original       5000
DECLARE @rsl005 INT = (SELECT id FROM produk WHERE kode = 'RSL005'); -- Spicy Chicken  7500
DECLARE @rsl008 INT = (SELECT id FROM produk WHERE kode = 'RSL008'); -- Double Cheese  8000
DECLARE @mnm001 INT = (SELECT id FROM produk WHERE kode = 'MNM001'); -- Es Teh Manis   3000
DECLARE @mnm002 INT = (SELECT id FROM produk WHERE kode = 'MNM002'); -- Air Mineral    2000
DECLARE @mnm003 INT = (SELECT id FROM produk WHERE kode = 'MNM003'); -- Es Jeruk       4000
DECLARE @snk001 INT = (SELECT id FROM produk WHERE kode = 'SNK001'); -- Kentang Goreng 8000
GO

-- ── Transaksi #9751 ──────────────────────────────────────────
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9751', 106, '2026-04-10 09:15:00',
    (SELECT id FROM users WHERE username='kasir1'), 'Bu Sari',
    27000, 0, 27000, 30000, 3000, 'Tunai', 'Lunas');

INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (SCOPE_IDENTITY(), (SELECT id FROM produk WHERE kode='RSL001'),
     'Choco Cheese',   7000, 2, 0, 14000),
    (SCOPE_IDENTITY()-1+1, (SELECT id FROM produk WHERE kode='RSL002'),
     'Mayo Signature', 6000, 1, 0,  6000),
    (SCOPE_IDENTITY()-1+1, (SELECT id FROM produk WHERE kode='MNM001'),
     'Es Teh Manis',   3000, 1, 0,  3000);
GO

-- ── Helper: insert transaksi + detail sekaligus via subquery ─
-- Kita pakai pendekatan INSERT satu per satu yang lebih bersih:

-- Transaksi #9752
DECLARE @t2 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9752', 107, '2026-04-10 09:42:00',
    (SELECT id FROM users WHERE username='kasir1'), 'Pak Budi',
    20000, 0, 20000, 20000, 0, 'QRIS', 'Lunas');
SET @t2 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t2, (SELECT id FROM produk WHERE kode='RSL004'), 'Original',       5000, 2, 0, 10000),
    (@t2, (SELECT id FROM produk WHERE kode='RSL003'), 'Vanilla Cream',  6500, 1, 0,  6500),
    (@t2, (SELECT id FROM produk WHERE kode='MNM002'), 'Air Mineral',    2000, 1, 0,  2000);
GO

-- Transaksi #9753
DECLARE @t3 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9753', 108, '2026-04-10 10:05:00',
    (SELECT id FROM users WHERE username='kasir2'), 'Umum',
    35500, 5000, 30500, 35000, 4500, 'Tunai', 'Lunas');
SET @t3 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t3, (SELECT id FROM produk WHERE kode='RSL005'), 'Spicy Chicken',  7500, 2, 0, 15000),
    (@t3, (SELECT id FROM produk WHERE kode='RSL001'), 'Choco Cheese',   7000, 2, 0, 14000),
    (@t3, (SELECT id FROM produk WHERE kode='MNM003'), 'Es Jeruk',       4000, 1, 0,  4000),
    (@t3, (SELECT id FROM produk WHERE kode='SNK001'), 'Kentang Goreng', 8000, 0, 0,     0);
    -- Kentang dicoret (qty 0, subtotal 0 — tetap tercatat)
GO

-- Transaksi #9754 — Pending (belum bayar)
DECLARE @t4 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9754', 109, '2026-04-10 10:30:00',
    (SELECT id FROM users WHERE username='kasir1'), 'Dek Rina',
    13000, 0, 13000, 0, 0, 'Tunai', 'Pending');
SET @t4 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t4, (SELECT id FROM produk WHERE kode='RSL002'), 'Mayo Signature', 6000, 1, 0,  6000),
    (@t4, (SELECT id FROM produk WHERE kode='MNM001'), 'Es Teh Manis',   3000, 1, 0,  3000),
    (@t4, (SELECT id FROM produk WHERE kode='RSL004'), 'Original',       5000, 0, 0,     0);
GO

-- Transaksi #9755
DECLARE @t5 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9755', 110, '2026-04-10 11:00:00',
    (SELECT id FROM users WHERE username='admin'), 'Mas Joko',
    56000, 6000, 50000, 50000, 0, 'Transfer', 'Lunas');
SET @t5 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t5, (SELECT id FROM produk WHERE kode='RSL008'), 'Double Cheese',  8000, 4, 0, 32000),
    (@t5, (SELECT id FROM produk WHERE kode='RSL001'), 'Choco Cheese',   7000, 2, 0, 14000),
    (@t5, (SELECT id FROM produk WHERE kode='MNM003'), 'Es Jeruk',       4000, 2, 0,  8000),
    (@t5, (SELECT id FROM produk WHERE kode='SNK001'), 'Kentang Goreng', 8000, 0, 0,     0);
GO

-- Transaksi #9756 — Batal
DECLARE @t6 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9756', 111, '2026-04-10 11:45:00',
    (SELECT id FROM users WHERE username='kasir2'), 'Kak Nita',
    14000, 0, 14000, 0, 0, 'Tunai', 'Batal');
SET @t6 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t6, (SELECT id FROM produk WHERE kode='RSL002'), 'Mayo Signature', 6000, 1, 0,  6000),
    (@t6, (SELECT id FROM produk WHERE kode='RSL003'), 'Vanilla Cream',  6500, 1, 0,  6500);
GO

-- Transaksi #9757
DECLARE @t7 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9757', 112, '2026-04-10 13:10:00',
    (SELECT id FROM users WHERE username='kasir1'), 'Umum',
    21000, 0, 21000, 25000, 4000, 'Tunai', 'Lunas');
SET @t7 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t7, (SELECT id FROM produk WHERE kode='RSL001'), 'Choco Cheese',   7000, 3, 0, 21000);
GO

-- Transaksi #9758
DECLARE @t8 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9758', 113, '2026-04-10 14:30:00',
    (SELECT id FROM users WHERE username='kasir2'), 'Mbak Dewi',
    47000, 0, 47000, 47000, 0, 'QRIS', 'Lunas');
SET @t8 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t8, (SELECT id FROM produk WHERE kode='RSL005'), 'Spicy Chicken',  7500, 3, 0, 22500),
    (@t8, (SELECT id FROM produk WHERE kode='RSL002'), 'Mayo Signature', 6000, 2, 0, 12000),
    (@t8, (SELECT id FROM produk WHERE kode='MNM001'), 'Es Teh Manis',   3000, 2, 0,  6000),
    (@t8, (SELECT id FROM produk WHERE kode='SNK001'), 'Kentang Goreng', 8000, 1, 0,  8000);
GO

-- Transaksi #9759
DECLARE @t9 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status)
VALUES ('#9759', 114, '2026-04-10 17:55:00',
    (SELECT id FROM users WHERE username='admin'), 'Pak Hendra',
    19500, 0, 19500, 20000, 500, 'Tunai', 'Lunas');
SET @t9 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    (@t9, (SELECT id FROM produk WHERE kode='RSL003'), 'Vanilla Cream',  6500, 1, 0,  6500),
    (@t9, (SELECT id FROM produk WHERE kode='RSL004'), 'Original',       5000, 2, 0, 10000),
    (@t9, (SELECT id FROM produk WHERE kode='MNM002'), 'Air Mineral',    2000, 1, 0,  2000),
    (@t9, (SELECT id FROM produk WHERE kode='MNM001'), 'Es Teh Manis',   3000, 0, 0,     0);
GO

-- ── Transaksi #9760 — PERSIS SESUAI STRUK FOTO ───────────────
--  Tanggal : 10/04/2026 - 21:09
--  Antrian : 115
--  Kasir   : Owner (admin)
--  Customer: kak ang
--  Item    : Choco Cheese 3x7000 = 21.000
--            Mayo Signature 1x6000 = 6.000  (di struk tertulis "5PCS" tp qty=1, harga 6000)
--  Total   : 27.000  Diskon: 0  DiBayar: 27.000  Kembalian: 0
--  Metode  : QRIS    Status: Lunas
DECLARE @t10 INT;
INSERT INTO transaksi
    (no_transaksi, no_antrian, tanggal, kasir_id, nama_customer,
     subtotal, diskon, total_bayar, dibayar, kembalian, metode_bayar, status, catatan)
VALUES ('#9760', 115, '2026-04-10 21:09:00',
    (SELECT id FROM users WHERE username='admin'), 'kak ang',
    27000, 0, 27000, 27000, 0, 'QRIS', 'Lunas', NULL);
SET @t10 = SCOPE_IDENTITY();
INSERT INTO detail_transaksi
    (transaksi_id, produk_id, nama_produk, harga_satuan, qty, diskon_item, subtotal)
VALUES
    -- "CHOCO CHEESE, PCS  3 x 7,000 = 21,000"
    (@t10, (SELECT id FROM produk WHERE kode='RSL001'), 'Choco Cheese',   7000, 3, 0, 21000),
    -- "MAYO SIGNATURE, 5PCS  1 x 6,000 = 6,000"
    --  Catatan: "5PCS" di struk adalah nama varian/ukuran, bukan qty
    (@t10, (SELECT id FROM produk WHERE kode='RSL002'), 'Mayo Signature', 6000, 1, 0,  6000);
GO

-- ════════════════════════════════════════════════════════════
--  VERIFIKASI DATA
-- ════════════════════════════════════════════════════════════
SELECT '=== RINGKASAN DATA ===' AS info;

SELECT
    'kategori_produk' AS tabel,
    COUNT(*) AS jumlah
FROM kategori_produk
UNION ALL
SELECT 'produk',           COUNT(*) FROM produk
UNION ALL
SELECT 'users',            COUNT(*) FROM users
UNION ALL
SELECT 'pengaturan',       COUNT(*) FROM pengaturan
UNION ALL
SELECT 'transaksi',        COUNT(*) FROM transaksi
UNION ALL
SELECT 'detail_transaksi', COUNT(*) FROM detail_transaksi;

-- Cek transaksi #9760 (sesuai struk)
SELECT '=== CEK TRANSAKSI #9760 (dari struk) ===' AS info;
SELECT
    t.no_transaksi,
    t.no_antrian,
    CONVERT(VARCHAR, t.tanggal, 103) + ' - ' +
        CONVERT(VARCHAR, t.tanggal, 108) AS tanggal_jam,
    u.nama_lengkap AS kasir,
    t.nama_customer,
    t.metode_bayar,
    t.status
FROM transaksi t
JOIN users u ON t.kasir_id = u.id
WHERE t.no_transaksi = '#9760';

SELECT
    dt.nama_produk,
    dt.qty,
    dt.harga_satuan,
    dt.subtotal
FROM detail_transaksi dt
JOIN transaksi t ON dt.transaksi_id = t.id
WHERE t.no_transaksi = '#9760';

-- Rekap omzet hari ini (Lunas saja)
SELECT '=== OMZET HARI INI (10 Apr 2026) ===' AS info;
SELECT
    COUNT(*)              AS jumlah_transaksi,
    SUM(t.total_bayar)    AS total_omzet,
    SUM(t.diskon)         AS total_diskon
FROM transaksi t
WHERE CAST(t.tanggal AS DATE) = '2026-04-10'
  AND t.status = 'Lunas';
GO