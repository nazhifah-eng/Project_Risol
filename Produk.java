import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Produk {
    private int id;
    private String kode;
    private String nama;
    private int kategoriId;
    private String kategoriNama;
    private BigDecimal harga;
    private int stok;
    private String satuan;
    private String deskripsi;
    private boolean aktif;

    public Produk() {}

    public Produk(int id, String kode, String nama, int kategoriId,
                  String kategoriNama, BigDecimal harga, int stok,
                  String satuan, boolean aktif) {
        this.id = id;
        this.kode = kode;
        this.nama = nama;
        this.kategoriId = kategoriId;
        this.kategoriNama = kategoriNama;
        this.harga = harga;
        this.stok = stok;
        this.satuan = satuan;
        this.aktif = aktif;
    }

    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }
    public String getKode()              { return kode; }
    public void setKode(String kode)     { this.kode = kode; }
    public String getNama()              { return nama; }
    public void setNama(String nama)     { this.nama = nama; }
    public int getKategoriId()           { return kategoriId; }
    public void setKategoriId(int id)    { this.kategoriId = id; }
    public String getKategoriNama()      { return kategoriNama; }
    public void setKategoriNama(String n){ this.kategoriNama = n; }
    public BigDecimal getHarga()         { return harga; }
    public void setHarga(BigDecimal h)   { this.harga = h; }
    public int getStok()                 { return stok; }
    public void setStok(int s)           { this.stok = s; }
    public String getSatuan()            { return satuan; }
    public void setSatuan(String s)      { this.satuan = s; }
    public String getDeskripsi()         { return deskripsi; }
    public void setDeskripsi(String d)   { this.deskripsi = d; }
    public boolean isAktif()             { return aktif; }
    public void setAktif(boolean a)      { this.aktif = a; }

    @Override
    public String toString() { return nama; }
}