import java.sql.*;
import java.util.Properties;
import java.io.*;

public class DatabaseConnection{
    private static DatabaseConnection instance;
    private Connection connection;
    private String host     = "localhost";
    private String port     = "1433";
    private String database = "LarisoleDB";
    private String username = "sa";
    private String password = "YourPassword123";

    private DatabaseConnection(){
        loadProperties();
        connect();
    }

    private void loadProperties(){
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)){
                Properties props = new Properties();
                props.load(in);
                host     = props.getProperty("db.host",     host);
                port     = props.getProperty("db.port",     port);
                database = props.getProperty("db.database", database);
                username = props.getProperty("db.username", username);
                password = props.getProperty("db.password", password);
            } catch (IOException e){
                System.err.println("Gagal membaca db.properties: " + e.getMessage());
            }
        }
    }

    private void connect(){
        try{
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = String.format(
                "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=false;trustServerCertificate=true",
                host, port, database
            );
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Koneksi database berhasil!");
        } catch (ClassNotFoundException e){
            System.err.println("Driver SQL Server tidak ditemukan: " + e.getMessage());
            throw new RuntimeException("Driver tidak ditemukan", e);
        } catch (SQLException e){
            System.err.println("Koneksi database gagal: " + e.getMessage());
            throw new RuntimeException("Koneksi gagal", e);
        }
    }

    public static synchronized DatabaseConnection getInstance(){
        if (instance == null){
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection(){
        try {
            if (connection == null || connection.isClosed()){
                System.out.println("Koneksi terputus, mencoba reconnect...");
                connect();
            }
        } catch (SQLException e){
            connect();
        }
        return connection;
    }

    public void closeConnection(){
        try {
            if (connection != null && !connection.isClosed()){
                connection.close();
                System.out.println("Koneksi database ditutup.");
            }
        } catch (SQLException e){
            System.err.println("Error saat menutup koneksi: " + e.getMessage());
        }
    }

    public String getHost(){ 
        return host; 
    }

    public String getDatabase(){ 
        return database; 
    }
}