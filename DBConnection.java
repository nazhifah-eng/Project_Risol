import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

/**
 * DBConnection.java
 * Singleton helper — satu koneksi dipakai semua form.
 *
 * Larisole Malang — Sistem Kasir
 */
public class DBConnection {

    private static Connection conn = null;

    // ── Sesuaikan tiga baris ini ──────────────────────────────
    private static final String HOST = "localhost";
    private static final String PORT = "1433";
    private static final String DB   = "kasir_larisole";
    private static final String USER = "sa";
    private static final String PASS = "123";
    // ─────────────────────────────────────────────────────────

    private DBConnection() {}

    /**
     * Buka koneksi baru dan simpan sebagai singleton.
     * Panggil dari tombol "Koneksi".
     */
    public static Connection connect() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            String url = "jdbc:sqlserver://" + HOST + ":" + PORT + ";"
                    + "databaseName=" + DB + ";"
                    + "encrypt=true;"
                    + "trustServerCertificate=true;";

            conn = DriverManager.getConnection(url, USER, PASS);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Koneksi gagal:\n" + e.getMessage(),
                    "Error Koneksi", JOptionPane.ERROR_MESSAGE);
            conn = null;
        }
        return conn;
    }

    /** Ambil koneksi yang sudah dibuka. */
    public static Connection getConnection() {
        return conn;
    }

    /** Tutup koneksi. */
    public static void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception ignored) {}
        conn = null;
    }

    /** Cek apakah sudah terkoneksi. */
    public static boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}