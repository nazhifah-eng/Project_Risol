import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// ============================================================
//  ProdukDAO — CRUD produk ke tabel produk & kategori_produk
//
//  FUNGSI UTAMA:
//   getSemuaProduk()      : produk aktif saja (untuk ComboBox transaksi)
//   getSemuaProdukAdmin() : semua produk termasuk nonaktif (untuk panel Produk)
//   cariProduk(keyword)   : filter by nama/kode (live search)
//   tambahProduk(p)       : INSERT produk baru
//   updateProduk(p)       : UPDATE data produk
//   hapusProduk(id)       : soft-delete (aktif = 0)
//   generateKodeProduk()  : auto-generate kode unik (RSL001, dst)
// ============================================================
class ProdukDAO {
    private final Connection conn;

    public ProdukDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /** Mengambil semua produk aktif beserta nama kategorinya (JOIN) */
    public List<Produk> getSemuaProduk() throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql =
            "SELECT p.*, k.nama AS kategori_nama " +
            "FROM produk p " +
            "LEFT JOIN kategori_produk k ON p.kategori_id = k.id " +
            "WHERE p.aktif = 1 " +
            "ORDER BY k.nama, p.nama";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    /**
     * Mengambil SEMUA produk (aktif + nonaktif) untuk panel manajemen.
     * Produk aktif ditampilkan lebih dulu (ORDER BY p.aktif DESC).
     */
    public List<Produk> getSemuaProdukAdmin() throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql =
            "SELECT p.*, k.nama AS kategori_nama " +
            "FROM produk p " +
            "LEFT JOIN kategori_produk k ON p.kategori_id = k.id " +
            "ORDER BY p.aktif DESC, k.nama, p.nama";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    /** Mencari produk berdasarkan kata kunci nama atau kode (LIKE) */
    public List<Produk> cariProduk(String keyword) throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql =
            "SELECT p.*, k.nama AS kategori_nama " +
            "FROM produk p " +
            "LEFT JOIN kategori_produk k ON p.kategori_id = k.id " +
            "WHERE p.aktif = 1 AND (p.nama LIKE ? OR p.kode LIKE ?) " +
            "ORDER BY p.nama";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    /** Menyimpan produk baru ke database (INSERT) */
    public boolean tambahProduk(Produk p) throws SQLException {
        String sql =
            "INSERT INTO produk (kode, nama, kategori_id, harga, stok, satuan, deskripsi) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getKode());
            ps.setString(2, p.getNama());
            ps.setInt(3, p.getKategoriId());
            ps.setBigDecimal(4, p.getHarga());
            ps.setInt(5, p.getStok());
            ps.setString(6, p.getSatuan());
            ps.setString(7, p.getDeskripsi());
            return ps.executeUpdate() > 0;
        }
    }

    /** Memperbarui data produk di database (UPDATE) */
    public boolean updateProduk(Produk p) throws SQLException {
        String sql =
            "UPDATE produk SET kode=?, nama=?, kategori_id=?, harga=?, stok=?, " +
            "satuan=?, deskripsi=?, aktif=?, updated_at=GETDATE() " +
            "WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getKode());
            ps.setString(2, p.getNama());
            ps.setInt(3, p.getKategoriId());
            ps.setBigDecimal(4, p.getHarga());
            ps.setInt(5, p.getStok());
            ps.setString(6, p.getSatuan());
            ps.setString(7, p.getDeskripsi());
            ps.setBoolean(8, p.isAktif());
            ps.setInt(9, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Soft-delete produk: set aktif = 0, produk tidak benar-benar dihapus
     * agar riwayat transaksi lama tetap valid.
     */
    public boolean hapusProduk(int id) throws SQLException {
        String sql = "UPDATE produk SET aktif = 0 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Membuat kode produk berikutnya secara otomatis.
     * Contoh: prefix "RSL" → "RSL004" jika max saat ini RSL003.
     */
    public String generateKodeProduk(String prefix) throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(kode, ?, LEN(kode)) AS INT)) " +
                     "FROM produk WHERE kode LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, prefix.length() + 1);
            ps.setString(2, prefix + "%");
            ResultSet rs = ps.executeQuery();
            int max = rs.next() ? rs.getInt(1) : 0;
            return prefix + String.format("%03d", max + 1);
        }
    }

    /** Mengubah ResultSet menjadi objek Produk */
    private Produk mapProduk(ResultSet rs) throws SQLException {
        return new Produk(
            rs.getInt("id"),
            rs.getString("kode"),
            rs.getString("nama"),
            rs.getInt("kategori_id"),
            rs.getString("kategori_nama"),
            rs.getBigDecimal("harga"),
            rs.getInt("stok"),
            rs.getString("satuan"),
            rs.getBoolean("aktif")
        );
    }
}

