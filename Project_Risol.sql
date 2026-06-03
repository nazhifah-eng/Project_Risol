create database RisolKasir;
GO;
use RisolKasir;

CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nama_lengkap VARCHAR(100),
    role VARCHAR(20),
    aktif BIT DEFAULT 1
);

INSERT INTO users
(username,password,nama_lengkap,role)
VALUES
('admin','admin123','Administrator','ADMIN');

CREATE TABLE kategori_produk (
    id INT IDENTITY(1,1) PRIMARY KEY,
    nama_kategori VARCHAR(100) NOT NULL,
    deskripsi VARCHAR(255),
    aktif BIT DEFAULT 1
);

INSERT INTO kategori_produk
(nama_kategori,deskripsi)
VALUES
('Risol Manis','Kategori risol manis'),
('Risol Gurih','Kategori risol gurih'),
('Minuman','Minuman pendamping');

CREATE TABLE produk (
    id INT IDENTITY(1,1) PRIMARY KEY,

    kode_produk VARCHAR(20) UNIQUE,
    nama_produk VARCHAR(100) NOT NULL,

    kategori_id INT,

    harga DECIMAL(18,2) NOT NULL,
    stok INT DEFAULT 0,

    deskripsi VARCHAR(255),

    aktif BIT DEFAULT 1,

    FOREIGN KEY (kategori_id)
    REFERENCES kategori_produk(id)
);

INSERT INTO produk
(kode_produk,nama_produk,kategori_id,harga,stok,deskripsi)
VALUES
('RS001','CHOCO CHEESE',1,7000,100,'Risol coklat keju'),

('RS002','MAYO SIGNATURE',2,6000,100,'Risol mayo'),

('RS003','SMOKED BEEF',2,7000,100,'Risol smoked beef'),

('RS004','GREEN TEA',3,5000,100,'Minuman');

CREATE TABLE transaksi (
    id INT IDENTITY(1,1) PRIMARY KEY,

    no_transaksi VARCHAR(30) NOT NULL UNIQUE,

    tanggal DATETIME DEFAULT GETDATE(),

    customer VARCHAR(100),

    kasir_id INT,

    metode_bayar VARCHAR(30),

    subtotal DECIMAL(18,2),

    diskon DECIMAL(18,2),

    total DECIMAL(18,2),

    dibayar DECIMAL(18,2),

    kembalian DECIMAL(18,2),

    status_transaksi VARCHAR(20),

    FOREIGN KEY (kasir_id)
    REFERENCES users(id)
);

CREATE TABLE detail_transaksi (
    id INT IDENTITY(1,1) PRIMARY KEY,

    transaksi_id INT NOT NULL,

    produk_id INT NOT NULL,

    nama_produk VARCHAR(100),

    harga DECIMAL(18,2),

    qty INT,

    subtotal DECIMAL(18,2),

    FOREIGN KEY (transaksi_id)
    REFERENCES transaksi(id),

    FOREIGN KEY (produk_id)
    REFERENCES produk(id)
);

CREATE TABLE keranjang (
    id INT IDENTITY(1,1) PRIMARY KEY,

    produk_id INT,

    qty INT,

    subtotal DECIMAL(18,2),

    FOREIGN KEY (produk_id)
    REFERENCES produk(id)
);

INSERT INTO produk
(kode_produk,nama_produk,kategori_id,harga,stok,deskripsi)
VALUES
('RS005','Risol Ayam',2,8000,100,'Risol isi ayam');

SELECT
p.id,
p.kode_produk,
p.nama_produk,
k.nama_kategori,
p.harga,
p.stok
FROM produk p
LEFT JOIN kategori_produk k
ON p.kategori_id = k.id;