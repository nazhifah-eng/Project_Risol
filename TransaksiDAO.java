import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class TransaksiDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean simpanTransaksi(Transaksi t, List<ItemKeranjang> keranjang) {
        String sqlT = "INSERT INTO transaksi " +
                      "(no_transaksi, tanggal, customer, kasir_id, metode_bayar, " +
                      " subtotal, diskon, total, dibayar, kembalian, status_transaksi) " +
                      "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            getConn().setAutoCommit(false);

            int transaksiId;
            try (PreparedStatement ps = getConn().prepareStatement(
                    sqlT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, t.getNoTransaksi());
                ps.setString(2, t.getNamaCustomer());
                ps.setInt(3, t.getKasirId());
                ps.setString(4, t.getMetodeBayar());
                ps.setBigDecimal(5, t.getSubtotal());
                ps.setBigDecimal(6, t.getDiskon() != null ? t.getDiskon() : BigDecimal.ZERO);
                ps.setBigDecimal(7, t.getTotalBayar());
                ps.setBigDecimal(8, t.getDibayar());
                ps.setBigDecimal(9, t.getKembalian());
                ps.setString(10, "lunas");
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                if (!keys.next()) throw new SQLException("Gagal mendapatkan ID transaksi");
                transaksiId = keys.getInt(1);
            }

            String sqlD = "INSERT INTO DetailTransaksi " +
                          "(transaksi_id, produk_id, nama_produk, harga, qty, subtotal) " +
                          "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pd = getConn().prepareStatement(sqlD)) {
                for (ItemKeranjang item : keranjang) {
                    pd.setInt(1, transaksiId);
                    pd.setInt(2, item.getProdukId());
                    pd.setString(3, item.getNamaProduk());
                    pd.setBigDecimal(4, item.getHargaSatuan());
                    pd.setInt(5, item.getQty());
                    pd.setBigDecimal(6, item.getSubtotal());
                    pd.addBatch();
                }
                pd.executeBatch();
            }

            ProdukDAO produkDAO = new ProdukDAO();
            for (ItemKeranjang item : keranjang) {
                produkDAO.updateStok(item.getProdukId(), -item.getQty());
            }

            getConn().commit();
            return true;

        } catch (SQLException e) {
            try { getConn().rollback(); } catch (SQLException ignored) {}
            System.err.println("simpanTransaksi error: " + e.getMessage());
            javax.swing.SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                    "SQL Error: " + e.getMessage(), "Debug",
                    JOptionPane.ERROR_MESSAGE));
            return false;
        } finally {
            try { getConn().setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public List<Transaksi> getAllTransaksi() {
        List<Transaksi> list = new ArrayList<>();
        String sql = "SELECT id, no_transaksi, tanggal, customer, kasir_id, " +
                     "metode_bayar, subtotal, diskon, total, dibayar, kembalian, status_transaksi " +
                     "FROM transaksi ORDER BY tanggal DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("getAllTransaksi error: " + e.getMessage());
        }
        return list;
    }

    public List<DetailTransaksi> getDetailByTransaksiId(int transaksiId) {
        List<DetailTransaksi> list = new ArrayList<>();
        String sql = "SELECT id, transaksi_id, produk_id, nama_produk, harga, qty, subtotal " +
                     "FROM DetailTransaksi WHERE transaksi_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, transaksiId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetailTransaksi d = new DetailTransaksi();
                    d.setId(rs.getInt("id"));
                    d.setTransaksiId(rs.getInt("transaksi_id"));
                    d.setProdukId(rs.getInt("produk_id"));
                    d.setNamaProduk(rs.getString("nama_produk"));
                    d.setHargaSatuan(rs.getBigDecimal("harga"));
                    d.setQty(rs.getInt("qty"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    list.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println("getDetailByTransaksiId error: " + e.getMessage());
        }
        return list;
    }

    private Transaksi mapRow(ResultSet rs) throws SQLException {
        Transaksi t = new Transaksi();
        t.setId(rs.getInt("id"));
        t.setNoTransaksi(rs.getString("no_transaksi"));
        Timestamp ts = rs.getTimestamp("tanggal");
        if (ts != null) t.setTanggal(ts.toLocalDateTime());
        t.setNamaCustomer(rs.getString("customer"));
        t.setKasirId(rs.getInt("kasir_id"));
        t.setMetodeBayar(rs.getString("metode_bayar"));
        t.setSubtotal(rs.getBigDecimal("subtotal"));
        t.setDiskon(rs.getBigDecimal("diskon"));
        t.setTotalBayar(rs.getBigDecimal("total"));
        t.setDibayar(rs.getBigDecimal("dibayar"));
        t.setKembalian(rs.getBigDecimal("kembalian"));
        t.setStatus(rs.getString("status_transaksi"));
        return t;
    }
}