// ============================================================
//  TransaksiDAO — Simpan dan baca transaksi penjualan
//
//  FUNGSI UTAMA:
//   simpanTransaksi()              : panggil SP sp_BuatTransaksi
//   getTransaksiHariIni()          : riwayat hari ini untuk tabel bawah
//   getTransaksiBerdasarkanTanggal(): filter laporan by range tanggal
//   getDetailTransaksi(id)         : detail item 1 transaksi
//   updateStatusTransaksi()        : lunasi transaksi Pending
//   batalTransaksi(id)             : batal + kembalikan stok (transaction)
//   getOmzetHariIni()              : total omzet hari ini (header)
//   getJumlahTransaksiHariIni()    : jumlah transaksi hari ini (header)
// ============================================================
class TransaksiDAO {
    private final Connection conn;

    public TransaksiDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Menyimpan transaksi baru lewat stored procedure sp_BuatTransaksi.
     * Item keranjang dikirim sebagai JSON string ke SP.
     * SP yang mengurus: increment nomor, insert header, insert detail, update stok.
     * Setelah SP selesai, no_transaksi & no_antrian dikembalikan ke objek trx.
     */
    public Transaksi simpanTransaksi(Transaksi trx, List<ItemKeranjang> items) throws SQLException {
        // Bangun JSON dari keranjang
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            ItemKeranjang item = items.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"produkId\":%d,\"namaProduk\":\"%s\",\"harga\":%.2f," +
                "\"qty\":%d,\"diskonItem\":%.2f,\"subtotal\":%.2f}",
                item.getProdukId(),
                item.getNamaProduk().replace("\"", ""),
                item.getHargaSatuan(),
                item.getQty(),
                item.getDiskonItem(),
                item.getSubtotal()
            ));
        }
        json.append("]");

        // Panggil stored procedure
        String sql = "{call sp_BuatTransaksi(?,?,?,?,?,?,?,?,?,?)}";
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, trx.getNamaCustomer());
            cs.setInt(2,    trx.getKasirId());
            cs.setString(3, trx.getMetodeBayar());
            cs.setBigDecimal(4, trx.getDiskon());
            cs.setBigDecimal(5, trx.getDibayar());
            cs.setString(6, trx.getStatus());
            cs.setString(7, trx.getCatatan());
            cs.setString(8, json.toString());
            cs.registerOutParameter(9,  Types.VARCHAR); // @out_no_transaksi
            cs.registerOutParameter(10, Types.INTEGER); // @out_no_antrian
            cs.execute();
            trx.setNoTransaksi(cs.getString(9));
            trx.setNoAntrian(cs.getInt(10));
        }
        return trx;
    }

    /** Mengambil semua transaksi hari ini, diurutkan terbaru di atas */
    public List<Transaksi> getTransaksiHariIni() throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql =
            "SELECT t.*, u.nama_lengkap AS kasir_nama " +
            "FROM transaksi t " +
            "LEFT JOIN users u ON t.kasir_id = u.id " +
            "WHERE CAST(t.tanggal AS DATE) = CAST(GETDATE() AS DATE) " +
            "ORDER BY t.tanggal DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapTransaksi(rs));
        }
        return list;
    }

    /** Mengambil transaksi dalam rentang tanggal (format: yyyy-MM-dd) */
    public List<Transaksi> getTransaksiBerdasarkanTanggal(
            String dari, String sampai) throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql =
            "SELECT t.*, u.nama_lengkap AS kasir_nama " +
            "FROM transaksi t " +
            "LEFT JOIN users u ON t.kasir_id = u.id " +
            "WHERE CAST(t.tanggal AS DATE) BETWEEN ? AND ? " +
            "ORDER BY t.tanggal DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dari);
            ps.setString(2, sampai);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTransaksi(rs));
        }
        return list;
    }

    /** Mengambil list item dari satu transaksi (untuk dialog detail & struk) */
    public List<DetailTransaksi> getDetailTransaksi(int transaksiId) throws SQLException {
        List<DetailTransaksi> list = new ArrayList<>();
        String sql = "SELECT * FROM detail_transaksi WHERE transaksi_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaksiId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetailTransaksi dt = new DetailTransaksi();
                dt.setId(rs.getInt("id"));
                dt.setTransaksiId(rs.getInt("transaksi_id"));
                dt.setProdukId(rs.getInt("produk_id"));
                dt.setNamaProduk(rs.getString("nama_produk"));
                dt.setHargaSatuan(rs.getBigDecimal("harga_satuan"));
                dt.setQty(rs.getInt("qty"));
                dt.setDiskonItem(rs.getBigDecimal("diskon_item"));
                dt.setSubtotal(rs.getBigDecimal("subtotal"));
                list.add(dt);
            }
        }
        return list;
    }

    /**
     * Mengubah status transaksi menjadi 'Lunas'.
     * Dipakai untuk melunasi transaksi yang statusnya 'Pending'.
     */
    public boolean updateStatusTransaksi(int id, String status,
            BigDecimal dibayar, String metode) throws SQLException {
        String sql =
            "UPDATE transaksi SET status=?, dibayar=?, " +
            "kembalian=dibayar-total_bayar, metode_bayar=? " +
            "WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setBigDecimal(2, dibayar);
            ps.setString(3, metode);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Membatalkan transaksi dan mengembalikan stok produk.
     * Menggunakan SQL Transaction agar atomik (all-or-nothing).
     */
    public boolean batalTransaksi(int id) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // Kembalikan stok produk sesuai qty di detail
            String sqlStok =
                "UPDATE pr SET pr.stok = pr.stok + dt.qty " +
                "FROM produk pr " +
                "JOIN detail_transaksi dt ON dt.produk_id = pr.id " +
                "WHERE dt.transaksi_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlStok)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            // Update status transaksi menjadi 'Batal'
            String sqlBatal = "UPDATE transaksi SET status='Batal' WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlBatal)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Total omzet hari ini (status Lunas) — ditampilkan di header aplikasi */
    public BigDecimal getOmzetHariIni() throws SQLException {
        String sql =
            "SELECT ISNULL(SUM(total_bayar), 0) FROM transaksi " +
            "WHERE CAST(tanggal AS DATE) = CAST(GETDATE() AS DATE) " +
            "AND status = 'Lunas'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    /** Jumlah transaksi hari ini (kecuali Batal) — ditampilkan di header */
    public int getJumlahTransaksiHariIni() throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM transaksi " +
            "WHERE CAST(tanggal AS DATE) = CAST(GETDATE() AS DATE) " +
            "AND status != 'Batal'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Mengubah ResultSet menjadi objek Transaksi */
    private Transaksi mapTransaksi(ResultSet rs) throws SQLException {
        Transaksi t = new Transaksi();
        t.setId(rs.getInt("id"));
        t.setNoTransaksi(rs.getString("no_transaksi"));
        t.setNoAntrian(rs.getInt("no_antrian"));
        Timestamp ts = rs.getTimestamp("tanggal");
        if (ts != null) t.setTanggal(ts.toLocalDateTime());
        t.setKasirId(rs.getInt("kasir_id"));
        t.setKasirNama(rs.getString("kasir_nama"));
        t.setNamaCustomer(rs.getString("nama_customer"));
        t.setSubtotal(rs.getBigDecimal("subtotal"));
        t.setDiskon(rs.getBigDecimal("diskon"));
        t.setTotalBayar(rs.getBigDecimal("total_bayar"));
        t.setDibayar(rs.getBigDecimal("dibayar"));
        t.setKembalian(rs.getBigDecimal("kembalian"));
        t.setMetodeBayar(rs.getString("metode_bayar"));
        t.setStatus(rs.getString("status"));
        t.setCatatan(rs.getString("catatan"));
        return t;
    }
}

// ============================================================
//  UserDAO — Autentikasi dan manajemen akun pengguna
//
//  FUNGSI UTAMA:
//   login(username, password)  : verifikasi login, return User atau null
//   getSemuaUser()             : daftar semua akun (panel Pengaturan)
//   tambahUser(u, password)    : buat akun baru dengan hash password
//   gantiPassword(id, pass)    : ubah password, disimpan sebagai SHA-256
// ============================================================
class UserDAO {
    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Memverifikasi login.
     * Password di-hash SHA-256 sebelum dibandingkan dengan database.
     * Return: objek User jika sukses, null jika gagal.
     *
     * CATATAN: Data awal di SQL menyimpan password plain text.
     * Setelah login pertama, gunakan menu Ganti Password agar
     * password tersimpan sebagai hash yang aman.
     */
    public User login(String username, String password) throws SQLException {
        // Coba cocokkan plain text dulu (untuk akun awal)
        String sql =
            "SELECT * FROM users WHERE username=? AND aktif=1 " +
            "AND (password=? OR password=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);              // plain text
            ps.setString(3, hashPassword(password)); // SHA-256
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setNamaLengkap(rs.getString("nama_lengkap"));
                u.setRole(rs.getString("role"));
                u.setAktif(rs.getBoolean("aktif"));
                return u;
            }
        }
        return null;
    }

    /** Mengambil semua user untuk ditampilkan di panel Kelola Akun */
    public List<User> getSemuaUser() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role, nama_lengkap";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setNamaLengkap(rs.getString("nama_lengkap"));
                u.setRole(rs.getString("role"));
                u.setAktif(rs.getBoolean("aktif"));
                list.add(u);
            }
        }
        return list;
    }

    /** Menambah user baru. Password otomatis di-hash SHA-256. */
    public boolean tambahUser(User u, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password, nama_lengkap, role) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, hashPassword(password));
            ps.setString(3, u.getNamaLengkap());
            ps.setString(4, u.getRole());
            return ps.executeUpdate() > 0;
        }
    }

    /** Mengganti password dan menyimpannya sebagai hash SHA-256. */
    public boolean gantiPassword(int userId, String passwordBaru) throws SQLException {
        String sql = "UPDATE users SET password=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(passwordBaru));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Hashing password menggunakan SHA-256.
     * Hasil: string hex 64 karakter.
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password; // fallback jika algo tidak tersedia
        }
    }
}

