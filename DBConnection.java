import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class DBConnection {
    private static Connection conn = null;

    private static final String HOST = "localhost";
    private static final String PORT = "1433";
    private static final String DB   = "kasir_larisole";
    private static final String USER = "sa";
    private static final String PASS = "123";

    private DBConnection() {}

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

    public static Connection getConnection() {
        return conn;
    }

    public static void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception ignored) {}
        conn = null;
    }

    public static boolean isConnected() {
        try {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}