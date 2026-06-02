import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdukDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KategoriProduk> getAllKategori() {
        List<KategoriProduk> list = new ArrayList<>();
        String sql = "SELECT id, nama, aktif FROM KategoriProduk WHERE aktif = 1 ORDER BY nama";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KategoriProduk k = new KategoriProduk(rs.getInt("id"), rs.getString("nama"));
                k.setAktif(rs.getBoolean("aktif"));
                list.add(k);
            }
        } catch (SQLException e) {
            System.err.println("getAllKategori error: " + e.getMessage());
        }
        return list;
    }

    public boolean tambahKategori(String nama) {
        String sql = "INSERT INTO KategoriProduk (nama, aktif) VALUES (?, 1)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, nama);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("tambahKategori error: " + e.getMessage());
            return false;
        }
    }

    public boolean hapusKategori(int id) {
        String sql = "UPDATE KategoriProduk SET aktif = 0 WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("hapusKategori error: " + e.getMessage());
            return false;
        }
    }


    public List<Produk> getAllProduk() {
        List<Produk> list = new ArrayList<>();
        String sql = "SELECT p.id, p.kode, p.nama, p.kategori_id, k.nama AS kategori_nama, " +
                     "p.harga, p.stok, p.satuan, p.deskripsi, p.aktif " +
                     "FROM Produk p LEFT JOIN KategoriProduk k ON p.kategori_id = k.id " +
                     "WHERE p.aktif = 1 ORDER BY p.nama";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("getAllProduk error: " + e.getMessage());
        }
        return list;
    }

    public List<Produk> cariProduk(String keyword) {
        List<Produk> list = new ArrayList<>();
        String sql = "SELECT p.id, p.kode, p.nama, p.kategori_id, k.nama AS kategori_nama, " +
                     "p.harga, p.stok, p.satuan, p.deskripsi, p.aktif " +
                     "FROM Produk p LEFT JOIN KategoriProduk k ON p.kategori_id = k.id " +
                     "WHERE p.aktif = 1 AND (p.nama LIKE ? OR p.kode LIKE ?) ORDER BY p.nama";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("cariProduk error: " + e.getMessage());
        }
        return list;
    }

    public Produk getProdukById(int id) {
        String sql = "SELECT p.id, p.kode, p.nama, p.kategori_id, k.nama AS kategori_nama, " +
                     "p.harga, p.stok, p.satuan, p.deskripsi, p.aktif " +
                     "FROM Produk p LEFT JOIN KategoriProduk k ON p.kategori_id = k.id " +
                     "WHERE p.id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("getProdukById error: " + e.getMessage());
        }
        return null;
    }

    public boolean tambahProduk(Produk p) {
        String sql = "INSERT INTO Produk (kode, nama, kategori_id, harga, stok, satuan, deskripsi, aktif) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getKode());
            ps.setString(2, p.getNama());
            ps.setInt(3, p.getKategoriId());
            ps.setBigDecimal(4, p.getHarga());
            ps.setInt(5, p.getStok());
            ps.setString(6, p.getSatuan());
            ps.setString(7, p.getDeskripsi());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("tambahProduk error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateProduk(Produk p) {
        String sql = "UPDATE Produk SET kode=?, nama=?, kategori_id=?, harga=?, " +
                     "stok=?, satuan=?, deskripsi=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getKode());
            ps.setString(2, p.getNama());
            ps.setInt(3, p.getKategoriId());
            ps.setBigDecimal(4, p.getHarga());
            ps.setInt(5, p.getStok());
            ps.setString(6, p.getSatuan());
            ps.setString(7, p.getDeskripsi());
            ps.setInt(8, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateProduk error: " + e.getMessage());
            return false;
        }
    }

    public boolean hapusProduk(int id) {
        String sql = "UPDATE Produk SET aktif = 0 WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("hapusProduk error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStok(int produkId, int jumlah) {
        String sql = "UPDATE Produk SET stok = stok + ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, jumlah);
            ps.setInt(2, produkId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updateStok error: " + e.getMessage());
            return false;
        }
    }

    private Produk mapRow(ResultSet rs) throws SQLException {
        Produk p = new Produk();
        p.setId(rs.getInt("id"));
        p.setKode(rs.getString("kode"));
        p.setNama(rs.getString("nama"));
        p.setKategoriId(rs.getInt("kategori_id"));
        p.setKategoriNama(rs.getString("kategori_nama"));
        p.setHarga(rs.getBigDecimal("harga"));
        p.setStok(rs.getInt("stok"));
        p.setSatuan(rs.getString("satuan"));
        p.setDeskripsi(rs.getString("deskripsi"));
        p.setAktif(rs.getBoolean("aktif"));
        return p;
    }
}