import model.*;
import util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class ProdukDAO {
    private final Connection conn;

    public ProdukDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    /** Mengambil semua produk aktif beserta nama kategorinya */
    public List<Produk> getSemuaProduk() throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql = """
            SELECT p.*, k.nama AS kategori_nama
            FROM produk p
            LEFT JOIN kategori_produk k ON p.kategori_id = k.id
            WHERE p.aktif = 1
            ORDER BY k.nama, p.nama
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    /** Mengambil semua produk termasuk yang tidak aktif (untuk manajemen) */
    public List<Produk> getSemuaProdukAdmin() throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql = """
            SELECT p.*, k.nama AS kategori_nama
            FROM produk p
            LEFT JOIN kategori_produk k ON p.kategori_id = k.id
            ORDER BY p.aktif DESC, k.nama, p.nama
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    public List<Produk> cariProduk(String keyword) throws SQLException {
        List<Produk> list = new ArrayList<>();
        String sql = """
            SELECT p.*, k.nama AS kategori_nama
            FROM produk p
            LEFT JOIN kategori_produk k ON p.kategori_id = k.id
            WHERE p.aktif = 1 AND (p.nama LIKE ? OR p.kode LIKE ?)
            ORDER BY p.nama
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapProduk(rs));
        }
        return list;
    }

    public boolean tambahProduk(Produk p) throws SQLException {
        String sql = "INSERT INTO produk (kode,nama,kategori_id,harga,stok,satuan,deskripsi) VALUES (?,?,?,?,?,?,?)";
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

    public boolean updateProduk(Produk p) throws SQLException {
        String sql = """
            UPDATE produk SET kode=?,nama=?,kategori_id=?,harga=?,stok=?,
            satuan=?,deskripsi=?,aktif=?,updated_at=GETDATE()
            WHERE id=?
            """;
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

    public boolean hapusProduk(int id) throws SQLException {
        String sql = "UPDATE produk SET aktif = 0 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public String generateKodeProduk(String prefix) throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(kode,4,LEN(kode)) AS INT)) FROM produk WHERE kode LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            ResultSet rs = ps.executeQuery();
            int max = rs.next() ? rs.getInt(1) : 0;
            return prefix + String.format("%03d", max + 1);
        }
    }

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

class TransaksiDAO {
    private final Connection conn;

    public TransaksiDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    public Transaksi simpanTransaksi(Transaksi trx, List<ItemKeranjang> items) throws SQLException {
        StringBuilder jsonItems = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            ItemKeranjang item = items.get(i);
            if (i > 0) jsonItems.append(",");
            jsonItems.append(String.format(
                "{\"produkId\":%d,\"namaProduk\":\"%s\",\"harga\":%.2f,\"qty\":%d,\"diskonItem\":%.2f,\"subtotal\":%.2f}",
                item.getProdukId(), item.getNamaProduk().replace("\"",""),
                item.getHargaSatuan(), item.getQty(),
                item.getDiskonItem(), item.getSubtotal()
            ));
        }
        jsonItems.append("]");

        String sql = "{call sp_BuatTransaksi(?,?,?,?,?,?,?,?,?,?)}";
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, trx.getNamaCustomer());
            cs.setInt(2, trx.getKasirId());
            cs.setString(3, trx.getMetodeBayar());
            cs.setBigDecimal(4, trx.getDiskon());
            cs.setBigDecimal(5, trx.getDibayar());
            cs.setString(6, trx.getStatus());
            cs.setString(7, trx.getCatatan());
            cs.setString(8, jsonItems.toString());
            cs.registerOutParameter(9, Types.VARCHAR);   // @no_transaksi
            cs.registerOutParameter(10, Types.INTEGER);  // @no_antrian

            cs.execute();

            trx.setNoTransaksi(cs.getString(9));
            trx.setNoAntrian(cs.getInt(10));
        }
        return trx;
    }

    public List<Transaksi> getTransaksiHariIni() throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql = """
            SELECT t.*, u.nama_lengkap AS kasir_nama
            FROM transaksi t
            LEFT JOIN users u ON t.kasir_id = u.id
            WHERE CAST(t.tanggal AS DATE) = CAST(GETDATE() AS DATE)
            ORDER BY t.tanggal DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapTransaksi(rs));
        }
        return list;
    }

    public List<Transaksi> getTransaksiBerdasarkanTanggal(
            String dari, String sampai) throws SQLException {
        List<Transaksi> list = new ArrayList<>();
        String sql = """
            SELECT t.*, u.nama_lengkap AS kasir_nama
            FROM transaksi t
            LEFT JOIN users u ON t.kasir_id = u.id
            WHERE CAST(t.tanggal AS DATE) BETWEEN ? AND ?
            ORDER BY t.tanggal DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dari);
            ps.setString(2, sampai);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTransaksi(rs));
        }
        return list;
    }

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

    public boolean updateStatusTransaksi(int id, String status,
            BigDecimal dibayar, String metode) throws SQLException {
        String sql = """
            UPDATE transaksi SET status=?, dibayar=?,
            kembalian=total_bayar-?, metode_bayar=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setBigDecimal(2, dibayar);
            ps.setBigDecimal(3, dibayar);
            ps.setString(4, metode);
            ps.setInt(5, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean batalTransaksi(int id) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sqlStok = """
                UPDATE p SET p.stok = p.stok + dt.qty
                FROM produk p
                JOIN detail_transaksi dt ON dt.produk_id = p.id
                WHERE dt.transaksi_id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(sqlStok)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            String sqlStatus = "UPDATE transaksi SET status='Batal' WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlStatus)) {
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

    public BigDecimal getOmzetHariIni() throws SQLException {
        String sql = """
            SELECT ISNULL(SUM(total_bayar),0) FROM transaksi
            WHERE CAST(tanggal AS DATE) = CAST(GETDATE() AS DATE)
            AND status = 'Lunas'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    public int getJumlahTransaksiHariIni() throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM transaksi
            WHERE CAST(tanggal AS DATE) = CAST(GETDATE() AS DATE)
            AND status != 'Batal'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

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

class UserDAO {
    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    public User login(String username, String password) throws SQLException {
        String hashedPass = hashPassword(password);
        String sql = """
            SELECT * FROM users
            WHERE username=? AND password=? AND aktif=1
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedPass);
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

    public boolean tambahUser(User u, String password) throws SQLException {
        String sql = "INSERT INTO users (username,password,nama_lengkap,role) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, hashPassword(password));
            ps.setString(3, u.getNamaLengkap());
            ps.setString(4, u.getRole());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean gantiPassword(int userId, String passwordBaru) throws SQLException {
        String sql = "UPDATE users SET password=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(passwordBaru));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}

class PengaturanDAO {
    private final Connection conn;

    public PengaturanDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    public String getNilai(String kunci) throws SQLException {
        String sql = "SELECT nilai FROM pengaturan WHERE kunci = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kunci);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("nilai") : "";
        }
    }

    public boolean setNilai(String kunci, String nilai) throws SQLException {
        String sql = "UPDATE pengaturan SET nilai=? WHERE kunci=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nilai);
            ps.setString(2, kunci);
            return ps.executeUpdate() > 0;
        }
    }
}

class KategoriDAO {
    private final Connection conn;

    public KategoriDAO() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

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

    public boolean tambahKategori(String nama) throws SQLException {
        String sql = "INSERT INTO kategori_produk (nama) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            return ps.executeUpdate() > 0;
        }
    }
}