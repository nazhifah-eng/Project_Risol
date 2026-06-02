import java.sql.*;
import java.util.Properties;
import java.io.*;

/**
 * DatabaseConnection — Singleton koneksi SQL Server.
 *
 * FUNGSI:
 *  - getInstance()     : ambil satu-satunya objek koneksi (Singleton)
 *  - getConnection()   : ambil objek Connection aktif; auto-reconnect jika putus
 *  - closeConnection() : tutup koneksi saat aplikasi ditutup
 *  - getHost()         : baca nama host untuk info di panel Pengaturan
 *  - getDatabase()     : baca nama database untuk info di panel Pengaturan
 *
 * KONFIGURASI:
 *  Buat file "db.properties" di folder yang sama dengan .jar untuk
 *  mengganti host/port/database/username/password tanpa compile ulang.
 *  Format:
 *      db.host=localhost
 *      db.port=1433
 *      db.database=LarisoleDB
 *      db.username=sa
 *      db.password=YourPassword123
 *
 *  Jika file tidak ada, nilai default di bawah yang dipakai.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    // Nilai default — ubah sesuai instalasi SQL Server Anda
    private String host     = "localhost";
    private String port     = "1433";
    private String database = "LarisoleDB";
    private String username = "sa";
    private String password = "YourPassword123";

    // Konstruktor private: hanya bisa dibuat lewat getInstance()
    private DatabaseConnection() {
        loadProperties();
        connect();
    }

    /**
     * Membaca konfigurasi dari file db.properties (jika ada).
     * Ini memudahkan penggantian host/password tanpa ubah kode.
     */
    private void loadProperties() {
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(in);
                host     = props.getProperty("db.host",     host);
                port     = props.getProperty("db.port",     port);
                database = props.getProperty("db.database", database);
                username = props.getProperty("db.username", username);
                password = props.getProperty("db.password", password);
            } catch (IOException e) {
                System.err.println("[DB] Gagal membaca db.properties: " + e.getMessage());
            }
        }
    }

    /**
     * Membuka koneksi ke SQL Server menggunakan JDBC.
     * Driver: mssql-jdbc (mssql-jdbc-xx.jar harus ada di classpath).
     */
    private void connect() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true",
                host, port, database
            );
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("[DB] Koneksi ke " + database + " berhasil!");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver tidak ditemukan. Pastikan mssql-jdbc.jar ada di classpath.");
            throw new RuntimeException("Driver SQL Server tidak ditemukan.", e);
        } catch (SQLException e) {
            System.err.println("[DB] Gagal konek: " + e.getMessage());
            throw new RuntimeException("Koneksi database gagal: " + e.getMessage(), e);
        }
    }

    /**
     * Mengembalikan satu-satunya instance DatabaseConnection (Singleton).
     * Thread-safe dengan keyword synchronized.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Mengembalikan Connection yang aktif.
     * Jika sudah tutup / null, akan reconnect otomatis.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Koneksi terputus, mencoba reconnect...");
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    /**
     * Menutup koneksi database secara bersih.
     * Dipanggil saat user logout atau aplikasi ditutup.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Koneksi ditutup.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error saat menutup koneksi: " + e.getMessage());
        }
    }

    /** Nama host server (ditampilkan di panel Pengaturan > Database) */
    public String getHost()     { return host; }

    /** Nama database aktif (ditampilkan di panel Pengaturan > Database) */
    public String getDatabase() { return database; }
}