// ============================================================
//  PengaturanDAO — Baca dan simpan konfigurasi toko
//
//  FUNGSI UTAMA:
//   getNilai(kunci)        : ambil satu nilai dari tabel pengaturan
//   setNilai(kunci, nilai) : update nilai di tabel pengaturan
// ============================================================
class PengaturanDAO {
    private final Connection conn;

    public PengaturanDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Mengambil nilai pengaturan berdasarkan kunci.
     * Contoh: getNilai("nama_toko") → "LARISOLE MALANG"
     */
    public String getNilai(String kunci) throws SQLException {
        String sql = "SELECT nilai FROM pengaturan WHERE kunci = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kunci);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("nilai") : "";
        }
    }

    /**
     * Memperbarui nilai pengaturan berdasarkan kunci.
     * Jika kunci tidak ada, tidak ada efek (tidak insert baru).
     */
    public boolean setNilai(String kunci, String nilai) throws SQLException {
        String sql = "UPDATE pengaturan SET nilai=? WHERE kunci=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nilai);
            ps.setString(2, kunci);
            return ps.executeUpdate() > 0;
        }
    }
}

// ============================================================
//  KategoriDAO — Baca dan tambah kategori produk
// ============================================================
class KategoriDAO {
    private final Connection conn;

    public KategoriDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /** Mengambil semua kategori aktif untuk ComboBox di form produk */
    public List<KategoriProduk> getSemuaKategori() throws SQLException {
        List<KategoriProduk> list = new ArrayList<>();
        String sql = "SELECT * FROM kategori_produk WHERE aktif=1 ORDER BY nama";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new KategoriProduk(rs.getInt("id"), rs.getString("nama")));
        }
        return list;
    }

    /** Menambah kategori baru */
    public boolean tambahKategori(String nama) throws SQLException {
        String sql = "INSERT INTO kategori_produk (nama) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            return ps.executeUpdate() > 0;
        }
    }
}