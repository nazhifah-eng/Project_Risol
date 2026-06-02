CREATE DATABASE LarisoleDB;
GO

USE LarisoleDB;
GO

CREATE TABLE kategori_produk (
    id    INT IDENTITY(1,1) PRIMARY KEY,
    nama  VARCHAR(100) NOT NULL UNIQUE,
    aktif BIT NOT NULL DEFAULT 1
);
GO


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


CREATE TABLE users (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    nama_lengkap VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'kasir',
    aktif        BIT          NOT NULL DEFAULT 1,
    created_at   DATETIME     NOT NULL DEFAULT GETDATE()
);
GO


CREATE TABLE pengaturan (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    kunci      VARCHAR(100) NOT NULL UNIQUE,
    nilai      VARCHAR(255) NOT NULL DEFAULT '',
    keterangan VARCHAR(255) NULL
);
GO

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

CREATE TABLE detail_transaksi (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    transaksi_id  INT           NOT NULL,
    produk_id     INT           NOT NULL,
    nama_produk   VARCHAR(100)  NOT NULL,
    harga_satuan  DECIMAL(18,2) NOT NULL DEFAULT 0,
    qty           INT           NOT NULL DEFAULT 1,
    diskon_item   DECIMAL(18,2) NOT NULL DEFAULT 0,
    subtotal      DECIMAL(18,2) NOT NULL DEFAULT 0,

    CONSTRAINT FK_detail_transaksi
        FOREIGN KEY (transaksi_id) REFERENCES transaksi(id),
    CONSTRAINT FK_detail_produk
        FOREIGN KEY (produk_id) REFERENCES produk(id)
);
GO

CREATE INDEX idx_transaksi_tanggal  ON transaksi(tanggal);
CREATE INDEX idx_transaksi_status   ON transaksi(status);
CREATE INDEX idx_detail_trx_id      ON detail_transaksi(transaksi_id);
CREATE INDEX idx_produk_aktif       ON produk(aktif);
GO

CREATE PROCEDURE sp_BuatTransaksi
    @nama_customer   VARCHAR(100),
    @kasir_id        INT,
    @metode_bayar    VARCHAR(30),
    @diskon          DECIMAL(18,2),
    @dibayar         DECIMAL(18,2),
    @status          VARCHAR(30),
    @catatan         VARCHAR(255),
    @items_json      NVARCHAR(MAX),
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
        SELECT @subtotal = ISNULL(SUM(CAST(JSON_VALUE(j.value, '$.subtotal') AS DECIMAL(18,2))), 0)
        FROM OPENJSON(@items_json) AS j;

        DECLARE @total DECIMAL(18,2)    = @subtotal - @diskon;
        DECLARE @kembalian DECIMAL(18,2) = @dibayar - @total;

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
            CAST(JSON_VALUE(j.value, '$.produkId')   AS INT),
            JSON_VALUE(j.value, '$.namaProduk'),
            CAST(JSON_VALUE(j.value, '$.harga')      AS DECIMAL(18,2)),
            CAST(JSON_VALUE(j.value, '$.qty')        AS INT),
            CAST(JSON_VALUE(j.value, '$.diskonItem') AS DECIMAL(18,2)),
            CAST(JSON_VALUE(j.value, '$.subtotal')   AS DECIMAL(18,2))
        FROM OPENJSON(@items_json) AS j;

        UPDATE pr
        SET pr.stok = pr.stok - CAST(JSON_VALUE(j.value, '$.qty') AS INT)
        FROM produk pr
        JOIN OPENJSON(@items_json) AS j
            ON pr.id = CAST(JSON_VALUE(j.value, '$.produkId') AS INT);

        COMMIT;

    END TRY
    BEGIN CATCH
        ROLLBACK;
        THROW; 
    END CATCH
END;
GO

INSERT INTO kategori_produk (nama) VALUES
('Risol'),
('Minuman'),
('Snack');
GO

INSERT INTO produk (kode, nama, kategori_id, harga, stok, satuan, deskripsi) VALUES
('RSL001', 'Choco Cheese',   1, 7000, 100, 'pcs', 'Risol isi coklat & keju'),
('RSL002', 'Mayo Signature', 1, 6000, 100, 'pcs', 'Risol isi mayonaise spesial'),
('RSL003', 'Vanilla Cream',  1, 6500, 100, 'pcs', 'Risol isi vanilla krim'),
('RSL004', 'Original',       1, 5000, 100, 'pcs', 'Risol original klasik'),
('RSL005', 'Spicy Chicken',  1, 7500,  80, 'pcs', 'Risol isi ayam pedas'),
('MNM001', 'Es Teh Manis',   2, 3000,  50, 'cup', 'Teh manis dingin'),
('MNM002', 'Air Mineral',    2, 2000,  50, 'botol','Air mineral 600ml'),
('SNK001', 'Kentang Goreng', 3, 8000,  30, 'porsi','Kentang goreng crispy');
GO

INSERT INTO users (username, password, nama_lengkap, role) VALUES
('admin',  'admin123',  'Administrator',  'owner'),
('kasir1', 'kasir123',  'Kasir Satu',     'kasir');
GO

INSERT INTO pengaturan (kunci, nilai, keterangan) VALUES
('nama_toko',    'LARISOLE MALANG',                    'Nama toko di struk'),
('alamat_toko',  'Jl. Sigura gura SAMPING FAMILY MART','Alamat toko'),
('instagram',    'larisole.malang',                    'Akun Instagram'),
('pesan_struk',  'Terima kasih sudah berbelanja!',     'Pesan footer struk'),
('no_transaksi', '1000',                               'Nomor transaksi terakhir'),
('no_antrian',   '100',                                'Nomor antrian terakhir');
GO

SELECT 'kategori_produk' AS tabel, COUNT(*) AS jumlah FROM kategori_produk
UNION ALL
SELECT 'produk',         COUNT(*) FROM produk
UNION ALL
SELECT 'users',          COUNT(*) FROM users
UNION ALL
SELECT 'pengaturan',     COUNT(*) FROM pengaturan
UNION ALL
SELECT 'transaksi',      COUNT(*) FROM transaksi
UNION ALL
SELECT 'detail_transaksi',COUNT(*) FROM detail_transaksi;
GO